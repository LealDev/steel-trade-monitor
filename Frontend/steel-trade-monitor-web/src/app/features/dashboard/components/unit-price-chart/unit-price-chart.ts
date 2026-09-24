import {
  Component,
  ElementRef,
  OnDestroy,
  effect,
  input,
  viewChild,
} from '@angular/core';
import { Chart } from 'chart.js/auto';

import { TimeSeriesPoint } from '../../../../core/models/time-series-point.model';
import { serieDePreco } from '../../dashboard.metrics';
import { TEMA_GRAFICO } from '../trade-volume-chart/trade-volume-chart';

/**
 * Preço médio por tonelada mês a mês (US$/t = FOB ÷ toneladas), com a
 * média do período tracejada como referência. Torna explícita a
 * divergência que no gráfico de volume × valor é preciso deduzir.
 */
@Component({
  selector: 'app-unit-price-chart',
  template: `
    <div class="chart-wrapper">
      <canvas #canvas></canvas>
    </div>
  `,
  styles: `
    .chart-wrapper {
      position: relative;
      height: 260px;
      margin-top: 1rem;
      padding: 1rem 1.25rem;
      border: 1px solid var(--border);
      border-radius: var(--radius);
      background: var(--surface);
    }
  `,
})
export class UnitPriceChart implements OnDestroy {
  readonly points = input.required<TimeSeriesPoint[]>();

  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private chart?: Chart;

  constructor() {
    effect(() => {
      const precos = serieDePreco(this.points());
      const canvas = this.canvas().nativeElement;

      const kgTotal = this.points().reduce((soma, p) => soma + p.kgLiquido, 0);
      const fobTotal = this.points().reduce((soma, p) => soma + p.valorFobUsd, 0);
      const mediaPeriodo = kgTotal > 0 ? fobTotal / (kgTotal / 1000) : 0;

      this.chart?.destroy();
      this.chart = new Chart(canvas, {
        type: 'line',
        data: {
          labels: precos.map(p => p.period),
          datasets: [
            {
              label: 'Preço do mês (US$/t)',
              data: precos.map(p => p.usdPorTonelada),
              borderColor: '#ffb020',
              backgroundColor: 'rgba(255, 176, 32, 0.1)',
              fill: true,
              tension: 0.25,
              pointRadius: 2.5,
            },
            {
              label: 'Média do período (US$/t)',
              data: precos.map(() => mediaPeriodo),
              borderColor: '#5c6a79',
              borderDash: [6, 6],
              pointRadius: 0,
              fill: false,
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          interaction: { mode: 'index', intersect: false },
          plugins: {
            title: {
              display: true,
              text: 'Preço médio por tonelada - quanto o mercado pagou pelo aço brasileiro',
              color: TEMA_GRAFICO.titulo,
              font: { family: "'Saira', sans-serif", size: 15, weight: 600 },
              padding: { bottom: 2 },
            },
            subtitle: {
              display: true,
              text: 'US$/t = valor FOB do mês ÷ toneladas do mês · acima da linha tracejada: melhor que a média do período',
              color: TEMA_GRAFICO.subtitulo,
              padding: { bottom: 14 },
            },
            legend: { labels: { color: TEMA_GRAFICO.ticks, boxWidth: 24, boxHeight: 2 } },
            tooltip: TEMA_GRAFICO.tooltip,
          },
          scales: {
            x: {
              ticks: { color: TEMA_GRAFICO.ticks },
              grid: { color: TEMA_GRAFICO.grid },
            },
            y: {
              title: { display: true, text: 'dólares por tonelada (US$/t)', color: TEMA_GRAFICO.subtitulo },
              ticks: { color: TEMA_GRAFICO.ticks },
              grid: { color: TEMA_GRAFICO.grid },
            },
          },
        },
      });
    });
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }
}
