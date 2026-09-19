import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';

interface HealthResponse {
  status: string;
}

@Component({
  selector: 'app-dashboard-page',
  template: `
    <section class="dashboard">
      <h2>Dashboard</h2>
      <p>
        Backend:
        @if (backendStatus() === null) {
          <span class="status status--loading">verificando…</span>
        } @else if (backendStatus() === 'UP') {
          <span class="status status--up">UP</span>
        } @else {
          <span class="status status--down">indisponível</span>
        }
      </p>
    </section>
  `,
  styles: `
    .dashboard {
      padding: 1.5rem;
    }
    .status {
      font-weight: 600;
    }
    .status--up { color: #15803d; }
    .status--down { color: #b91c1c; }
    .status--loading { color: #6b7280; }
  `,
})
export class DashboardPage {
  private readonly http = inject(HttpClient);

  readonly backendStatus = signal<string | null>(null);

  constructor() {
    this.http.get<HealthResponse>('/api/actuator/health').subscribe({
      next: health => this.backendStatus.set(health.status),
      error: () => this.backendStatus.set('DOWN'),
    });
  }
}
