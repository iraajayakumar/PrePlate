import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Header from './Header';

/** Layout shared by every authenticated page: sidebar + header + routed content. */
export default function AppShell() {
  return (
    <div className="min-h-screen bg-white">
      <Sidebar />
      <div className="px-4 pb-28 pt-4 sm:px-8 lg:pb-10 lg:pl-36 lg:pr-10">
        <Header />
        <main>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
