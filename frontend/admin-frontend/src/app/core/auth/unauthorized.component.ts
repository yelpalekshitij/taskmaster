import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [CommonModule, MatButtonModule],
  template: `
    <div class="unauthorized-container">
      <h1>Access Denied</h1>
      <p>You do not have permission to access this page.</p>
      <button mat-raised-button color="primary" (click)="goHome()">Go to Dashboard</button>
    </div>
  `,
  styles: [`
    .unauthorized-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100vh;
      gap: 16px;
      text-align: center;
    }
  `]
})
export class UnauthorizedComponent {
  constructor(private router: Router) {}

  goHome(): void {
    this.router.navigate(['/dashboard']);
  }
}
