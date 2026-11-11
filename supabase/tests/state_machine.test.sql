-- Проверка автомата состояний по документу «Модель состояний», версия 3.
-- Номера T1–T28 соответствуют строкам таблицы переходов. Запуск: supabase test db
begin;
create extension if not exists pgtap with schema extensions;
select no_plan();

-- ---------------------------------------------------------------- вспомогательное
create schema tests;
grant usage on schema tests to authenticated;

create function tests.u(n int) returns uuid language sql immutable as $$
  select ('00000000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid;
$$;

-- Пользователь с профилем; lat/lng — смещение от базовой точки в градусах.
create function tests.mk(n int, dlat float8 default 0, dlng float8 default 0,
                         p_visible boolean default true, p_radius int default 1) returns void
language plpgsql as $$
begin
  insert into auth.users (id, instance_id, aud, role, email)
  values (tests.u(n), '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'u' || n || '@test.local');
  insert into public.profiles (id, name, birth_year, visible, radius_km)
  values (tests.u(n), 'User ' || n, 1995, p_visible, p_radius);
  insert into public.locations (user_id, geog) values (tests.u(n), public._snap(55.75 + dlat, 37.61 + dlng));
end $$;

-- Выполнить выражение от имени пользователя n; вернуть результат текстом либо 'ERR:<код>'.
create function tests.call(n int, p_sql text) returns text language plpgsql as $$
declare
  r text;
begin
  perform set_config('request.jwt.claims', json_build_object('sub', tests.u(n), 'role', 'authenticated')::text, true);
  perform set_config('role', 'authenticated', true);
  begin
    execute 'select (' || p_sql || ')::text' into r;
  exception when others then
    r := 'ERR:' || sqlerrm;
  end;
  perform set_config('role', 'none', true);
  return r;
end $$;

-- То же для операторов (insert/update): 'ok:<число строк>' либо 'ERR:...'.
create function tests.run(n int, p_sql text) returns text language plpgsql as $$
declare
  r text; k int;
begin
  perform set_config('request.jwt.claims', json_build_object('sub', tests.u(n), 'role', 'authenticated')::text, true);
  perform set_config('role', 'authenticated', true);
  begin
    execute p_sql;
    get diagnostics k = row_count;
    r := 'ok:' || k;
  exception when others then
    r := 'ERR:' || sqlerrm;
  end;
  perform set_config('role', 'none', true);
  return r;
end $$;

create function tests.conn(a int, b int) returns public.connections language sql as $$
  select c from public.connections c
  where c.user_low = least(tests.u(a), tests.u(b)) and c.user_high = greatest(tests.u(a), tests.u(b));
$$;
create function tests.cid(a int, b int) returns text language sql as $$ select quote_literal((tests.conn(a, b)).id); $$;
create function tests.st(a int, b int) returns text language sql as $$ select coalesce((tests.conn(a, b)).state::text, 'none'); $$;
create function tests.uq(n int) returns text language sql as $$ select quote_literal(tests.u(n)); $$;
-- как пользователь a видит b на карте: отношение либо 'absent'
create function tests.seen(a int, b int) returns text language sql as $$
  select tests.call(a, format('coalesce((select relation from nearby_users() where user_id = %s), ''absent'')', tests.uq(b)));
$$;
-- как пользователь a видит связь с b в списках
create function tests.listed(a int, b int) returns text language sql as $$
  select tests.call(a, format('coalesce((select relation || ''/'' || can_send from my_connections() where partner_id = %s), ''absent'')', tests.uq(b)));
$$;
create function tests.age_rejection(a int, b int, p_age interval) returns void language sql as $$
  update public.connections c set rejected_at = now() - p_age where c.id = (tests.conn(a, b)).id;
$$;
grant execute on all functions in schema tests to authenticated;

select tests.mk(1, 0, 0, false);       -- A, начинает в S0
select tests.mk(2, 0.002, 0);          -- ~220 м
select tests.mk(3, 0.004, 0);
select tests.mk(4, -0.002, 0);
select tests.mk(5, 0, 0.004);
select tests.mk(6, 0.002, 0.004);
select tests.mk(7, 0.02, 0);           -- ~2,2 км
select tests.mk(8, 0.2, 0, true, 10);  -- ~22 км

-- ================================================================ S0
select is(tests.call(1, 'set_radius(3)'), '3', 'T1: S0, E3 — радиус сохранён');
select is(tests.call(1, 'set_radius(2)'), 'ERR:bad_radius', 'T1: радиус вне {1,3,5,10} отклоняется');
select is(tests.call(1, format('send_request(%s)', tests.uq(2))), 'ERR:not_visible', 'T3: S0, E4 недоступно без видимости');
select is(tests.seen(1, 2), 'ERR:not_visible', 'T3: в S0 пользователь никого не видит');
select is(tests.seen(2, 1), 'absent', 'T3: в S0 пользователь не отображается у других');
select is(tests.call(1, format('send_message(%L, ''hi'')', gen_random_uuid())), 'ERR:not_found', 'T3: S0, E9 недоступно');
select is(tests.call(1, 'set_visibility(true)'), 'true', 'T2: S0, E1 → S1');

-- ================================================================ S1
select is(tests.call(1, 'set_radius(1)'), '1', 'T4: S1, E3 — радиус изменён');
select is(tests.call(1, '(select count(*) from nearby_users())'), '5', 'T4: в радиусе 1 км видны пятеро');
select is(tests.call(1, 'set_radius(10)'), '10', 'T4: радиус 10 км');
select is(tests.seen(1, 7), 'absent', 'G1: взаимный радиус — у A 10 км, у собеседника 1 км при расстоянии 2,2 км → не виден');
select is(tests.call(1, format('send_request(%s)', tests.uq(7))), 'ERR:out_of_range', 'T6: G1 не выполнено → запрос отклонён');
update public.profiles set radius_km = 3 where id = tests.u(7);
select is(tests.seen(1, 7), 'none', 'G1: радиусы 10 и 3 км при расстоянии 2,2 км → виден');
select is(tests.seen(7, 1), 'none', 'G1: видимость симметрична');
select is(tests.seen(1, 8), 'absent', 'G1: дальше 10 км не виден никогда');
select is(tests.call(1, 'set_radius(1)'), '1', 'возврат радиуса 1 км');

update public.locations set updated_at = now() - interval '31 minutes' where user_id = tests.u(2);
select is(tests.seen(1, 2), 'absent', 'свежесть: местоположение старше 30 минут не участвует в поиске');
select is(tests.call(1, format('send_request(%s)', tests.uq(2))), 'ERR:out_of_range', 'свежесть: и в проверке G1');
update public.locations set updated_at = now() where user_id = tests.u(2);

select is(tests.call(1, 'set_visibility(false)'), 'false', 'T5: S1, E2 → S0');
select is(tests.call(1, 'set_visibility(true)'), 'true', 'T2: снова S1');
select is(tests.call(1, format('send_request(%s)', tests.uq(1))), 'ERR:bad_target', 'T6: A ≠ B');
select is(tests.call(1, format('respond_request(%L, ''accept'')', gen_random_uuid())), 'ERR:not_found', 'T7: S1, E5 недоступно');
select is(tests.call(1, format('unblock_user(%L)', gen_random_uuid())), 'ERR:not_found', 'T7: S1, E8 недоступно');

update public.profiles set banned_until = now() + interval '1 hour' where id = tests.u(2);
select is(tests.call(1, format('send_request(%s)', tests.uq(2))), 'ERR:user_unavailable', 'T6: G2 не выполнено → запрос отклонён');
select is(tests.seen(1, 2), 'absent', 'отстранённый не отображается на карте');
update public.profiles set banned_until = null where id = tests.u(2);

select is(tests.call(1, format('send_request(%s)', tests.uq(2))), 'pending', 'T6: S1, E4 при G1 и G2 → S2');
select is((tests.conn(1, 2)).requester, tests.u(1), 'T6: requester = A');

-- ================================================================ S2
select is(tests.seen(1, 2), 'outgoing', 'S2: отправитель видит «запрос отправлен»');
select is(tests.seen(2, 1), 'incoming', 'S2: адресат видит входящий запрос');
select is(tests.call(1, format('send_request(%s)', tests.uq(2))), 'ERR:already_pending', 'T15: повторный E4 от отправителя');
select is(tests.call(1, format('respond_request(%s, ''accept'')', tests.cid(1, 2))), 'ERR:not_addressee', 'T15: E5 от отправителя');
select is(tests.call(1, format('respond_request(%s, ''reject'')', tests.cid(1, 2))), 'ERR:not_addressee', 'T15: E6 от отправителя');
select is(tests.call(1, format('respond_request(%s, ''block'')', tests.cid(1, 2))), 'ERR:not_addressee', 'T15: E7 от отправителя');
select is(tests.call(1, format('send_message(%s, ''hi'')', tests.cid(1, 2))), 'ERR:cannot_send', 'T15: E9 в S2');
select is(tests.call(2, format('unblock_user(%s)', tests.cid(1, 2))), 'ERR:not_blocked_by_you', 'T15: E8 в S2');
select is(tests.call(2, format('block_user(%s)', tests.cid(1, 2))), 'ERR:not_active', 'T15: блокировка чата в S2');
select is(tests.call(3, format('respond_request(%s, ''accept'')', tests.cid(1, 2))), 'ERR:not_found', 'посторонний не может ответить на чужой запрос');
select is(tests.call(2, format('respond_request(%s, ''block'')', tests.cid(1, 2))), 'ERR:block_not_available', 'T12: E7 при первом отказе (G4 не выполнено)');
select is(tests.call(2, format('respond_request(%s, ''reject'', true, ''spam'')', tests.cid(1, 2))), 'ERR:complaint_requires_block', 'T15: E10 без E7');

update public.profiles set banned_until = now() + interval '1 hour' where id = tests.u(1);
select is(tests.call(2, format('respond_request(%s, ''accept'')', tests.cid(1, 2))), 'ERR:user_unavailable', 'T13: E5 при невыполненном G2');
update public.profiles set banned_until = null where id = tests.u(1);

select is(tests.call(1, 'set_radius(3)'), '3', 'T14: E3 в S2');
select is(tests.call(1, 'set_visibility(false)'), 'false', 'T14: E2 в S2');
select is(tests.st(1, 2), 'pending', 'T14: состояние S2 не изменилось');
select is(tests.call(1, 'set_visibility(true)'), 'true', 'T14: E1 в S2');
select is(tests.call(1, 'set_radius(1)'), '1', 'возврат радиуса');

select is(tests.call(2, 'set_visibility(false)'), 'false', 'адресат выключает видимость');
select is(tests.call(2, format('respond_request(%s, ''accept'')', tests.cid(1, 2))), 'active', 'T8: E5 при выключенной видимости → S3');
select isnt((tests.conn(1, 2)).accepted_at, null, 'T8: accepted_at задано');

select is(tests.call(1, format('send_request(%s)', tests.uq(3))), 'pending', 'T6: запрос второму человеку при активном чате с первым');
select is(tests.call(3, format('send_request(%s)', tests.uq(1))), 'active', 'T9: встречный E4 → S3');

select is(tests.call(1, format('send_request(%s)', tests.uq(4))), 'pending', 'T6: запрос третьему');
select is(tests.call(4, 'set_visibility(false)'), 'false', 'адресат выключает видимость');
select is(tests.call(4, format('respond_request(%s, ''reject'')', tests.cid(1, 4))), 'rejected', 'T10: E6 при выключенной видимости → S4');
select is((tests.conn(1, 4)).rejects_low + (tests.conn(1, 4)).rejects_high, 1, 'T10: счётчик отказов = 1');
select is(tests.call(4, 'set_visibility(true)'), 'true', 'адресат снова видим');

-- ================================================================ S4
select is(tests.seen(1, 4), 'cooldown', 'S4: отправитель видит ожидание 24 часа');
select is(tests.call(1, format('send_request(%s)', tests.uq(4))), 'ERR:cooldown', 'T24: E4 до истечения 24 часов');
select is(tests.call(4, format('send_request(%s)', tests.uq(1))), 'ERR:cooldown', 'T24: E4 от отказавшего до истечения 24 часов');
select is(tests.call(4, format('respond_request(%s, ''accept'')', tests.cid(1, 4))), 'ERR:not_pending', 'T24: E5 в S4');
select is(tests.call(4, format('respond_request(%s, ''block'')', tests.cid(1, 4))), 'ERR:not_pending', 'T24: E7 в S4');
select is(tests.call(4, format('unblock_user(%s)', tests.cid(1, 4))), 'ERR:not_blocked_by_you', 'T24: E8 в S4');
select is(tests.call(1, format('send_message(%s, ''hi'')', tests.cid(1, 4))), 'ERR:cannot_send', 'T24: E9 в S4');
select is(tests.call(1, 'set_radius(3)'), '3', 'T23: E3 в S4');
select is(tests.st(1, 4), 'rejected', 'T23: состояние S4 не изменилось');
select is(tests.call(1, 'set_radius(1)'), '1', 'возврат радиуса');

select tests.age_rejection(1, 4, interval '23 hours 59 minutes');
select is(tests.call(1, format('send_request(%s)', tests.uq(4))), 'ERR:cooldown', 'T24: за минуту до истечения срока');
select tests.age_rejection(1, 4, interval '24 hours 1 minute');
select is(tests.seen(1, 4), 'none', 'T21: E11 — через 24 часа пара снова в S1');
select is(tests.listed(1, 4), 'absent', 'T21: истёкший отказ не показывается в списках');
select is(tests.call(1, format('send_request(%s)', tests.uq(4))), 'pending', 'T21 → T6: повторный запрос принят');
select is((select count(*)::int from public.connections c
           where tests.u(1) in (c.user_low, c.user_high) and tests.u(4) in (c.user_low, c.user_high)), 1,
          'инвариант: одна запись связи на пару');
select is((tests.conn(1, 4)).rejects_low + (tests.conn(1, 4)).rejects_high, 1, 'T21: счётчик отказов сохранён');

-- T22: срок истёк при выключенной видимости → S0
select is(tests.call(5, format('send_request(%s)', tests.uq(6))), 'pending', 'пара 5–6: запрос');
select is(tests.call(6, format('respond_request(%s, ''reject'')', tests.cid(5, 6))), 'rejected', 'пара 5–6: отказ');
select tests.age_rejection(5, 6, interval '25 hours');
select is(tests.call(5, 'set_visibility(false)'), 'false', 'отправитель выключает видимость');
select is(tests.call(5, format('send_request(%s)', tests.uq(6))), 'ERR:not_visible', 'T22: E11 при выключенной видимости → S0');
select is(tests.listed(5, 6), 'absent', 'T22: связь не показывается');

-- ================================================================ S2 → S5: блокировка со второго отказа
select is(tests.call(4, format('(select can_block_on_reject from my_connections() where partner_id = %s)', tests.uq(1))), 'true',
          'G4: при втором запросе адресату доступна блокировка');
select is(tests.call(4, format('respond_request(%s, ''block'', true, ''spam'')', tests.cid(1, 4))), 'blocked', 'T11: E7 с E10 при G4 → S5');
select is((tests.conn(1, 4)).blocked_by, tests.u(4), 'T11: blocked_by = адресат');
select is((tests.conn(1, 4)).rejects_low + (tests.conn(1, 4)).rejects_high, 2, 'T11: счётчик отказов = 2');
select is((select count(*)::int from public.complaints where from_user = tests.u(4) and to_user = tests.u(1)), 1, 'T11: жалоба записана');

-- ================================================================ S5 (чата не было)
select is(tests.seen(1, 4), 'absent', 'S5: заблокированный не видит заблокировавшего на карте');
select is(tests.seen(4, 1), 'absent', 'S5: заблокировавший не видит заблокированного на карте');
select is(tests.listed(1, 4), 'cooldown/false', 'S5: заблокированному блокировка выглядит как обычный отказ');
select is(tests.listed(4, 1), 'blocked_by_me/false', 'S5: заблокировавший видит запись в «Заблокированных»');
select is(tests.call(1, format('send_request(%s)', tests.uq(4))), 'ERR:request_unavailable', 'T28: E4 в S5');
select is(tests.call(4, format('send_request(%s)', tests.uq(1))), 'ERR:request_unavailable', 'T28: E4 от заблокировавшего');
select is(tests.call(1, format('unblock_user(%s)', tests.cid(1, 4))), 'ERR:not_blocked_by_you', 'T28: E8 от другого участника');
select is(tests.call(1, format('send_message(%s, ''hi'')', tests.cid(1, 4))), 'ERR:cannot_send', 'T28: E9 в S5');
select is(tests.call(4, format('respond_request(%s, ''accept'')', tests.cid(1, 4))), 'ERR:not_pending', 'T28: E5 в S5');
select is(tests.call(4, 'set_radius(3)'), '3', 'T27: E3 в S5');
select is(tests.st(1, 4), 'blocked', 'T27: состояние S5 не изменилось');
select is(tests.call(4, 'set_radius(1)'), '1', 'возврат радиуса');
select tests.age_rejection(1, 4, interval '25 hours');
select is(tests.st(1, 4), 'blocked', 'T28: E11 в S5 ничего не меняет');
select is(tests.call(1, format('send_request(%s)', tests.uq(4))), 'ERR:request_unavailable', 'T28: и после 24 часов запрос недоступен');

select tests.age_rejection(1, 4, interval '1 hour');
select is(tests.call(4, format('unblock_user(%s)', tests.cid(1, 4))), 'rejected', 'T26: E8, чата не было, 24 часа не прошли → S4');
select is(tests.call(1, format('send_request(%s)', tests.uq(4))), 'ERR:cooldown', 'T26: остаток срока сохраняется');
select tests.age_rejection(1, 4, interval '25 hours');
select is(tests.seen(1, 4), 'none', 'T26: после срока пара в S1');
select is(tests.call(1, format('send_request(%s)', tests.uq(4))), 'pending', 'T26 → T6: запрос снова возможен');
select is(tests.call(4, format('respond_request(%s, ''block'')', tests.cid(1, 4))), 'blocked', 'T11: блокировка без жалобы');
select tests.age_rejection(1, 4, interval '25 hours');
select is(tests.call(4, format('unblock_user(%s)', tests.cid(1, 4))), 'rejected', 'T26: E8, чата не было, срок истёк');
select is(tests.seen(1, 4), 'none', 'T26: сразу S1');
select is((tests.conn(1, 4)).rejects_low + (tests.conn(1, 4)).rejects_high, 3, 'T26: счётчик отказов сохранён');

-- ================================================================ S3
select alike(tests.call(1, format('send_message(%s, ''  привет  '')', tests.cid(1, 2))), '%привет%', 'T16: E9 при G2 — сообщение добавлено');
select is((select body from public.messages where connection_id = (tests.conn(1, 2)).id), 'привет', 'T16: текст очищен от пробелов по краям');
select is(tests.call(2, format('(select unread from my_connections() where partner_id = %s)', tests.uq(1))), '1', 'счётчик непрочитанных = 1');
select is(tests.call(2, format('mark_read(%s)', tests.cid(1, 2))), '', 'отметка о прочтении');
select is(tests.call(2, format('(select unread from my_connections() where partner_id = %s)', tests.uq(1))), '0', 'счётчик непрочитанных = 0');
select is(tests.call(1, format('send_message(%s, ''   '')', tests.cid(1, 2))), 'ERR:empty_message', 'пустое сообщение отклоняется');
select is(tests.call(1, format('send_message(%s, repeat(''a'', 1001))', tests.cid(1, 2))), 'ERR:message_too_long', 'сообщение длиннее 1000 символов отклоняется');
select is(tests.call(3, format('send_message(%s, ''hi'')', tests.cid(1, 2))), 'ERR:not_found', 'инвариант: посторонний не может писать в чужой чат');

update public.profiles set banned_until = now() + interval '1 hour' where id = tests.u(2);
select is(tests.call(1, format('send_message(%s, ''hi'')', tests.cid(1, 2))), 'ERR:user_unavailable', 'T17: E9 при невыполненном G2');
select is(tests.call(2, format('send_message(%s, ''hi'')', tests.cid(1, 2))), 'ERR:banned', 'T17: отстранённый не может писать');
select is(tests.listed(1, 2), 'chat/false', 'T17: чат остаётся, отправка недоступна');
update public.profiles set banned_until = null where id = tests.u(2);

update public.locations set geog = public._snap(56.75, 37.61) where user_id = tests.u(2);
select is(tests.call(1, 'set_visibility(false)'), 'false', 'T19: E2 в S3');
select alike(tests.call(2, format('send_message(%s, ''далеко'')', tests.cid(1, 2))), '%далеко%', 'T19: чат работает без видимости и вне радиуса');
select is(tests.st(1, 2), 'active', 'T19: состояние S3 не изменилось');
select is(tests.call(1, 'set_visibility(true)'), 'true', 'T19: E1 в S3');
update public.locations set geog = public._snap(55.752, 37.61) where user_id = tests.u(2);
select is(tests.call(2, 'set_visibility(true)'), 'true', 'собеседник снова видим');
select is(tests.seen(1, 2), 'chat', 'S3: на карте собеседник отмечен как чат');

select is(tests.call(1, format('send_request(%s)', tests.uq(2))), 'ERR:already_connected', 'T20: E4 в S3');
select is(tests.call(2, format('respond_request(%s, ''accept'')', tests.cid(1, 2))), 'ERR:not_pending', 'T20: E5 в S3');
select is(tests.call(2, format('respond_request(%s, ''reject'')', tests.cid(1, 2))), 'ERR:not_pending', 'T20: E6 в S3');
select is(tests.call(2, format('unblock_user(%s)', tests.cid(1, 2))), 'ERR:not_blocked_by_you', 'T20: E8 в S3');
select is(tests.call(2, format('block_user(%s, true, ''bad'')', tests.cid(1, 2))), 'ERR:bad_reason', 'E10: причина только из списка');
select is(tests.st(1, 2), 'active', 'неудачная блокировка откатывается целиком');

select is(tests.call(2, format('block_user(%s, true, ''abuse'')', tests.cid(1, 2))), 'blocked', 'T18: E7 с E10 от участника → S5');
select is((tests.conn(1, 2)).blocked_by, tests.u(2), 'T18: blocked_by = инициатор');
select is((select count(*)::int from public.complaints where to_user = tests.u(1)), 2, 'T18: жалоба записана (вторая на этого пользователя)');

-- ================================================================ S5 (чат был)
select is(tests.call(1, format('send_message(%s, ''hi'')', tests.cid(1, 2))), 'ERR:cannot_send', 'T28: E9 от заблокированного');
select is(tests.call(2, format('send_message(%s, ''hi'')', tests.cid(1, 2))), 'ERR:cannot_send', 'T28: E9 от заблокировавшего');
select is(tests.listed(1, 2), 'chat/false', 'S5: заблокированный видит чат без возможности писать');
select is(tests.listed(2, 1), 'blocked_by_me/false', 'S5: заблокировавший видит запись в «Заблокированных»');
select is(tests.seen(1, 2), 'absent', 'S5: пара не видит друг друга на карте');
select is(tests.call(1, format('block_user(%s)', tests.cid(1, 2))), 'ERR:not_active', 'T28: E7 в S5');
select is(tests.call(1, format('unblock_user(%s)', tests.cid(1, 2))), 'ERR:not_blocked_by_you', 'T28: E8 от другого участника');
select is((select count(*)::int from public.messages where connection_id = (tests.conn(1, 2)).id), 2, 'S5: история сообщений сохранена');
select is(tests.call(2, format('unblock_user(%s)', tests.cid(1, 2))), 'active', 'T25: E8 от blocked_by, чат был → S3');
select alike(tests.call(1, format('send_message(%s, ''снова'')', tests.cid(1, 2))), '%снова%', 'T25: переписка возобновлена');
select is(tests.call(2, format('block_user(%s, true, ''abuse'')', tests.cid(1, 2))), 'blocked', 'повторная блокировка с жалобой');
select is((select count(*)::int from public.complaints where to_user = tests.u(1)), 2, 'инвариант: повторная жалоба от того же человека не считается');
select is(tests.call(2, format('unblock_user(%s)', tests.cid(1, 2))), 'active', 'разблокировка');

-- ================================================================ жалобы и отстранение
select tests.mk(50, 1, 1);
select tests.mk(n, 1, 1) from generate_series(60, 71) n;

select public._complain(tests.u(60), tests.u(50), 'spam');
select public._complain(tests.u(61), tests.u(50), 'spam');
select public._complain(tests.u(61), tests.u(50), 'abuse');
select is((select ban_level from public.profiles where id = tests.u(50)), 0, '2 жалобы от разных людей (и одна повторная) — отстранения нет');
select public._complain(tests.u(62), tests.u(50), 'other');
select is((select ban_level from public.profiles where id = tests.u(50)), 1, '3 жалобы — первая ступень');
select ok((select banned_until between now() + interval '23 hours 59 minutes' and now() + interval '24 hours 1 minute'
           from public.profiles where id = tests.u(50)), '3 жалобы — отстранение на 24 часа');
select is(tests.call(50, 'set_visibility(true)'), 'ERR:banned', 'отстранённый не может выполнять действия');
select is(tests.call(50, '(select count(*) from my_connections())'), 'ERR:banned', 'отстранённый не видит списки');

update public.profiles set banned_until = now() - interval '1 minute' where id = tests.u(50);
select is(tests.call(50, 'set_visibility(true)'), 'true', 'по истечении срока доступ возвращается без фонового процесса');

select public._complain(tests.u(n), tests.u(50), 'spam') from generate_series(63, 65) n;
select ok((select ban_level = 2 and banned_until between now() + interval '71 hours' and now() + interval '73 hours'
           from public.profiles where id = tests.u(50)), '6 жалоб — отстранение на 3 дня');
select public._complain(tests.u(n), tests.u(50), 'spam') from generate_series(66, 68) n;
select ok((select ban_level = 3 and banned_until between now() + interval '167 hours' and now() + interval '169 hours'
           from public.profiles where id = tests.u(50)), '9 жалоб — отстранение на 7 дней');
select public._complain(tests.u(n), tests.u(50), 'spam') from generate_series(69, 71) n;
select ok((select ban_level = 4 and banned_until = 'infinity' from public.profiles where id = tests.u(50)), '12 жалоб — отстранение навсегда');

-- ================================================================ профиль
insert into auth.users (id, instance_id, aud, role, email)
values (tests.u(90), '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'u90@test.local');
select is(tests.call(90, 'set_visibility(true)'), 'ERR:profile_required', 'без профиля действия недоступны');
select is(tests.call(90, format('(save_profile(''Юный'', %s)).name', extract(year from now())::int - 17)), 'ERR:age_restricted', 'регистрация младше 18 лет отклоняется');
select is(tests.call(90, '(save_profile(''Много'', 1990, '''', ''{1,2,3,4,5,6}'')).name'), 'ERR:too_many_interests', 'не более 5 интересов');
select is(tests.call(90, '(save_profile(''  Анна '', 1990, ''о себе'', ''{1,3}'')).name'), 'Анна', 'профиль создан');
select is((select count(*)::int from public.profile_interests where profile_id = tests.u(90)), 2, 'интересы сохранены');
select is(tests.call(90, '(save_profile(''Анна'', 1990, '''', ''{2}'')).bio'), '', 'профиль обновлён');
select is((select count(*)::int from public.profile_interests where profile_id = tests.u(90)), 1, 'интересы заменены');

-- ================================================================ приватность и доступ к данным
select set_config('request.jwt.claims', json_build_object('sub', tests.u(1))::text, true);
select ok((
  select extensions.st_distance(l.geog, extensions.st_setsrid(extensions.st_makepoint(n.lng, n.lat), 4326)::extensions.geography) between 1 and 101
  from public.locations l, public.nearby_users() n
  where l.user_id = tests.u(3) and n.user_id = tests.u(3)), 'точка на карте смещена относительно сохранённой на 30–100 м');
select is(tests.call(1, format('(select lat::text || lng::text from nearby_users() where user_id = %s)', tests.uq(3))),
          tests.call(1, format('(select lat::text || lng::text from nearby_users() where user_id = %s)', tests.uq(3))),
          'смещение постоянно между запросами');
select ok(extensions.st_distance(public._snap(55.7512345, 37.6198765),
                                 extensions.st_setsrid(extensions.st_makepoint(37.6198765, 55.7512345), 4326)::extensions.geography) < 200,
          'огрубление сдвигает точку не дальше ячейки сетки');
select ok(extensions.st_equals(public._snap(55.7512345, 37.6198765)::extensions.geometry,
                               public._snap(55.7512, 37.6199)::extensions.geometry),
          'близкие точки попадают в одну ячейку сетки');

select is(tests.call(1, '(select count(*) from connections)'), '0', 'RLS: таблица связей недоступна напрямую');
select is(tests.call(1, '(select count(*) from locations)'), '0', 'RLS: координаты других пользователей недоступны');
select is(tests.call(1, '(select count(*) from complaints)'), '0', 'RLS: жалобы недоступны');
select is(tests.call(1, '(select count(*) from profiles)'), '1', 'RLS: виден только свой профиль');
select is(tests.call(1, '(select count(*) from messages)'), '3', 'RLS: участник видит сообщения своего чата');
select is(tests.call(3, '(select count(*) from messages)'), '0', 'RLS: посторонний не видит чужие сообщения');
select alike(tests.run(1, format('insert into messages (connection_id, sender, body) values (%s, %s, ''x'')', tests.cid(1, 2), tests.uq(1))),
            'ERR:%row-level security%', 'инвариант: вставка в messages в обход функции запрещена');
select is(tests.run(1, 'update profiles set banned_until = null, visible = true'), 'ok:0', 'RLS: профиль нельзя изменить напрямую');
select is(tests.run(1, 'update connections set state = ''active'''), 'ok:0', 'RLS: связь нельзя изменить напрямую');
select alike(tests.call(1, format('_complain(%s, %s, ''spam'')', tests.uq(1), tests.uq(3))), 'ERR:permission denied%', 'служебные функции клиенту недоступны');
select alike(tests.call(1, format('_g1(%s, %s)', tests.uq(1), tests.uq(3))), 'ERR:permission denied%', 'проверка расстояния до произвольного пользователя клиенту недоступна');

select * from finish();
rollback;
