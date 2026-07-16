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
    showInNavbar: true,
    exact: true
  },
  {
    label: 'Korisnici',
    path: '/users',
    icon: 'pi pi-users',
    roles: ['ADMIN'],
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
    label: 'Assignmenti',
    path: '/assignments',
    icon: 'pi pi-list-check',
    roles: ['PROF', 'ADMIN', 'STUDENT'],
    showInNavbar: true
  },
  {
    label: 'Predaje',
    path: '/submissions',
    icon: 'pi pi-folder',
    roles: ['PROF', 'ADMIN', 'STUDENT'],
    showInNavbar: true,
    exact: true
  },
  {
    label: 'Grupe',
    path: '/groups',
    icon: 'pi pi-sitemap',
    roles: ['PROF', 'ADMIN', 'STUDENT'],
    showInNavbar: true
  },
  {
    label: 'Profil',
    path: '/profile',
    icon: 'pi pi-user',
    showInNavbar: true
  },
  {
    label: 'Postavke',
    path: '/settings',
    icon: 'pi pi-cog',
    showInNavbar: true
  },
];

export function getRolesForPath(path: string): AppRole[] {
  return APP_NAV_ITEMS.find(item => item.path === path)?.roles ?? [];
}
