import { NavLink } from 'react-router-dom';
import { ASSET_PATHS } from '../../utils/constants';
import { useAuth } from '../../context/AuthContext';

const STUDENT_NAV = [
  { to: '/', label: 'Home', icon: ASSET_PATHS.icons.home, end: true },
  { to: '/food-history', label: 'Food History', icon: ASSET_PATHS.icons.foodHistory },
];

const ADMIN_NAV = [
  { to: '/admin', label: 'Dashboard', icon: ASSET_PATHS.icons.adminDashboard, end: true },
  { to: '/admin/menu', label: 'Menu', icon: ASSET_PATHS.icons.adminMenu },
  { to: '/admin/waste', label: 'Waste', icon: ASSET_PATHS.icons.adminWaste },
];

function NavItem({ to, label, icon, end }) {
  return (
    <NavLink to={to} end={end} title={label} aria-label={label}
      className={({ isActive }) =>
        `flex flex-col items-center gap-1 rounded-lg p-2 text-[11px] font-medium text-forest transition ${isActive ? 'bg-butter/80 ring-2 ring-tangerine' : 'hover:bg-white/40'}`}>
      <img src={icon} alt="" className="h-9 w-9" />
      <span className="hidden lg:block">{label}</span>
    </NavLink>
  );
}

export default function Sidebar() {
  const { isAdmin, logout } = useAuth();
  const items = isAdmin ? ADMIN_NAV : STUDENT_NAV;

  return (
    <nav aria-label="Main navigation"
      className="fixed inset-x-0 bottom-0 z-40 flex items-center justify-around bg-sage-soft px-2 py-2 shadow-[0_-4px_0_0_var(--color-tangerine)] lg:inset-y-4 lg:left-4 lg:w-24 lg:flex-col lg:justify-start lg:gap-4 lg:px-2 lg:py-8 lg:shadow-[8px_8px_0_0_var(--color-tangerine)]">
      {items.map((item) => <NavItem key={item.to} {...item} />)}
      <button type="button" onClick={logout} title="Logout" aria-label="Logout"
        className="flex flex-col items-center gap-1 rounded-lg p-2 text-[11px] font-medium text-forest hover:bg-white/40 lg:mt-auto">
        <img src={ASSET_PATHS.icons.logout} alt="" className="h-9 w-9" />
        <span className="hidden lg:block">Logout</span>
      </button>
    </nav>
  );
}
