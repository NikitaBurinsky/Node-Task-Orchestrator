import axios from 'axios';
import type { AxiosError, InternalAxiosRequestConfig } from 'axios';
//import { setupMockAdapter } from './mockAdapter';
import type {
  ServerDto,
  ScriptDto,
  ServerGroupDto,
  TaskDto,
  AuthRequestDto,
  AuthResponseDto,
  BulkTaskRequestDto,
  StatsDto,
  PingResultDto,
  ServerPingResponseDto,
} from '../types/api';

const baseURL = 'https://api.nto.formatis.online/api';

const api = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

const refreshApi = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

let accessToken: string | null = null;
let authFailureHandler: (() => void) | null = null;

type RetriableRequestConfig = InternalAxiosRequestConfig & { _retry?: boolean };

export const setAccessToken = (token: string | null) => {
  accessToken = token;
};

export const setAuthFailureHandler = (handler: (() => void) | null) => {
  authFailureHandler = handler;
};

// setupMockAdapter(api);

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const axiosError = error as AxiosError;
    const originalRequest = axiosError.config as RetriableRequestConfig | undefined;
    const status = axiosError.response?.status;

    const requestUrl = originalRequest?.url ?? '';
    const isAuthRequest =
      requestUrl.includes('/auth/login') ||
      requestUrl.includes('/auth/register') ||
      requestUrl.includes('/auth/refresh') ||
      requestUrl.includes('/auth/logout');

    if (status === 401 && originalRequest && !isAuthRequest && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const response = await refreshApi.post<AuthResponseDto>('/auth/refresh');
        const newToken = response.data.accessToken;
        if (newToken) {
          setAccessToken(newToken);
          originalRequest.headers = originalRequest.headers ?? {};
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          return api(originalRequest);
        }
      } catch (refreshError) {
        setAccessToken(null);
        if (authFailureHandler) {
          authFailureHandler();
        }
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export const authApi = {
  login: (data: AuthRequestDto) => api.post<AuthResponseDto>('/auth/login', data),
  register: (data: AuthRequestDto) => api.post<AuthResponseDto>('/auth/register', data),
  refresh: () => refreshApi.post<AuthResponseDto>('/auth/refresh'),
  logout: () => api.post<void>('/auth/logout'),
};

export const serversApi = {
  getAll: (hostname?: string) => api.get<ServerDto[]>('/servers', { params: { hostname } }),
  getById: (id: number) => api.get<ServerDto>(`/servers/${id}`),
  create: (data: ServerDto) => api.post<ServerDto>('/servers', data),
  delete: (id: number) => api.delete(`/servers/${id}`),
  ping: (id: number) => api.get<ServerPingResponseDto>(`/servers/${id}/ping`),
};

export const scriptsApi = {
  getAll: () => api.get<ScriptDto[]>('/scripts'),
  getById: (id: number) => api.get<ScriptDto>(`/scripts/${id}`),
  create: (data: ScriptDto) => api.post<ScriptDto>('/scripts', data),
  delete: (id: number) => api.delete(`/scripts/${id}`),
};

export const groupsApi = {
  getAll: () => api.get<ServerGroupDto[]>('/groups'),
  getById: (id: number) => api.get<ServerGroupDto>(`/groups/${id}`),
  create: (data: ServerGroupDto) => api.post<ServerGroupDto>('/groups', data),
  delete: (id: number) => api.delete(`/groups/${id}`),
  addServer: (groupId: number, serverId: number) =>
    api.post(`/groups/${groupId}/servers/${serverId}`),
  removeServer: (groupId: number, serverId: number) =>
    api.delete(`/groups/${groupId}/servers/${serverId}`),
  execute: (id: number, scriptId: number) =>
    api.post<TaskDto[]>(`/groups/${id}/execute`, null, { params: { scriptId } }),
  ping: (id: number) => api.get<PingResultDto>(`/groups/${id}/ping`),
  getLastStatus: (id: number) => api.get<TaskDto[]>(`/groups/${id}/status/last`),
};

export const tasksApi = {
  getAll: (params?: { status?: string }) => api.get<TaskDto[]>('/tasks', { params }),
  getById: (id: number) => api.get<TaskDto>(`/tasks/${id}`),
  create: (data: TaskDto) => api.post<TaskDto>('/tasks', data),
  createBulk: (data: BulkTaskRequestDto) => api.post<TaskDto[]>('/tasks/bulk', data),
};

export const statsApi = {
  getStats: () => api.get<StatsDto>('/stats'),
};

export default api;
