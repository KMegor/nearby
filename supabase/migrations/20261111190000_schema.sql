-- Схема данных. Все изменения выполняются только через функции (см. следующую миграцию):
-- у таблиц включён RLS и нет политик на запись, поэтому клиент не может обойти автомат состояний.

create extension if not exists postgis with schema extensions;

create type public.connection_state as enum ('pending', 'active', 'rejected', 'blocked');

-- ---------------------------------------------------------------- профили
create table public.profiles (
  id           uuid primary key references auth.users (id) on delete cascade,
  name         text not null check (char_length(name) between 1 and 50),
  birth_year   int  not null check (birth_year between 1900 and 2100),
  bio          text not null default '' check (char_length(bio) <= 200),
  avatar_path  text,
  visible      boolean not null default false,                         -- S0 / S1
  radius_km    int not null default 1 check (radius_km in (1, 3, 5, 10)),
  ban_level    int not null default 0,                                 -- ступень отстранения
  banned_until timestamptz,                                            -- 'infinity' = навсегда
  created_at   timestamptz not null default now()
);

create table public.interests (
  id   smallint primary key,
  name text not null unique
);

create table public.profile_interests (
  profile_id  uuid not null references public.profiles (id) on delete cascade,
  interest_id smallint not null references public.interests (id),
  primary key (profile_id, interest_id)
);

insert into public.interests (id, name) values
  (1, 'Музыка'), (2, 'Спорт'), (3, 'Игры'), (4, 'Кино'), (5, 'Книги'), (6, 'Путешествия'),
  (7, 'Еда'), (8, 'Технологии'), (9, 'Искусство'), (10, 'Животные'), (11, 'Фотография'), (12, 'Прогулки');

-- ---------------------------------------------------------------- местоположение
-- Хранится уже огрублённая точка (сетка ~200 м). Клиенту таблица недоступна совсем.
create table public.locations (
  user_id    uuid primary key references public.profiles (id) on delete cascade,
  geog       extensions.geography(Point, 4326) not null,
  updated_at timestamptz not null default now()
);
create index locations_geog_idx on public.locations using gist (geog);

-- ---------------------------------------------------------------- связь пары
-- Одна запись на неупорядоченную пару: user_low < user_high.
create table public.connections (
  id             uuid not null default gen_random_uuid() unique,
  user_low       uuid not null references public.profiles (id) on delete cascade,
  user_high      uuid not null references public.profiles (id) on delete cascade,
  requester      uuid not null,
  state          public.connection_state not null,
  rejects_low    int not null default 0,   -- сколько отказов получил user_low на свои запросы
  rejects_high   int not null default 0,   -- сколько отказов получил user_high на свои запросы
  rejected_at    timestamptz,
  accepted_at    timestamptz,              -- задано, если чат когда-либо существовал
  blocked_by     uuid,
  last_read_low  bigint not null default 0,  -- id последнего прочитанного сообщения
  last_read_high bigint not null default 0,
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now(),
  primary key (user_low, user_high),
  check (user_low < user_high),
  check (requester in (user_low, user_high)),
  check (blocked_by is null or blocked_by in (user_low, user_high)),
  check ((state = 'blocked') = (blocked_by is not null)),
  check (state <> 'rejected' or rejected_at is not null),
  check (state <> 'active' or accepted_at is not null)
);
create index connections_high_idx on public.connections (user_high);

-- ---------------------------------------------------------------- сообщения
create table public.messages (
  id            bigint generated always as identity primary key,
  connection_id uuid not null references public.connections (id) on delete cascade,
  sender        uuid not null references public.profiles (id) on delete cascade,
  body          text not null check (char_length(body) between 1 and 1000),
  created_at    timestamptz not null default now()
);
create index messages_conn_idx on public.messages (connection_id, id desc);

-- ---------------------------------------------------------------- жалобы
create table public.complaints (
  id         bigint generated always as identity primary key,
  from_user  uuid not null references public.profiles (id) on delete cascade,
  to_user    uuid not null references public.profiles (id) on delete cascade,
  reason     text not null check (reason in ('spam', 'abuse', 'other')),
  created_at timestamptz not null default now(),
  unique (from_user, to_user),
  check (from_user <> to_user)
);
create index complaints_to_idx on public.complaints (to_user);

-- ---------------------------------------------------------------- доступ
alter table public.profiles          enable row level security;
alter table public.interests         enable row level security;
alter table public.profile_interests enable row level security;
alter table public.locations         enable row level security;
alter table public.connections       enable row level security;
alter table public.messages          enable row level security;
alter table public.complaints        enable row level security;

-- Читать напрямую можно только: свой профиль, справочник интересов, свои интересы
-- и сообщения своих чатов (нужно для Realtime). Остальное отдаётся функциями.
create policy profiles_select_own on public.profiles
  for select to authenticated using (id = (select auth.uid()));

create policy interests_select_all on public.interests
  for select to authenticated using (true);

create policy profile_interests_select_own on public.profile_interests
  for select to authenticated using (profile_id = (select auth.uid()));

-- Таблица connections закрыта от прямого чтения, поэтому участие в чате проверяется функцией.
create or replace function public._is_participant(p_connection uuid, p_user uuid)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from connections c where c.id = p_connection and p_user in (c.user_low, c.user_high));
$$;

create policy messages_select_participant on public.messages
  for select to authenticated using (public._is_participant(connection_id, (select auth.uid())));

do $$
begin
  if exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
    alter publication supabase_realtime add table public.messages;
  end if;
end $$;
