import { Card } from "@/components/ui";
import { formatWhen } from "@/lib/ops";
import type { YoutubeQuota } from "@/lib/types";

export function YoutubeQuotaCard({ quota }: { quota: YoutubeQuota }) {
  const pct = Math.min(100, Math.round((quota.used / Math.max(quota.limit, 1)) * 1000) / 10);
  const bar = pct >= 90 ? "bg-red-500" : pct >= 70 ? "bg-amber-500" : "bg-emerald-600";
  const last = quota.last;
  return (
    <Card className="px-4 py-3">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">YouTube quota today</p>
        <p className="text-xs text-zinc-500">Resets midnight Pacific</p>
      </div>
      <p className="mt-1 text-2xl font-semibold tabular-nums">
        {quota.used.toLocaleString("en-IN")}
        <span className="text-base font-medium text-zinc-400"> / {quota.limit.toLocaleString("en-IN")}</span>
      </p>
      <div className="mt-2 h-2 overflow-hidden rounded-full bg-zinc-100">
        <div className={`h-full rounded-full ${bar}`} style={{ width: `${pct}%` }} />
      </div>
      <p className="mt-2 text-xs text-zinc-500">
        {(quota.limit - quota.used).toLocaleString("en-IN")} left · {pct}% · {quota.runs} check
        {quota.runs === 1 ? "" : "s"}
        {last
          ? ` · last run ${last.units} unit${last.units === 1 ? "" : "s"} (${last.videos} video, ${last.searches} search)`
          : " · no check yet today"}
        {last?.at ? ` · ${formatWhen(last.at)}` : ""}
        {" · video 1 · search 100"}
      </p>
    </Card>
  );
}
