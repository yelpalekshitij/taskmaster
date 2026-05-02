import { Routes } from '@angular/router';
import { autoLoginPartialRoutesGuard } from 'angular-auth-oidc-client';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [autoLoginPartialRoutesGuard]
  },
  {
    path: 'tasks',
    loadComponent: () => import('./features/tasks/task-list.component').then(m => m.TaskListComponent),
    canActivate: [autoLoginPartialRoutesGuard]
  },
  {
    path: 'tasks/board',
    loadComponent: () => import('./features/tasks/kanban-board.component').then(m => m.KanbanBoardComponent),
    canActivate: [autoLoginPartialRoutesGuard]
  },
  {
    path: 'notifications',
    loadComponent: () => import('./features/notifications/notification-list.component').then(m => m.NotificationListComponent),
    canActivate: [autoLoginPartialRoutesGuard]
  },
  {
    path: 'profile',
    loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [autoLoginPartialRoutesGuard]
  },
  { path: 'login', loadComponent: () => import('./core/auth/login.component').then(m => m.LoginComponent) },
  { path: 'callback', loadComponent: () => import('./core/auth/callback.component').then(m => m.CallbackComponent) },
  { path: '**', redirectTo: '/login' }
];
