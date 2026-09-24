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
import { resumirSerie, rotuloPeriodo, ultimosMeses } from './dashboard.metrics';

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
        <div class="dashboard__controles">
          @if (fontes().length > 0) {
            <div class="dashboard__badges">
              @for (fonte of fontes(); track fonte.fonte) {
                <app-freshness-badge [freshness]="fonte" />
              }
            </div>
          }
          <div class="dashboard__periodos" role="group" aria-label="Período de análise">
            @for (opcao of periodosDisponiveis; track opcao) {
              <button
                type="button"
                class="periodo-btn"
                [class.periodo-btn--ativo]="periodoMeses() === opcao"
                (click)="periodoMeses.set(opcao)"
              >
                {{ opcao }} meses
              </button>
            }
          </div>
        </div>
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
      padding: 1.5rem 1.75rem 2rem;
    }
    .dashboard__header h2 {
      margin: 0 0 0.35rem;
      font-size: 1.25rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      display: flex;
      align-items: baseline;
      gap: 0.85rem;
      flex-wrap: wrap;
    }
    .dashboard__periodo {
      font-family: var(--font-mono);
      font-size: 0.72rem;
      font-weight: 500;
      text-transform: none;
      letter-spacing: 0;
      color: var(--ember);
      background: var(--warn-soft);
      border: 1px solid transparent;
      border-radius: 999px;
      padding: 0.18rem 0.7rem;
      white-space: nowrap;
    }
    .dashboard__controles {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      flex-wrap: wrap;
      margin-bottom: 1rem;
    }
    .dashboard__badges {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
    }
    .dashboard__periodos {
      display: inline-flex;
      border: 1px solid var(--border);
      border-radius: 8px;
      overflow: hidden;
      background: var(--surface);
    }
    .periodo-btn {
      font: 600 0.72rem var(--font-display);
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--muted);
      background: transparent;
      border: none;
      border-right: 1px solid var(--border);
      padding: 0.42rem 0.9rem;
      cursor: pointer;
      transition: color 0.15s ease, background 0.15s ease;
    }
    .periodo-btn:last-child { border-right: none; }
    .periodo-btn:hover { color: var(--text); background: var(--surface-2); }
    .periodo-btn--ativo {
      color: #140901;
      background: linear-gradient(145deg, var(--molten), #d95c0e);
    }
    .dashboard__kpis {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
      gap: 0.85rem;
      margin-bottom: 1.1rem;
    }
    .dashboard__kpis app-kpi-card {
      animation: rise-in 0.45s ease backwards;
    }
    .dashboard__kpis app-kpi-card:nth-child(2) { animation-delay: 0.06s; }
    .dashboard__kpis app-kpi-card:nth-child(3) { animation-delay: 0.12s; }
    .dashboard__kpis app-kpi-card:nth-child(4) { animation-delay: 0.18s; }
    app-trade-volume-chart, app-unit-price-chart {
      display: block;
      animation: rise-in 0.5s ease 0.2s backwards;
    }
    .status {
      font-family: var(--font-mono);
      font-size: 0.85em;
      font-weight: 600;
    }
    .status--up { color: var(--ok); }
    .status--down { color: var(--err); }
    .status--loading { color: var(--faint); }
    .dashboard__header p { color: var(--muted); font-size: 0.85rem; margin: 0 0 0.6rem; }
    .placeholder {
      padding: 3rem 1.5rem;
      border: 1px dashed var(--border-strong);
      border-radius: var(--radius);
      background: var(--surface);
      color: var(--muted);
      text-align: center;
    }
    .placeholder code {
      font-family: var(--font-mono);
      font-size: 0.85em;
      color: var(--ember);
      background: var(--surface-2);
      padding: 0.1rem 0.4rem;
      border-radius: 4px;
    }
    .placeholder--skeleton {
      color: var(--faint);
      animation: pulse 1.2s ease-in-out infinite;
    }
    .placeholder--erro {
      border-color: var(--err);
      color: var(--err);
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

  /**
   * Janela máxima buscada do backend: 24 meses (24 pontos agregados — leve
   * mesmo com o histórico crescendo). Períodos menores são fatias locais da
   * mesma resposta: trocar o seletor não gera requisição nova.
   * Para ampliar o teto no futuro, basta ajustar estas duas constantes.
   */
  static readonly JANELA_MAXIMA_MESES = 24;
  readonly periodosDisponiveis = [6, 12, 24];

  readonly backendStatus = signal<string | null>(null);
  readonly serieCompleta = signal<TimeSeriesPoint[]>([]);
  readonly periodoMeses = signal(12);
  readonly estadoSerie = signal<EstadoSerie>('carregando');
  readonly fontes = signal<SourceFreshness[]>([]);

  readonly serie = computed(() => ultimosMeses(this.serieCompleta(), this.periodoMeses()));
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

    this.tradeAnalytics
      .getTimeSeries({ flow: 'EXPORT', ncmChapter: 72, from: this.inicioDaJanelaMaxima() })
      .subscribe({
        next: pontos => {
          this.serieCompleta.set(pontos);
          this.estadoSerie.set(pontos.length > 0 ? 'pronto' : 'vazio');
        },
        error: () => this.estadoSerie.set('erro'),
      });
  }

  private inicioDaJanelaMaxima(): string {
    const inicio = new Date();
    inicio.setDate(1);
    inicio.setMonth(inicio.getMonth() - (DashboardPage.JANELA_MAXIMA_MESES - 1));
    return `${inicio.getFullYear()}-${String(inicio.getMonth() + 1).padStart(2, '0')}`;
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
