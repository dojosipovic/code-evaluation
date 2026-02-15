import { Routes } from '@angular/router';
import { Login } from './login/login';

export const routes: Routes = [
    { path: 'login', component: Login },

    { path: '', pathMatch: 'full', redirectTo: 'login' },
    { path: '**', redirectTo: 'login' }
];
