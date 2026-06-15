import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Dashboard } from './pages/dashboard/dashboard';
import { authGuard } from './services/auth/auth.guard';
import { guestGuard } from './services/auth/guest.guard';
import { AppLayout } from './layout/app-layout';
import { getRolesForPath } from './config/app-navigation.config';
import { Users } from './pages/users/users';
import { Register } from './pages/register/register';
import { Tasks } from './pages/tasks/tasks';
import { Groups } from './pages/groups/groups';
import { GroupView } from './pages/group-view/group-view';
import { AssignmentSolve } from './pages/assignment-solve/assignment-solve';

export const routes: Routes = [
    { path: 'login', canActivate: [guestGuard], component: Login },
    { path: 'register', canActivate: [guestGuard], component: Register },
    { path: 'assignment/:id/solve', canActivate: [authGuard], component: AssignmentSolve },
    {
        path: '',
        component: AppLayout,
        canActivate: [authGuard],
        children: [
            { path: 'dashboard', canActivate: [authGuard], component: Dashboard, data: { roles: getRolesForPath('/dashboard'), breadcrumb: 'Dashboard' } },
            { path: 'users/:tab', canActivate: [authGuard], component: Users, data: { roles: getRolesForPath('/users'), breadcrumb: 'Access management' } },
            { path: 'profile', canActivate: [authGuard], component: Dashboard, data: { roles: getRolesForPath('/profile'), breadcrumb: 'Profil' } },
            { path: 'settings', canActivate: [authGuard], component: Dashboard, data: { roles: getRolesForPath('/settings'), breadcrumb: 'Postavke' } },
            { path: 'tasks', canActivate: [authGuard], component: Tasks, data: { roles: getRolesForPath('/tasks'), breadcrumb: 'Zadaci' } },
            { path: 'groups/:id/:tab', canActivate: [authGuard], component: GroupView, data: { roles: getRolesForPath('/groups'), breadcrumb: 'Grupe', backTo: '/groups' } },
            { path: 'groups', canActivate: [authGuard], component: Groups, data: { roles: getRolesForPath('/groups'), breadcrumb: 'Grupe' } },

            { path: 'users', redirectTo: 'users/users', pathMatch: 'full' },
            { path: 'groups/:id', redirectTo: 'groups/:id/users', pathMatch: 'full' },
            { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
        ]
    },
    // { path: 'dashboard', canActivate: [authGuard], component: Dashboard },

    // { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    { path: '**', redirectTo: 'login' }
];
