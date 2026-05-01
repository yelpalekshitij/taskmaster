export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'ON_HOLD' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Task {
  id: string;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: TaskPriority;
  assignee?: TaskUser;
  createdBy: TaskUser;
  dueDate?: string;
  createdAt: string;
  updatedAt: string;
  tenantId: string;
  tags?: string[];
  comments?: TaskComment[];
}

export interface TaskUser {
  id: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
}

export interface TaskComment {
  id: string;
  content: string;
  author: TaskUser;
  createdAt: string;
}

export interface TaskPage {
  content: Task[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
}

export interface CreateTaskInput {
  title: string;
  description?: string;
  priority: TaskPriority;
  assigneeId?: string;
  dueDate?: string;
  tags?: string[];
}

export interface UpdateTaskStatusInput {
  taskId: string;
  status: TaskStatus;
}

export interface AssignTaskInput {
  taskId: string;
  assigneeId: string;
}

export interface AddCommentInput {
  taskId: string;
  content: string;
}

export interface TaskFilter {
  status?: TaskStatus;
  priority?: TaskPriority;
  assigneeId?: string;
  page?: number;
  size?: number;
}
