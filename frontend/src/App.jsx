import { Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './components/common/ProtectedRoute';
import AppShell from './components/layout/AppShell';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import StudentHomePage from './pages/student/StudentHomePage';
import FoodHistoryPage from './pages/student/FoodHistoryPage';
import ProfilePage from './pages/ProfilePage';
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import AdminMenuPage from './pages/admin/AdminMenuPage';
import AdminWastePage from './pages/admin/AdminWastePage';
import NotFoundPage from './pages/NotFoundPage';
import { ROLES } from './utils/constants';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<ProtectedRoute role={ROLES.STUDENT} />}>
        <Route element={<AppShell />}>
          <Route path="/" element={<StudentHomePage />} />
          <Route path="/food-history" element={<FoodHistoryPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute role={ROLES.ADMIN} />}>
        <Route element={<AppShell />}>
          <Route path="/admin" element={<AdminDashboardPage />} />
          <Route path="/admin/menu" element={<AdminMenuPage />} />
          <Route path="/admin/waste" element={<AdminWastePage />} />
        </Route>
      </Route>

      {/* Profile is available to any signed-in user */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route path="/profile" element={<ProfilePage />} />
        </Route>
      </Route>

      <Route path="/home" element={<Navigate to="/" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
