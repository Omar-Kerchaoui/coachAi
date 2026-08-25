import { useEffect, useState, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { api, isApiError } from "../lib/api";

type CoachStyle = "SUPPORTIVE" | "BALANCED" | "TOUGH_LOVE";

type Step = "checking" | "current-weight" | "target-weight" | "coach-style" | "submitting";

const COACH_STYLES: { value: CoachStyle; label: string; blurb: string }[] = [
  { value: "SUPPORTIVE", label: "Supportive", blurb: "Gentle, encouraging, always in your corner." },
  { value: "BALANCED", label: "Balanced", blurb: "Honest and practical, without the edge." },
  { value: "TOUGH_LOVE", label: "Tough love", blurb: "Direct, no excuses, keeps you accountable." },
];

// Progressive-question onboarding (product spec §58): asks one thing at a
// time rather than presenting a form. On mount, checks whether the user
// already has an active goal — resuming a partial onboarding never
// re-creates one (docs/backlog L10-311 AC).
export default function OnboardingPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState<Step>("checking");
  const [currentWeight, setCurrentWeight] = useState("");
  const [targetWeight, setTargetWeight] = useState("");
  const [coachStyle, setCoachStyle] = useState<CoachStyle>("BALANCED");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<unknown[]>("/goals")
      .then(({ data }) => {
        const hasActiveGoal = Array.isArray(data) && data.some((g) => (g as { status?: string }).status === "ACTIVE");
        setStep(hasActiveGoal ? "checking" : "current-weight");
        if (hasActiveGoal) {
          navigate("/dashboard");
        }
      })
      .catch(() => setStep("current-weight"));
  }, [navigate]);

  async function handleFinish() {
    setStep("submitting");
    setError(null);
    try {
      const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
      await api.patch("/users/me", { timezone });
      await api.patch("/users/me/preferences", { coachStyle });
      await api.post("/goals", {
        startingWeightKg: Number(currentWeight),
        targetWeightKg: Number(targetWeight),
      });
      navigate("/dashboard");
    } catch (err: unknown) {
      const data = (err as { response?: { data?: unknown } })?.response?.data;
      setError(isApiError(data) ? data.message : "Something went wrong setting up your plan.");
      setStep("coach-style");
    }
  }

  if (step === "checking") {
    return null;
  }

  return (
    <div className="mx-auto flex min-h-[70vh] max-w-md flex-col justify-center px-4">
      {step === "current-weight" && (
        <QuestionStep
          question="What's your current weight?"
          hint="In kilograms — we'll use this as your starting point."
        >
          <NumberField value={currentWeight} onChange={setCurrentWeight} suffix="kg" autoFocus />
          <NextButton disabled={!currentWeight} onClick={() => setStep("target-weight")} />
        </QuestionStep>
      )}

      {step === "target-weight" && (
        <QuestionStep question="And where do you want to get to?">
          <NumberField value={targetWeight} onChange={setTargetWeight} suffix="kg" autoFocus />
          <NextButton disabled={!targetWeight} onClick={() => setStep("coach-style")} />
        </QuestionStep>
      )}

      {(step === "coach-style" || step === "submitting") && (
        <QuestionStep question="How do you want me to coach you?">
          <div className="flex flex-col gap-2">
            {COACH_STYLES.map((s) => (
              <button
                key={s.value}
                type="button"
                onClick={() => setCoachStyle(s.value)}
                className={`rounded-md border px-4 py-3 text-left ${
                  coachStyle === s.value ? "border-brand-600 bg-brand-50" : "border-slate-300"
                }`}
              >
                <div className="font-medium text-slate-900">{s.label}</div>
                <div className="text-sm text-slate-500">{s.blurb}</div>
              </button>
            ))}
          </div>
          {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
          <NextButton
            disabled={step === "submitting"}
            label={step === "submitting" ? "Setting up your plan…" : "Start coaching"}
            onClick={handleFinish}
          />
        </QuestionStep>
      )}
    </div>
  );
}

function QuestionStep({ question, hint, children }: { question: string; hint?: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold text-slate-900">{question}</h1>
      {hint && <p className="-mt-2 text-sm text-slate-500">{hint}</p>}
      {children}
    </div>
  );
}

function NumberField({
  value,
  onChange,
  suffix,
  autoFocus,
}: {
  value: string;
  onChange: (v: string) => void;
  suffix?: string;
  autoFocus?: boolean;
}) {
  return (
    <div className="flex items-center gap-2">
      <input
        type="number"
        inputMode="decimal"
        autoFocus={autoFocus}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-32 rounded-md border border-slate-300 px-3 py-2 text-lg"
      />
      {suffix && <span className="text-slate-500">{suffix}</span>}
    </div>
  );
}

function NextButton({ onClick, disabled, label = "Next" }: { onClick: () => void; disabled?: boolean; label?: string }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className="mt-2 self-start rounded-md bg-brand-600 px-5 py-2 font-medium text-white hover:bg-brand-700 disabled:opacity-50"
    >
      {label}
    </button>
  );
}
