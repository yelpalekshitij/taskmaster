import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatBadgeModule,
    MatTooltipModule,
    MatDividerModule,
  ],
  template: `
    <mat-toolbar color="primary" class="app-toolbar">
      <span class="app-title">
        <mat-icon>task_alt</mat-icon>
        TaskMaster
      </span>

      <nav class="nav-links" *ngIf="isAuthenticated$ | async">
        <a mat-button routerLink="/dashboard" routerLinkActive="active-link">
          <mat-icon>dashboard</mat-icon>
          Dashboard
        </a>
        <a mat-button routerLink="/tasks" routerLinkActive="active-link" [routerLinkActiveOptions]="{exact: true}">
          <mat-icon>list</mat-icon>
          Tasks
        </a>
        <a mat-button routerLink="/tasks/board" routerLinkActive="active-link">
          <mat-icon>view_kanban</mat-icon>
          Board
        </a>
      </nav>

      <span class="spacer"></span>

      <ng-container *ngIf="isAuthenticated$ | async">
        <button mat-icon-button
                routerLink="/notifications"
                matTooltip="Notifications"
                [matBadge]="unreadCount > 0 ? unreadCount.toString() : null"
                matBadgeColor="warn">
          <mat-icon>notifications</mat-icon>
        </button>

        <button mat-icon-button [matMenuTriggerFor]="userMenu" matTooltip="Account">
          <mat-icon>account_circle</mat-icon>
        </button>

        <mat-menu #userMenu="matMenu">
          <div class="user-menu-header" *ngIf="userEmail$ | async as email">
            <mat-icon>person</mat-icon>
            <span>{{ email }}</span>
          </div>
          <mat-divider></mat-divider>
          <a mat-menu-item routerLink="/profile">
            <mat-icon>manage_accounts</mat-icon>
            Profile
          </a>
          <button mat-menu-item (click)="logout()">
            <mat-icon>logout</mat-icon>
            Logout
          </button>
        </mat-menu>
      </ng-container>
    </mat-toolbar>

    <main class="main-content">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    .app-toolbar {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      z-index: 1000;
    }

    .app-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 20px;
      font-weight: 600;
      margin-right: 24px;
    }

    .nav-links {
      display: flex;
      gap: 4px;

      a {
        display: flex;
        align-items: center;
        gap: 4px;
        color: white;
        text-decoration: none;

        &.active-link {
          background-color: rgba(255,255,255,0.2);
          border-radius: 4px;
        }
      }
    }

    .spacer {
      flex: 1 1 auto;
    }

    .main-content {
      margin-top: 64px;
      min-height: calc(100vh - 64px);
    }

    .user-menu-header {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      font-size: 14px;
      color: rgba(0,0,0,0.6);
    }

    ::ng-deep .mat-badge-content {
      font-size: 10px;
    }
  `]
})
export class AppComponent implements OnInit {
  private readonly oidcService = inject(OidcSecurityService);

  isAuthenticated$!: Observable<boolean>;
  userEmail$!: Observable<string>;
  unreadCount = 0;

  ngOnInit(): void {
    this.isAuthenticated$ = this.oidcService.isAuthenticated$.pipe(
      map(auth => auth.isAuthenticated)
    );

    this.userEmail$ = this.oidcService.getUserData().pipe(
      map(userData => userData?.email || userData?.preferred_username || '')
    );
  }

  logout(): void {
    this.oidcService.logoff().subscribe();
  }
}
