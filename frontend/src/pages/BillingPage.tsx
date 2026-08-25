import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { api } from "../lib/api";

interface SubscriptionSummary {
  hasSubscription: boolean;
  planName: string | null;
  status:
    | "INCOMPLETE"
    | "INCOMPLETE_EXPIRED"
    | "TRIALING"
    | "ACTIVE"
    | "PAST_DUE"
    | "CANCELED"
    | "UNPAID"
    | "PAUSED"
    | null;
  trialEnd: string | null;
  currentPeriodEnd: string | null;
  cancelAtPeriodEnd: boolean;
}

const STATUS_LABELS: Record<string, string> = {
  INCOMPLETE: "Awaiting checkout",
  INCOMPLETE_EXPIRED: "Checkout expired",
  TRIALING: "Free trial",
  ACTIVE: "Active",
  PAST_DUE: "Payment past due",
  CANCELED: "Canceled",
  UNPAID: "Unpaid",
  PAUSED: "Paused",
};

export default function BillingPage() {
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["subscription-me"],
    queryFn: async () => (await api.get<SubscriptionSummary>("/subscriptions/me")).data,
  });

  const portal = useMutation({
    mutationFn: async () => (await api.post<{ url: string }>("/subscriptions/portal-session")).data,
    onSuccess: (data) => {
      window.location.href = data.url;
    },
  });

  const cancel = useMutation({
    mutationFn: async () => api.post("/subscriptions/cancel"),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["subscription-me"] }),
  });

  if (isLoading) {
    return <div className="px-4 py-16 text-center text-slate-500">Loading billing details…</div>;
  }

  if (!data?.hasSubscription) {
    return (
      <div className="mx-auto max-w-lg px-4 py-16 text-center">
        <h1 className="text-2xl font-semibold text-slate-900">No active subscription</h1>
        <p className="mt-2 text-slate-600">Choose a plan to start coaching over WhatsApp.</p>
        <Link
          to="/pricing"
          className="mt-6 inline-block rounded-md bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700"
        >
          View pricing
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-10">
      <h1 className="mb-6 text-2xl font-semibold text-slate-900">Billing</h1>

      <div className="rounded-lg border border-slate-200 p-6">
        <div className="flex items-center justify-between">
          <span className="text-sm text-slate-500">Plan</span>
          <span className="font-medium text-slate-900">{data.planName}</span>
        </div>
        <div className="mt-3 flex items-center justify-between">
          <span className="text-sm text-slate-500">Status</span>
          <span className="font-medium text-slate-900">{STATUS_LABELS[data.status ?? ""] ?? data.status}</span>
        </div>
        {data.trialEnd && (
          <div className="mt-3 flex items-center justify-between">
            <span className="text-sm text-slate-500">Trial ends</span>
            <span className="font-medium text-slate-900">{new Date(data.trialEnd).toLocaleDateString()}</span>
          </div>
        )}
        {data.currentPeriodEnd && (
          <div className="mt-3 flex items-center justify-between">
            <span className="text-sm text-slate-500">
              {data.cancelAtPeriodEnd ? "Access ends" : "Renews"}
            </span>
            <span className="font-medium text-slate-900">{new Date(data.currentPeriodEnd).toLocaleDateString()}</span>
          </div>
        )}
        {data.cancelAtPeriodEnd && (
          <p className="mt-4 text-sm text-amber-600">
            Your subscription is set to cancel at the end of the current period.
          </p>
        )}
      </div>

      <div className="mt-6 flex gap-3">
        <button
          type="button"
          onClick={() => portal.mutate()}
          disabled={portal.isPending}
          className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
        >
          {portal.isPending ? "Opening…" : "Manage payment & plan"}
        </button>
        {!data.cancelAtPeriodEnd && (
          <button
            type="button"
            onClick={() => cancel.mutate()}
            disabled={cancel.isPending}
            className="rounded-md border border-red-200 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50 disabled:opacity-50"
          >
            {cancel.isPending ? "Cancelling…" : "Cancel subscription"}
          </button>
        )}
      </div>
    </div>
  );
}
