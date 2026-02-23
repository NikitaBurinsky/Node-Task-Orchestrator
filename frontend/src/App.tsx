import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { AuthGuard } from './components/AuthGuard';
import { Layout } from './components/Layout';
import { Login } from './pages/Login';
import { Dashboard } from './pages/Dashboard';
import { Servers } from './pages/Servers';
import { ServerGroups } from './pages/ServerGroups';
import { GroupDetail } from './pages/GroupDetail';
import { Scripts } from './pages/Scripts';
import { Tasks } from './pages/Tasks';
import { TaskTerminal } from './pages/TaskTerminal';
import { Register } from './pages/Register';

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} /> {}
          <Route
            path="/"
            element={
              <AuthGuard>
                <Layout />
              </AuthGuard>
            }
          >
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="servers" element={<Servers />} />
            <Route path="groups" element={<ServerGroups />} />
            <Route path="groups/:id" element={<GroupDetail />} />
            <Route path="scripts" element={<Scripts />} />
            <Route path="tasks" element={<Tasks />} />
            <Route path="tasks/:id" element={<TaskTerminal />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
