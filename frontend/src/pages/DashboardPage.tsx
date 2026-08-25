import { useQuery } from "@tanstack/react-query";
import { Link, Navigate } from "react-router-dom";
import { api } from "../lib/api";

interface ProgressSummary {
  startingWeightKg: number;
  currentWeightKg: number;
  targetWeightKg: number;
  weightLostKg: number;
  weightRemainingKg: number;
  progressPercent: number;
  phase: "EARLY" | "MIDDLE" | "FINAL" | "ACHIEVED";
  trendDirection: "DOWN" | "FLAT" | "UP";
}

export default function DashboardPage() {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ["progress-summary"],
    queryFn: async () => (await api.get<ProgressSummary>("/progress/summary")).data,
    retry: false,
  });

  if (isLoading) {
    return <div className="px-4 py-16 text-center text-slate-500">Loading your progress…</div>;
  }

  // No active goal yet (404) means onboarding was never finished.
  const status = (error as { response?: { status?: number } })?.response?.status;
  if (isError && status === 404) {
    return <Navigate to="/onboarding" replace />;
  }

  if (isError || !data) {
    return <div className="px-4 py-16 text-center text-red-600">Couldn't load your dashboard. Try refreshing.</div>;
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <h1 className="mb-6 text-2xl font-semibold text-slate-900">Your progress</h1>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <Stat label="Current" value={`${data.currentWeightKg}kg`} />
        <Stat label="Lost so far" value={`${data.weightLostKg}kg`} />
        <Stat label="Remaining" value={`${data.weightRemainingKg}kg`} />
        <Stat label="Target" value={`${data.targetWeightKg}kg`} />
      </div>

      <div className="mt-8">
        <div className="mb-1 flex justify-between text-sm text-slate-600">
          <span>Progress</span>
          <span>{data.progressPercent}%</span>
        </div>
        <div className="h-3 w-full overflow-hidden rounded-full bg-slate-100">
          <div
            className="h-full rounded-full bg-brand-600 transition-all"
            style={{ width: `${Math.min(100, Math.max(0, data.progressPercent))}%` }}
          />
        </div>
        <p className="mt-2 text-sm text-slate-500">
          {data.phase === "ACHIEVED" ? "Goal reached — nice work." : `Trend: ${data.trendDirection.toLowerCase()}`}
        </p>
      </div>

      <div className="mt-10 flex gap-4 text-sm">
        <Link to="/progress" className="text-brand-700 hover:underline">
          View full progress →
        </Link>
        <Link to="/plan" className="text-brand-700 hover:underline">
          Today's plan →
        </Link>
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-slate-200 p-4">
      <div className="text-xs uppercase tracking-wide text-slate-500">{label}</div>
      <div className="mt-1 text-xl font-semibold text-slate-900">{value}</div>
    </div>
  );
}
