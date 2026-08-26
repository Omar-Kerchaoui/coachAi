export default function TermsPage() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-10">
      <h1 className="mb-2 text-2xl font-semibold text-slate-900">Terms of Service</h1>
      <div className="mb-8 rounded-md border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800">
        This page is a placeholder pending legal counsel review (docs/backlog L10-1107) — it is not
        finalized legal text and should not be relied on as such.
      </div>

      <div className="space-y-4 text-sm text-slate-600">
        <p>
          LOSE10 is a behavioral coaching product, not a medical service. It does not diagnose,
          treat, or replace advice from a qualified healthcare professional — see the AI and health
          disclaimers below.
        </p>
        <p>
          You must be at least 18 years old to use LOSE10. Coaching is delivered primarily over
          WhatsApp; standard messaging rates from your carrier may apply.
        </p>
        <p>
          Subscriptions renew automatically until cancelled. You can cancel at any time from your
          billing settings — cancellation takes effect at the end of the current billing period, not
          immediately.
        </p>
        <h2 className="pt-4 text-base font-semibold text-slate-900">AI &amp; Health Disclaimer</h2>
        <p>
          Your coach is an AI system. It is not a doctor, dietitian, or therapist. Nothing it says is
          medical advice. If you're experiencing a medical emergency, self-harm crisis, or symptoms
          that concern you, contact emergency services or a qualified professional directly — not
          the app.
        </p>
      </div>
    </div>
  );
}
