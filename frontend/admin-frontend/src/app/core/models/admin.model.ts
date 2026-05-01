export interface Tenant {
  id: string;
  name: string;
  slug: string;
  active: boolean;
  createdAt: string;
  userCount?: number;
  taskCount?: number;
  adminEmail?: string;
}

export interface TenantPage {
  content: Tenant[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
}

export interface CreateTenantRequest {
  name: string;
  slug: string;
  adminEmail: string;
  adminFirstName: string;
  adminLastName: string;
  adminPassword: string;
}

export interface AdminUser {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  active: boolean;
  tenantId: string;
  createdAt: string;
  lastLogin?: string;
}

export interface AdminUserPage {
  content: AdminUser[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
}

export interface Role {
  id: string;
  name: string;
  description: string;
  permissions: string[];
}

export interface TenantStats {
  tenantId: string;
  tenantName: string;
  totalUsers: number;
  activeUsers: number;
  totalTasks: number;
  completedTasks: number;
  inProgressTasks: number;
  overdueTasks: number;
  taskCompletionRate: number;
}

export interface GlobalStats {
  totalTenants: number;
  activeTenants: number;
  totalUsers: number;
  activeUsers: number;
  totalTasks: number;
  completedTasks: number;
}
