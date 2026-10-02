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
  live_checked_at?: string | null;
  updated_at?: string;
};

export type LiveSource = {
  id: string;
  mandir_id: string;
  video_id: string;
  channel_id: string;
  channel_name: string;
  official: boolean;
  follow_channel: boolean;
  title_match: string;
  priority: number;
  enabled: boolean;
  note: string;
  last_checked_at: string | null;
  last_live: boolean;
  last_title: string;
  last_viewers: number | null;
  last_error: string;
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

export const YOUTUBE_DAILY_LIMIT = 10_000;

export type YoutubeQuota = {
  day: string;
  used: number;
  limit: number;
  runs: number;
  last?: {
    at: string;
    units: number;
    videos: number;
    playlists: number;
    channels: number;
    searches: number;
    live?: number;
    replaced?: number;
  };
};

export type MemberPlace = {
  label: string;
  address: string;
};

export type MemberRow = {
  id: string;
  display_name: string;
  city: string;
  tier: string;
  member_id: string;
  valid_until: string;
  audience: string;
  profile_complete: boolean;
  phone_e164: string;
  addresses: MemberPlace[];
  updated_at?: string;
  purchase_id?: string;
  invoice_number?: string;
  amount_inr?: number | null;
  purchased_on?: string;
  valid_from?: string;
  invoice_valid_until?: string;
};

export const DESK_KINDS = ["VISIT", "CONCIERGE", "POOJA", "YATRA"] as const;
export type DeskRequestKind = (typeof DESK_KINDS)[number];

export const DESK_STATUSES = ["new", "open", "done"] as const;
export type DeskRequestStatus = (typeof DESK_STATUSES)[number];

export type DeskRequest = {
  id: string;
  user_id: string;
  kind: DeskRequestKind;
  summary: string;
  city: string;
  mandir_id: string | null;
  status: DeskRequestStatus;
  staff_note: string;
  created_at: string;
  updated_at?: string;
};

export type DeskRequestRow = DeskRequest & {
  display_name: string;
  phone_e164: string;
  tier: string;
};

export type NoticeRow = {
  id: string;
  title: string;
  body: string;
  sent_count: number;
  created_at: string;
};

export type BuyRequestStatus = "pending" | "approved" | "declined";

export type BuyRequestRow = {
  id: string;
  user_id: string;
  tier: string;
  amount_inr: number;
  status: BuyRequestStatus;
  staff_note: string;
  created_at: string;
  display_name: string;
  city: string;
  phone_e164: string;
  member_id: string;
  current_tier: string;
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
