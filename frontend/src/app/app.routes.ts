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
import { SubmissionView } from './pages/submission-view/submission-view';
import { AssignmentEvaluation } from './pages/assignment-evaluation/assignment-evaluation';
import { AssignmentSubmissions } from './pages/assignment-submissions/assignment-submissions';
import { Assignments } from './pages/assignments/assignments';
import { Submissions } from './pages/submissions/submissions';
import { Profile } from './pages/profile/profile';

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
            { path: 'profile', canActivate: [authGuard], component: Profile, data: { roles: getRolesForPath('/profile'), breadcrumb: 'Profil' } },
            { path: 'settings', canActivate: [authGuard], component: Dashboard, data: { roles: getRolesForPath('/settings'), breadcrumb: 'Postavke' } },
            { path: 'tasks', canActivate: [authGuard], component: Tasks, data: { roles: getRolesForPath('/tasks'), breadcrumb: 'Zadaci' } },
            { path: 'assignments', canActivate: [authGuard], component: Assignments, data: { roles: getRolesForPath('/assignments'), breadcrumb: 'Assignmenti' } },
            { path: 'assignments/:id/evaluate', canActivate: [authGuard], component: AssignmentEvaluation, data: { roles: getRolesForPath('/groups'), breadcrumb: 'Ocjenjivanje' } },
            { path: 'assignments/:id/submissions', canActivate: [authGuard], component: AssignmentSubmissions, data: { roles: getRolesForPath('/groups'), breadcrumb: 'Predaje' } },
            { path: 'submissions', canActivate: [authGuard], component: Submissions, data: { roles: getRolesForPath('/submissions'), breadcrumb: 'Predaje' } },
            { path: 'submissions/:id', canActivate: [authGuard], component: SubmissionView, data: { breadcrumb: 'Submission' } },
            { path: 'groups/:id/:tab', canActivate: [authGuard], component: GroupView, data: { roles: getRolesForPath('/groups'), breadcrumb: 'Grupe', backTo: '/groups' } },
            { path: 'groups', canActivate: [authGuard], component: Groups, data: { roles: getRolesForPath('/groups'), breadcrumb: 'Grupe' } },

            { path: 'users', redirectTo: 'users/users', pathMatch: 'full' },
            { path: 'groups/:id', redirectTo: 'groups/:id/tasks', pathMatch: 'full' },
            { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
        ]
    },
    { path: '**', redirectTo: 'login' }
];
