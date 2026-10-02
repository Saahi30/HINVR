-- YouTube feeds behind each mandir's live slot. The live-check function reads
-- these, and writes the best live one into mandirs.live / mandirs.live_url.
-- Mandirs with no enabled source keep their hand-set live fields.
-- Safe to re-run.

alter table public.mandirs
  add column if not exists live_checked_at timestamptz;

create table if not exists public.live_sources (
  id uuid primary key default gen_random_uuid(),
  mandir_id text not null references public.mandirs (id) on delete cascade,
  video_id text not null default '',
  channel_id text not null default '',
  channel_name text not null default '',
  official boolean not null default false,
  follow_channel boolean not null default false,
  title_match text not null default '',
  priority int not null default 100,
  enabled boolean not null default true,
  note text not null default '',
  last_checked_at timestamptz,
  last_live boolean not null default false,
  last_title text not null default '',
  last_viewers int,
  last_error text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint live_sources_has_ref check (video_id <> '' or channel_id <> '')
);

create index if not exists live_sources_mandir
  on public.live_sources (mandir_id, enabled);

drop trigger if exists live_sources_touch on public.live_sources;
create trigger live_sources_touch
  before update on public.live_sources
  for each row execute function private.touch_updated_at();

alter table public.live_sources enable row level security;

revoke all on public.live_sources from anon, authenticated;
grant select, insert, update, delete on public.live_sources to authenticated;

drop policy if exists live_sources_staff_select on public.live_sources;
create policy live_sources_staff_select
  on public.live_sources for select to authenticated
  using (private.is_staff());

drop policy if exists live_sources_staff_insert on public.live_sources;
create policy live_sources_staff_insert
  on public.live_sources for insert to authenticated
  with check (private.is_staff());

drop policy if exists live_sources_staff_update on public.live_sources;
create policy live_sources_staff_update
  on public.live_sources for update to authenticated
  using (private.is_staff())
  with check (private.is_staff());

drop policy if exists live_sources_staff_delete on public.live_sources;
create policy live_sources_staff_delete
  on public.live_sources for delete to authenticated
  using (private.is_staff());

-- Mandirs the phone app already bundles are published so the checker's live
-- state reaches them. New ones stay drafts until the Live screen can hold them.
insert into public.mandirs (
  id, name, place, city, scene, live, pass_accepted, next_aarti, timings,
  updated_label, sort_order, published
) values
  ('jagannath', 'Jagannath Temple', 'Puri, Odisha', 'Puri', 'Somnath', false, false, '6:00 PM',
    'Open 5:30 AM–9:30 PM · Timings vary by ritual', '', 60, true),
  ('siddhivinayak', 'Siddhivinayak', 'Mumbai, Maharashtra', 'Mumbai', 'Shirdi', false, true, '7:30 PM',
    'Wednesday–Monday 5:30 AM–9:50 PM · Tuesday from 3:15 AM', '', 70, true),
  ('iskcon-bengaluru', 'ISKCON Bengaluru', 'Rajajinagar, Karnataka', 'Bengaluru', 'Tirupati', false, true, '7:00 PM',
    'Morning 4:15 AM–5:00 AM · Darshan through the evening', '', 80, true),
  ('mahakal', 'Mahakaleshwar', 'Ujjain, Madhya Pradesh', 'Ujjain', 'Kashi', false, false, null, '', '', 200, false),
  ('ram-lalla', 'Ram Lalla', 'Ayodhya, Uttar Pradesh', 'Ayodhya', 'Tirupati', false, false, null, '', '', 210, false),
  ('laxmi-narayan', 'Laxmi Narayan Dev', 'Vadtal, Gujarat', 'Vadtal', 'Somnath', false, false, null, '', '', 220, false),
  ('mahavir-patna', 'Mahavir Mandir', 'Patna, Bihar', 'Patna', 'Shirdi', false, false, null, '', '', 230, false),
  ('pashupatinath-mandsaur', 'Pashupatinath', 'Mandsaur, Madhya Pradesh', 'Mandsaur', 'Kashi', false, false, null, '', '', 240, false),
  ('harni-hanuman', 'Harni Bhidbhanjan Maruti', 'Vadodara, Gujarat', 'Vadodara', 'Shirdi', false, false, null, '', '', 250, false),
  ('puliyakulam-vinayagar', 'Puliyakulam Vinayagar', 'Coimbatore, Tamil Nadu', 'Coimbatore', 'Tirupati', false, false, null, '', '', 260, false),
  ('radha-rani-barsana', 'Radha Rani', 'Barsana, Uttar Pradesh', 'Barsana', 'Tirupati', false, false, null, '', '', 270, false),
  ('vrindavan-chandrodaya', 'Vrindavan Chandrodaya Mandir', 'Vrindavan, Uttar Pradesh', 'Vrindavan', 'Tirupati', false, false, null, '', '', 280, false),
  ('krishna-janmabhoomi', 'Krishna Janmabhoomi', 'Mathura, Uttar Pradesh', 'Mathura', 'Tirupati', false, false, null, '', '', 290, false),
  ('narnarayan-kalupur', 'NarNarayan Dev', 'Kalupur, Ahmedabad', 'Ahmedabad', 'Somnath', false, false, null, '', '', 300, false),
  ('naga-sai', 'Sri Naga Sai Mandir', 'Coimbatore, Tamil Nadu', 'Coimbatore', 'Shirdi', false, false, null, '', '', 310, false),
  ('khatu-shyam', 'Khatu Shyam', 'Sikar, Rajasthan', 'Khatu', 'Kedarnath', false, false, null, '', '', 320, false),
  ('baidyanath', 'Baba Baidyanath', 'Deoghar, Jharkhand', 'Deoghar', 'Kashi', false, false, null, '', '', 330, false),
  ('sawariya-seth', 'Sawariya Seth', 'Ladusa, Rajasthan', 'Ladusa', 'Kedarnath', false, false, null, '', '', 340, false),
  ('chhapaiya', 'Chhapaiya Swaminarayan', 'Chhapaiya, Uttar Pradesh', 'Chhapaiya', 'Somnath', false, false, null, '', '', 350, false),
  ('narnarayan-bhuj', 'Swaminarayan Mandir', 'Bhuj, Gujarat', 'Bhuj', 'Somnath', false, false, null, '', '', 360, false),
  ('rapar', 'Radha Krishna Dev', 'Rapar, Gujarat', 'Rapar', 'Somnath', false, false, null, '', '', 370, false),
  ('narayanpura', 'Narayanpura Swaminarayan', 'Narayanpura, Ahmedabad', 'Ahmedabad', 'Somnath', false, false, null, '', '', 380, false),
  ('muli', 'Muli Swaminarayan', 'Muli, Gujarat', 'Muli', 'Somnath', false, false, null, '', '', 390, false),
  ('sardhar', 'Sardhar Swaminarayan', 'Sardhar, Gujarat', 'Sardhar', 'Somnath', false, false, null, '', '', 400, false)
on conflict (id) do nothing;

-- Streams seen live on 29 Sep 2026. Only seeds a mandir that has no sources yet.
insert into public.live_sources (
  mandir_id, video_id, channel_id, channel_name, official, follow_channel,
  title_match, priority, enabled, note
)
select v.mandir_id, v.video_id, v.channel_id, v.channel_name, v.official, v.follow_channel,
  v.title_match, v.priority, v.enabled, v.note
from (values
  ('tirupati', '', 'UCTboTRX74UydvU_cBdm_cCQ', '', false, true, '', 10, true, ''),
  ('jagannath', '_pplsMPNVmQ', '', 'JAY JAGANNATH TV', false, false, '', 10, true, ''),
  ('jagannath', '_-q3Ys8_q38', '', 'Jay Jagannath TV Hindi', false, false, '', 20, true, ''),
  ('siddhivinayak', 'V2FnGzYYux8', '', 'Shree Siddhivinayak Ganapati Temple Trust', true, true, '', 10, true, ''),
  ('siddhivinayak', 'G5ZxZDxpI3w', '', 'Mayur Bhakti', false, false, '', 50, true, ''),
  ('siddhivinayak', '6jL-hYt9dGc', '', 'Mayur Bhakti', false, false, '', 60, true, ''),
  ('ram-lalla', '6wiU9W9--60', '', 'Ds Bhakti Marg', false, false, '', 10, true, ''),
  ('ram-lalla', 'ezjTFDQOrEY', '', 'Ds Bhakti Marg', false, false, '', 20, true, ''),
  ('ram-lalla', 'pSbx_Sor6dw', '', 'Maheshwari Bhakti', false, false, '', 30, true, ''),
  ('ram-lalla', '8B46HB7mtTc', '', 'Divine Spiritual', false, false, '', 40, true, ''),
  ('mahakal', '-BdYhAVkykw', '', 'divyadarshan', false, false, '', 10, true, ''),
  ('mahakal', 'XFiHN4mH87A', '', 'divyadarshan', false, false, '', 20, true, ''),
  ('mahakal', '_50-xFwoWWw', '', 'divyadarshan', false, false, '', 30, true, ''),
  ('mahakal', 'rkx_ACL7PkU', '', 'prakritibhajan', false, false, '', 40, true, ''),
  ('shirdi', 'JUwyHAJ4aVE', '', 'Sai Ki Mahima with Aushim Khetarpal', false, false, '', 10, true, ''),
  ('shirdi', 'qTVJUJZCzGU', '', 'Sai Baba Live Darshan', false, false, '', 20, true, ''),
  ('shirdi', 'iVlf_a-XhiU', '', 'Maag Music', false, false, '', 30, true, ''),
  ('iskcon-bengaluru', '8lllk7ivf0Y', '', 'ISKCON Bangalore', true, true, '', 10, true, ''),
  ('laxmi-narayan', 'IIuk2Y9dqHc', '', 'VADTAL MANDIR', true, true, 'vadtal', 10, true, ''),
  ('laxmi-narayan', 'P32yfttB2L0', '', 'VADTAL MANDIR', true, false, '', 20, true, ''),
  ('kashi', 'Hm7ASpZy0fo', '', 'Arti sangarh', false, false, '', 10, true, ''),
  ('kashi', 'HnYIKaIVhWQ', '', 'Shubh Darshan India', false, false, '', 20, true, ''),
  ('kashi', 'kKxi2BMDyYE', '', 'Live Darshan', false, false, '', 30, true, ''),
  ('kashi', 'J0nRdUOQps8', '', 'Bhakti Preet', false, false, '', 40, true, ''),
  ('mahavir-patna', 'R5J87Q5y1MM', '', 'BHAKTI LIVE', false, false, '', 10, true, ''),
  ('pashupatinath-mandsaur', 'o4EhGSikFfg', '', 'Krishna Gyan Sagar', false, false, '', 10, false,
    'Title says Sawan 2026 in late September. Likely a looped recording.'),
  ('harni-hanuman', 'ir8ydZrL19w', '', 'Harni Hanumanji Official', true, true, '', 10, true, ''),
  ('puliyakulam-vinayagar', 'fwINCxFyifU', '', 'Aalaya Magimai Tv', false, false, '', 10, true,
    'Festival broadcast. May end after the event.'),
  ('radha-rani-barsana', 'T2WQEZCEiE8', '', 'Shyam Mangal Gaan', false, false, '', 10, true, ''),
  ('radha-rani-barsana', 'vNykMXzpDJw', '', 'Supertone Digital', false, false, '', 20, true, ''),
  ('vrindavan-chandrodaya', 'qA9fFxvKetc', '', 'Vrindavan Chandrodaya Mandir', true, true, '', 10, true, ''),
  ('kedarnath', 'TLr6s02q2X0', '', 'Swarg Marg', false, false, '', 10, true, ''),
  ('kedarnath', '7NtwzA3WQzU', '', 'Swarg Marg', false, false, '', 20, true, ''),
  ('krishna-janmabhoomi', '8tXVauzNY8M', '', 'Solotune Bhakti Dhara', false, false, '', 10, false,
    'Title says Janmashtami 2026 after the festival. Likely a looped recording.'),
  ('narnarayan-kalupur', 'OhX67OEayv8', '', 'Kalupur Mandir', true, true, 'kalupur', 10, true, ''),
  ('naga-sai', 'QEYD_Mo9-yE', '', 'Sri Naga Sai Mandir Live', true, true, '', 10, true, ''),
  ('khatu-shyam', 'cd_dNNKX9Ds', '', 'Shyam Bhakti Rang', false, false, '', 10, false,
    'Morning aarti title while live at night. Likely a looped recording.'),
  ('baidyanath', 'MO4aPrUvq1Y', '', 'KGS Astro', false, false, '', 10, true, ''),
  ('baidyanath', '4kWiCyMZhq4', '', 'KGS Astro', false, false, '', 20, true, ''),
  ('sawariya-seth', 'H3I-I3ExQCg', '', 'Live Darshan Sawariya Seth Ladusa', false, false, '', 10, true, ''),
  ('chhapaiya', 'e-SxaP4wxPg', '', 'Kalupur Mandir', true, true, 'chhapaiya', 10, true, ''),
  ('narnarayan-bhuj', '2TTW4nQb-pw', '', 'Bhuj Mandir', true, true, 'ghanshyam', 10, true, ''),
  ('narnarayan-bhuj', 'GA_kh_SSQbw', '', 'Bhuj Mandir', true, false, '', 50, true, 'Sabha mandap camera.'),
  ('rapar', 'P2B8_y48Ofk', '', 'Bhuj Mandir', true, true, 'rapar', 10, true, ''),
  ('narayanpura', 'iOgcaHVLom8', '', 'Swaminarayan Darshan', false, false, '', 10, true, ''),
  ('muli', 'N9v8BR71JLw', '', 'Swaminarayan Darshan', false, false, '', 10, true, ''),
  ('sardhar', 'M_2GBoS9Xr4', '', 'Sardhar Katha', false, false, '', 10, true, '')
) as v (mandir_id, video_id, channel_id, channel_name, official, follow_channel,
  title_match, priority, enabled, note)
where exists (select 1 from public.mandirs m where m.id = v.mandir_id)
  and not exists (select 1 from public.live_sources s where s.mandir_id = v.mandir_id);
