import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { api } from "../lib/api";

export default function SettingsPage() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [confirmText, setConfirmText] = useState("");
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  const exportData = useMutation({
    mutationFn: async () => (await api.get("/users/me/export")).data,
    onSuccess: (data) => {
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "lose10-data-export.json";
      link.click();
      URL.revokeObjectURL(url);
    },
  });

  const deleteAccount = useMutation({
    mutationFn: async () => api.delete("/users/me"),
    onSuccess: async () => {
      await logout();
      navigate("/");
    },
  });

  return (
    <div className="mx-auto max-w-2xl px-4 py-10">
      <h1 className="mb-6 text-2xl font-semibold text-slate-900">Settings</h1>

      <section className="mb-8 rounded-lg border border-slate-200 p-5">
        <h2 className="text-lg font-semibold text-slate-900">Your data</h2>
        <p className="mt-1 text-sm text-slate-500">
          Download everything LOSE10 has stored about you — profile, goals, weight history, workouts,
          nutrition, check-ins, memories, and your full conversation history.
        </p>
        <button
          type="button"
          onClick={() => exportData.mutate()}
          disabled={exportData.isPending}
          className="mt-4 rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
        >
          {exportData.isPending ? "Preparing export…" : "Download my data"}
        </button>
      </section>

      <section className="mb-8 rounded-lg border border-slate-200 p-5">
        <h2 className="text-lg font-semibold text-slate-900">Privacy &amp; consent</h2>
        <p className="mt-1 text-sm text-slate-500">Review or update what you've consented to.</p>
        <Link to="/privacy" className="mt-4 inline-block text-sm font-medium text-brand-700 hover:underline">
          Manage consent →
        </Link>
      </section>

      <section className="rounded-lg border border-red-200 p-5">
        <h2 className="text-lg font-semibold text-red-700">Delete account</h2>
        <p className="mt-1 text-sm text-slate-500">
          Permanently deletes your profile, goals, weight history, workouts, nutrition logs,
          check-ins, memories, WhatsApp connection, and conversation history. This cannot be undone.
          Billing records are retained and anonymized as required by law.
        </p>

        {!showDeleteConfirm ? (
          <button
            type="button"
            onClick={() => setShowDeleteConfirm(true)}
            className="mt-4 rounded-md border border-red-300 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50"
          >
            Delete my account
          </button>
        ) : (
          <div className="mt-4">
            <label className="block text-sm text-slate-700">
              Type <span className="font-mono font-semibold">DELETE</span> to confirm.
            </label>
            <input
              value={confirmText}
              onChange={(e) => setConfirmText(e.target.value)}
              className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
            />
            <div className="mt-3 flex gap-2">
              <button
                type="button"
                onClick={() => deleteAccount.mutate()}
                disabled={confirmText !== "DELETE" || deleteAccount.isPending}
                className="rounded-md bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-700 disabled:opacity-40"
              >
                {deleteAccount.isPending ? "Deleting…" : "Permanently delete"}
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowDeleteConfirm(false);
                  setConfirmText("");
                }}
                className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
              >
                Cancel
              </button>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}
