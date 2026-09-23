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

/**
 * Gráfico de linha da série temporal: toneladas e US$ FOB por mês.
 * Componente burro: recebe os pontos prontos por input.
 */
@Component({
  selector: 'app-trade-volume-chart',
  template: `
    <div class="chart-wrapper">
      <canvas #canvas></canvas>
    </div>
  `,
  styles: `
    .chart-wrapper {
      position: relative;
      height: 360px;
    }
  `,
})
export class TradeVolumeChart implements OnDestroy {
  readonly points = input.required<TimeSeriesPoint[]>();

  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private chart?: Chart;

  constructor() {
    effect(() => {
      const points = this.points();
      const canvas = this.canvas().nativeElement;
      this.chart?.destroy();
      this.chart = new Chart(canvas, {
        type: 'line',
        data: {
          labels: points.map(p => p.period),
          datasets: [
            {
              label: 'Volume embarcado (mil t)',
              data: points.map(p => p.kgLiquido / 1_000_000),
              borderColor: '#1d4ed8',
              backgroundColor: 'rgba(29, 78, 216, 0.1)',
              fill: true,
              tension: 0.25,
              yAxisID: 'y',
            },
            {
              label: 'Valor FOB (milhões de US$)',
              data: points.map(p => p.valorFobUsd / 1_000_000),
              borderColor: '#b45309',
              tension: 0.25,
              yAxisID: 'y1',
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
              text: 'Volume embarcado × valor FOB — exportações mensais de ferro e aço',
              font: { size: 15, weight: 600 },
              padding: { bottom: 2 },
            },
            subtitle: {
              display: true,
              text: 'FOB (Free on Board): valor da mercadoria no embarque, sem frete e seguro',
              color: '#6b7280',
              padding: { bottom: 12 },
            },
          },
          scales: {
            y: {
              position: 'left',
              title: { display: true, text: 'milhares de toneladas (mil t)' },
            },
            y1: {
              position: 'right',
              grid: { drawOnChartArea: false },
              title: { display: true, text: 'milhões de dólares FOB (US$ mi)' },
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
