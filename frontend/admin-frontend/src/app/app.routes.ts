import { Routes } from '@angular/router';
import { autoLoginPartialRoutesGuard } from 'angular-auth-oidc-client';
import { masterAdminGuard, tenantAdminGuard, anyAdminGuard } from './core/guards/role.guard';
import { UnauthorizedComponent } from './core/auth/unauthorized.component';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./core/auth/login.component').then(m => m.LoginComponent) },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [autoLoginPartialRoutesGuard, anyAdminGuard]
  },
  {
    path: 'tenants',
    loadComponent: () => import('./features/tenant-management/tenant-list.component').then(m => m.TenantListComponent),
    canActivate: [autoLoginPartialRoutesGuard, masterAdminGuard]
  },
  {
    path: 'tenants/:id/users',
    loadComponent: () => import('./features/user-management/user-list.component').then(m => m.UserListComponent),
    canActivate: [autoLoginPartialRoutesGuard, tenantAdminGuard]
  },
  {
    path: 'users',
    loadComponent: () => import('./features/user-management/user-list.component').then(m => m.UserListComponent),
    canActivate: [autoLoginPartialRoutesGuard, tenantAdminGuard]
  },
  {
    path: 'roles',
    loadComponent: () => import('./features/role-management/role-list.component').then(m => m.RoleListComponent),
    canActivate: [autoLoginPartialRoutesGuard, tenantAdminGuard]
  },
  {
    path: 'reports',
    loadComponent: () => import('./features/reports/reports.component').then(m => m.ReportsComponent),
    canActivate: [autoLoginPartialRoutesGuard, anyAdminGuard]
  },
  {
    path: 'unauthorized',
    component: UnauthorizedComponent
  },
  { path: 'callback', loadComponent: () => import('./core/auth/callback.component').then(m => m.CallbackComponent) },
  { path: '**', redirectTo: '/login' }
];
