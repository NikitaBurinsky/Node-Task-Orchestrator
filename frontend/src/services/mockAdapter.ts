import MockAdapter from 'axios-mock-adapter';
import type { AxiosInstance } from 'axios';
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

const delay = () => new Promise((resolve) => setTimeout(resolve, 300 + Math.random() * 300));

class MockDataStore {
  private servers: ServerDto[] = [
    {
      id: 1,
      hostname: 'web-prod-01',
      ipAddress: '192.168.1.10',
      port: 22,
      username: 'admin',
      password: 'secret123',
    },
    {
      id: 2,
      hostname: 'db-master',
      ipAddress: '192.168.1.20',
      port: 22,
      username: 'root',
      password: 'dbpass',
    },
    {
      id: 3,
      hostname: 'cache-redis',
      ipAddress: '192.168.1.30',
      port: 22,
      username: 'ubuntu',
      password: 'redis123',
    },
  ];

  private scripts: ScriptDto[] = [
    {
      id: 1,
      name: 'System Info',
      content: '#!/bin/bash\nuname -a\ndf -h\nfree -m',
      ownerName: 'admin',
      isPublic: true,
    },
    {
      id: 2,
      name: 'Docker Status',
      content: '#!/bin/bash\ndocker ps -a\ndocker images',
      ownerName: 'admin',
      isPublic: true,
    },
    {
      id: 3,
      name: 'Disk Cleanup',
      content: '#!/bin/bash\nsudo apt-get clean\nsudo apt-get autoremove -y',
      ownerName: 'admin',
      isPublic: false,
    },
  ];

  private groups: ServerGroupDto[] = [
    {
      id: 1,
      name: 'Production Servers',
      serverIds: [1, 2],
    },
    {
      id: 2,
      name: 'Cache Cluster',
      serverIds: [3],
    },
  ];

  private tasks: TaskDto[] = [
    {
      id: 1,
      status: 'SUCCESS',
      output: 'Linux web-prod-01 5.15.0 x86_64\n/dev/sda1       50G   25G   25G  50% /\ntotal        used        free\nMem:          16384        8192        8192',
      serverId: 1,
      scriptId: 1,
      startedAt: new Date(Date.now() - 3600000).toISOString(),
      finishedAt: new Date(Date.now() - 3540000).toISOString(),
    },
  ];

  private nextServerId = 4;
  private nextScriptId = 4;
  private nextGroupId = 3;
  private nextTaskId = 2;

  private safeCounter = 42;
  private unsafeCounter = 17;

  getServers() {
    return [...this.servers];
  }

  getServerById(id: number) {
    return this.servers.find((s) => s.id === id);
  }

  getServersByHostname(hostname: string) {
    return this.servers.filter((s) => s.hostname.includes(hostname));
  }

  createServer(server: ServerDto) {
    const newServer = { ...server, id: this.nextServerId++ };
    this.servers.push(newServer);
    return newServer;
  }

  getScripts() {
    return [...this.scripts];
  }

  getScriptById(id: number) {
    return this.scripts.find((s) => s.id === id);
  }

  createScript(script: ScriptDto) {
    const newScript = { ...script, id: this.nextScriptId++, ownerName: 'admin' };
    this.scripts.push(newScript);
    return newScript;
  }

  deleteScript(id: number) {
    const index = this.scripts.findIndex((s) => s.id === id);
    if (index !== -1) {
      this.scripts.splice(index, 1);
    }
  }

  getGroups() {
    return [...this.groups];
  }

  getGroupById(id: number) {
    return this.groups.find((g) => g.id === id);
  }

  createGroup(group: ServerGroupDto) {
    const newGroup = { ...group, id: this.nextGroupId++, serverIds: [] };
    this.groups.push(newGroup);
    return newGroup;
  }

  deleteGroup(id: number) {
    const index = this.groups.findIndex((g) => g.id === id);
    if (index !== -1) {
      this.groups.splice(index, 1);
    }
  }

  addServerToGroup(groupId: number, serverId: number) {
    const group = this.groups.find((g) => g.id === groupId);
    if (group && !group.serverIds?.includes(serverId)) {
      group.serverIds = [...(group.serverIds || []), serverId];
    }
  }

  removeServerFromGroup(groupId: number, serverId: number) {
    const group = this.groups.find((g) => g.id === groupId);
    if (group) {
      group.serverIds = (group.serverIds || []).filter((id) => id !== serverId);
    }
  }

  getTasks() {
    return [...this.tasks];
  }

  getTaskById(id: number) {
    return this.tasks.find((t) => t.id === id);
  }

  createTask(task: TaskDto) {
    const newTask: TaskDto = {
      ...task,
      id: this.nextTaskId++,
      status: 'PENDING',
      output: '',
      startedAt: new Date().toISOString(),
    };
    this.tasks.push(newTask);

    setTimeout(() => this.updateTaskStatus(newTask.id!, 'RUNNING'), 1000);
    setTimeout(() => this.simulateTaskCompletion(newTask.id!), 5000);

    return newTask;
  }

  private updateTaskStatus(taskId: number, status: TaskDto['status']) {
    const task = this.tasks.find((t) => t.id === taskId);
    if (task) {
      task.status = status;
      if (status === 'RUNNING') {
        task.output = '> Connecting to server...\n> Executing script...\n';
      }
    }
  }

  private simulateTaskCompletion(taskId: number) {
    const task = this.tasks.find((t) => t.id === taskId);
    if (task) {
      task.status = 'SUCCESS';
      task.finishedAt = new Date().toISOString();
      const script = this.scripts.find((s) => s.id === task.scriptId);
      const server = this.servers.find((s) => s.id === task.serverId);
      task.output = `> Connecting to server...\n> Executing script...\n> Connected to ${server?.hostname}\n\n${script?.content}\n\n> Script completed successfully\n> Exit code: 0`;
    }
  }

  createBulkTasks(request: BulkTaskRequestDto) {
    const groupId = this.nextGroupId - 1;
    return request.serverIds.map((serverId) =>
      this.createTask({ serverId, scriptId: request.scriptId, sourceGroupId: groupId })
    );
  }

  getLastGroupTasks(groupId: number) {
    return this.tasks.filter((t) => t.sourceGroupId === groupId);
  }

  getStats(): StatsDto {
    this.safeCounter += Math.floor(Math.random() * 5);
    this.unsafeCounter += Math.floor(Math.random() * 2);
    return {
      safeCounter: this.safeCounter,
      unsafeCounter: this.unsafeCounter,
    };
  }

  pingServer(serverId: number): PingResultDto {
    return {
      status: 'online',
      latency: Math.floor(Math.random() * 50) + 10,
      timestamp: new Date().toISOString(),
    };
  }

  pingGroup(groupId: number): PingResultDto {
    const group = this.groups.find((g) => g.id === groupId);
    const result: PingResultDto = {};
    (group?.serverIds || []).forEach((serverId) => {
      result[serverId.toString()] = Math.random() > 0.1;
    });
    return result;
  }
}

const store = new MockDataStore();

export function setupMockAdapter(axiosInstance: AxiosInstance) {
  const mock = new MockAdapter(axiosInstance, { delayResponse: 0 });

  mock.onPost('/api/auth/login').reply(async (config) => {
    await delay();
    const data: AuthRequestDto = JSON.parse(config.data);
    if (data.username && data.password) {
      const response: AuthResponseDto = {
        token: 'mock-jwt-token-' + btoa(data.username),
      };
      return [200, response];
    }
    return [401, { message: 'Invalid credentials' }];
  });

  mock.onPost('/api/auth/register').reply(async (config) => {
    await delay();
    const response: AuthResponseDto = {
      token: 'mock-jwt-token-new-user',
    };
    return [200, response];
  });

  mock.onGet('/api/servers').reply(async (config) => {
    await delay();
    const hostname = config.params?.hostname;
    if (hostname) {
      return [200, store.getServersByHostname(hostname)];
    }
    return [200, store.getServers()];
  });

  mock.onGet(/\/api\/servers\/\d+$/).reply(async (config) => {
    await delay();
    const id = parseInt(config.url!.split('/').pop()!);
    const server = store.getServerById(id);
    return server ? [200, server] : [404, { message: 'Server not found' }];
  });

  mock.onPost('/api/servers').reply(async (config) => {
    await delay();
    const server: ServerDto = JSON.parse(config.data);
    const newServer = store.createServer(server);
    return [200, newServer];
  });

  mock.onGet(/\/api\/servers\/\d+\/ping$/).reply(async (config) => {
    await delay();
    const id = parseInt(config.url!.split('/')[3]);
    return [200, store.pingServer(id)];
  });

  mock.onGet('/api/scripts').reply(async () => {
    await delay();
    return [200, store.getScripts()];
  });

  mock.onGet(/\/api\/scripts\/\d+$/).reply(async (config) => {
    await delay();
    const id = parseInt(config.url!.split('/').pop()!);
    const script = store.getScriptById(id);
    return script ? [200, script] : [404, { message: 'Script not found' }];
  });

  mock.onPost('/api/scripts').reply(async (config) => {
    await delay();
    const script: ScriptDto = JSON.parse(config.data);
    const newScript = store.createScript(script);
    return [200, newScript];
  });

  mock.onDelete(/\/api\/scripts\/\d+$/).reply(async (config) => {
    await delay();
    const id = parseInt(config.url!.split('/').pop()!);
    store.deleteScript(id);
    return [200];
  });

  mock.onGet('/api/groups').reply(async () => {
    await delay();
    return [200, store.getGroups()];
  });

  mock.onGet(/\/api\/groups\/\d+$/).reply(async (config) => {
    await delay();
    const id = parseInt(config.url!.split('/').pop()!);
    const group = store.getGroupById(id);
    return group ? [200, group] : [404, { message: 'Group not found' }];
  });

  mock.onPost('/api/groups').reply(async (config) => {
    await delay();
    const group: ServerGroupDto = JSON.parse(config.data);
    const newGroup = store.createGroup(group);
    return [200, newGroup];
  });

  mock.onDelete(/\/api\/groups\/\d+$/).reply(async (config) => {
    await delay();
    const id = parseInt(config.url!.split('/').pop()!);
    store.deleteGroup(id);
    return [200];
  });

  mock.onPost(/\/api\/groups\/\d+\/servers\/\d+$/).reply(async (config) => {
    await delay();
    const parts = config.url!.split('/');
    const groupId = parseInt(parts[3]);
    const serverId = parseInt(parts[5]);
    store.addServerToGroup(groupId, serverId);
    return [200];
  });

  mock.onDelete(/\/api\/groups\/\d+\/servers\/\d+$/).reply(async (config) => {
    await delay();
    const parts = config.url!.split('/');
    const groupId = parseInt(parts[3]);
    const serverId = parseInt(parts[5]);
    store.removeServerFromGroup(groupId, serverId);
    return [200];
  });

  mock.onPost(/\/api\/groups\/\d+\/execute$/).reply(async (config) => {
    await delay();
    const groupId = parseInt(config.url!.split('/')[3]);
    const scriptId = parseInt(config.params.scriptId);
    const group = store.getGroupById(groupId);
    const tasks = (group?.serverIds || []).map((serverId) =>
      store.createTask({ serverId, scriptId, sourceGroupId: groupId })
    );
    return [200, tasks];
  });

  mock.onGet(/\/api\/groups\/\d+\/ping$/).reply(async (config) => {
    await delay();
    const id = parseInt(config.url!.split('/')[3]);
    return [200, store.pingGroup(id)];
  });

  mock.onGet(/\/api\/groups\/\d+\/status\/last$/).reply(async (config) => {
    await delay();
    const id = parseInt(config.url!.split('/')[3]);
    return [200, store.getLastGroupTasks(id)];
  });

  mock.onGet('/api/tasks').reply(async () => {
    await delay();
    return [200, store.getTasks()];
  });

  mock.onGet(/\/api\/tasks\/\d+$/).reply(async (config) => {
    await delay();
    const id = parseInt(config.url!.split('/').pop()!);
    const task = store.getTaskById(id);
    return task ? [200, task] : [404, { message: 'Task not found' }];
  });

  mock.onPost('/api/tasks').reply(async (config) => {
    await delay();
    const task: TaskDto = JSON.parse(config.data);
    const newTask = store.createTask(task);
    return [200, newTask];
  });

  mock.onPost('/api/tasks/bulk').reply(async (config) => {
    await delay();
    const request: BulkTaskRequestDto = JSON.parse(config.data);
    const tasks = store.createBulkTasks(request);
    return [200, tasks];
  });

  mock.onGet('/api/stats').reply(async () => {
    await delay();
    return [200, store.getStats()];
  });

  return mock;
}
