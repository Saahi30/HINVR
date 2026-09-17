-- Attach LiveBhagwan YouTube darshan streams to existing mandirs,
-- and insert mandirs that were missing from the catalog.

insert into public.mandirs (
  id, name, place, city, scene, photo_url, live, vr, pass_accepted, next_aarti,
  timings, updated_label, live_url, deity, summary, address, official_website,
  sort_order, published
)
values
  (
    'kashi', 'Kashi Vishwanath', 'Varanasi, Uttar Pradesh', 'Kashi', 'Kashi',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/VishwanathThumbnail.jpg', true, true, true, '4:30 PM',
    'Mangala 3:00 AM · Saptarishi aarti 7:00 PM', 'Updated just now', 'https://www.youtube.com/watch?v=J0nRdUOQps8',
    'Shri Vishwanath', 'A jyotirlinga shrine in the sacred lanes of Kashi, beside the Ganga.', 'Varanasi, Uttar Pradesh', '',
    20, true
  ),
  (
    'shirdi', 'Sai Baba', 'Shirdi, Maharashtra', 'Shirdi', 'Shirdi',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/1661929612732_saibaba.jpeg', true, false, true, null,
    'Kakad aarti 5:00 AM · Shej aarti 10:00 PM', 'Updated just now', 'https://www.youtube.com/watch?v=6Uf3aMujHa8',
    'Shri Sai Baba', 'The samadhi shrine of Sai Baba, centred on faith, patience, and service.', 'Shirdi, Maharashtra', '',
    30, true
  ),
  (
    'somnath', 'Somnath', 'Gir Somnath, Gujarat', 'Somnath', 'Somnath',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/1705928484187_Somnath-Temple-Photo.jpg', true, false, true, null,
    'Mangala 6:00 AM · Aarti 7:00 PM', 'Updated just now', 'https://www.youtube.com/watch?v=J4z7CIrvsuw',
    'Lord Shiva', 'A sea-facing jyotirlinga at Prabhas Patan, rebuilt as a symbol of continuity.', 'Gir Somnath, Gujarat', '',
    50, true
  ),
  (
    'jagannath', 'Jagannath Temple', 'Puri, Odisha', 'Puri', 'Somnath',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/1765787601725_JagnathJi360_420.png', true, false, false, '6:00 PM',
    'Open 5:30 AM–9:30 PM · Timings vary by ritual', 'Updated just now', 'https://www.youtube.com/watch?v=oX0HEkwdWqo',
    'Lord Jagannath', 'Jagannath Temple is a living place of worship in Puri, Odisha, with timings and visitor guidance kept current by the HINVR desk.', 'Puri, Odisha', '',
    80, true
  ),
  (
    'dwarkadhish', 'Dwarkadhish', 'Dwarka, Gujarat', 'Dwarka', 'Somnath',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/dwarkadhish-shringar-darshan.jpg', true, false, true, '7:30 PM',
    'Morning 6:30 AM–1:00 PM · Evening 5:00–9:30 PM', 'Updated just now', 'https://www.youtube.com/watch?v=E7tDjVkkHJ4',
    'Lord Krishna', 'Dwarkadhish is a living place of worship in Dwarka, Gujarat, with timings and visitor guidance kept current by the HINVR desk.', 'Dwarka, Gujarat', '',
    90, true
  ),
  (
    'siddhivinayak', 'Siddhivinayak', 'Mumbai, Maharashtra', 'Mumbai', 'Shirdi',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/1661936121433_siddhivinayak.jpeg', true, false, true, '7:30 PM',
    'Wednesday–Monday 5:30 AM–9:50 PM · Tuesday from 3:15 AM', 'Updated just now', 'https://www.youtube.com/watch?v=1vGVGya72bQ',
    'Lord Ganesha', 'Siddhivinayak is a living place of worship in Mumbai, Maharashtra, with timings and visitor guidance kept current by the HINVR desk.', 'Mumbai, Maharashtra', '',
    120, true
  ),
  (
    'mahakal', 'Mahakaleshwar', 'Ujjain, Madhya Pradesh', 'Ujjain', 'Kashi',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/1739895956583_mahakal_bg.jpg', true, false, true, null,
    'Bhasma aarti before dawn · Darshan through the day', 'Updated just now', 'https://www.youtube.com/watch?v=vOIx1wXmc7I',
    'Lord Mahakal', 'A jyotirlinga in Ujjain, known for the pre-dawn Bhasma aarti and a living Shaiva city around the shrine.', 'Mahakaleshwar Temple Road, Ujjain, Madhya Pradesh 456006', 'https://shreemahakaleshwar.com/',
    130, true
  ),
  (
    'mahalakshmi', 'Mahalakshmi', 'Mumbai, Maharashtra', 'Mumbai', 'Tirupati',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/mahalaxmi-mumbai.png', true, false, false, null,
    'Morning and evening aarti · Darshan through the day', 'Updated just now', 'https://www.youtube.com/watch?v=DHRoHpI_rcI',
    'Mahalakshmi', 'Mahalakshmi is a living place of worship in Mumbai, Maharashtra, with timings and visitor guidance kept current by the HINVR desk.', 'Mumbai, Maharashtra', '',
    140, true
  ),
  (
    'jagannath-ahmedabad', 'Jagannathji', 'Jamalpur, Ahmedabad', 'Ahmedabad', 'Somnath',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/1783929583797_JagannathRathYatra.png', true, false, false, '6:00 PM',
    'Morning and evening aarti · Rath Yatra in Ashadha', 'Updated just now', 'https://www.youtube.com/watch?v=2qsfeM6lInw',
    'Lord Jagannath', 'Jagannathji is a living place of worship in Jamalpur, Ahmedabad, with timings and visitor guidance kept current by the HINVR desk.', 'Jamalpur, Ahmedabad', '',
    150, true
  ),
  (
    'mohankheda', 'Mohankheda', 'Dhar, Madhya Pradesh', 'Dhar', 'Kedarnath',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/1723659948051_mohankheda.png', true, false, false, null,
    'Daily darshan · Jain pilgrimage site', 'Updated just now', 'https://www.youtube.com/watch?v=mKJ9J7J7tUM',
    'Acharya Rajendra Suri', 'Mohankheda is a Digambar Jain pilgrimage site in Dhar, Madhya Pradesh, associated with Acharya Rajendra Suri.', 'Dhar, Madhya Pradesh', '',
    160, true
  ),
  (
    'ashapura-mata', 'Ashapura Mata', 'Lakhpat, Kutch', 'Lakhpat', 'Kedarnath',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/1723659373125_ashapuramata.jpeg', true, false, false, null,
    'Morning and evening aarti', 'Updated just now', 'https://www.youtube.com/watch?v=SEQ3p6hELo8',
    'Ashapura Mata', 'Ashapura Mata is a living place of worship in Lakhpat, Kutch, with timings and visitor guidance kept current by the HINVR desk.', 'Lakhpat, Kutch', '',
    170, true
  ),
  (
    'sarangpur', 'Kashtbhanjan Dev', 'Sarangpur, Gujarat', 'Sarangpur', 'Shirdi',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/salangpurThumbnail.jpeg', true, false, false, null,
    'Hanuman darshan through the day · Aarti morning and evening', 'Updated just now', 'https://www.youtube.com/watch?v=KomUNKwJu6c',
    'Kashtbhanjan Hanuman', 'Kashtbhanjan Dev is a living place of worship in Sarangpur, Gujarat, with timings and visitor guidance kept current by the HINVR desk.', 'Sarangpur, Gujarat', '',
    180, true
  ),
  (
    'vadtal', 'Laxminarayan Dev', 'Vadtal, Gujarat', 'Vadtal', 'Tirupati',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/1780044517234_vadtaldham.png', true, false, false, null,
    'Mangala aarti at dawn · Shringar and evening aarti', 'Updated just now', 'https://www.youtube.com/watch?v=IIuk2Y9dqHc',
    'Laxminarayan Dev', 'The Swaminarayan seat at Vadtal, with daily darshan of Laxminarayan Dev and Harikrishna Maharaj.', 'Vadtal, Gujarat', '',
    190, true
  ),
  (
    'khatu-shyam', 'Khatu Shyam', 'Khatu, Rajasthan', 'Sikar', 'Kashi',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/1780047009461_khatushyamji.png', true, false, true, null,
    'Mangala aarti at dawn · Darshan through the evening', 'Updated just now', 'https://www.youtube.com/watch?v=dke2IQtIfUM',
    'Khatu Shyam', 'The shrine of Barbarika at Khatu in Sikar district, worshipped as Shyam, a form of Krishna''s grace.', 'Khatu Shyam Mandir, Khatu, Sikar, Rajasthan 332602', '',
    200, true
  ),
  (
    'narnarayan-bhuj', 'NarNarayan Dev Bhuj', 'Bhuj, Kutch', 'Bhuj', 'Tirupati',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/1780048186962_ShreeSwaminarayanMandir-Bhuj.png', true, false, false, null,
    'Mangala aarti · Shringar darshan · Evening aarti', 'Updated just now', 'https://www.youtube.com/watch?v=oJ8nu5DDRoc',
    'NarNarayan Dev', 'The historic Swaminarayan mandir in Bhuj, with daily aarti and shringar darshan.', 'Bhuj, Kutch', '',
    210, true
  ),
  (
    'narnarayan-kalupur', 'NarNarayan Dev Kalupur', 'Kalupur, Ahmedabad', 'Kalupur', 'Tirupati',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/1780050208861_KalupurSwaminarayanMandir.png', true, false, false, null,
    'Mangala aarti · Shringar darshan · Evening aarti', 'Updated just now', 'https://www.youtube.com/watch?v=5ppU7BqHdvs',
    'NarNarayan Dev', 'The first Swaminarayan mandir, Kalupur Ahmedabad, seat of the NarNarayan Dev Gadi.', 'Kalupur, Ahmedabad', '',
    220, true
  ),
  (
    'gopinathji-gadhada', 'Gopinathji', 'Gadhada, Gujarat', 'Gadhada', 'Tirupati',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/1780050959554_GadhpurDham.png', true, false, false, null,
    'Daily aarti and shringar darshan at Gadhpur Dham', 'Updated just now', 'https://www.youtube.com/watch?v=xWi4BUuDb7k',
    'Gopinathji Maharaj', 'Gadhpur Dham in Gadhada, where Swaminarayan resided, with daily darshan of Gopinathji.', 'Gadhada, Gujarat', '',
    230, true
  ),
  (
    'ranchhodraiji-dakor', 'Ranchhodraiji', 'Dakor, Gujarat', 'Dakor', 'Somnath',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/1780051497234_ShriRanchhodraiji.png', true, false, false, null,
    'Mangala aarti · Shringar darshan · Evening aarti', 'Updated just now', 'https://www.youtube.com/watch?v=sylr5qU9uoU',
    'Ranchhodraiji', 'The Krishna shrine at Dakor, Gujarat, known for Ranchhodrai darshan and aarti.', 'Dakor, Gujarat', '',
    240, true
  ),
  (
    'radha-govinda-hyderabad', 'Radha-Govinda', 'Banjara Hills, Hyderabad', 'Hyderabad', 'Tirupati',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/1780052590256_HareKrishnaGoldenTemple.png', true, false, false, null,
    'Mangala aarti at dawn · Kirtan and evening aarti', 'Updated just now', 'https://www.youtube.com/watch?v=TP7KeclRHcc',
    'Radha-Govinda', 'The Hare Krishna Golden Temple in Banjara Hills, with daily darshan of Sri Sri Radha-Govinda.', 'Banjara Hills, Hyderabad', '',
    250, true
  ),
  (
    'ram-lalla', 'Ram Lalla', 'Ayodhya, Uttar Pradesh', 'Ayodhya', 'Kashi',
    'https://storage.googleapis.com/livebhagwan-7dd31.appspot.com/1780053121561_ramlalla.png', true, false, true, null,
    'Mangala aarti at dawn · Shringar and evening aarti', 'Updated just now', 'https://www.youtube.com/watch?v=xThGBtk3E_A',
    'Ram Lalla', 'The Janmabhoomi shrine in Ayodhya, built around the childhood form of Sri Rama.', 'Ram Janmabhoomi, Ayodhya, Uttar Pradesh 224123', 'https://srjbtkshetra.org/',
    260, true
  )
on conflict (id) do update set
  live = excluded.live,
  live_url = excluded.live_url,
  updated_label = excluded.updated_label,
  photo_url = case
    when public.mandirs.photo_url = '' then excluded.photo_url
    else public.mandirs.photo_url
  end,
  deity = case
    when public.mandirs.deity = '' then excluded.deity
    else public.mandirs.deity
  end,
  summary = case
    when public.mandirs.summary = '' then excluded.summary
    else public.mandirs.summary
  end,
  address = case
    when public.mandirs.address = '' then excluded.address
    else public.mandirs.address
  end,
  official_website = case
    when public.mandirs.official_website = '' then excluded.official_website
    else public.mandirs.official_website
  end,
  timings = case
    when public.mandirs.timings = '' then excluded.timings
    else public.mandirs.timings
  end;
