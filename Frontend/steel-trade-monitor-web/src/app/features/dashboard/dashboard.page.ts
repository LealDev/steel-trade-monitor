import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';

import { SourceFreshness } from '../../core/models/source-freshness.model';
import { TimeSeriesPoint } from '../../core/models/time-series-point.model';
import { DataFreshnessService } from '../../core/services/data-freshness.service';
import { TradeAnalyticsService } from '../../core/services/trade-analytics.service';
import { FreshnessBadge } from '../../shared/components/freshness-badge/freshness-badge';
import { TradeVolumeChart } from './components/trade-volume-chart/trade-volume-chart';

interface HealthResponse {
  status: string;
}

type EstadoSerie = 'carregando' | 'pronto' | 'vazio' | 'erro';

@Component({
  selector: 'app-dashboard-page',
  imports: [TradeVolumeChart, FreshnessBadge],
  template: `
    <section class="dashboard">
      <header class="dashboard__header">
        <h2>Exportações de ferro e aço (capítulo 72 NCM)</h2>
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
        @if (fontes().length > 0) {
          <div class="dashboard__badges">
            @for (fonte of fontes(); track fonte.fonte) {
              <app-freshness-badge [freshness]="fonte" />
            }
          </div>
        }
      </header>

      @switch (estadoSerie()) {
        @case ('carregando') {
          <div class="placeholder placeholder--skeleton" aria-busy="true">
            Carregando série temporal…
          </div>
        }
        @case ('vazio') {
          <div class="placeholder">
            <strong>Sem dados para exibir.</strong>
            <p>
              Dispare uma coleta com
              <code>POST /api/v1/ingestion/comexstat/runs</code> (Swagger em
              <code>/api/swagger-ui.html</code>) e recarregue a página.
            </p>
          </div>
        }
        @case ('erro') {
          <div class="placeholder placeholder--erro">
            Não foi possível carregar a série temporal. Verifique o backend e
            recarregue a página.
          </div>
        }
        @case ('pronto') {
          <app-trade-volume-chart [points]="serie()" />
        }
      }
    </section>
  `,
  styles: `
    .dashboard {
      padding: 1.5rem;
    }
    .dashboard__header h2 {
      margin: 0 0 0.25rem;
      font-size: 1.15rem;
    }
    .dashboard__badges {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
      margin-bottom: 0.75rem;
    }
    .status {
      font-weight: 600;
    }
    .status--up { color: #15803d; }
    .status--down { color: #b91c1c; }
    .status--loading { color: #6b7280; }
    .placeholder {
      padding: 2.5rem 1.5rem;
      border: 1px dashed #d1d5db;
      border-radius: 8px;
      color: #374151;
      text-align: center;
    }
    .placeholder--skeleton {
      color: #6b7280;
      animation: pulse 1.2s ease-in-out infinite;
    }
    .placeholder--erro {
      border-color: #fca5a5;
      color: #b91c1c;
    }
    @keyframes pulse {
      50% { opacity: 0.45; }
    }
  `,
})
export class DashboardPage {
  private readonly http = inject(HttpClient);
  private readonly tradeAnalytics = inject(TradeAnalyticsService);
  private readonly dataFreshness = inject(DataFreshnessService);

  readonly backendStatus = signal<string | null>(null);
  readonly serie = signal<TimeSeriesPoint[]>([]);
  readonly estadoSerie = signal<EstadoSerie>('carregando');
  readonly fontes = signal<SourceFreshness[]>([]);

  constructor() {
    this.http.get<HealthResponse>('/api/actuator/health').subscribe({
      next: health => this.backendStatus.set(health.status),
      error: () => this.backendStatus.set('DOWN'),
    });

    this.dataFreshness.getFreshness().subscribe({
      next: fontes => this.fontes.set(fontes),
      error: () => this.fontes.set([]),
    });

    this.tradeAnalytics.getTimeSeries({ flow: 'EXPORT', ncmChapter: 72 }).subscribe({
      next: pontos => {
        this.serie.set(pontos);
        this.estadoSerie.set(pontos.length > 0 ? 'pronto' : 'vazio');
      },
      error: () => this.estadoSerie.set('erro'),
    });
  }
}
