package nto.services;

import nto.core.entities.ServerEntity;
import nto.core.entities.SshUsernameEntity;
import nto.infrastructure.services.ssh.SshSessionManager;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SshSessionManagerTest {

    @Test
    void initShouldCreateAndStartClient() {
        SshSessionManager manager = new SshSessionManager();

        manager.init();

        SshClient client = (SshClient) ReflectionTestUtils.getField(manager, "client");
        assertNotNull(client);
        assertTrue(client.isStarted());
        manager.destroy();
    }

    @Test
    void destroyShouldCloseSessionsAndStopClient() {
        SshSessionManager manager = new SshSessionManager();
        ClientSession openSession = mock(ClientSession.class);
        ClientSession closedSession = mock(ClientSession.class);
        when(openSession.isOpen()).thenReturn(true);
        when(closedSession.isOpen()).thenReturn(false);

        Map<Long, ClientSession> sessions = sessions(manager);
        sessions.put(1L, openSession);
        sessions.put(2L, closedSession);

        SshClient client = mock(SshClient.class);
        when(client.isStarted()).thenReturn(true);
        ReflectionTestUtils.setField(manager, "client", client);

        manager.destroy();

        verify(openSession).close(true);
        verify(closedSession, never()).close(true);
        verify(client).stop();
        assertTrue(sessions.isEmpty());
    }

    @Test
    void destroyShouldSkipStoppingWhenClientIsNull() {
        SshSessionManager manager = new SshSessionManager();

        manager.destroy();

        assertTrue(sessions(manager).isEmpty());
    }

    @Test
    void destroyShouldSkipStoppingWhenClientIsNotStarted() {
        SshSessionManager manager = new SshSessionManager();
        SshClient client = mock(SshClient.class);
        when(client.isStarted()).thenReturn(false);
        ReflectionTestUtils.setField(manager, "client", client);

        manager.destroy();

        verify(client, never()).stop();
    }

    @Test
    void getOrCreateSessionShouldReturnExistingOpenSession() throws IOException {
        SshSessionManager manager = new SshSessionManager();
        SshClient client = mock(SshClient.class);
        ReflectionTestUtils.setField(manager, "client", client);

        ServerEntity server = server(1L, "root");
        ClientSession existing = mock(ClientSession.class);
        when(existing.isOpen()).thenReturn(true);
        when(existing.isClosed()).thenReturn(false);
        sessions(manager).put(1L, existing);

        ClientSession result = manager.getOrCreateSession(server);

        assertSame(existing, result);
        verify(client, never()).connect(anyString(), anyString(), anyInt());
    }

    @Test
    void getOrCreateSessionShouldReplaceExistingSessionWhenItIsClosed() throws IOException {
        SshSessionManager manager = new SshSessionManager();
        SshClient client = mock(SshClient.class);
        ReflectionTestUtils.setField(manager, "client", client);

        ConnectFuture connectFuture = mock(ConnectFuture.class);
        ClientSession existing = mock(ClientSession.class);
        ClientSession session = mock(ClientSession.class);
        AuthFuture authFuture = mock(AuthFuture.class);

        when(existing.isOpen()).thenReturn(true);
        when(existing.isClosed()).thenReturn(true);
        sessions(manager).put(1L, existing);

        when(client.connect("root", "10.0.0.1", 22)).thenReturn(connectFuture);
        when(connectFuture.verify(eq(10L), eq(TimeUnit.SECONDS))).thenReturn(connectFuture);
        when(connectFuture.getSession()).thenReturn(session);
        when(session.auth()).thenReturn(authFuture);
        when(authFuture.verify(eq(10L), eq(TimeUnit.SECONDS))).thenReturn(authFuture);

        ClientSession result = manager.getOrCreateSession(server(1L, "root"));

        assertSame(session, result);
    }

    @Test
    void getOrCreateSessionShouldReplaceExistingSessionWhenItIsNotOpen() throws IOException {
        SshSessionManager manager = new SshSessionManager();
        SshClient client = mock(SshClient.class);
        ReflectionTestUtils.setField(manager, "client", client);

        ConnectFuture connectFuture = mock(ConnectFuture.class);
        ClientSession existing = mock(ClientSession.class);
        ClientSession session = mock(ClientSession.class);
        AuthFuture authFuture = mock(AuthFuture.class);

        when(existing.isOpen()).thenReturn(false);
        sessions(manager).put(1L, existing);

        when(client.connect("root", "10.0.0.1", 22)).thenReturn(connectFuture);
        when(connectFuture.verify(eq(10L), eq(TimeUnit.SECONDS))).thenReturn(connectFuture);
        when(connectFuture.getSession()).thenReturn(session);
        when(session.auth()).thenReturn(authFuture);
        when(authFuture.verify(eq(10L), eq(TimeUnit.SECONDS))).thenReturn(authFuture);

        ClientSession result = manager.getOrCreateSession(server(1L, "root"));

        assertSame(session, result);
    }

    @Test
    void getOrCreateSessionShouldThrowWhenUsernameMissing() {
        SshSessionManager manager = new SshSessionManager();
        ReflectionTestUtils.setField(manager, "client", mock(SshClient.class));

        ServerEntity server = server(1L, null);

        assertThrows(IllegalStateException.class, () -> manager.getOrCreateSession(server));
    }

    @Test
    void getOrCreateSessionShouldThrowWhenUsernameBlank() {
        SshSessionManager manager = new SshSessionManager();
        ReflectionTestUtils.setField(manager, "client", mock(SshClient.class));

        ServerEntity server = server(1L, " ");

        assertThrows(IllegalStateException.class, () -> manager.getOrCreateSession(server));
    }

    @Test
    void getOrCreateSessionShouldCreateAndCacheNewSession() throws IOException {
        SshSessionManager manager = new SshSessionManager();
        SshClient client = mock(SshClient.class);
        ReflectionTestUtils.setField(manager, "client", client);

        ConnectFuture connectFuture = mock(ConnectFuture.class);
        ClientSession session = mock(ClientSession.class);
        AuthFuture authFuture = mock(AuthFuture.class);

        when(client.connect("root", "10.0.0.1", 22)).thenReturn(connectFuture);
        when(connectFuture.verify(eq(10L), eq(TimeUnit.SECONDS))).thenReturn(connectFuture);
        when(connectFuture.getSession()).thenReturn(session);
        when(session.auth()).thenReturn(authFuture);
        when(authFuture.verify(eq(10L), eq(TimeUnit.SECONDS))).thenReturn(authFuture);

        ServerEntity server = server(2L, "root");

        ClientSession result = manager.getOrCreateSession(server);

        assertSame(session, result);
        assertEquals(session, sessions(manager).get(2L));
        verify(session).addPasswordIdentity("pw");
    }

    @Test
    void invalidateSessionShouldCloseAndRemoveOpenSession() {
        SshSessionManager manager = new SshSessionManager();
        ClientSession session = mock(ClientSession.class);
        when(session.isClosed()).thenReturn(false);
        sessions(manager).put(5L, session);

        manager.invalidateSession(5L);

        verify(session).close(true);
        assertTrue(sessions(manager).isEmpty());
    }

    @Test
    void invalidateSessionShouldSkipCloseForAlreadyClosedSession() {
        SshSessionManager manager = new SshSessionManager();
        ClientSession session = mock(ClientSession.class);
        when(session.isClosed()).thenReturn(true);
        sessions(manager).put(5L, session);

        manager.invalidateSession(5L);

        verify(session, never()).close(true);
        assertTrue(sessions(manager).isEmpty());
    }

    @Test
    void invalidateSessionShouldDoNothingWhenSessionMissing() {
        SshSessionManager manager = new SshSessionManager();

        manager.invalidateSession(5L);

        assertTrue(sessions(manager).isEmpty());
    }

    @Test
    void invalidateSessionShouldSwallowCloseExceptions() {
        SshSessionManager manager = new SshSessionManager();
        ClientSession session = mock(ClientSession.class);
        when(session.isClosed()).thenReturn(false);
        doThrow(new RuntimeException("boom")).when(session).close(true);
        sessions(manager).put(5L, session);

        manager.invalidateSession(5L);

        verify(session).close(true);
        assertTrue(sessions(manager).isEmpty());
    }

    @SuppressWarnings("unchecked")
    private Map<Long, ClientSession> sessions(SshSessionManager manager) {
        return (Map<Long, ClientSession>) ReflectionTestUtils.getField(manager, "sessions");
    }

    private ServerEntity server(Long id, String sshUsername) {
        ServerEntity server = new ServerEntity();
        server.setId(id);
        server.setIpAddress("10.0.0.1");
        server.setPort(22);
        server.setPassword("pw");
        if (sshUsername != null) {
            server.setSshUsername(SshUsernameEntity.builder().username(sshUsername).build());
        }
        return server;
    }
}
