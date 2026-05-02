import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { StatusBadgeComponent } from './status-badge.component';
import { TaskStatus } from '../../core/models/task.model';

describe('StatusBadgeComponent', () => {
  let component: StatusBadgeComponent;
  let fixture: ComponentFixture<StatusBadgeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusBadgeComponent],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(StatusBadgeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('label getter', () => {
    const cases: [TaskStatus, string][] = [
      ['TODO', 'To Do'],
      ['IN_PROGRESS', 'In Progress'],
      ['ON_HOLD', 'On Hold'],
      ['DONE', 'Done'],
      ['SCHEDULED', 'Scheduled'],
    ];

    cases.forEach(([status, expected]) => {
      it(`returns "${expected}" for ${status}`, () => {
        component.status = status;
        expect(component.label).toBe(expected);
      });
    });
  });

  describe('badgeClass getter', () => {
    it('returns badge-todo for TODO', () => {
      component.status = 'TODO';
      expect(component.badgeClass).toBe('badge-todo');
    });

    it('returns badge-in-progress for IN_PROGRESS', () => {
      component.status = 'IN_PROGRESS';
      expect(component.badgeClass).toBe('badge-in-progress');
    });

    it('returns badge-on-hold for ON_HOLD', () => {
      component.status = 'ON_HOLD';
      expect(component.badgeClass).toBe('badge-on-hold');
    });

    it('returns badge-done for DONE', () => {
      component.status = 'DONE';
      expect(component.badgeClass).toBe('badge-done');
    });

    it('returns badge-todo for SCHEDULED', () => {
      component.status = 'SCHEDULED';
      expect(component.badgeClass).toBe('badge-todo');
    });
  });

  it('renders the label text in the template', () => {
    component.status = 'IN_PROGRESS';
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement.querySelector('.status-badge');
    expect(el?.textContent?.trim()).toBe('In Progress');
  });

  it('applies the badge class to the span element', () => {
    component.status = 'DONE';
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement.querySelector('.status-badge');
    expect(el?.classList).toContain('badge-done');
  });
});
