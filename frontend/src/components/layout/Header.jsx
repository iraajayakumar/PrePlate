import { Link } from 'react-router-dom';
import { ASSET_PATHS } from '../../utils/constants';
import { useAuth } from '../../context/AuthContext';
import NotificationBell from './NotificationBell';

export default function Header() {
  const { isAdmin } = useAuth();
  return (
    <header className="flex items-center justify-between pb-4">
      <Link to={isAdmin ? '/admin' : '/'} aria-label="PrePlate home">
        <img src={ASSET_PATHS.logo} alt="PrePlate - Know the demand, prepare smarter, waste less" className="h-14 sm:h-20 lg:h-24" />
      </Link>
      <div className="flex items-center gap-3 sm:gap-6">
        {!isAdmin && <NotificationBell />}
        <Link to="/profile" aria-label="My profile">
          <img src={ASSET_PATHS.icons.profile} alt="" className="h-11 w-11 sm:h-14 sm:w-14" />
        </Link>
      </div>
    </header>
  );
}
