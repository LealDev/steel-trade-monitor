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
      height: 240px;
      margin-top: 1rem;
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
              borderColor: '#0f766e',
              backgroundColor: 'rgba(15, 118, 110, 0.08)',
              fill: true,
              tension: 0.25,
            },
            {
              label: 'Média do período (US$/t)',
              data: precos.map(() => mediaPeriodo),
              borderColor: '#9ca3af',
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
              font: { size: 15, weight: 600 },
              padding: { bottom: 2 },
            },
            subtitle: {
              display: true,
              text: 'US$/t = valor FOB do mês ÷ toneladas do mês · acima da linha tracejada: melhor que a média do período',
              color: '#6b7280',
              padding: { bottom: 12 },
            },
          },
          scales: {
            y: {
              title: { display: true, text: 'dólares por tonelada (US$/t)' },
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
