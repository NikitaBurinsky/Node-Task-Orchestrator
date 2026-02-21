import axios from 'axios';
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
} from '../types/api';

const api = axios.create({
  baseURL: 'https://formatis.online/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// setupMockAdapter(api);

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const authApi = {
  login: (data: AuthRequestDto) => api.post<AuthResponseDto>('/auth/login', data),
  register: (data: AuthRequestDto) => api.post<AuthResponseDto>('/auth/register', data),
};

export const serversApi = {
  getAll: (hostname?: string) => api.get<ServerDto[]>('/servers', { params: { hostname } }),
  getById: (id: number) => api.get<ServerDto>(`/servers/${id}`),
  create: (data: ServerDto) => api.post<ServerDto>('/servers', data),
  ping: (id: number) => api.get<PingResultDto>(`/servers/${id}/ping`),
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
  getAll: () => api.get<TaskDto[]>('/tasks'),
  getById: (id: number) => api.get<TaskDto>(`/tasks/${id}`),
  create: (data: TaskDto) => api.post<TaskDto>('/tasks', data),
  createBulk: (data: BulkTaskRequestDto) => api.post<TaskDto[]>('/tasks/bulk', data),
};

export const statsApi = {
  getStats: () => api.get<StatsDto>('/stats'),
};

export default api;
