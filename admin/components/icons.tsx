export function IconGrid({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="none" className={className} aria-hidden>
      <rect x="1.5" y="1.5" width="5.5" height="5.5" rx="1" stroke="currentColor" />
      <rect x="9" y="1.5" width="5.5" height="5.5" rx="1" stroke="currentColor" />
      <rect x="1.5" y="9" width="5.5" height="5.5" rx="1" stroke="currentColor" />
      <rect x="9" y="9" width="5.5" height="5.5" rx="1" stroke="currentColor" />
    </svg>
  );
}

export function IconTemple({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="none" className={className} aria-hidden>
      <path d="M8 1.5 2.5 6h11L8 1.5Z" stroke="currentColor" strokeLinejoin="round" />
      <path d="M4 6.5v6.5M8 6.5V13M12 6.5V13M2.5 13h11" stroke="currentColor" />
    </svg>
  );
}

export function IconHome({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="none" className={className} aria-hidden>
      <path d="M2.5 7.2 8 2.5l5.5 4.7V13a.5.5 0 0 1-.5.5H3a.5.5 0 0 1-.5-.5V7.2Z" stroke="currentColor" />
      <path d="M6.5 13.5v-4h3v4" stroke="currentColor" />
    </svg>
  );
}

export function IconUsers({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="none" className={className} aria-hidden>
      <circle cx="6" cy="5.5" r="2" stroke="currentColor" />
      <path d="M2.5 13c.3-2.2 1.8-3.5 3.5-3.5S9.2 10.8 9.5 13" stroke="currentColor" />
      <circle cx="11" cy="6" r="1.6" stroke="currentColor" />
      <path d="M10.2 9.6c1.4.2 2.5 1.2 2.8 3.4" stroke="currentColor" />
    </svg>
  );
}

export function IconShield({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="none" className={className} aria-hidden>
      <path d="M8 1.5 3 3.5v4.2c0 3 2 5.2 5 6.8 3-1.6 5-3.8 5-6.8V3.5L8 1.5Z" stroke="currentColor" strokeLinejoin="round" />
    </svg>
  );
}

export function IconMenu({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="none" className={className} aria-hidden>
      <path d="M2.5 4h11M2.5 8h11M2.5 12h11" stroke="currentColor" strokeLinecap="round" />
    </svg>
  );
}

export function IconClose({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="none" className={className} aria-hidden>
      <path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" strokeLinecap="round" />
    </svg>
  );
}
