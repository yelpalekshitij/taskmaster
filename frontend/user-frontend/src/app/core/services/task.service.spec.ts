import { TestBed } from '@angular/core/testing';
import { ApolloTestingModule, ApolloTestingController } from 'apollo-angular/testing';
import { TaskService } from './task.service';
import { Task, TaskPage } from '../models/task.model';

const mockTask: Task = {
  id: 'task-1',
  title: 'Test Task',
  status: 'TODO',
  priority: 'MEDIUM',
  createdBy: 'user-1',
  tenantId: 'tenant-1',
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
  tags: [],
};

const mockPage: TaskPage = {
  content: [mockTask],
  totalElements: 1,
  totalPages: 1,
  pageNumber: 0,
  pageSize: 20,
};

describe('TaskService', () => {
  let service: TaskService;
  let controller: ApolloTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ApolloTestingModule],
      providers: [TaskService],
    });

    service = TestBed.inject(TaskService);
    controller = TestBed.inject(ApolloTestingController);
  });

  afterEach(() => controller.verify());

  describe('getTasks', () => {
    it('emits the tasks page from the server response', () => {
      let result: TaskPage | undefined;

      service.getTasks().subscribe(page => (result = page));

      const op = controller.expectOne('GetTasks');
      op.flushData({ tasks: mockPage });

      expect(result).toEqual(mockPage);
    });

    it('sends status and priority filter variables when provided', () => {
      service.getTasks({ status: 'TODO', priority: 'HIGH', page: 1, size: 10 }).subscribe();

      const op = controller.expectOne('GetTasks');
      expect(op.operation.variables['filter']).toEqual({
        status: 'TODO',
        priority: 'HIGH',
        assignedTo: undefined,
      });
      expect(op.operation.variables['page']).toBe(1);
      expect(op.operation.variables['size']).toBe(10);
      op.flushData({ tasks: mockPage });
    });

    it('sends null filter when no filter provided', () => {
      service.getTasks().subscribe();

      const op = controller.expectOne('GetTasks');
      expect(op.operation.variables['filter']).toBeNull();
      op.flushData({ tasks: mockPage });
    });
  });

  describe('getMyTasks', () => {
    it('emits my tasks page from the server response', () => {
      let result: TaskPage | undefined;

      service.getMyTasks(undefined, 0, 5).subscribe(page => (result = page));

      const op = controller.expectOne('GetMyTasks');
      op.flushData({ myTasks: mockPage });

      expect(result).toEqual(mockPage);
    });

    it('sends page and size variables', () => {
      service.getMyTasks(undefined, 2, 15).subscribe();

      const op = controller.expectOne('GetMyTasks');
      expect(op.operation.variables['page']).toBe(2);
      expect(op.operation.variables['size']).toBe(15);
      op.flushData({ myTasks: mockPage });
    });
  });

  describe('getTask', () => {
    it('emits the single task from the server response', () => {
      let result: Task | undefined;

      service.getTask('task-1').subscribe(t => (result = t));

      const op = controller.expectOne('GetTask');
      expect(op.operation.variables['id']).toBe('task-1');
      op.flushData({ task: mockTask });

      expect(result).toEqual(mockTask);
    });
  });

  describe('createTask', () => {
    it('emits the created task from the server response', () => {
      let result: Task | undefined;

      service.createTask({ title: 'New Task', priority: 'HIGH' }).subscribe(t => (result = t));

      const op = controller.expectOne('CreateTask');
      expect(op.operation.variables['input']).toEqual({ title: 'New Task', priority: 'HIGH' });
      op.flushData({ createTask: { ...mockTask, title: 'New Task' } });

      expect(result?.title).toBe('New Task');
    });
  });

  describe('updateTaskStatus', () => {
    it('sends id and status to the mutation', () => {
      service.updateTaskStatus({ taskId: 'task-1', status: 'DONE' }).subscribe();

      const op = controller.expectOne('UpdateTaskStatus');
      expect(op.operation.variables['id']).toBe('task-1');
      expect(op.operation.variables['status']).toBe('DONE');
      op.flushData({ updateTaskStatus: { id: 'task-1', status: 'DONE', updatedAt: '' } });
    });
  });

  describe('assignTask', () => {
    it('sends task id and user id to the mutation', () => {
      service.assignTask({ taskId: 'task-1', assigneeId: 'user-42' }).subscribe();

      const op = controller.expectOne('AssignTask');
      expect(op.operation.variables['id']).toBe('task-1');
      expect(op.operation.variables['userId']).toBe('user-42');
      op.flushData({ assignTask: { id: 'task-1', assignedTo: 'user-42' } });
    });
  });

  describe('addComment', () => {
    it('sends taskId and content to the mutation', () => {
      service.addComment({ taskId: 'task-1', content: 'A comment' }).subscribe();

      const op = controller.expectOne('AddComment');
      expect(op.operation.variables['taskId']).toBe('task-1');
      expect(op.operation.variables['content']).toBe('A comment');
      op.flushData({ addComment: { id: 'c-1', content: 'A comment', createdAt: '', userId: 'u-1' } });
    });
  });
});
