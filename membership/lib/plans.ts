export const PLANS = [
  {
    name: "Darshan",
    price: "₹999",
    amount: 999,
    audience: "For watching and planning",
    benefits: ["Official live darshan", "Mandir directory and favourites", "Basic concierge requests"],
  },
  {
    name: "Gold",
    price: "₹4,999",
    amount: 4999,
    audience: "For parents and regular visits",
    benefits: [
      "Everything in Darshan",
      "Digital QR temple pass",
      "Visit and accessibility requests",
      "Priority concierge chat",
    ],
    recommended: true,
  },
  {
    name: "Platinum",
    price: "₹14,999",
    amount: 14999,
    audience: "For families needing a human desk",
    benefits: [
      "Everything in Gold",
      "Phone concierge",
      "Family profile support",
      "Physical card request",
      "Partner assist priority",
    ],
  },
  {
    name: "NRI",
    price: "$149",
    amount: 12499,
    audience: "For family abroad",
    benefits: ["Everything in Platinum", "International support hours"],
  },
] as const;
