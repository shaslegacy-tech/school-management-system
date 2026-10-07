import axios, { type InternalAxiosRequestConfig } from "axios";
import { useSessionStore } from "@/stores/session-store";

type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  userId: string;
  displayName: string;
  role: "SUPER_ADMIN" | "BRANCH_ADMIN" | "TEACHER" | "PARENT";
  branchId: string | null;
};

type RetryRequestConfig = InternalAxiosRequestConfig & { refreshAttempted?: boolean };

export const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL ?? "/api/v1",
  timeout: 15_000,
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
});

apiClient.interceptors.request.use((config) => {
  const accessToken = useSessionStore.getState().accessToken;
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
  return config;
});

let refreshPromise: Promise<AuthResponse> | null = null;

apiClient.interceptors.response.use(undefined, async (error: unknown) => {
  if (!axios.isAxiosError(error) || error.response?.status !== 401 || !error.config) {
    return Promise.reject(error);
  }

  const request = error.config as RetryRequestConfig;
  const isAuthRequest = request.url?.includes("/auth/") ?? false;
  if (isAuthRequest || request.refreshAttempted) return Promise.reject(error);

  request.refreshAttempted = true;
  const session = useSessionStore.getState();
  if (!session.refreshToken || !session.user) {
    session.clearSession();
    return Promise.reject(error);
  }

  try {
    refreshPromise ??= apiClient.post<AuthResponse>("/auth/refresh", { refreshToken: session.refreshToken })
      .then((response) => response.data)
      .finally(() => { refreshPromise = null; });
    const renewed = await refreshPromise;
    const user = {
      id: renewed.userId,
      displayName: renewed.displayName,
      role: renewed.role,
      branchId: renewed.branchId,
    };
    useSessionStore.getState().setSession(renewed.accessToken, renewed.refreshToken, user);
    request.headers.Authorization = `Bearer ${renewed.accessToken}`;
    return apiClient(request);
  } catch (refreshError) {
    useSessionStore.getState().clearSession();
    return Promise.reject(refreshError);
  }
});