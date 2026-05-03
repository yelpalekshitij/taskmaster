import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
import { TaskService } from '../../core/services/task.service';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { Task, TaskPage } from '../../core/models/task.model';

function makeTask(overrides: Partial<Task> = {}): Task {
  return {
    id: 'task-1',
    title: 'Task',
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

function makePage(tasks: Task[]): TaskPage {
  return { content: tasks, totalElements: tasks.length, totalPages: 1, pageNumber: 0, pageSize: 20 };
}

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let mockTaskService: jest.Mocked<Pick<TaskService, 'getMyTasks'>>;
  let mockOidcService: { getUserData: jest.Mock };

  beforeEach(async () => {
    mockTaskService = { getMyTasks: jest.fn() };
    mockOidcService = { getUserData: jest.fn().mockReturnValue(of({ given_name: 'Alice' })) };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: TaskService, useValue: mockTaskService },
        { provide: OidcSecurityService, useValue: mockOidcService },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    mockTaskService.getMyTasks.mockReturnValue(of(makePage([])));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('sets userName from OIDC given_name on init', () => {
    mockTaskService.getMyTasks.mockReturnValue(of(makePage([])));
    fixture.detectChanges();
    expect(component.userName).toBe('Alice');
  });

  it('falls back to preferred_username when given_name is absent', () => {
    mockOidcService.getUserData.mockReturnValue(of({ preferred_username: 'alice_user' }));
    mockTaskService.getMyTasks.mockReturnValue(of(makePage([])));
    fixture.detectChanges();
    expect(component.userName).toBe('alice_user');
  });

  it('sets loading=false and populates recentTasks on successful load', () => {
    const tasks = [makeTask(), makeTask({ id: 'task-2' })];
    mockTaskService.getMyTasks.mockReturnValue(of(makePage(tasks)));
    fixture.detectChanges();

    expect(component.loading).toBe(false);
    expect(component.error).toBe(false);
    expect(component.recentTasks.length).toBe(2);
  });

  it('limits recentTasks to 5 even if more are returned', () => {
    const tasks = Array.from({ length: 8 }, (_, i) => makeTask({ id: `t-${i}` }));
    mockTaskService.getMyTasks.mockReturnValue(of(makePage(tasks)));
    fixture.detectChanges();

    expect(component.recentTasks.length).toBe(5);
  });

  it('sets loading=false and error=true when service call fails', () => {
    mockTaskService.getMyTasks.mockReturnValue(throwError(() => new Error('Network error')));
    fixture.detectChanges();

    expect(component.loading).toBe(false);
    expect(component.error).toBe(true);
  });

  describe('computeStats', () => {
    it('counts total tasks correctly', () => {
      const tasks = [makeTask(), makeTask({ id: 't2' }), makeTask({ id: 't3' })];
      mockTaskService.getMyTasks.mockReturnValue(of(makePage(tasks)));
      fixture.detectChanges();

      expect(component.stats.total).toBe(3);
    });

    it('counts IN_PROGRESS tasks correctly', () => {
      const tasks = [
        makeTask({ status: 'IN_PROGRESS' }),
        makeTask({ id: 't2', status: 'IN_PROGRESS' }),
        makeTask({ id: 't3', status: 'TODO' }),
      ];
      mockTaskService.getMyTasks.mockReturnValue(of(makePage(tasks)));
      fixture.detectChanges();

      expect(component.stats.inProgress).toBe(2);
    });

    it('counts overdue tasks (past due, not DONE)', () => {
      const yesterday = new Date(Date.now() - 86_400_000).toISOString();
      const tasks = [
        makeTask({ dueDate: yesterday, status: 'TODO' }),
        makeTask({ id: 't2', dueDate: yesterday, status: 'DONE' }), // should NOT count
        makeTask({ id: 't3', status: 'IN_PROGRESS' }), // no dueDate — should NOT count
      ];
      mockTaskService.getMyTasks.mockReturnValue(of(makePage(tasks)));
      fixture.detectChanges();

      expect(component.stats.overdue).toBe(1);
    });

    it('counts tasks completed today', () => {
      const todayMidStr = new Date().toISOString();
      const yesterdayStr = new Date(Date.now() - 86_400_000).toISOString();
      const tasks = [
        makeTask({ status: 'DONE', updatedAt: todayMidStr }),
        makeTask({ id: 't2', status: 'DONE', updatedAt: yesterdayStr }), // done yesterday
        makeTask({ id: 't3', status: 'IN_PROGRESS', updatedAt: todayMidStr }), // not DONE
      ];
      mockTaskService.getMyTasks.mockReturnValue(of(makePage(tasks)));
      fixture.detectChanges();

      expect(component.stats.doneToday).toBe(1);
    });
  });

  describe('isOverdue', () => {
    it('returns true for a past date', () => {
      const past = new Date(Date.now() - 86_400_000).toISOString();
      expect(component.isOverdue(past)).toBe(true);
    });

    it('returns false for a future date', () => {
      const future = new Date(Date.now() + 86_400_000).toISOString();
      expect(component.isOverdue(future)).toBe(false);
    });
  });
});
