import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { TaskListComponent } from './task-list.component';
import { TaskService } from '../../core/services/task.service';
import { Task, TaskPage } from '../../core/models/task.model';

function makeTask(overrides: Partial<Task> = {}): Task {
  return {
    id: 'task-1',
    title: 'Test Task',
    status: 'TODO',
    priority: 'MEDIUM',
    createdBy: 'user-1',
    tenantId: 'tenant-1',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    tags: [],
    ...overrides,
  };
}

function makePage(tasks: Task[], total = tasks.length): TaskPage {
  return { content: tasks, totalElements: total, totalPages: 1, pageNumber: 0, pageSize: 20 };
}

describe('TaskListComponent', () => {
  let component: TaskListComponent;
  let fixture: ComponentFixture<TaskListComponent>;
  let mockTaskService: jest.Mocked<Pick<TaskService, 'getTasks' | 'updateTaskStatus'>>;
  let mockDialog: { open: jest.Mock };

  beforeEach(async () => {
    mockTaskService = {
      getTasks: jest.fn().mockReturnValue(of(makePage([makeTask()]))),
      updateTaskStatus: jest.fn().mockReturnValue(of({ id: 'task-1', status: 'DONE' })),
    };
    mockDialog = { open: jest.fn().mockReturnValue({ afterClosed: () => of(null) }) };

    await TestBed.configureTestingModule({
      imports: [TaskListComponent],
      providers: [
        { provide: TaskService, useValue: mockTaskService },
        { provide: MatDialog, useValue: mockDialog },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(TaskListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('calls getTasks on init with default page and size', () => {
    expect(mockTaskService.getTasks).toHaveBeenCalledWith(
      expect.objectContaining({ page: 0, size: 20 })
    );
  });

  it('populates dataSource on successful load', () => {
    expect(component.dataSource.data.length).toBe(1);
    expect(component.totalElements).toBe(1);
    expect(component.loading).toBe(false);
    expect(component.error).toBe(false);
  });

  it('sets error=true when getTasks fails', () => {
    mockTaskService.getTasks.mockReturnValueOnce(throwError(() => new Error('fail')));
    component.loadTasks();

    expect(component.error).toBe(true);
    expect(component.loading).toBe(false);
  });

  describe('onStatusFilter', () => {
    it('resets currentPage to 0 and calls loadTasks', () => {
      component.currentPage = 3;
      component.onStatusFilter('IN_PROGRESS');

      expect(component.selectedStatus).toBe('IN_PROGRESS');
      expect(component.currentPage).toBe(0);
      expect(mockTaskService.getTasks).toHaveBeenCalledWith(
        expect.objectContaining({ status: 'IN_PROGRESS', page: 0 })
      );
    });
  });

  describe('onPriorityFilter', () => {
    it('resets currentPage to 0 and calls loadTasks', () => {
      component.currentPage = 2;
      component.onPriorityFilter('HIGH');

      expect(component.selectedPriority).toBe('HIGH');
      expect(component.currentPage).toBe(0);
      expect(mockTaskService.getTasks).toHaveBeenCalledWith(
        expect.objectContaining({ priority: 'HIGH', page: 0 })
      );
    });
  });

  describe('onPage', () => {
    it('updates page and size then reloads', () => {
      component.onPage({ pageIndex: 2, pageSize: 50, length: 100 } as any);

      expect(component.currentPage).toBe(2);
      expect(component.pageSize).toBe(50);
      expect(mockTaskService.getTasks).toHaveBeenCalledWith(
        expect.objectContaining({ page: 2, size: 50 })
      );
    });
  });

  describe('updateStatus', () => {
    it('calls updateTaskStatus with correct input then reloads', () => {
      const task = makeTask();
      component.updateStatus(task, 'DONE');

      expect(mockTaskService.updateTaskStatus).toHaveBeenCalledWith({
        taskId: 'task-1',
        status: 'DONE',
      });
    });
  });

  describe('openCreateDialog', () => {
    it('opens the dialog and reloads when result is truthy', () => {
      mockDialog.open.mockReturnValue({ afterClosed: () => of({ title: 'New' }) });
      const callsBefore = (mockTaskService.getTasks as jest.Mock).mock.calls.length;

      component.openCreateDialog();

      expect(mockDialog.open).toHaveBeenCalled();
      expect((mockTaskService.getTasks as jest.Mock).mock.calls.length).toBeGreaterThan(callsBefore);
    });

    it('does not reload when dialog is dismissed', () => {
      mockDialog.open.mockReturnValue({ afterClosed: () => of(null) });
      const callsBefore = (mockTaskService.getTasks as jest.Mock).mock.calls.length;

      component.openCreateDialog();

      expect((mockTaskService.getTasks as jest.Mock).mock.calls.length).toBe(callsBefore);
    });
  });

  describe('isOverdue', () => {
    it('returns true for a past due task that is not DONE', () => {
      const past = new Date(Date.now() - 86_400_000).toISOString();
      const task = makeTask({ dueDate: past, status: 'TODO' });
      expect(component.isOverdue(task)).toBe(true);
    });

    it('returns false for a DONE task regardless of due date', () => {
      const past = new Date(Date.now() - 86_400_000).toISOString();
      const task = makeTask({ dueDate: past, status: 'DONE' });
      expect(component.isOverdue(task)).toBe(false);
    });

    it('returns false when no due date is set', () => {
      const task = makeTask({ dueDate: undefined, status: 'IN_PROGRESS' });
      expect(component.isOverdue(task)).toBe(false);
    });

    it('returns false for a future due date', () => {
      const future = new Date(Date.now() + 86_400_000).toISOString();
      const task = makeTask({ dueDate: future, status: 'TODO' });
      expect(component.isOverdue(task)).toBe(false);
    });
  });
});
