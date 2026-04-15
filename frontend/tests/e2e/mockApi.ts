import type { Page, Route } from '@playwright/test';

type TaskStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';

interface Server {
  id: number;
  hostname: string;
  ipAddress: string;
  port: number;
  username: string;
  password?: string;
}

interface Script {
  id: number;
  name: string;
  content: string;
  ownerName: string;
  isPublic: boolean;
}

interface Group {
  id: number;
  name: string;
  serverIds: number[];
}

interface TaskRecord {
  id: number;
  status: TaskStatus;
  output: string;
  serverId: number;
  scriptId: number;
  startedAt: string;
  finishedAt?: string;
  pollCount: number;
}

function nowIso() {
  return new Date().toISOString();
}

function json(route: Route, body: unknown, status = 200) {
  return route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  });
}

export async function installMockApi(page: Page) {
  let serverSeq = 4;
  let scriptSeq = 3;
  let groupSeq = 3;
  let taskSeq = 200;

  const serverPingMap: Record<number, boolean> = {
    1: true,
    2: false,
    3: true,
  };

  const servers: Server[] = [
    { id: 1, hostname: 'web-01', ipAddress: '10.0.0.1', port: 22, username: 'root' },
    { id: 2, hostname: 'db-01', ipAddress: '10.0.0.2', port: 22, username: 'postgres' },
    { id: 3, hostname: 'cache-01', ipAddress: '10.0.0.3', port: 22, username: 'redis' },
  ];

  const scripts: Script[] = [
    {
      id: 1,
      name: 'deploy.sh',
      content: '#!/bin/bash\necho Deploying...',
      ownerName: 'operator',
      isPublic: true,
    },
    {
      id: 2,
      name: 'health-check.sh',
      content: '#!/bin/bash\necho HEALTH OK',
      ownerName: 'operator',
      isPublic: true,
    },
  ];

  const groups: Group[] = [
    { id: 1, name: 'Default', serverIds: [1, 2, 3] },
    { id: 2, name: 'Edge', serverIds: [1] },
  ];

  const tasks: TaskRecord[] = [
    {
      id: 100,
      status: 'RUNNING',
      output: '[task:100] Running deployment...',
      serverId: 1,
      scriptId: 1,
      startedAt: nowIso(),
      pollCount: 0,
    },
  ];

  const toGroupDto = (group: Group) => ({
    id: group.id,
    name: group.name,
    servers: group.serverIds
      .map((serverId) => servers.find((server) => server.id === serverId))
      .filter(Boolean),
  });

  const toTaskDto = (task: TaskRecord) => ({
    id: task.id,
    status: task.status,
    output: task.output,
    serverId: task.serverId,
    scriptId: task.scriptId,
    startedAt: task.startedAt,
    finishedAt: task.finishedAt,
  });

  const parseBody = async <T>(route: Route): Promise<T | null> => {
    const body = route.request().postData();
    if (!body) return null;
    try {
      return JSON.parse(body) as T;
    } catch {
      return null;
    }
  };

  const maybeFailAuth = (route: Route) => {
    const auth = route.request().headers().authorization;
    const url = route.request().url();
    if (url.includes('/auth/')) {
      return false;
    }
    if (!auth) {
      void json(route, { message: 'Unauthorized' }, 401);
      return true;
    }
    return false;
  };

  await page.route('https://api.nto.formatis.online/api/**', async (route) => {
    const request = route.request();
    const method = request.method();
    const url = new URL(request.url());
    const path = url.pathname.replace('/api', '');

    if (path === '/auth/refresh' && method === 'POST') {
      return json(route, { accessToken: 'test-access-token', expiresIn: 900 });
    }
    if (path === '/auth/login' && method === 'POST') {
      return json(route, { accessToken: 'test-access-token', expiresIn: 900 });
    }
    if (path === '/auth/register' && method === 'POST') {
      return json(route, { accessToken: 'test-access-token', expiresIn: 900 });
    }
    if (path === '/auth/logout' && method === 'POST') {
      return json(route, {});
    }

    if (maybeFailAuth(route)) {
      return;
    }

    if (path === '/servers' && method === 'GET') {
      const hostname = url.searchParams.get('hostname');
      if (!hostname) {
        return json(route, servers);
      }
      return json(
        route,
        servers.filter((server) => server.hostname.toLowerCase().includes(hostname.toLowerCase()))
      );
    }

    if (path === '/servers' && method === 'POST') {
      const payload = await parseBody<Partial<Server>>(route);
      const server: Server = {
        id: serverSeq++,
        hostname: payload?.hostname ?? `server-${serverSeq}`,
        ipAddress: payload?.ipAddress ?? `10.0.0.${serverSeq}`,
        port: payload?.port ?? 22,
        username: payload?.username ?? 'root',
        password: payload?.password,
      };
      servers.push(server);
      serverPingMap[server.id] = true;
      groups.find((group) => group.name === 'Default')?.serverIds.push(server.id);
      return json(route, server, 201);
    }

    const serverByIdMatch = path.match(/^\/servers\/(\d+)$/);
    if (serverByIdMatch && method === 'GET') {
      const id = Number(serverByIdMatch[1]);
      const server = servers.find((item) => item.id === id);
      if (!server) {
        return json(route, { message: 'Server not found' }, 404);
      }
      return json(route, server);
    }

    if (serverByIdMatch && method === 'DELETE') {
      const id = Number(serverByIdMatch[1]);
      const index = servers.findIndex((item) => item.id === id);
      if (index < 0) {
        return json(route, { message: 'Server not found' }, 404);
      }
      servers.splice(index, 1);
      groups.forEach((group) => {
        group.serverIds = group.serverIds.filter((serverId) => serverId !== id);
      });
      delete serverPingMap[id];
      return json(route, {});
    }

    const serverPingMatch = path.match(/^\/servers\/(\d+)\/ping$/);
    if (serverPingMatch && method === 'GET') {
      const id = Number(serverPingMatch[1]);
      if (!servers.some((item) => item.id === id)) {
        return json(route, { message: 'Server not found' }, 404);
      }
      return json(route, {
        serverId: id,
        alive: serverPingMap[id] ?? false,
        timestamp: nowIso(),
      });
    }

    if (path === '/scripts' && method === 'GET') {
      return json(route, scripts);
    }

    if (path === '/scripts' && method === 'POST') {
      const payload = await parseBody<Partial<Script>>(route);
      const script: Script = {
        id: scriptSeq++,
        name: payload?.name ?? `script-${scriptSeq}`,
        content: payload?.content ?? '#!/bin/bash\necho hello',
        ownerName: payload?.ownerName ?? 'operator',
        isPublic: Boolean(payload?.isPublic ?? true),
      };
      scripts.push(script);
      return json(route, script, 201);
    }

    const scriptByIdMatch = path.match(/^\/scripts\/(\d+)$/);
    if (scriptByIdMatch && method === 'GET') {
      const id = Number(scriptByIdMatch[1]);
      const script = scripts.find((item) => item.id === id);
      if (!script) {
        return json(route, { message: 'Script not found' }, 404);
      }
      return json(route, script);
    }

    if (scriptByIdMatch && method === 'DELETE') {
      const id = Number(scriptByIdMatch[1]);
      const index = scripts.findIndex((item) => item.id === id);
      if (index < 0) {
        return json(route, { message: 'Script not found' }, 404);
      }
      scripts.splice(index, 1);
      return json(route, {});
    }

    if (path === '/groups' && method === 'GET') {
      return json(route, groups.map(toGroupDto));
    }

    if (path === '/groups' && method === 'POST') {
      const payload = await parseBody<{ name?: string }>(route);
      const group: Group = {
        id: groupSeq++,
        name: payload?.name ?? `Group ${groupSeq}`,
        serverIds: [],
      };
      groups.push(group);
      return json(route, toGroupDto(group), 201);
    }

    const groupByIdMatch = path.match(/^\/groups\/(\d+)$/);
    if (groupByIdMatch && method === 'GET') {
      const id = Number(groupByIdMatch[1]);
      const group = groups.find((item) => item.id === id);
      if (!group) {
        return json(route, { message: 'Group not found' }, 404);
      }
      return json(route, toGroupDto(group));
    }

    if (groupByIdMatch && method === 'DELETE') {
      const id = Number(groupByIdMatch[1]);
      const index = groups.findIndex((item) => item.id === id);
      if (index < 0) {
        return json(route, { message: 'Group not found' }, 404);
      }
      groups.splice(index, 1);
      return json(route, {});
    }

    const groupAddServerMatch = path.match(/^\/groups\/(\d+)\/servers\/(\d+)$/);
    if (groupAddServerMatch && method === 'POST') {
      const groupId = Number(groupAddServerMatch[1]);
      const serverId = Number(groupAddServerMatch[2]);
      const group = groups.find((item) => item.id === groupId);
      if (!group) {
        return json(route, { message: 'Group not found' }, 404);
      }
      if (!group.serverIds.includes(serverId)) {
        group.serverIds.push(serverId);
      }
      return json(route, toGroupDto(group), 201);
    }

    if (groupAddServerMatch && method === 'DELETE') {
      const groupId = Number(groupAddServerMatch[1]);
      const serverId = Number(groupAddServerMatch[2]);
      const group = groups.find((item) => item.id === groupId);
      if (!group) {
        return json(route, { message: 'Group not found' }, 404);
      }
      group.serverIds = group.serverIds.filter((id) => id !== serverId);
      return json(route, {});
    }

    const groupPingMatch = path.match(/^\/groups\/(\d+)\/ping$/);
    if (groupPingMatch && method === 'GET') {
      const groupId = Number(groupPingMatch[1]);
      const group = groups.find((item) => item.id === groupId);
      if (!group) {
        return json(route, { message: 'Group not found' }, 404);
      }
      const result: Record<string, boolean> = {};
      group.serverIds.forEach((serverId) => {
        result[String(serverId)] = serverPingMap[serverId] ?? false;
      });
      return json(route, result);
    }

    const groupExecuteMatch = path.match(/^\/groups\/(\d+)\/execute$/);
    if (groupExecuteMatch && method === 'POST') {
      const groupId = Number(groupExecuteMatch[1]);
      const scriptId = Number(url.searchParams.get('scriptId'));
      const group = groups.find((item) => item.id === groupId);
      if (!group || !scriptId) {
        return json(route, { message: 'Invalid execute request' }, 400);
      }

      const created: TaskRecord[] = group.serverIds.map((serverId) => ({
        id: taskSeq++,
        status: 'RUNNING',
        output: `[task:${taskSeq}] Executing script ${scriptId} on server ${serverId}`,
        serverId,
        scriptId,
        startedAt: nowIso(),
        pollCount: 0,
      }));
      tasks.unshift(...created);
      return json(route, created.map((task) => toTaskDto(task)), 201);
    }

    if (path === '/tasks' && method === 'GET') {
      const status = url.searchParams.get('status');
      const filtered = status
        ? tasks.filter((task) => task.status === status)
        : tasks;
      return json(route, filtered.map((task) => toTaskDto(task)));
    }

    const taskByIdMatch = path.match(/^\/tasks\/(\d+)$/);
    if (taskByIdMatch && method === 'GET') {
      const id = Number(taskByIdMatch[1]);
      const task = tasks.find((item) => item.id === id);
      if (!task) {
        return json(route, { message: 'Task not found' }, 404);
      }

      task.pollCount += 1;
      if (task.status === 'RUNNING' && task.pollCount >= 2) {
        task.status = 'SUCCESS';
        task.finishedAt = nowIso();
        task.output = `${task.output}\nDone.`;
      }

      return json(route, toTaskDto(task));
    }

    if (path === '/tasks' && method === 'POST') {
      const payload = await parseBody<Partial<TaskRecord>>(route);
      const task: TaskRecord = {
        id: taskSeq++,
        status: payload?.status ?? 'PENDING',
        output: payload?.output ?? '',
        serverId: payload?.serverId ?? 1,
        scriptId: payload?.scriptId ?? 1,
        startedAt: nowIso(),
        finishedAt: payload?.finishedAt,
        pollCount: 0,
      };
      tasks.unshift(task);
      return json(route, toTaskDto(task), 201);
    }

    return json(route, { message: `Unhandled mock endpoint: ${method} ${path}` }, 501);
  });
}
