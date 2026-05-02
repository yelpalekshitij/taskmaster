import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { take } from 'rxjs/operators';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <div class="login-page">
      <div class="login-card">
        <div class="app-logo">
          <mat-icon class="logo-icon">admin_panel_settings</mat-icon>
        </div>
        <h1 class="app-title">TaskMaster</h1>
        <p class="app-subtitle">Admin Portal</p>
        <p class="app-description">
          Manage tenants, users, roles and system settings across your organization.
        </p>
        <button mat-raised-button color="primary" class="sign-in-btn" (click)="login()">
          <mat-icon>lock_open</mat-icon>
          Sign In
        </button>
      </div>
      <p class="footer-text">TaskMaster &copy; {{ currentYear }}</p>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      height: 100vh;
    }
    .login-page {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #0d1b4b 0%, #1a2f7a 50%, #1565c0 100%);
      position: relative;
    }
    .login-page::before {
      content: '';
      position: absolute;
      inset: 0;
      background: radial-gradient(ellipse at 70% 30%, rgba(33, 150, 243, 0.15) 0%, transparent 60%);
      pointer-events: none;
    }
    .login-card {
      background: #ffffff;
      border-radius: 20px;
      padding: 52px 44px;
      text-align: center;
      box-shadow: 0 32px 80px rgba(0, 0, 0, 0.35);
      max-width: 420px;
      width: calc(100% - 32px);
      position: relative;
      animation: slide-up 0.4s ease-out;
    }
    @keyframes slide-up {
      from { opacity: 0; transform: translateY(24px); }
      to { opacity: 1; transform: translateY(0); }
    }
    .app-logo {
      width: 80px;
      height: 80px;
      border-radius: 20px;
      background: linear-gradient(135deg, #1565c0, #1e88e5);
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 24px;
      box-shadow: 0 8px 24px rgba(21, 101, 192, 0.35);
    }
    .logo-icon {
      font-size: 40px;
      width: 40px;
      height: 40px;
      color: white;
    }
    .app-title {
      font-size: 30px;
      font-weight: 700;
      color: #0d1b4b;
      margin: 0 0 6px;
      letter-spacing: -0.5px;
    }
    .app-subtitle {
      font-size: 13px;
      font-weight: 600;
      color: #1565c0;
      margin: 0 0 20px;
      text-transform: uppercase;
      letter-spacing: 2px;
    }
    .app-description {
      font-size: 14px;
      color: #616161;
      margin: 0 0 36px;
      line-height: 1.6;
    }
    .sign-in-btn {
      width: 100%;
      height: 52px;
      font-size: 15px;
      font-weight: 600;
      letter-spacing: 0.5px;
      border-radius: 12px !important;
    }
    .sign-in-btn mat-icon {
      margin-right: 8px;
      font-size: 20px;
    }
    .footer-text {
      margin-top: 24px;
      font-size: 12px;
      color: rgba(255, 255, 255, 0.45);
    }
  `]
})
export class LoginComponent implements OnInit {
  private oidcService = inject(OidcSecurityService);
  private router = inject(Router);

  readonly currentYear = new Date().getFullYear();

  ngOnInit(): void {
    this.oidcService.isAuthenticated$.pipe(take(1)).subscribe(({ isAuthenticated }) => {
      if (isAuthenticated) {
        this.router.navigate(['/dashboard']);
      }
    });
  }

  login(): void {
    this.oidcService.authorize();
  }
}
