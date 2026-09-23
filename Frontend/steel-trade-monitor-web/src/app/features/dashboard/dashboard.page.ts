import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';

import { SourceFreshness } from '../../core/models/source-freshness.model';
import { TimeSeriesPoint } from '../../core/models/time-series-point.model';
import { DataFreshnessService } from '../../core/services/data-freshness.service';
import { TradeAnalyticsService } from '../../core/services/trade-analytics.service';
import { FreshnessBadge } from '../../shared/components/freshness-badge/freshness-badge';
import { KpiCard } from '../../shared/components/kpi-card/kpi-card';
import { TradeVolumeChart } from './components/trade-volume-chart/trade-volume-chart';
import { UnitPriceChart } from './components/unit-price-chart/unit-price-chart';
import { resumirSerie, rotuloPeriodo } from './dashboard.metrics';

interface HealthResponse {
  status: string;
}

type EstadoSerie = 'carregando' | 'pronto' | 'vazio' | 'erro';

@Component({
  selector: 'app-dashboard-page',
  imports: [TradeVolumeChart, UnitPriceChart, FreshnessBadge, KpiCard],
  template: `
    <section class="dashboard">
      <header class="dashboard__header">
        <h2>
          Exportações de ferro e aço (capítulo 72 NCM)
          @if (periodoExibido()) {
            <span class="dashboard__periodo">Período: {{ periodoExibido() }}</span>
          }
        </h2>
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
          @if (resumo(); as r) {
            <div class="dashboard__kpis">
              <app-kpi-card
                label="Preço médio do período"
                [value]="usdPorTonelada(r.precoMedioPeriodoUsdPorTonelada)"
                hint="US$/t = dólares por tonelada (valor FOB ÷ toneladas)"
              />
              <app-kpi-card
                [label]="'Preço em ' + r.ultimoPeriodo"
                [value]="usdPorTonelada(r.precoUltimoMesUsdPorTonelada)"
                [trendPercent]="r.variacaoPrecoPercentual"
                hint="variação vs mês anterior"
              />
              <app-kpi-card
                label="Volume exportado"
                [value]="toneladas(r.volumeTotalToneladas)"
                [hint]="'mi t = milhões de toneladas · ' + periodoHint()"
              />
              <app-kpi-card
                label="Valor FOB (Free on Board)"
                [value]="usd(r.valorTotalFobUsd)"
                [hint]="'valor no embarque, sem frete e seguro · ' + periodoHint()"
              />
            </div>
          }
          <app-trade-volume-chart [points]="serie()" />
          <app-unit-price-chart [points]="serie()" />
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
      display: flex;
      align-items: baseline;
      gap: 0.75rem;
      flex-wrap: wrap;
    }
    .dashboard__periodo {
      font-size: 0.8rem;
      font-weight: 500;
      color: #4b5563;
      background: #f3f4f6;
      border: 1px solid #e5e7eb;
      border-radius: 999px;
      padding: 0.15rem 0.6rem;
      white-space: nowrap;
    }
    .dashboard__badges {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
      margin-bottom: 0.75rem;
    }
    .dashboard__kpis {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 0.75rem;
      margin-bottom: 1rem;
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
  readonly resumo = computed(() => resumirSerie(this.serie()));
  readonly periodoExibido = computed(() => rotuloPeriodo(this.serie()));

  private readonly formatoCompacto = new Intl.NumberFormat('pt-BR', {
    notation: 'compact',
    maximumFractionDigits: 2,
  });
  private readonly formatoInteiro = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 });

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

  usdPorTonelada(valor: number): string {
    return `US$ ${this.formatoInteiro.format(valor)}/t`;
  }

  toneladas(valor: number): string {
    return `${this.formatoCompacto.format(valor)} t`;
  }

  usd(valor: number): string {
    return `US$ ${this.formatoCompacto.format(valor)}`;
  }

  periodoHint(): string {
    const meses = this.serie().length;
    return meses === 1 ? 'em 1 mês' : `em ${meses} meses`;
  }
}
