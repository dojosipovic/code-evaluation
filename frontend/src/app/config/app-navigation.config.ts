import { AppRole } from "./app-types";


export interface AppNavItem {
  label: string;
  path: string;
  icon?: string;
  roles?: AppRole[];
  showInNavbar?: boolean;
  exact?: boolean;
};

export const APP_NAV_ITEMS: AppNavItem[] = [
  {
    label: 'Dashboard',
    path: '/dashboard',
    icon: 'pi pi-home',
    roles: ['PROF', 'ADMIN', 'STUDENT'],
    showInNavbar: true,
    exact: true
  },
  {
    label: 'Korisnici',
    path: '/users',
    icon: 'pi pi-users',
    roles: ['PROF', 'ADMIN', 'STUDENT'],
    showInNavbar: true
  },
  {
    label: 'Zadaci',
    path: '/tasks',
    icon: 'pi pi-receipt',
    roles: ['PROF', 'ADMIN'],
    showInNavbar: true
  },
  {
    label: 'Profil',
    path: '/profile',
    icon: 'pi pi-user',
    roles: ['PROF', 'ADMIN', 'STUDENT'],
    showInNavbar: true
  },
  {
    label: 'Postavke',
    path: '/settings',
    icon: 'pi pi-cog',
    roles: ['PROF', 'ADMIN', 'STUDENT'],
    showInNavbar: true
  },
];

export function getRolesForPath(path: string): AppRole[] {
  return APP_NAV_ITEMS.find(item => item.path === path)?.roles ?? [];
}
