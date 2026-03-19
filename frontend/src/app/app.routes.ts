import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Dashboard } from './pages/dashboard/dashboard';
import { authGuard } from './services/auth/auth.guard';
import { guestGuard } from './services/auth/guest.guard';
import { AppLayout } from './layout/app-layout';
import { getRolesForPath } from './config/app-navigation.config';

export const routes: Routes = [
    { path: 'login', canActivate: [guestGuard], component: Login },
    {
        path: '',
        component: AppLayout,
        canActivate: [authGuard],
        children: [
            { path: 'dashboard', canActivate: [authGuard], component: Dashboard, data: { roles: getRolesForPath('/dashboard') } },
            { path: 'profile', canActivate: [authGuard], component: Dashboard, data: { roles: getRolesForPath('/profile') } },
            { path: 'settings', canActivate: [authGuard], component: Dashboard, data: { roles: getRolesForPath('/settings') } },
            { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
        ]
    },
    // { path: 'dashboard', canActivate: [authGuard], component: Dashboard },

    // { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    { path: '**', redirectTo: 'login' }
];
