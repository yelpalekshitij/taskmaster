import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles: string[];
}

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
    MatSidenavModule,
    MatListModule,
    MatTooltipModule,
    MatDividerModule,
  ],
  template: `
    <mat-sidenav-container class="sidenav-container">
      <!-- Sidebar -->
      <mat-sidenav mode="side" opened class="sidenav" *ngIf="isAuthenticated$ | async">
        <a class="sidenav-header" routerLink="/dashboard">
          <mat-icon class="logo-icon">admin_panel_settings</mat-icon>
          <div>
            <div class="app-name">TaskMaster</div>
            <div class="app-subtitle">Admin Console</div>
          </div>
        </a>

        <mat-divider></mat-divider>

        <mat-nav-list>
          <ng-container *ngFor="let item of getVisibleNavItems()">
            <a mat-list-item
               [routerLink]="item.route"
               routerLinkActive="active-nav-item">
              <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
              <span matListItemTitle>{{ item.label }}</span>
            </a>
          </ng-container>
        </mat-nav-list>

        <div class="sidenav-footer">
          <mat-divider></mat-divider>
          <div class="user-info" *ngIf="userEmail$ | async as email">
            <mat-icon>account_circle</mat-icon>
            <div class="user-details">
              <span class="user-email">{{ email }}</span>
              <span class="user-role" *ngIf="userRole$ | async as role">{{ role }}</span>
            </div>
          </div>
          <button mat-button class="logout-btn" (click)="logout()">
            <mat-icon>logout</mat-icon>
            Sign Out
          </button>
        </div>
      </mat-sidenav>

      <!-- Main Content -->
      <mat-sidenav-content>
        <!-- Top Bar (when not authenticated) -->
        <mat-toolbar color="primary" *ngIf="!(isAuthenticated$ | async)" class="top-toolbar">
          <mat-icon>admin_panel_settings</mat-icon>
          <span class="toolbar-title">TaskMaster Admin</span>
        </mat-toolbar>

        <main>
          <router-outlet></router-outlet>
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .sidenav-container {
      height: 100vh;
    }

    .sidenav {
      width: 240px;
      display: flex;
      flex-direction: column;
      background-color: #311b92;
      color: white;
    }

    .sidenav-header {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 20px 16px;
      text-decoration: none;
      color: white;
      cursor: pointer;

      .logo-icon {
        font-size: 36px;
        width: 36px;
        height: 36px;
        color: #ce93d8;
      }

      .app-name {
        font-size: 18px;
        font-weight: 600;
        line-height: 1.2;
      }

      .app-subtitle {
        font-size: 11px;
        color: rgba(255,255,255,0.6);
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }
    }

    mat-nav-list {
      flex: 1;
      padding-top: 8px;

      ::ng-deep .mat-mdc-list-item {
        color: rgba(255,255,255,0.8) !important;
        border-radius: 0;
        margin: 2px 8px;
        border-radius: 4px;

        &:hover {
          background-color: rgba(255,255,255,0.1) !important;
        }
      }

      .active-nav-item {
        background-color: rgba(255,255,255,0.15) !important;
        color: white !important;

        ::ng-deep .mat-icon { color: #ce93d8 !important; }
      }
    }

    .sidenav-footer {
      padding: 0 8px 16px;

      .user-info {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 12px 8px;
        color: rgba(255,255,255,0.8);

        mat-icon { color: rgba(255,255,255,0.6); }

        .user-details {
          display: flex;
          flex-direction: column;
          overflow: hidden;
        }

        .user-email {
          font-size: 12px;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .user-role {
          font-size: 10px;
          color: rgba(255,255,255,0.5);
          text-transform: uppercase;
        }
      }

      .logout-btn {
        width: 100%;
        color: rgba(255,255,255,0.7);
        justify-content: flex-start;
        gap: 8px;

        &:hover { color: white; }
      }
    }

    mat-sidenav-content {
      background-color: #f5f5f5;
    }

    .top-toolbar {
      .toolbar-title {
        margin-left: 8px;
        font-size: 18px;
      }
    }

    main {
      min-height: 100%;
    }
  `]
})
export class AppComponent implements OnInit {
  private oidcService = inject(OidcSecurityService);

  isAuthenticated$!: Observable<boolean>;
  userEmail$!: Observable<string>;
  userRole$!: Observable<string>;
  userRoles: string[] = [];

  navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard', roles: ['MASTER_ADMIN', 'TENANT_ADMIN'] },
    { label: 'Tenants', icon: 'business', route: '/tenants', roles: ['MASTER_ADMIN'] },
    { label: 'Users', icon: 'people', route: '/users', roles: ['TENANT_ADMIN', 'MASTER_ADMIN'] },
    { label: 'Roles', icon: 'security', route: '/roles', roles: ['TENANT_ADMIN', 'MASTER_ADMIN'] },
    { label: 'Reports', icon: 'bar_chart', route: '/reports', roles: ['MASTER_ADMIN', 'TENANT_ADMIN'] },
  ];

  ngOnInit(): void {
    this.isAuthenticated$ = this.oidcService.isAuthenticated$.pipe(
      map(auth => auth.isAuthenticated)
    );

    this.userEmail$ = this.oidcService.getUserData().pipe(
      map(userData => userData?.email || userData?.preferred_username || '')
    );

    this.userRole$ = this.oidcService.getAccessToken().pipe(
      map(token => {
        const roles = this.parseRolesFromToken(token);
        if (roles.includes('MASTER_ADMIN')) return 'Master Admin';
        if (roles.includes('TENANT_ADMIN')) return 'Tenant Admin';
        return 'Admin';
      })
    );

    this.oidcService.getAccessToken().subscribe(token => {
      this.userRoles = this.parseRolesFromToken(token);
    });
  }

  private parseRolesFromToken(token: string): string[] {
    if (!token) return [];
    try {
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      return payload?.realm_access?.roles || [];
    } catch {
      return [];
    }
  }

  getVisibleNavItems(): NavItem[] {
    return this.navItems.filter(item =>
      item.roles.some(role => this.userRoles.includes(role))
    );
  }

  logout(): void {
    this.oidcService.logoff().subscribe();
  }
}
