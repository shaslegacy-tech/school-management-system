"use client";

import { motion } from "framer-motion";
import { ArrowRight, Home, LockKeyhole, LogOut, Mail } from "lucide-react";
import { useState, type FormEvent } from "react";
import SchoolDashboard from "@/components/school-dashboard";
import { apiClient } from "@/lib/api-client";
import { useSessionStore, type SessionUser } from "@/stores/session-store";

export default function AppEntry() {
  const user = useSessionStore((state) => state.user);
  const accessToken = useSessionStore((state) => state.accessToken);
  const refreshToken = useSessionStore((state) => state.refreshToken);
  const setSession = useSessionStore((state) => state.setSession);
  const clearSession = useSessionStore((state) => state.clearSession);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  async function signIn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      const { data } = await apiClient.post<{
        accessToken: string;
        refreshToken: string;
        userId: string;
        displayName: string;
        role: SessionUser["role"];
        branchId: string | null;
      }>("/auth/login", { email, password });
      setSession(data.accessToken, data.refreshToken, {
        id: data.userId,
        displayName: data.displayName,
        role: data.role,
        branchId: data.branchId,
      });
    } catch {
      setError("Sign-in failed. Check your email and password, then try again.");
    } finally {
      setSubmitting(false);
    }
  }

  async function signOut() {
    try {
      if (refreshToken) await apiClient.post("/auth/logout", { refreshToken });
    } finally {
      clearSession();
    }
  }

  if (user && accessToken && ["SUPER_ADMIN", "BRANCH_ADMIN"].includes(user.role)) {
    return <SchoolDashboard user={user} onLogout={signOut} />;
  }

  if (user && accessToken) {
    return <main className="auth-page"><section className="auth-card"><div className="auth-brand"><span className="auth-mark"><Home size={19} /></span><span>Little Home School</span></div><h1 className="auth-title">Your workspace is being prepared</h1><p className="auth-copy">This account is signed in, but its role workspace is not available yet.</p><button className="primary-action auth-submit" onClick={signOut} type="button"><LogOut size={15} /> Sign out</button></section></main>;
  }

  return <main className="auth-page"><motion.section className="auth-card" initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: .35 }}>
    <div className="auth-brand"><span className="auth-mark"><Home size={19} /></span><span>Little Home School</span></div>
    <div className="auth-eyebrow">CAMPUS PORTAL</div>
    <h1 className="auth-title">Welcome back</h1>
    <p className="auth-copy">Sign in to your school workspace.</p>
    <form className="auth-form" onSubmit={signIn}>
      <label className="auth-label">Work email<span className="auth-input-wrap"><Mail size={15} /><input autoComplete="username" autoFocus onChange={(event) => setEmail(event.target.value)} placeholder="name@school.edu" required type="email" value={email} /></span></label>
      <label className="auth-label">Password<span className="auth-input-wrap"><LockKeyhole size={15} /><input autoComplete="current-password" onChange={(event) => setPassword(event.target.value)} placeholder="Enter your password" required type="password" value={password} /></span></label>
      {error && <p className="auth-error" role="alert">{error}</p>}
      <button className="primary-action auth-submit" disabled={submitting} type="submit">{submitting ? "Signing in..." : "Sign in"}<ArrowRight size={15} /></button>
    </form>
    <div className="auth-footnote">Access is managed by your school administrator.</div>
  </motion.section></main>;
}