import { useQuery } from "@tanstack/react-query";
import { api } from "../lib/api";

interface WeightEntry {
  id: string;
  weightKg: number;
  recordedAt: string;
}

interface TrendPoint {
  date: string;
  rollingAverageKg: number;
}

interface WeightTrend {
  points: TrendPoint[];
  trendKgPerWeek: number;
  direction: "DOWN" | "FLAT" | "UP";
}

interface Goal {
  id: string;
  status: string;
  startingWeightKg: number;
  targetWeightKg: number;
  weightToLoseKg: number;
}

const CHART_WIDTH = 640;
const CHART_HEIGHT = 220;
const PADDING = 24;

export default function ProgressPage() {
  const { data: history } = useQuery({
    queryKey: ["weight-history"],
    queryFn: async () => (await api.get<{ content: WeightEntry[] }>("/weight", { params: { size: 100 } })).data.content,
  });

  const { data: trend } = useQuery({
    queryKey: ["weight-trend"],
    queryFn: async () => (await api.get<WeightTrend>("/weight/trend")).data,
  });

  const { data: goals } = useQuery({
    queryKey: ["goals"],
    queryFn: async () => (await api.get<Goal[]>("/goals")).data,
  });

  const activeGoal = goals?.find((g) => g.status === "ACTIVE");

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <h1 className="mb-2 text-2xl font-semibold text-slate-900">Your progress</h1>
      <p className="mb-6 text-sm text-slate-500">
        The bold line is your trend — the number that actually matters. Individual points bounce around day to day; that's normal
        and not worth reacting to on its own.
      </p>

      {activeGoal && (
        <div className="mb-6 grid grid-cols-3 gap-4">
          <Stat label="Starting" value={`${activeGoal.startingWeightKg}kg`} />
          <Stat label="Target" value={`${activeGoal.targetWeightKg}kg`} />
          <Stat label="To go" value={`${activeGoal.weightToLoseKg}kg`} />
        </div>
      )}

      {trend && history && (
        <WeightChart trend={trend} history={history} goal={activeGoal} />
      )}

      {trend && (
        <p className="mt-3 text-sm text-slate-600">
          Trend: {trend.direction === "DOWN" ? "trending down" : trend.direction === "UP" ? "trending up" : "holding steady"}
          {trend.trendKgPerWeek !== 0 && ` (${Math.abs(trend.trendKgPerWeek).toFixed(2)}kg/week)`}
        </p>
      )}

      {(!history || history.length === 0) && (
        <p className="mt-6 text-center text-slate-500">Log your first weigh-in to start seeing your progress here.</p>
      )}
    </div>
  );
}

function WeightChart({ trend, history, goal }: { trend: WeightTrend; history: WeightEntry[]; goal?: Goal }) {
  const values = [
    ...history.map((h) => h.weightKg),
    ...trend.points.map((p) => p.rollingAverageKg),
    ...(goal ? [goal.startingWeightKg, goal.targetWeightKg] : []),
  ];
  if (values.length === 0) {
    return null;
  }
  const min = Math.min(...values) - 1;
  const max = Math.max(...values) + 1;

  const dateToX = (dateMs: number, minMs: number, maxMs: number) =>
    PADDING + ((dateMs - minMs) / Math.max(1, maxMs - minMs)) * (CHART_WIDTH - 2 * PADDING);
  const valueToY = (v: number) => CHART_HEIGHT - PADDING - ((v - min) / (max - min)) * (CHART_HEIGHT - 2 * PADDING);

  const allDates = [
    ...history.map((h) => new Date(h.recordedAt).getTime()),
    ...trend.points.map((p) => new Date(p.date).getTime()),
  ];
  const minMs = Math.min(...allDates);
  const maxMs = Math.max(...allDates);

  const trendPath = trend.points
    .map((p, i) => `${i === 0 ? "M" : "L"} ${dateToX(new Date(p.date).getTime(), minMs, maxMs)} ${valueToY(p.rollingAverageKg)}`)
    .join(" ");

  return (
    <svg viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`} className="w-full rounded-lg border border-slate-200 bg-white">
      {goal && (
        <line
          x1={PADDING}
          x2={CHART_WIDTH - PADDING}
          y1={valueToY(goal.targetWeightKg)}
          y2={valueToY(goal.targetWeightKg)}
          stroke="#94a3b8"
          strokeDasharray="4 4"
        />
      )}
      {/* Individual logged entries: small, faded — de-emphasized relative to the trend line */}
      {history.map((h) => (
        <circle
          key={h.id}
          cx={dateToX(new Date(h.recordedAt).getTime(), minMs, maxMs)}
          cy={valueToY(h.weightKg)}
          r={2.5}
          fill="#cbd5e1"
        />
      ))}
      {/* Rolling-average trend: bold, the number that matters */}
      <path d={trendPath} fill="none" stroke="#0d9488" strokeWidth={3} strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-slate-200 p-4 text-center">
      <div className="text-xs uppercase tracking-wide text-slate-500">{label}</div>
      <div className="mt-1 text-xl font-semibold text-slate-900">{value}</div>
    </div>
  );
}
