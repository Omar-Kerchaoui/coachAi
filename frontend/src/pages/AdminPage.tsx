import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { api } from "../lib/api";

interface AdminUserListItem {
  id: string;
  email: string;
  phoneE164: string | null;
  status: string;
  createdAt: string;
}

interface AdminUserDetail {
  user: { id: string; email: string; status: string; emailVerified: boolean; createdAt: string };
  conversationThreadId: string;
  coachingMode: "AI_ACTIVE" | "HUMAN_ACTIVE" | "PAUSED";
  coachingPausedReason: string | null;
  goals: { id: string; status: string; startingWeightKg: number; targetWeightKg: number }[];
  latestWeight: { weightKg: number; recordedAt: string } | null;
  subscription: { hasSubscription: boolean; planName: string | null; status: string | null };
  recentCheckIns: { id: string; type: string; completed: boolean }[];
  memories: { id: string; category: string; key: string; content: string }[];
  safetyEvents: { id: string; category: string; severity: string; resolved: boolean; createdAt: string }[];
  notes: { id: string; note: string; createdAt: string }[];
  aiUsage: { requestCount: number; totalCostUsd: number };
  recentMessages: { id: string; direction: string; sender: string; content: string; createdAt: string }[];
}

interface SafetyEventRow {
  id: string;
  userId: string;
  category: string;
  severity: string;
  resolved: boolean;
  createdAt: string;
}

interface BusinessMetrics {
  metricDate: string;
  mrrCents: number;
  activeSubscribers: number;
  trialingSubscribers: number;
  dau: number;
  wau: number;
  stalledUsers: number;
  atRiskUsers: number;
}

interface AiCostDashboard {
  totalCostUsd: number;
  totalRequests: number;
  distinctUsers: number;
}

interface WhatsAppCostDashboard {
  messagesSent: number;
  messagesReceived: number;
  estimatedCostUsd: number;
}

export default function AdminPage() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [note, setNote] = useState("");
  const [manualMessage, setManualMessage] = useState("");

  const { data: users } = useQuery({
    queryKey: ["admin-users", query],
    queryFn: async () => (await api.get<{ content: AdminUserListItem[] }>("/admin/users", { params: { q: query } })).data.content,
  });

  const { data: detail } = useQuery({
    queryKey: ["admin-user-detail", selectedUserId],
    queryFn: async () => (await api.get<AdminUserDetail>(`/admin/users/${selectedUserId}`)).data,
    enabled: !!selectedUserId,
  });

  const { data: safetyQueue } = useQuery({
    queryKey: ["admin-safety-queue"],
    queryFn: async () => (await api.get<SafetyEventRow[]>("/admin/safety-events")).data,
  });

  const { data: businessMetrics } = useQuery({
    queryKey: ["admin-metrics-business"],
    queryFn: async () => (await api.get<BusinessMetrics>("/admin/metrics/business")).data,
  });

  const { data: aiCost } = useQuery({
    queryKey: ["admin-metrics-ai-cost"],
    queryFn: async () => (await api.get<AiCostDashboard>("/admin/metrics/ai-cost")).data,
  });

  const { data: whatsappCost } = useQuery({
    queryKey: ["admin-metrics-whatsapp-cost"],
    queryFn: async () => (await api.get<WhatsAppCostDashboard>("/admin/metrics/whatsapp-cost")).data,
  });

  const setCoachingState = useMutation({
    mutationFn: async (mode: string) =>
      api.post(`/admin/users/${selectedUserId}/coaching-state`, { mode, reason: mode === "PAUSED" ? "Admin action" : undefined }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-user-detail", selectedUserId] }),
  });

  const addNote = useMutation({
    mutationFn: async () => api.post(`/admin/users/${selectedUserId}/notes`, { note }),
    onSuccess: () => {
      setNote("");
      queryClient.invalidateQueries({ queryKey: ["admin-user-detail", selectedUserId] });
    },
  });

  const sendManualMessage = useMutation({
    mutationFn: async () => api.post(`/admin/conversations/${detail?.conversationThreadId}/messages`, { message: manualMessage }),
    onSuccess: () => {
      setManualMessage("");
      queryClient.invalidateQueries({ queryKey: ["admin-user-detail", selectedUserId] });
    },
  });

  const resolveSafetyEvent = useMutation({
    mutationFn: async (id: string) => api.post(`/admin/safety-events/${id}/resolve`, { resolution: "Reviewed and resolved by admin" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-safety-queue"] }),
  });

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <h1 className="mb-6 text-2xl font-semibold text-slate-900">Admin</h1>

      <div className="mb-8 grid grid-cols-2 gap-4 sm:grid-cols-5">
        <MetricCard label="MRR" value={`$${((businessMetrics?.mrrCents ?? 0) / 100).toFixed(2)}`} />
        <MetricCard label="Active subs" value={String(businessMetrics?.activeSubscribers ?? 0)} />
        <MetricCard label="DAU / WAU" value={`${businessMetrics?.dau ?? 0} / ${businessMetrics?.wau ?? 0}`} />
        <MetricCard label="AI cost (30d)" value={`$${(aiCost?.totalCostUsd ?? 0).toFixed(2)}`} />
        <MetricCard label="WhatsApp cost (30d)" value={`$${(whatsappCost?.estimatedCostUsd ?? 0).toFixed(2)}`} />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="lg:col-span-1">
          <h2 className="mb-2 text-lg font-semibold text-slate-900">Users</h2>
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by email or phone"
            className="mb-3 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          <ul className="divide-y divide-slate-200 rounded-md border border-slate-200">
            {users?.map((u) => (
              <li key={u.id}>
                <button
                  type="button"
                  onClick={() => setSelectedUserId(u.id)}
                  className={`block w-full px-3 py-2 text-left text-sm hover:bg-slate-50 ${selectedUserId === u.id ? "bg-brand-50" : ""}`}
                >
                  <div className="font-medium text-slate-900">{u.email}</div>
                  <div className="text-xs text-slate-500">{u.status}</div>
                </button>
              </li>
            ))}
          </ul>

          <h2 className="mb-2 mt-8 text-lg font-semibold text-slate-900">Safety queue</h2>
          <ul className="space-y-2">
            {safetyQueue?.map((e) => (
              <li key={e.id} className="rounded-md border border-red-200 bg-red-50 p-3 text-sm">
                <div className="font-medium text-red-800">
                  {e.category} — {e.severity}
                </div>
                <div className="text-xs text-red-600">{new Date(e.createdAt).toLocaleString()}</div>
                <button
                  type="button"
                  onClick={() => resolveSafetyEvent.mutate(e.id)}
                  className="mt-2 rounded-md border border-red-300 px-2 py-1 text-xs font-medium text-red-700 hover:bg-red-100"
                >
                  Resolve
                </button>
              </li>
            ))}
            {safetyQueue?.length === 0 && <li className="text-sm text-slate-500">No unresolved safety events.</li>}
          </ul>
        </div>

        <div className="lg:col-span-2">
          {!detail && <p className="text-slate-500">Select a user to view details.</p>}
          {detail && (
            <div>
              <h2 className="text-lg font-semibold text-slate-900">{detail.user.email}</h2>
              <p className="text-sm text-slate-500">
                Coaching mode: <span className="font-medium">{detail.coachingMode}</span> · Subscription:{" "}
                <span className="font-medium">{detail.subscription.status ?? "none"}</span> · AI usage:{" "}
                {detail.aiUsage.requestCount} requests (${detail.aiUsage.totalCostUsd.toFixed(4)})
              </p>

              <div className="mt-3 flex gap-2">
                {(["AI_ACTIVE", "HUMAN_ACTIVE", "PAUSED"] as const).map((mode) => (
                  <button
                    key={mode}
                    type="button"
                    onClick={() => setCoachingState.mutate(mode)}
                    disabled={detail.coachingMode === mode}
                    className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-40"
                  >
                    {mode}
                  </button>
                ))}
              </div>

              {detail.coachingMode === "HUMAN_ACTIVE" && (
                <div className="mt-4 flex gap-2">
                  <input
                    value={manualMessage}
                    onChange={(e) => setManualMessage(e.target.value)}
                    placeholder="Send a message as the human coach…"
                    className="flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm"
                  />
                  <button
                    type="button"
                    onClick={() => sendManualMessage.mutate()}
                    disabled={!manualMessage || sendManualMessage.isPending}
                    className="rounded-md bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700 disabled:opacity-50"
                  >
                    Send
                  </button>
                </div>
              )}

              <h3 className="mb-1 mt-6 text-sm font-semibold uppercase tracking-wide text-slate-500">Recent conversation</h3>
              <div className="max-h-64 space-y-1 overflow-y-auto rounded-md border border-slate-200 p-3">
                {detail.recentMessages.map((m) => (
                  <div key={m.id} className="text-sm">
                    <span className="font-medium text-slate-700">{m.sender}:</span> <span className="text-slate-600">{m.content}</span>
                  </div>
                ))}
              </div>

              <h3 className="mb-1 mt-6 text-sm font-semibold uppercase tracking-wide text-slate-500">Safety events</h3>
              <ul className="text-sm text-slate-600">
                {detail.safetyEvents.length === 0 && <li>None.</li>}
                {detail.safetyEvents.map((e) => (
                  <li key={e.id}>
                    {e.category} ({e.severity}) — {e.resolved ? "resolved" : "open"}
                  </li>
                ))}
              </ul>

              <h3 className="mb-1 mt-6 text-sm font-semibold uppercase tracking-wide text-slate-500">Admin notes</h3>
              <ul className="mb-2 text-sm text-slate-600">
                {detail.notes.map((n) => (
                  <li key={n.id}>{n.note}</li>
                ))}
              </ul>
              <div className="flex gap-2">
                <input
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  placeholder="Add a note…"
                  className="flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm"
                />
                <button
                  type="button"
                  onClick={() => addNote.mutate()}
                  disabled={!note || addNote.isPending}
                  className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
                >
                  Add
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function MetricCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-slate-200 p-3">
      <div className="text-xs uppercase tracking-wide text-slate-500">{label}</div>
      <div className="mt-1 text-lg font-semibold text-slate-900">{value}</div>
    </div>
  );
}
