import { Link } from "react-router-dom";

export default function LandingPage() {
  return (
    <div className="mx-auto flex max-w-3xl flex-col items-center gap-6 px-4 py-24 text-center">
      <h1 className="text-4xl font-bold text-slate-900">Lose 10kg with a coach who remembers you.</h1>
      <p className="max-w-xl text-slate-600">
        LOSE10 is an AI personal coach that works with you every day over WhatsApp — tracking your progress, adapting
        your plan, and getting to know you better every week.
      </p>
      <Link
        to="/register"
        className="rounded-md bg-brand-600 px-6 py-3 font-medium text-white hover:bg-brand-700"
      >
        Get started
      </Link>
    </div>
  );
}
