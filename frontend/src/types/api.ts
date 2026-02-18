export type TaskStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';

export interface TaskDto {
  id?: number;
  status?: TaskStatus;
  output?: string;
  serverId?: number;
  scriptId?: number;
  sourceGroupId?: number;
  startedAt?: string;
  finishedAt?: string;
}

export interface BulkTaskRequestDto {
  scriptId: number;
  serverIds: number[];
}

export interface ServerDto {
  id?: number;
  hostname: string;
  ipAddress: string;
  port?: number;
  username: string;
  password?: string;
}

export interface ScriptDto {
  id?: number;
  name?: string;
  content?: string;
  ownerName?: string;
  isPublic?: boolean;
}

export interface ServerGroupDto {
  id?: number;
  name: string;
  serverIds?: number[];
}

export interface UserDto {
  id?: number;
  username?: string;
  password?: string;
}

export interface AuthRequestDto {
  username?: string;
  password?: string;
}

export interface AuthResponseDto {
  token?: string;
}

export interface StatsDto {
  [key: string]: number;
}

export interface PingResultDto {
  [key: string]: boolean;
}
