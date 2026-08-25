import { useMutation, useQuery } from "@tanstack/react-query";
import axios from "axios";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, isApiError } from "../lib/api";
import { useAuth } from "../context/AuthContext";

interface PlanSummary {
  id: string;
  name: string;
  amountCents: number;
  currency: string;
  billingInterval: "MONTH" | "YEAR";
  trialDays: number;
}

export default function PricingPage() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [couponCode, setCouponCode] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const { data: plans, isLoading } = useQuery({
    queryKey: ["plans"],
    queryFn: async () => (await api.get<PlanSummary[]>("/plans")).data,
  });

  const checkout = useMutation({
    mutationFn: async (planId: string) => {
      const { data } = await api.post<{ url: string }>("/subscriptions/checkout-session", {
        planId,
        couponCode: couponCode || undefined,
      });
      return data;
    },
    onSuccess: (data) => {
      window.location.href = data.url;
    },
    onError: (error) => {
      const responseData = axios.isAxiosError(error) ? error.response?.data : undefined;
      setErrorMessage(
        isApiError(responseData) ? responseData.message : "Something went wrong starting checkout. Please try again."
      );
    },
  });

  function handleSelectPlan(planId: string) {
    setErrorMessage(null);
    if (!isAuthenticated) {
      navigate("/register");
      return;
    }
    checkout.mutate(planId);
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-16">
      <div className="text-center">
        <h1 className="text-3xl font-semibold text-slate-900">Simple, honest pricing</h1>
        <p className="mt-2 text-slate-600">Every plan starts with a free trial. Cancel anytime — no games.</p>
      </div>

      {isLoading && <p className="mt-10 text-center text-slate-500">Loading plans…</p>}

      {plans && (
        <div className="mt-10 grid gap-6 sm:grid-cols-2">
          {plans.map((plan) => (
            <div key={plan.id} className="rounded-xl border border-slate-200 p-6 shadow-sm">
              <h2 className="text-lg font-semibold text-slate-900">{plan.name}</h2>
              <p className="mt-2 text-3xl font-bold text-slate-900">
                ${(plan.amountCents / 100).toFixed(2)}
                <span className="text-base font-normal text-slate-500">
                  /{plan.billingInterval === "MONTH" ? "mo" : "yr"}
                </span>
              </p>
              {plan.trialDays > 0 && (
                <p className="mt-1 text-sm text-brand-700">{plan.trialDays}-day free trial</p>
              )}
              <button
                type="button"
                onClick={() => handleSelectPlan(plan.id)}
                disabled={checkout.isPending}
                className="mt-6 w-full rounded-md bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700 disabled:opacity-50"
              >
                {checkout.isPending ? "Redirecting…" : `Choose ${plan.name}`}
              </button>
            </div>
          ))}
        </div>
      )}

      <div className="mx-auto mt-8 max-w-xs">
        <label htmlFor="coupon" className="block text-sm text-slate-600">
          Have a coupon code?
        </label>
        <input
          id="coupon"
          type="text"
          value={couponCode}
          onChange={(e) => setCouponCode(e.target.value)}
          className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          placeholder="COUPONCODE"
        />
      </div>

      {errorMessage && <p className="mt-6 text-center text-sm text-red-600">{errorMessage}</p>}
    </div>
  );
}
