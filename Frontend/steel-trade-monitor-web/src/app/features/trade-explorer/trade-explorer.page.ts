import { Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { forkJoin } from 'rxjs';

import { CountryRanking } from '../../core/models/country-ranking.model';
import { TimeSeriesPoint } from '../../core/models/time-series-point.model';
import { TransportBreakdown } from '../../core/models/transport-breakdown.model';
import { TradeAnalyticsService, TradeFilter } from '../../core/services/trade-analytics.service';
import { TradeVolumeChart } from '../dashboard/components/trade-volume-chart/trade-volume-chart';
import { UnitPriceChart } from '../dashboard/components/unit-price-chart/unit-price-chart';
import { rotuloPeriodo } from '../dashboard/dashboard.metrics';

type Estado = 'carregando' | 'pronto' | 'vazio' | 'erro';

/**
 * Trade Explorer: análise interativa com filtros combinados.
 * O usuário escolhe o recorte; os gráficos e tabelas reagem.
 */
@Component({
  selector: 'app-trade-explorer-page',
  imports: [DecimalPipe, TradeVolumeChart, UnitPriceChart],
  template: `
    <section class="explorer">
      <header class="explorer__header">
        <h2>
          Trade Explorer
          @if (periodoExibido()) {
            <span class="explorer__periodo">Período: {{ periodoExibido() }}</span>
          }
        </h2>
        <p class="explorer__sub">
          Explore as importações e exportações por capítulo NCM, período e recorte.
        </p>
      </header>

      <form class="filtros" aria-label="Filtros">
        <label>
          Fluxo
          <select [value]="fluxo()" (change)="aoMudarFluxo($event)">
            <option value="EXPORT">Exportação</option>
            <option value="IMPORT">Importação</option>
          </select>
        </label>
        <label>
          Capítulo NCM
          <select [value]="capitulo()" (change)="aoMudarCapitulo($event)">
            <option value="72">72 — Ferro fundido, ferro e aço</option>
            <option value="73">73 — Obras de ferro ou aço</option>
          </select>
        </label>
        <label>
          De
          <input type="month" [value]="de()" (change)="aoMudarDe($event)" />
        </label>
        <label>
          Até
          <input type="month" [value]="ate()" (change)="aoMudarAte($event)" />
        </label>
      </form>

      @switch (estado()) {
        @case ('carregando') {
          <div class="placeholder placeholder--skeleton" aria-busy="true">Carregando…</div>
        }
        @case ('erro') {
          <div class="placeholder placeholder--erro">
            Não foi possível carregar os dados. Verifique o backend e tente novamente.
          </div>
        }
        @case ('vazio') {
          <div class="placeholder">
            <strong>Sem dados para este recorte.</strong>
            <p>O fluxo/capítulo escolhido pode ainda não ter sido coletado da fonte.</p>
          </div>
        }
        @case ('pronto') {
          <app-trade-volume-chart [points]="serie()" />
          <app-unit-price-chart [points]="serie()" />

          <div class="paineis">
            <div class="painel">
              <h3>Top países <small>por valor FOB</small></h3>
              <table>
                <thead>
                  <tr><th>#</th><th>País</th><th class="num">mil t</th><th class="num">US$ mi FOB</th></tr>
                </thead>
                <tbody>
                  @for (pais of ranking(); track pais.nomePais; let i = $index) {
                    <tr>
                      <td>{{ i + 1 }}</td>
                      <td>{{ pais.nomePais }}</td>
                      <td class="num">{{ pais.kgLiquido / 1_000_000 | number: '1.0-0' }}</td>
                      <td class="num">{{ pais.valorFobUsd / 1_000_000 | number: '1.0-0' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <div class="painel">
              <h3>Por via de transporte <small>por volume</small></h3>
              <table>
                <thead>
                  <tr><th>Via</th><th class="num">mil t</th><th class="num">US$ mi FOB</th><th class="num">% vol.</th></tr>
                </thead>
                <tbody>
                  @for (via of vias(); track via.via) {
                    <tr>
                      <td>{{ via.via }}</td>
                      <td class="num">{{ via.kgLiquido / 1_000_000 | number: '1.0-0' }}</td>
                      <td class="num">{{ via.valorFobUsd / 1_000_000 | number: '1.0-0' }}</td>
                      <td class="num">{{ percentualVolume(via) | number: '1.0-1' }}%</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        }
      }
    </section>
  `,
  styles: `
    .explorer { padding: 1.5rem 1.75rem 2rem; }
    .explorer__header h2 {
      margin: 0 0 0.25rem;
      font-size: 1.25rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      display: flex;
      align-items: baseline;
      gap: 0.85rem;
      flex-wrap: wrap;
    }
    .explorer__periodo {
      font-family: var(--font-mono);
      font-size: 0.72rem;
      font-weight: 500;
      text-transform: none;
      letter-spacing: 0;
      color: var(--ember);
      background: var(--warn-soft);
      border-radius: 999px;
      padding: 0.18rem 0.7rem;
      white-space: nowrap;
    }
    .explorer__sub { margin: 0 0 1rem; color: var(--muted); font-size: 0.88rem; }
    .filtros {
      display: flex;
      gap: 1.1rem;
      flex-wrap: wrap;
      margin-bottom: 1.25rem;
      padding: 1rem 1.2rem;
      border: 1px solid var(--border);
      border-radius: var(--radius);
      background: linear-gradient(180deg, var(--surface-2), var(--surface));
    }
    .filtros label {
      display: flex;
      flex-direction: column;
      gap: 0.3rem;
      font-family: var(--font-display);
      font-size: 0.68rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: var(--muted);
    }
    .filtros select, .filtros input {
      font: 400 0.9rem var(--font-body);
      color: var(--text);
      padding: 0.42rem 0.6rem;
      border: 1px solid var(--border);
      border-radius: 6px;
      background: var(--bg);
      min-width: 11rem;
      transition: border-color 0.15s ease, box-shadow 0.15s ease;
    }
    .filtros select:focus, .filtros input:focus {
      outline: none;
      border-color: var(--molten);
      box-shadow: 0 0 0 3px var(--molten-soft);
    }
    .paineis {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
      gap: 1rem;
      margin-top: 1.25rem;
    }
    .painel {
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 1.1rem 1.3rem;
      background: var(--surface);
      animation: rise-in 0.5s ease backwards;
    }
    .painel:nth-child(2) { animation-delay: 0.08s; }
    .painel h3 {
      margin: 0 0 0.8rem;
      font-size: 0.86rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.07em;
    }
    .painel h3 small {
      color: var(--faint);
      font-weight: 500;
      text-transform: none;
      letter-spacing: 0;
    }
    .painel table { width: 100%; border-collapse: collapse; font-size: 0.84rem; }
    .painel th, .painel td { padding: 0.42rem 0.55rem; text-align: left; }
    .painel thead th {
      font-family: var(--font-display);
      color: var(--faint);
      font-size: 0.66rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      border-bottom: 1px solid var(--border);
    }
    .painel tbody tr { border-bottom: 1px solid color-mix(in srgb, var(--border) 45%, transparent); }
    .painel tbody tr:hover { background: var(--molten-soft); }
    .painel tbody td:first-child { color: var(--muted); }
    .painel .num {
      text-align: right;
      font-family: var(--font-mono);
      font-variant-numeric: tabular-nums;
    }
    app-trade-volume-chart, app-unit-price-chart {
      display: block;
      animation: rise-in 0.5s ease backwards;
    }
    .placeholder {
      padding: 3rem 1.5rem;
      border: 1px dashed var(--border-strong);
      border-radius: var(--radius);
      background: var(--surface);
      color: var(--muted);
      text-align: center;
    }
    .placeholder--skeleton { color: var(--faint); animation: pulse 1.2s ease-in-out infinite; }
    .placeholder--erro { border-color: var(--err); color: var(--err); }
    @keyframes pulse { 50% { opacity: 0.45; } }
  `,
})
export class TradeExplorerPage {
  private readonly tradeAnalytics = inject(TradeAnalyticsService);

  readonly fluxo = signal<'EXPORT' | 'IMPORT'>('EXPORT');
  readonly capitulo = signal(72);
  readonly de = signal('');
  readonly ate = signal('');

  readonly estado = signal<Estado>('carregando');
  readonly serie = signal<TimeSeriesPoint[]>([]);
  readonly ranking = signal<CountryRanking[]>([]);
  readonly vias = signal<TransportBreakdown[]>([]);

  readonly periodoExibido = computed(() => rotuloPeriodo(this.serie()));

  constructor() {
    this.carregar();
  }

  aoMudarFluxo(evento: Event): void {
    this.fluxo.set((evento.target as HTMLSelectElement).value as 'EXPORT' | 'IMPORT');
    this.carregar();
  }

  aoMudarCapitulo(evento: Event): void {
    this.capitulo.set(Number((evento.target as HTMLSelectElement).value));
    this.carregar();
  }

  aoMudarDe(evento: Event): void {
    this.de.set((evento.target as HTMLInputElement).value);
    this.carregar();
  }

  aoMudarAte(evento: Event): void {
    this.ate.set((evento.target as HTMLInputElement).value);
    this.carregar();
  }

  percentualVolume(via: TransportBreakdown): number {
    const total = this.vias().reduce((soma, v) => soma + v.kgLiquido, 0);
    return total > 0 ? (via.kgLiquido / total) * 100 : 0;
  }

  private carregar(): void {
    this.estado.set('carregando');
    const filtro: TradeFilter = {
      flow: this.fluxo(),
      ncmChapter: this.capitulo(),
      from: this.de() || undefined,
      to: this.ate() || undefined,
    };
    forkJoin({
      serie: this.tradeAnalytics.getTimeSeries(filtro),
      ranking: this.tradeAnalytics.getTopCountries(filtro, 10),
      vias: this.tradeAnalytics.getByTransport(filtro),
    }).subscribe({
      next: ({ serie, ranking, vias }) => {
        this.serie.set(serie);
        this.ranking.set(ranking);
        this.vias.set(vias);
        this.estado.set(serie.length > 0 ? 'pronto' : 'vazio');
      },
      error: () => this.estado.set('erro'),
    });
  }
}
