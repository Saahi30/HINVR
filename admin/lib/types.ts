export type StaffRole = "owner" | "editor";

export type Staff = {
  user_id: string;
  email: string;
  role: StaffRole;
};

export type Mandir = {
  id: string;
  name: string;
  place: string;
  city: string;
  scene: string;
  photo_url: string;
  live: boolean;
  vr: boolean;
  pass_accepted: boolean;
  next_aarti: string | null;
  timings: string;
  updated_label: string;
  live_url: string;
  vr_url: string;
  deity: string;
  summary: string;
  history: string;
  significance: string;
  architecture: string;
  dress_code: string;
  best_time: string;
  visitor_notes: string;
  facilities: string;
  address: string;
  official_website: string;
  contact_phone: string;
  sort_order: number;
  published: boolean;
  updated_at?: string;
};

export type HomeService = {
  id: string;
  title: string;
  benefit: string;
  photo_url: string;
  route: string;
  scene: string;
  tall: boolean;
  sort_order: number;
  published: boolean;
};

export type HomeSettings = {
  headline: string;
  eyebrow: string;
};

export type MemberRow = {
  id: string;
  display_name: string;
  city: string;
  tier: string;
  member_id: string;
  valid_until: string;
  audience: string;
};

export const MANDIR_SCENES = [
  "Tirupati",
  "Kashi",
  "Shirdi",
  "Kedarnath",
  "Somnath",
] as const;

export const SERVICE_ROUTES = [
  { value: "live", label: "Live Darshan" },
  { value: "vr", label: "VR Darshan" },
  { value: "pass", label: "Priority Pass" },
  { value: "pooja", label: "Book Pandit" },
  { value: "concierge", label: "Concierge" },
  { value: "yatra", label: "Yatra" },
] as const;

export const SERVICE_SCENES = [
  "LiveAarti",
  "VrHall",
  "PassDesk",
  "PanditDoor",
  "ConciergeDesk",
  "YatraRoad",
] as const;

export const emptyMandir = (sortOrder = 100): Mandir => ({
  id: "",
  name: "",
  place: "",
  city: "",
  scene: "Tirupati",
  photo_url: "",
  live: false,
  vr: false,
  pass_accepted: false,
  next_aarti: "",
  timings: "",
  updated_label: "Updated just now",
  live_url: "",
  vr_url: "",
  deity: "",
  summary: "",
  history: "",
  significance: "",
  architecture: "",
  dress_code: "",
  best_time: "",
  visitor_notes: "",
  facilities: "",
  address: "",
  official_website: "",
  contact_phone: "",
  sort_order: sortOrder,
  published: true,
});
