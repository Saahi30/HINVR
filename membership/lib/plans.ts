// Amounts must match public.membership_prices (the server prices every request from that table).
// Every plan, NRI included, is invoiced in rupees.
export const PLANS = [
  {
    name: "Darshan",
    tier: "Darshan",
    price: "₹999",
    amount: 999,
    audience: "For watching and planning",
    benefits: ["Official live darshan", "Mandir directory and favourites", "Basic concierge requests"],
  },
  {
    name: "Gold",
    tier: "Gold",
    price: "₹4,999",
    amount: 4999,
    audience: "For parents and regular visits",
    benefits: [
      "Everything in Darshan",
      "Digital QR temple pass",
      "Visit and accessibility requests",
      "Priority concierge chat",
    ],
  },
  {
    name: "Platinum",
    tier: "Platinum",
    price: "₹14,999",
    amount: 14999,
    audience: "For families needing a human desk",
    benefits: [
      "Everything in Gold",
      "Phone concierge",
      "Family profile support",
      "Physical card request",
      "Partner Priority Assist",
    ],
  },
  {
    name: "NRI",
    tier: "Nri",
    price: "₹12,499",
    amount: 12499,
    audience: "For family abroad",
    benefits: ["Everything in Platinum", "International support hours"],
  },
] as const;
