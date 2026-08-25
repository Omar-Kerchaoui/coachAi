import { Route, Routes } from "react-router-dom";
import NavBar from "./components/NavBar";
import RequireAuth from "./components/RequireAuth";
import PlaceholderPage from "./pages/PlaceholderPage";
import LandingPage from "./pages/LandingPage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import OnboardingPage from "./pages/OnboardingPage";
import DashboardPage from "./pages/DashboardPage";

export default function App() {
  return (
    <div className="min-h-screen bg-white">
      <NavBar />
      <main>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/pricing" element={<PlaceholderPage title="Pricing" epic="Epic 8 — Billing" />} />
          <Route path="/how-it-works" element={<PlaceholderPage title="How it works" />} />
          <Route path="/faq" element={<PlaceholderPage title="FAQ" />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/onboarding"
            element={
              <RequireAuth>
                <OnboardingPage />
              </RequireAuth>
            }
          />
          <Route
            path="/dashboard"
            element={
              <RequireAuth>
                <DashboardPage />
              </RequireAuth>
            }
          />
          <Route
            path="/progress"
            element={
              <RequireAuth>
                <PlaceholderPage title="Progress" epic="Epic 9 — Dashboard, Admin & Analytics" />
              </RequireAuth>
            }
          />
          <Route
            path="/plan"
            element={
              <RequireAuth>
                <PlaceholderPage title="Plan" epic="Epic 6 — Coaching Domains" />
              </RequireAuth>
            }
          />
          <Route
            path="/coach"
            element={
              <RequireAuth>
                <PlaceholderPage title="Coach" epic="Epic 7 — WhatsApp Integration" />
              </RequireAuth>
            }
          />
          <Route
            path="/memories"
            element={
              <RequireAuth>
                <PlaceholderPage title="Memories" epic="Epic 5 — Memory & Behavior Engine" />
              </RequireAuth>
            }
          />
          <Route
            path="/settings"
            element={
              <RequireAuth>
                <PlaceholderPage title="Settings" />
              </RequireAuth>
            }
          />
          <Route
            path="/billing"
            element={
              <RequireAuth>
                <PlaceholderPage title="Billing" epic="Epic 8 — Stripe & Billing" />
              </RequireAuth>
            }
          />
          <Route path="/privacy" element={<PlaceholderPage title="Privacy Policy" epic="Epic 10 — pending legal review" />} />
          <Route path="/terms" element={<PlaceholderPage title="Terms of Service" epic="Epic 10 — pending legal review" />} />
          <Route
            path="/admin"
            element={
              <RequireAuth>
                <PlaceholderPage title="Admin" epic="Epic 9 — Dashboard, Admin & Analytics" />
              </RequireAuth>
            }
          />
          <Route path="*" element={<PlaceholderPage title="Page not found" />} />
        </Routes>
      </main>
    </div>
  );
}
