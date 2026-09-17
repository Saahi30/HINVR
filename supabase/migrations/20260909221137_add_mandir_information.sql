-- Editorial and visitor information managed by the HINVR desk.

alter table public.mandirs
  add column if not exists deity text not null default '',
  add column if not exists summary text not null default '',
  add column if not exists history text not null default '',
  add column if not exists significance text not null default '',
  add column if not exists architecture text not null default '',
  add column if not exists dress_code text not null default '',
  add column if not exists best_time text not null default '',
  add column if not exists visitor_notes text not null default '',
  add column if not exists facilities text not null default '',
  add column if not exists address text not null default '',
  add column if not exists official_website text not null default '',
  add column if not exists contact_phone text not null default '';

update public.mandirs
set
  deity = 'Sri Venkateswara',
  summary = 'A hill sanctuary at Tirumala and one of India''s most visited places of worship.',
  history = 'The present temple complex grew through the patronage of South Indian dynasties and later Vijayanagara rulers. Its living ritual tradition is older than many of the structures seen today.',
  significance = 'Devotees worship Sri Venkateswara as a form of Vishnu who offers refuge in the present age. The darshan, laddu prasadam, and tonsure vows are central to the Tirumala pilgrimage.',
  architecture = 'Dravidian temple architecture with a gilded Ananda Nilayam vimana, monumental gopurams, pillared halls, and layered prakaram corridors.',
  dress_code = 'Traditional or modest clothing. Dhoti or pyjama with upper cloth for men; saree, half-saree, or churidar for women is encouraged for sevas.',
  best_time = 'Weekday mornings outside major festivals. Expect heavy crowds throughout the year.',
  visitor_notes = 'Use only official TTD booking channels. Phones, cameras, footwear, and luggage are restricted inside.',
  facilities = 'Official special-entry booking\nFree buses around Tirumala\nLuggage and footwear counters\nWheelchair assistance\nLaddu prasadam counters',
  address = 'Tirumala, Tirupati, Andhra Pradesh 517504',
  official_website = 'https://www.tirumala.org/',
  contact_phone = '1800 425 4141'
where id = 'tirupati' and summary = '';

update public.mandirs
set
  deity = 'Shri Vishwanath',
  summary = 'A jyotirlinga shrine in the sacred lanes of Kashi, close to the Ganga and Dashashwamedh Ghat.',
  history = 'The shrine has been rebuilt across centuries. The present structure dates to the eighteenth century under Ahilyabai Holkar, with later additions including its celebrated gold-plated spires.',
  significance = 'Kashi is revered as Shiva''s city. Pilgrims combine Vishwanath darshan with Ganga snan, Annapurna darshan, and rites for ancestors.',
  architecture = 'A compact North Indian complex with gold-clad shikharas, stone courtyards, and the contemporary Kashi Vishwanath Dham corridor connecting toward the ghats.',
  dress_code = 'Modest clothing with shoulders and knees covered. Leather items and electronics may need to be left outside.',
  best_time = 'Mangala Aarti with an official booking, or early weekday mornings before the lanes fill.',
  visitor_notes = 'Carry accepted photo ID. Security is strict; use official lockers and follow the current gate instructions.',
  facilities = 'Official lockers\nWheelchair access through selected gates\nDrinking water\nPrasad counters\nGanga corridor access',
  address = 'Lahori Tola, Varanasi, Uttar Pradesh 221001',
  official_website = 'https://shrikashivishwanath.org/',
  contact_phone = ''
where id = 'kashi' and summary = '';

update public.mandirs
set
  deity = 'Shri Sai Baba',
  summary = 'The samadhi shrine of Sai Baba in Shirdi, built around a tradition of faith, patience, and service.',
  history = 'Sai Baba lived in Shirdi in the late nineteenth and early twentieth centuries. The Samadhi Mandir developed around the site where his mortal remains were interred in 1918.',
  significance = 'The shrine welcomes devotees across traditions. Kakad, Madhyan, Dhup, and Shej aartis structure the devotional day.',
  architecture = 'A marble samadhi hall with a silver throne, connected courtyards, Dwarkamai, Chavadi, and other places associated with Sai Baba''s life.',
  dress_code = 'Clean, modest clothing. Cover shoulders and knees and observe the separate security and queue instructions.',
  best_time = 'Weekday mornings outside Thursday, public holidays, and festival periods.',
  visitor_notes = 'Use the Sansthan''s official portal for darshan and aarti booking. Ignore unofficial paid live-darshan claims.',
  facilities = 'Official darshan booking\nPrasadalaya\nAccommodation desk\nLuggage and footwear counters\nWheelchair assistance',
  address = 'Mauli Nagar, Shirdi, Maharashtra 423109',
  official_website = 'https://sai.org.in/',
  contact_phone = ''
where id = 'shirdi' and summary = '';

update public.mandirs
set
  deity = 'Lord Shiva',
  summary = 'A Himalayan jyotirlinga shrine beneath the Kedarnath massif, reached through a demanding seasonal pilgrimage.',
  history = 'The stone shrine is associated with the Pandavas in tradition and with Adi Shankaracharya in its later revival. It has endured the severe climate and the 2013 floods.',
  significance = 'Kedarnath is one of the twelve jyotirlingas and a principal stop in the Uttarakhand Char Dham.',
  architecture = 'Massive grey stone blocks, a pyramidal tower, and a compact mandapa designed for the high-altitude Himalayan setting.',
  dress_code = 'Warm layered clothing, rain protection, sturdy footwear, and modest temple attire.',
  best_time = 'Late May to June or September to closing, subject to official opening dates and weather.',
  visitor_notes = 'High altitude and weather are serious constraints. Check medical fitness, official advisories, and helicopter or trek rules before travel.',
  facilities = 'Seasonal registration\nTrek and helicopter services\nMedical posts\nPony and palanquin services\nLimited accessibility',
  address = 'Kedarnath, Rudraprayag, Uttarakhand 246445',
  official_website = 'https://badrinath-kedarnath.gov.in/',
  contact_phone = ''
where id = 'kedarnath' and summary = '';

update public.mandirs
set
  deity = 'Lord Shiva',
  summary = 'A sea-facing jyotirlinga at Prabhas Patan, rebuilt as a symbol of continuity on Gujarat''s western coast.',
  history = 'Somnath is remembered through repeated destruction and rebuilding. The present temple was completed in the twentieth century in the Maru-Gurjara tradition.',
  significance = 'Revered as the first among the twelve jyotirlingas, Somnath is also associated with the sacred confluence at Prabhas.',
  architecture = 'A soaring sandstone shikhara, carved mandapas, axial sea view, and the famous arrow pillar pointing toward the unobstructed southern ocean.',
  dress_code = 'Modest clothing. Electronics and bags may be restricted by current security rules.',
  best_time = 'October to March; arrive before sunset for the coastal setting and evening aarti.',
  visitor_notes = 'Check official timings for the evening light-and-sound programme. Photography rules change by zone.',
  facilities = 'Prasad counters\nWheelchair access\nCloakroom\nParking\nLight-and-sound programme',
  address = 'Somnath Mandir Road, Prabhas Patan, Gujarat 362268',
  official_website = 'https://somnath.org/',
  contact_phone = ''
where id = 'somnath' and summary = '';
