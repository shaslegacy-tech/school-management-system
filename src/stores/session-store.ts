import { create } from "zustand";

export type SessionUser = {
  id: string;
  displayName: string;
  role: "SUPER_ADMIN" | "BRANCH_ADMIN" | "TEACHER" | "PARENT";
  branchId: string | null;
};

type SessionState = {
  accessToken: string | null;
  refreshToken: string | null;
  user: SessionUser | null;
  setSession: (accessToken: string, refreshToken: string, user: SessionUser) => void;
  clearSession: () => void;
};

export const useSessionStore = create<SessionState>((set) => ({
  accessToken: null,
  refreshToken: null,
  user: null,
  setSession: (accessToken, refreshToken, user) => set({ accessToken, refreshToken, user }),
  clearSession: () => set({ accessToken: null, refreshToken: null, user: null }),
}));