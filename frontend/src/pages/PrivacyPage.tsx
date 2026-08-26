import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "../context/AuthContext";
import { api } from "../lib/api";

interface ConsentRecord {
  consentType: string;
  version: string;
  grantedAt: string;
  revokedAt: string | null;
  active: boolean;
}

const CONSENT_TYPES: { type: string; label: string; description: string }[] = [
  { type: "TERMS_OF_SERVICE", label: "Terms of Service", description: "Required to use LOSE10." },
  { type: "PRIVACY_POLICY", label: "Privacy Policy", description: "Required to use LOSE10." },
  { type: "AI_COACHING", label: "AI coaching data processing", description: "Lets the AI coach use your logged data to personalize coaching." },
  { type: "WHATSAPP_COMMS", label: "WhatsApp messages", description: "Lets us message you on WhatsApp for coaching and reminders." },
  { type: "MARKETING", label: "Marketing communications", description: "Occasional product updates and offers. Optional." },
];

const CURRENT_VERSION = "1.0";

export default function PrivacyPage() {
  const { isAuthenticated } = useAuth();
  const queryClient = useQueryClient();

  const { data: consents } = useQuery({
    queryKey: ["consents"],
    queryFn: async () => (await api.get<ConsentRecord[]>("/users/me/consents")).data,
    enabled: isAuthenticated,
  });

  const grant = useMutation({
    mutationFn: async (consentType: string) =>
      api.post("/users/me/consents", { consentType, version: CURRENT_VERSION, source: "privacy_page" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["consents"] }),
  });

  const revoke = useMutation({
    mutationFn: async (consentType: string) => api.delete(`/users/me/consents/${consentType}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["consents"] }),
  });

  function isActive(type: string): boolean {
    return consents?.some((c) => c.consentType === type && c.active) ?? false;
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-10">
      <h1 className="mb-2 text-2xl font-semibold text-slate-900">Privacy Policy</h1>
      <div className="mb-8 rounded-md border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800">
        This page is a placeholder pending legal counsel review (docs/backlog L10-1107) — it is not
        finalized legal text and should not be relied on as such.
      </div>

      <p className="mb-8 text-sm text-slate-600">
        LOSE10 collects the data you log (weight, workouts, nutrition, check-ins) and your coaching
        conversations to provide personalized coaching. We never sell your data. You can export or
        delete all of it at any time from your account settings.
      </p>

      {isAuthenticated && (
        <>
          <h2 className="mb-4 text-lg font-semibold text-slate-900">Your consent choices</h2>
          <div className="space-y-4">
            {CONSENT_TYPES.map((c) => {
              const active = isActive(c.type);
              return (
                <div key={c.type} className="flex items-start justify-between rounded-lg border border-slate-200 p-4">
                  <div>
                    <div className="font-medium text-slate-900">{c.label}</div>
                    <div className="text-sm text-slate-500">{c.description}</div>
                    <div className="mt-1 text-xs text-slate-400">{active ? "Granted" : "Not granted"}</div>
                  </div>
                  <button
                    type="button"
                    onClick={() => (active ? revoke.mutate(c.type) : grant.mutate(c.type))}
                    disabled={grant.isPending || revoke.isPending}
                    className={`shrink-0 rounded-md px-3 py-1.5 text-sm font-medium disabled:opacity-50 ${
                      active
                        ? "border border-red-200 text-red-600 hover:bg-red-50"
                        : "bg-brand-600 text-white hover:bg-brand-700"
                    }`}
                  >
                    {active ? "Revoke" : "Grant"}
                  </button>
                </div>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}
