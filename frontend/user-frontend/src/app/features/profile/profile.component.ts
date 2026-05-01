import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { NotificationService } from '../../core/services/notification.service';
import { NotificationPreferences } from '../../core/models/notification.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  template: `
    <div class="page-container">
      <h1>Profile</h1>

      <!-- User Info Card -->
      <mat-card class="profile-card">
        <mat-card-header>
          <div mat-card-avatar class="profile-avatar">
            <mat-icon>account_circle</mat-icon>
          </div>
          <mat-card-title>{{ userData?.given_name || userData?.preferred_username || 'User' }}</mat-card-title>
          <mat-card-subtitle>{{ userData?.email }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <div class="user-details">
            <div class="detail-row" *ngIf="userData?.given_name">
              <mat-icon>person</mat-icon>
              <span>{{ userData.given_name }} {{ userData?.family_name }}</span>
            </div>
            <div class="detail-row">
              <mat-icon>email</mat-icon>
              <span>{{ userData?.email }}</span>
            </div>
            <div class="detail-row" *ngIf="userData?.preferred_username">
              <mat-icon>badge</mat-icon>
              <span>{{ userData.preferred_username }}</span>
            </div>
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Notification Preferences -->
      <mat-card class="preferences-card">
        <mat-card-header>
          <mat-card-title>
            <mat-icon>notifications_active</mat-icon>
            Notification Preferences
          </mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <div *ngIf="loadingPrefs" class="loading-container">
            <mat-spinner diameter="32"></mat-spinner>
          </div>

          <form [formGroup]="preferencesForm" *ngIf="!loadingPrefs">
            <div class="pref-section">
              <h3>Channels</h3>
              <div class="pref-row">
                <div class="pref-info">
                  <mat-icon>email</mat-icon>
                  <div>
                    <span class="pref-label">Email Notifications</span>
                    <span class="pref-desc">Receive notifications via email</span>
                  </div>
                </div>
                <mat-slide-toggle formControlName="emailEnabled" color="primary"></mat-slide-toggle>
              </div>

              <mat-divider></mat-divider>

              <div class="pref-row">
                <div class="pref-info">
                  <mat-icon>notifications</mat-icon>
                  <div>
                    <span class="pref-label">Push Notifications</span>
                    <span class="pref-desc">Receive push notifications in browser</span>
                  </div>
                </div>
                <mat-slide-toggle formControlName="pushEnabled" color="primary"></mat-slide-toggle>
              </div>
            </div>

            <mat-divider></mat-divider>

            <div class="pref-section">
              <h3>Events</h3>

              <div class="pref-row">
                <div class="pref-info">
                  <mat-icon>assignment_ind</mat-icon>
                  <div>
                    <span class="pref-label">Task Assigned</span>
                    <span class="pref-desc">When a task is assigned to you</span>
                  </div>
                </div>
                <mat-slide-toggle formControlName="taskAssigned" color="primary"></mat-slide-toggle>
              </div>

              <mat-divider></mat-divider>

              <div class="pref-row">
                <div class="pref-info">
                  <mat-icon>update</mat-icon>
                  <div>
                    <span class="pref-label">Task Updated</span>
                    <span class="pref-desc">When your task is updated</span>
                  </div>
                </div>
                <mat-slide-toggle formControlName="taskUpdated" color="primary"></mat-slide-toggle>
              </div>

              <mat-divider></mat-divider>

              <div class="pref-row">
                <div class="pref-info">
                  <mat-icon>comment</mat-icon>
                  <div>
                    <span class="pref-label">New Comments</span>
                    <span class="pref-desc">When someone comments on your task</span>
                  </div>
                </div>
                <mat-slide-toggle formControlName="taskCommented" color="primary"></mat-slide-toggle>
              </div>

              <mat-divider></mat-divider>

              <div class="pref-row">
                <div class="pref-info">
                  <mat-icon>alarm</mat-icon>
                  <div>
                    <span class="pref-label">Due Soon</span>
                    <span class="pref-desc">When a task is due within 24 hours</span>
                  </div>
                </div>
                <mat-slide-toggle formControlName="taskDueSoon" color="primary"></mat-slide-toggle>
              </div>

              <mat-divider></mat-divider>

              <div class="pref-row">
                <div class="pref-info">
                  <mat-icon>warning</mat-icon>
                  <div>
                    <span class="pref-label">Overdue Tasks</span>
                    <span class="pref-desc">When a task becomes overdue</span>
                  </div>
                </div>
                <mat-slide-toggle formControlName="taskOverdue" color="primary"></mat-slide-toggle>
              </div>
            </div>

            <div class="form-actions">
              <button mat-raised-button color="primary" (click)="savePreferences()" [disabled]="savingPrefs">
                <mat-spinner diameter="18" *ngIf="savingPrefs"></mat-spinner>
                <span *ngIf="!savingPrefs">Save Preferences</span>
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .profile-card, .preferences-card {
      margin-bottom: 24px;
    }

    .profile-avatar {
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: #e3f2fd;
      border-radius: 50%;

      mat-icon {
        font-size: 40px;
        width: 40px;
        height: 40px;
        color: #1565c0;
      }
    }

    .user-details {
      margin-top: 16px;
    }

    .detail-row {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 8px 0;
      font-size: 14px;

      mat-icon { color: rgba(0,0,0,0.5); }
    }

    mat-card-title {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .pref-section {
      padding: 16px 0;

      h3 {
        margin: 0 0 16px;
        font-size: 14px;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.5px;
        color: rgba(0,0,0,0.6);
      }
    }

    .pref-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 0;
    }

    .pref-info {
      display: flex;
      align-items: center;
      gap: 12px;

      mat-icon { color: rgba(0,0,0,0.5); }

      div {
        display: flex;
        flex-direction: column;
        gap: 2px;
      }

      .pref-label {
        font-size: 14px;
        font-weight: 500;
      }

      .pref-desc {
        font-size: 12px;
        color: rgba(0,0,0,0.5);
      }
    }

    .form-actions {
      padding-top: 16px;
      display: flex;
      justify-content: flex-end;
    }
  `]
})
export class ProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private oidcService = inject(OidcSecurityService);
  private notificationService = inject(NotificationService);
  private snackBar = inject(MatSnackBar);

  userData: any;
  loadingPrefs = false;
  savingPrefs = false;

  preferencesForm: FormGroup = this.fb.group({
    emailEnabled: [true],
    pushEnabled: [false],
    taskAssigned: [true],
    taskUpdated: [true],
    taskCommented: [true],
    taskDueSoon: [true],
    taskOverdue: [true],
  });

  ngOnInit(): void {
    this.oidcService.getUserData().subscribe(data => {
      this.userData = data;
    });

    this.loadPreferences();
  }

  loadPreferences(): void {
    this.loadingPrefs = true;
    this.notificationService.getPreferences().subscribe({
      next: (prefs) => {
        this.preferencesForm.patchValue(prefs);
        this.loadingPrefs = false;
      },
      error: () => {
        this.loadingPrefs = false;
      }
    });
  }

  savePreferences(): void {
    this.savingPrefs = true;
    const values = this.preferencesForm.value;

    this.notificationService.updatePreferences(values).subscribe({
      next: () => {
        this.savingPrefs = false;
        this.snackBar.open('Preferences saved', 'Close', { duration: 3000 });
      },
      error: () => {
        this.savingPrefs = false;
        this.snackBar.open('Failed to save preferences', 'Close', { duration: 3000 });
      }
    });
  }
}
