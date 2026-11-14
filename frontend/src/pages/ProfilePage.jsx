import ShadowCard from '../components/common/ShadowCard';
import { ASSET_PATHS } from '../utils/constants';
import { useAuth } from '../context/AuthContext';

export default function ProfilePage() {
  const { user } = useAuth();
  const fields = [
    ['Name', user.fullName],
    ['Registration Number', user.registrationNumber],
    ['Department', user.department],
    ['Hostel Name', user.hostelName],
    ['Hostel Room', user.hostelRoom],
  ];
  return (
    <div className="mx-auto max-w-2xl">
      <ShadowCard tone="butter" className="p-6 sm:p-8">
        <div className="mb-5 flex items-center gap-4">
          <img src={ASSET_PATHS.icons.profile} alt="" className="h-16 w-16" />
          <h1 className="text-3xl font-semibold">My <span className="text-sage">Profile</span></h1>
        </div>
        <dl className="divide-y divide-forest/20 bg-white/70">
          {fields.map(([label, value]) => (
            <div key={label} className="grid grid-cols-2 gap-4 px-4 py-3">
              <dt className="font-medium text-forest">{label}</dt>
              <dd>{value || '—'}</dd>
            </div>
          ))}
        </dl>
      </ShadowCard>
    </div>
  );
}
