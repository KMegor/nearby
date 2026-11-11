-- Автомат состояний (документ «Модель состояний», версия 3).
-- Каждое событие E1–E10 — отдельная функция; E11 и срок отстранения проверяются по времени
-- в момент обращения. Ошибки возвращаются коротким кодом в тексте исключения.

-- ================================================================ служебные
create or replace function public._fail(p_code text) returns void
language plpgsql as $$
begin
  raise exception '%', p_code using errcode = 'P0001';
end $$;

create or replace function public._is_banned(p_user uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select coalesce((select banned_until > now() from profiles where id = p_user), false);
$$;

-- Текущий пользователь: вошёл, заполнил профиль и не отстранён.
create or replace function public._me() returns uuid
language plpgsql stable security definer set search_path = public as $$
declare
  v_id uuid := auth.uid();
begin
  if v_id is null then perform _fail('not_authenticated'); end if;
  if not exists (select 1 from profiles where id = v_id) then perform _fail('profile_required'); end if;
  if _is_banned(v_id) then perform _fail('banned'); end if;
  return v_id;
end $$;

-- Привязка к сетке ~200 м. Клиент делает то же самое до отправки; сервер повторяет на случай
-- изменённого клиента. Операция идемпотентна.
create or replace function public._snap(p_lat double precision, p_lng double precision)
returns extensions.geography
language plpgsql immutable set search_path = public, extensions as $$
declare
  v_step constant double precision := 0.002;                    -- ~222 м по широте
  v_lat double precision := round(p_lat / v_step) * v_step;
  v_lng_step double precision := v_step / greatest(cos(radians(v_lat)), 0.01);
  v_lng double precision := round(p_lng / v_lng_step) * v_lng_step;
begin
  return st_setsrid(st_makepoint(v_lng, v_lat), 4326)::geography;
end $$;

-- Точка для показа на карте: постоянное смещение 30–100 м, зависящее от пользователя и ячейки.
-- Если смещённая точка вышла бы за круг смотрящего, отдаётся несмещённая.
create or replace function public._display_point(
  p_user uuid, p_geog extensions.geography, p_viewer extensions.geography, p_viewer_radius_km int)
returns extensions.geography
language plpgsql immutable set search_path = public, extensions as $$
declare
  v_hash text := md5(p_user::text || st_astext(p_geog::geometry));
  v_angle double precision := (('x' || substr(v_hash, 1, 6))::bit(24)::int % 360);
  v_dist  double precision := 30 + (('x' || substr(v_hash, 7, 4))::bit(16)::int % 71);
  v_point geography := st_project(p_geog, v_dist, radians(v_angle));
begin
  if st_distance(p_viewer, v_point) > p_viewer_radius_km * 1000 then
    return p_geog;
  end if;
  return v_point;
end $$;

-- Условие G1: взаимный радиус, оба видимы, местоположение обоих свежее, A <> B.
create or replace function public._g1(p_a uuid, p_b uuid) returns boolean
language sql stable security definer set search_path = public, extensions as $$
  select p_a <> p_b and exists (
    select 1
    from profiles pa
    join locations la on la.user_id = pa.id
    join profiles pb on pb.id = p_b
    join locations lb on lb.user_id = pb.id
    where pa.id = p_a
      and pa.visible and pb.visible
      and la.updated_at > now() - interval '30 minutes'
      and lb.updated_at > now() - interval '30 minutes'
      and st_dwithin(la.geog, lb.geog, least(pa.radius_km, pb.radius_km) * 1000)
  );
$$;

-- Сколько отказов получил p_requester на свои запросы в этой паре (для условия G4).
create or replace function public._rejects_of(c public.connections, p_requester uuid) returns int
language sql immutable as $$
  select case when p_requester = c.user_low then c.rejects_low else c.rejects_high end;
$$;

create or replace function public._cooldown_until(c public.connections) returns timestamptz
language sql immutable as $$
  select c.rejected_at + interval '24 hours';
$$;

-- Как связь выглядит для пользователя p_me. Заблокированному блокировка не раскрывается:
-- он видит обычный отказ либо чат, в который нельзя писать.
--   none | outgoing | incoming | chat | cooldown | blocked_by_me
create or replace function public._relation(c public.connections, p_me uuid) returns text
language sql stable as $$
  select case
    when c.id is null then 'none'
    when c.state = 'pending' then case when c.requester = p_me then 'outgoing' else 'incoming' end
    when c.state = 'active' then 'chat'
    when c.state = 'blocked' and c.blocked_by = p_me then 'blocked_by_me'
    when c.state = 'blocked' and c.accepted_at is not null then 'chat'
    when now() < public._cooldown_until(c) then 'cooldown'
    else 'none'
  end;
$$;

-- Жалоба и автоматическое отстранение: каждые 3 жалобы от разных людей — следующая ступень.
create or replace function public._complain(p_from uuid, p_to uuid, p_reason text) returns void
language plpgsql security definer set search_path = public as $$
declare
  v_count int;
  v_level int;
begin
  if p_reason is null or p_reason not in ('spam', 'abuse', 'other') then perform _fail('bad_reason'); end if;
  perform 1 from profiles where id = p_to for update;
  insert into complaints (from_user, to_user, reason) values (p_from, p_to, p_reason)
  on conflict (from_user, to_user) do nothing;
  if not found then return; end if;            -- повторная жалоба на того же человека не считается

  select count(*) into v_count from complaints where to_user = p_to;
  if v_count % 3 = 0 then
    v_level := v_count / 3;
    update profiles set
      ban_level = v_level,
      banned_until = case v_level
        when 1 then now() + interval '24 hours'
        when 2 then now() + interval '3 days'
        when 3 then now() + interval '7 days'
        else 'infinity'::timestamptz end
    where id = p_to;
  end if;
end $$;

-- ================================================================ профиль
create or replace function public.save_profile(
  p_name text, p_birth_year int, p_bio text default '', p_interest_ids smallint[] default '{}')
returns public.profiles
language plpgsql security definer set search_path = public as $$
declare
  v_id uuid := auth.uid();
  v_row profiles;
begin
  if v_id is null then perform _fail('not_authenticated'); end if;
  if _is_banned(v_id) then perform _fail('banned'); end if;
  if p_birth_year > extract(year from now())::int - 18 then perform _fail('age_restricted'); end if;
  if coalesce(cardinality(p_interest_ids), 0) > 5 then perform _fail('too_many_interests'); end if;

  insert into profiles (id, name, birth_year, bio)
  values (v_id, btrim(p_name), p_birth_year, coalesce(btrim(p_bio), ''))
  on conflict (id) do update set name = excluded.name, birth_year = excluded.birth_year, bio = excluded.bio
  returning * into v_row;

  delete from profile_interests where profile_id = v_id;
  insert into profile_interests (profile_id, interest_id)
  select distinct v_id, i from unnest(coalesce(p_interest_ids, '{}')) as i;
  return v_row;
end $$;

create or replace function public.set_avatar(p_path text) returns void
language plpgsql security definer set search_path = public as $$
begin
  update profiles set avatar_path = p_path where id = _me();
end $$;

-- ================================================================ режим: E1, E2, E3
create or replace function public.set_visibility(p_visible boolean) returns boolean
language plpgsql security definer set search_path = public as $$
begin
  update profiles set visible = p_visible where id = _me();
  return p_visible;
end $$;

create or replace function public.set_radius(p_km int) returns int
language plpgsql security definer set search_path = public as $$
begin
  if p_km is null or p_km not in (1, 3, 5, 10) then perform _fail('bad_radius'); end if;
  update profiles set radius_km = p_km where id = _me();
  return p_km;
end $$;

create or replace function public.update_location(p_lat double precision, p_lng double precision) returns void
language plpgsql security definer set search_path = public, extensions as $$
declare
  v_me uuid := _me();
begin
  if p_lat is null or p_lng is null or abs(p_lat) > 90 or abs(p_lng) > 180 then perform _fail('bad_location'); end if;
  insert into locations (user_id, geog, updated_at) values (v_me, _snap(p_lat, p_lng), now())
  on conflict (user_id) do update set geog = excluded.geog, updated_at = excluded.updated_at;
end $$;

-- ================================================================ поиск (S1)
create or replace function public.nearby_users()
returns table (
  user_id uuid, name text, age int, bio text, avatar_path text, interests text[],
  lat double precision, lng double precision,
  relation text, connection_id uuid, retry_at timestamptz, can_block_on_reject boolean)
language plpgsql stable security definer set search_path = public, extensions as $$
declare
  v_me uuid := _me();
  v_my profiles;
  v_geog geography;
begin
  select * into v_my from profiles where id = v_me;
  if not v_my.visible then perform _fail('not_visible'); end if;
  select l.geog into v_geog from locations l
  where l.user_id = v_me and l.updated_at > now() - interval '30 minutes';
  if v_geog is null then perform _fail('location_stale'); end if;

  return query
  select
    p.id, p.name, extract(year from now())::int - p.birth_year, p.bio, p.avatar_path,
    coalesce((select array_agg(i.name order by i.id)
              from profile_interests pi join interests i on i.id = pi.interest_id
              where pi.profile_id = p.id), '{}'),
    st_y(dp.g::geometry), st_x(dp.g::geometry),
    _relation(c, v_me),
    c.id,
    case when _relation(c, v_me) = 'cooldown' then _cooldown_until(c) end,
    coalesce(c.state = 'pending' and c.requester = p.id and _rejects_of(c, p.id) >= 1, false)
  from profiles p
  join locations l on l.user_id = p.id
  left join connections c on c.user_low = least(v_me, p.id) and c.user_high = greatest(v_me, p.id)
  cross join lateral (select _display_point(p.id, l.geog, v_geog, v_my.radius_km) as g) dp
  where p.id <> v_me
    and p.visible
    and l.updated_at > now() - interval '30 minutes'
    and st_dwithin(l.geog, v_geog, least(p.radius_km, v_my.radius_km) * 1000)
    and not coalesce(p.banned_until > now(), false)
    and coalesce(c.state <> 'blocked', true);          -- заблокированная пара не видит друг друга
end $$;

-- ================================================================ E4: отправить запрос
create or replace function public.send_request(p_to uuid) returns text
language plpgsql security definer set search_path = public as $$
declare
  v_me uuid := _me();
  c connections;
begin
  if p_to is null or p_to = v_me then perform _fail('bad_target'); end if;
  if not exists (select 1 from profiles where id = p_to) then perform _fail('bad_target'); end if;

  select * into c from connections
  where user_low = least(v_me, p_to) and user_high = greatest(v_me, p_to) for update;

  -- состояния, в которых E4 недоступно независимо от условий
  if c.state = 'active' then perform _fail('already_connected'); end if;
  if c.state = 'blocked' then perform _fail('request_unavailable'); end if;
  if c.state = 'pending' and c.requester = v_me then perform _fail('already_pending'); end if;
  if c.state = 'rejected' and now() < _cooldown_until(c) then perform _fail('cooldown'); end if;

  if _is_banned(p_to) then perform _fail('user_unavailable'); end if;      -- G2
  if not (select visible from profiles where id = v_me) then perform _fail('not_visible'); end if;  -- G3
  if not _g1(v_me, p_to) then perform _fail('out_of_range'); end if;       -- G1

  if c.id is null then
    insert into connections (user_low, user_high, requester, state)
    values (least(v_me, p_to), greatest(v_me, p_to), v_me, 'pending')
    on conflict (user_low, user_high) do nothing;
    if not found then perform _fail('try_again'); end if;                  -- одновременный встречный запрос
    return 'pending';
  elsif c.state = 'pending' then
    -- встречный запрос адресата считается взаимным согласием
    update connections set state = 'active', accepted_at = now(), updated_at = now() where id = c.id;
    return 'active';
  else
    -- срок отказа истёк: запись используется повторно, счётчики отказов сохраняются
    update connections set state = 'pending', requester = v_me, updated_at = now() where id = c.id;
    return 'pending';
  end if;
end $$;

-- ================================================================ E5, E6, E7 (из S2): ответ адресата
-- p_action: accept | reject | block
create or replace function public.respond_request(
  p_connection uuid, p_action text, p_complain boolean default false, p_reason text default null)
returns text
language plpgsql security definer set search_path = public as $$
declare
  v_me uuid := _me();
  c connections;
begin
  select * into c from connections where id = p_connection for update;
  if c.id is null or v_me not in (c.user_low, c.user_high) then perform _fail('not_found'); end if;
  if c.state <> 'pending' then perform _fail('not_pending'); end if;
  if c.requester = v_me then perform _fail('not_addressee'); end if;

  if p_action = 'accept' then
    if _is_banned(c.requester) then perform _fail('user_unavailable'); end if;   -- G2
    update connections set state = 'active', accepted_at = now(), updated_at = now() where id = c.id;
    return 'active';

  elsif p_action in ('reject', 'block') then
    if p_action = 'block' and _rejects_of(c, c.requester) < 1 then
      perform _fail('block_not_available');                                       -- G4
    end if;
    if p_complain and p_action <> 'block' then perform _fail('complaint_requires_block'); end if;

    update connections set
      state = case when p_action = 'block' then 'blocked' else 'rejected' end::connection_state,
      blocked_by = case when p_action = 'block' then v_me end,
      rejected_at = now(),
      rejects_low  = rejects_low  + (c.requester = c.user_low)::int,
      rejects_high = rejects_high + (c.requester = c.user_high)::int,
      updated_at = now()
    where id = c.id;
    if p_action = 'block' and p_complain then perform _complain(v_me, c.requester, p_reason); end if;
    return case when p_action = 'block' then 'blocked' else 'rejected' end;
  end if;

  perform _fail('bad_action');
  return null;
end $$;

-- ================================================================ E7 (из S3): блокировка в чате
create or replace function public.block_user(
  p_connection uuid, p_complain boolean default false, p_reason text default null)
returns text
language plpgsql security definer set search_path = public as $$
declare
  v_me uuid := _me();
  c connections;
begin
  select * into c from connections where id = p_connection for update;
  if c.id is null or v_me not in (c.user_low, c.user_high) then perform _fail('not_found'); end if;
  -- для уже заблокированного чат выглядит активным, поэтому ответ тот же, что и для S2/S4
  if c.state <> 'active' then perform _fail('not_active'); end if;

  update connections set state = 'blocked', blocked_by = v_me, updated_at = now() where id = c.id;
  if p_complain then
    perform _complain(v_me, case when v_me = c.user_low then c.user_high else c.user_low end, p_reason);
  end if;
  return 'blocked';
end $$;

-- ================================================================ E8: разблокировать
create or replace function public.unblock_user(p_connection uuid) returns text
language plpgsql security definer set search_path = public as $$
declare
  v_me uuid := _me();
  c connections;
begin
  select * into c from connections where id = p_connection for update;
  if c.id is null or v_me not in (c.user_low, c.user_high) then perform _fail('not_found'); end if;
  if c.state <> 'blocked' or c.blocked_by <> v_me then perform _fail('not_blocked_by_you'); end if;

  if c.accepted_at is not null then
    update connections set state = 'active', blocked_by = null, updated_at = now() where id = c.id;
    return 'active';
  end if;
  -- чата не было: возврат в состояние после отказа, срок 24 часа отсчитывается от прежнего rejected_at
  update connections set state = 'rejected', blocked_by = null, updated_at = now() where id = c.id;
  return 'rejected';
end $$;

-- ================================================================ E9: сообщение
create or replace function public.send_message(p_connection uuid, p_body text) returns public.messages
language plpgsql security definer set search_path = public as $$
declare
  v_me uuid := _me();
  c connections;
  v_other uuid;
  v_msg messages;
begin
  select * into c from connections where id = p_connection for share;
  if c.id is null or v_me not in (c.user_low, c.user_high) then perform _fail('not_found'); end if;
  if c.state <> 'active' then perform _fail('cannot_send'); end if;
  v_other := case when v_me = c.user_low then c.user_high else c.user_low end;
  if _is_banned(v_other) then perform _fail('user_unavailable'); end if;          -- G2
  if p_body is null or char_length(btrim(p_body)) = 0 then perform _fail('empty_message'); end if;
  if char_length(btrim(p_body)) > 1000 then perform _fail('message_too_long'); end if;

  insert into messages (connection_id, sender, body) values (c.id, v_me, btrim(p_body)) returning * into v_msg;
  update connections set updated_at = now(),
    last_read_low  = case when v_me = user_low  then v_msg.id else last_read_low  end,
    last_read_high = case when v_me = user_high then v_msg.id else last_read_high end
  where id = c.id;
  return v_msg;
end $$;

create or replace function public.mark_read(p_connection uuid) returns void
language plpgsql security definer set search_path = public as $$
declare
  v_me uuid := _me();
begin
  update connections c set
    last_read_low  = case when v_me = c.user_low  then lm.id else c.last_read_low  end,
    last_read_high = case when v_me = c.user_high then lm.id else c.last_read_high end
  from (select coalesce(max(m.id), 0) as id from messages m where m.connection_id = p_connection) lm
  where c.id = p_connection and v_me in (c.user_low, c.user_high);
end $$;

-- ================================================================ списки: запросы, чаты, заблокированные
create or replace function public.my_connections()
returns table (
  connection_id uuid, partner_id uuid, name text, age int, bio text, avatar_path text, interests text[],
  relation text, can_send boolean, can_block_on_reject boolean, retry_at timestamptz,
  last_message text, last_message_at timestamptz, unread int, updated_at timestamptz)
language plpgsql stable security definer set search_path = public as $$
declare
  v_me uuid := _me();
begin
  return query
  select
    c.id, p.id, p.name, extract(year from now())::int - p.birth_year, p.bio, p.avatar_path,
    coalesce((select array_agg(i.name order by i.id)
              from profile_interests pi join interests i on i.id = pi.interest_id
              where pi.profile_id = p.id), '{}'),
    r.rel,
    r.rel = 'chat' and c.state = 'active' and not coalesce(p.banned_until > now(), false),
    r.rel = 'incoming' and _rejects_of(c, c.requester) >= 1,
    case when r.rel = 'cooldown' then _cooldown_until(c) end,
    lm.body, lm.created_at,
    case when r.rel = 'chat' then (
      select count(*)::int from messages m
      where m.connection_id = c.id and m.sender <> v_me
        and m.id > case when v_me = c.user_low then c.last_read_low else c.last_read_high end)
    else 0 end,
    c.updated_at
  from connections c
  join profiles p on p.id = case when v_me = c.user_low then c.user_high else c.user_low end
  cross join lateral (select _relation(c, v_me) as rel) r
  left join lateral (
    select m.body, m.created_at from messages m where m.connection_id = c.id order by m.id desc limit 1
  ) lm on r.rel = 'chat'
  where v_me in (c.user_low, c.user_high) and r.rel <> 'none'
  order by coalesce(lm.created_at, c.updated_at) desc;
end $$;

-- ================================================================ права
-- Служебные функции недоступны клиенту; открыты только события автомата и списки.
revoke execute on all functions in schema public from public, anon, authenticated;

grant execute on function
  public.save_profile(text, int, text, smallint[]),
  public.set_avatar(text),
  public.set_visibility(boolean),
  public.set_radius(int),
  public.update_location(double precision, double precision),
  public.nearby_users(),
  public.send_request(uuid),
  public.respond_request(uuid, text, boolean, text),
  public.block_user(uuid, boolean, text),
  public.unblock_user(uuid),
  public.send_message(uuid, text),
  public.mark_read(uuid),
  public.my_connections(),
  public._is_participant(uuid, uuid)          -- вызывается политикой чтения сообщений
to authenticated;

alter default privileges in schema public revoke execute on functions from public, anon, authenticated;
