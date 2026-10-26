import Link from "next/link";
import { Badge, Card, cx } from "@/components/ui";
import {
  DEDICATION_LABELS,
  HANDOFF_LABELS,
  INSIGHT_PERIODS,
  OFFERING_LABELS,
  fillDays,
  percent,
  ranked,
  type InsightPeriod,
} from "@/lib/insights";
import { formatWhen } from "@/lib/ops";
import type { Insights, InsightsDay, VrCrash } from "@/lib/types";

export function PeriodSwitch({ current }: { current: InsightPeriod }) {
  return (
    <div className="flex gap-2">
      {INSIGHT_PERIODS.map((days) => (
        <Link
          key={days}
          href={`/insights?days=${days}`}
          aria-current={days === current ? "page" : undefined}
          className={
            days === current
              ? "rounded-md bg-zinc-900 px-3 py-2 text-sm text-white"
              : "rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-600"
          }
        >
          {days} days
        </Link>
      ))}
    </div>
  );
}

function Tile({ label, value, hint }: { label: string; value: number | string; hint: string }) {
  return (
    <Card className="px-4 py-3">
      <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold tabular-nums">{value}</p>
      <p className="mt-1 text-xs text-zinc-500">{hint}</p>
    </Card>
  );
}

function Section({ title, hint, children }: { title: string; hint?: string; children: React.ReactNode }) {
  return (
    <Card className="overflow-hidden">
      <div className="border-b border-zinc-200 px-4 py-3">
        <h2 className="text-sm font-semibold">{title}</h2>
        {hint ? <p className="mt-0.5 text-xs text-zinc-500">{hint}</p> : null}
      </div>
      <div className="px-4 py-3">{children}</div>
    </Card>
  );
}

function Empty({ children }: { children: React.ReactNode }) {
  return <p className="py-2 text-sm text-zinc-500">{children}</p>;
}

/** Offerings per India day, one bar each; hover a bar for the day's breakdown. */
function DailyBars({ days }: { days: InsightsDay[] }) {
  const max = Math.max(1, ...days.map((d) => d.total));
  const width = 720;
  const height = 140;
  const gap = days.length > 40 ? 1 : 3;
  const bar = (width - gap * (days.length - 1)) / days.length;
  return (
    <svg viewBox={`0 0 ${width} ${height + 18}`} className="h-44 w-full" role="img" aria-label="Offerings per day">
      {days.map((d, i) => {
        const h = d.total === 0 ? 1.5 : Math.max(3, (d.total / max) * height);
        const x = i * (bar + gap);
        const breakdown = ranked(d.kinds)
          .map(([kind, n]) => `${OFFERING_LABELS[kind] ?? kind} ${n}`)
          .join(", ");
        return (
          <g key={d.day}>
            <rect
              x={x}
              y={height - h}
              width={bar}
              height={h}
              rx={Math.min(2, bar / 3)}
              className={d.total === 0 ? "fill-zinc-200" : "fill-amber-500"}
            >
              <title>{`${d.day}: ${d.total} offerings${breakdown ? ` (${breakdown})` : ""}`}</title>
            </rect>
            {i === 0 || i === days.length - 1 ? (
              <text
                x={i === 0 ? x : x + bar}
                y={height + 14}
                textAnchor={i === 0 ? "start" : "end"}
                className="fill-zinc-500 text-[11px]"
              >
                {d.day.slice(5)}
              </text>
            ) : null}
          </g>
        );
      })}
    </svg>
  );
}

/** Horizontal share bars for a count map. */
function Shares({ counts, labels, empty }: { counts: Record<string, number>; labels: Record<string, string>; empty: string }) {
  const rows = ranked(counts);
  const total = rows.reduce((sum, [, n]) => sum + n, 0);
  if (total === 0) return <Empty>{empty}</Empty>;
  return (
    <ul className="space-y-2">
      {rows.map(([key, n]) => (
        <li key={key}>
          <div className="flex justify-between text-sm">
            <span>{labels[key] ?? key}</span>
            <span className="tabular-nums text-zinc-500">
              {n} · {percent(n, total)}%
            </span>
          </div>
          <div className="mt-1 h-1.5 rounded-full bg-zinc-100">
            <div className="h-1.5 rounded-full bg-amber-500" style={{ width: `${Math.max(2, percent(n, total))}%` }} />
          </div>
        </li>
      ))}
    </ul>
  );
}

function Crashes({ rows }: { rows: VrCrash[] }) {
  if (rows.length === 0) {
    return <Empty>No crash reports. A headset sends one, the next time HINVR opens, after it closes unexpectedly.</Empty>;
  }
  return (
    <ul className="divide-y divide-zinc-100">
      {rows.map((row) => (
        <li key={row.id} className="py-2.5">
          <details>
            <summary className="flex cursor-pointer list-none flex-wrap items-baseline gap-x-3 gap-y-1">
              <span className="text-sm font-medium">{row.kind || "Crash"}</span>
              <span className="text-xs text-zinc-500">
                {formatWhen(row.happened_at ?? row.received_at)} · {row.headset}
              </span>
              {row.app_version ? <Badge>{row.app_version}</Badge> : null}
              {row.message ? <span className="w-full truncate text-xs text-zinc-600">{row.message}</span> : null}
            </summary>
            <pre className="mt-2 max-h-72 overflow-auto rounded-md bg-zinc-950 p-3 text-[11px] leading-relaxed text-zinc-100">
              {row.trace || "No trace was sent."}
            </pre>
          </details>
        </li>
      ))}
    </ul>
  );
}

export function InsightsView({ data, seesCrashes }: { data: Insights; seesCrashes: boolean }) {
  const days = fillDays(data.daily, data.since, data.days);
  const handoffs = data.handoffs;
  const nothingYet = data.offerings.total === 0;
  return (
    <div className="space-y-6">
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        <Tile label="Offerings" value={data.offerings.total} hint={`${data.offerings.members} members offered`} />
        <Tile
          label="Headsets"
          value={data.headsets.active_week}
          hint={`active this week · ${data.headsets.paired} paired`}
        />
        <Tile label="Japa rounds" value={data.offerings.japa_rounds} hint="108 beads each" />
        <Tile
          label="View in VR"
          value={`${percent(handoffs.claimed, handoffs.requests)}%`}
          hint={`${handoffs.claimed} of ${handoffs.requests} opened${
            handoffs.median_seconds != null ? ` · ${handoffs.median_seconds} s median` : ""
          }`}
        />
        <Tile
          label="Crashes"
          value={data.crashes.total}
          hint={data.crashes.total ? `from ${data.crashes.headsets} headset${data.crashes.headsets === 1 ? "" : "s"}` : "none reported"}
        />
      </div>

      <Section title="Offerings per day" hint="India dates. Hover a bar for what was offered.">
        {nothingYet ? (
          <Empty>No offerings in this period. Each offering made in the headset sanctum appears here the same day.</Empty>
        ) : (
          <DailyBars days={days} />
        )}
      </Section>

      <div className="grid gap-6 lg:grid-cols-2">
        <Section title="Darshan windows chosen" hint="The live mandir members kept in the sanctum while offering.">
          {data.mandirs.length === 0 ? (
            <Empty>No offerings were made with a live darshan window yet.</Empty>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-zinc-500">
                  <th className="pb-2 font-medium">Mandir</th>
                  <th className="pb-2 text-right font-medium">Offerings</th>
                  <th className="pb-2 text-right font-medium">Members</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100">
                {data.mandirs.map((row) => (
                  <tr key={row.mandir_id}>
                    <td className="py-1.5">{row.name}</td>
                    <td className="py-1.5 text-right tabular-nums">{row.offerings}</td>
                    <td className="py-1.5 text-right tabular-nums">{row.members}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Section>

        <Section title="What was offered">
          <Shares counts={data.kinds} labels={OFFERING_LABELS} empty="Offerings by kind appear here." />
        </Section>

        <Section title="Offered for" hint="The sankalp's “For” choice.">
          <Shares
            counts={data.dedications}
            labels={DEDICATION_LABELS}
            empty="No dedications yet. Members choose who a visit is for on the sankalp panel."
          />
        </Section>

        <Section title="View in VR" hint="Requests from the phone, and how many a headset opened.">
          {handoffs.requests === 0 ? (
            <Empty>No requests yet. Members send one from a temple or live darshan in the phone app.</Empty>
          ) : (
            <ul className="space-y-2 text-sm">
              {Object.entries(handoffs.targets).map(([target, row]) => (
                <li key={target} className="flex justify-between">
                  <span>{HANDOFF_LABELS[target] ?? target}</span>
                  <span className="tabular-nums text-zinc-500">
                    {row.claimed} / {row.requests} opened · {percent(row.claimed, row.requests)}%
                  </span>
                </li>
              ))}
            </ul>
          )}
          <p className={cx("mt-3 text-xs", data.headsets.low_battery ? "text-amber-700" : "text-zinc-500")}>
            {data.headsets.reporting_week} headsets reported battery this week
            {data.headsets.low_battery ? ` · ${data.headsets.low_battery} at 20% or below` : ""}.
          </p>
        </Section>
      </div>

      {data.offerings.festival > 0 ? (
        <p className="text-sm text-zinc-600">
          Festival pujas: {data.offerings.festival} offerings (deepmala and abhishek) in this period.
        </p>
      ) : null}

      {seesCrashes ? (
        <Section title="Recent headset crashes" hint="The 20 latest reports. Open one for its stack trace.">
          <Crashes rows={data.recent_crashes ?? []} />
        </Section>
      ) : null}
    </div>
  );
}
