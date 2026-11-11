-- Хранилище аватаров. Читать может любой (аватар виден в карточке на карте),
-- записывать — только владелец и только в свою папку <id пользователя>/.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('avatars', 'avatars', true, 1048576, array['image/jpeg'])
on conflict (id) do nothing;

create policy avatars_select_own on storage.objects
  for select to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);

create policy avatars_insert_own on storage.objects
  for insert to authenticated
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);

create policy avatars_update_own on storage.objects
  for update to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text)
  with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);

create policy avatars_delete_own on storage.objects
  for delete to authenticated
  using (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);

-- Путь аватара в профиле может указывать только в свою папку.
create or replace function public.set_avatar(p_path text) returns void
language plpgsql security definer set search_path = public as $$
declare
  v_me uuid := _me();
begin
  if p_path is not null and p_path not like v_me::text || '/%' then perform _fail('bad_avatar_path'); end if;
  update profiles set avatar_path = p_path where id = v_me;
end $$;
