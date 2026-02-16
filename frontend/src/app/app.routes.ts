import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Dashboard } from './pages/dashboard/dashboard';
import { authGuard } from './services/auth/auth.guard';
import { guestGuard } from './services/auth/guest.guard';

export const routes: Routes = [
    { path: 'login', canActivate: [guestGuard], component: Login },
    { path: 'dashboard', canActivate: [authGuard], component: Dashboard },

    { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    { path: '**', redirectTo: 'login' }
];
