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

Chart.defaults.font.family = "'Archivo', sans-serif";
Chart.defaults.color = '#93a1b0';

/** Opções compartilhadas do tema escuro dos gráficos. */
export const TEMA_GRAFICO = {
  grid: 'rgba(255, 255, 255, 0.055)',
  ticks: '#93a1b0',
  titulo: '#e9eef3',
  subtitulo: '#5c6a79',
  tooltip: {
    backgroundColor: '#1e2731',
    borderColor: '#3d4a59',
    borderWidth: 1,
    titleColor: '#e9eef3',
    bodyColor: '#93a1b0',
    padding: 10,
    cornerRadius: 8,
  },
} as const;

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
      height: 380px;
      padding: 1rem 1.25rem;
      border: 1px solid var(--border);
      border-radius: var(--radius);
      background: var(--surface);
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
              borderColor: '#6aa9e9',
              backgroundColor: 'rgba(106, 169, 233, 0.12)',
              fill: true,
              tension: 0.25,
              pointRadius: 2.5,
              yAxisID: 'y',
            },
            {
              label: 'Valor FOB (milhões de US$)',
              data: points.map(p => p.valorFobUsd / 1_000_000),
              borderColor: '#ff7d26',
              tension: 0.25,
              pointRadius: 2.5,
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
              text: 'Volume embarcado × valor FOB - exportações mensais de ferro e aço',
              color: TEMA_GRAFICO.titulo,
              font: { family: "'Saira', sans-serif", size: 15, weight: 600 },
              padding: { bottom: 2 },
            },
            subtitle: {
              display: true,
              text: 'FOB (Free on Board): valor da mercadoria no embarque, sem frete e seguro',
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
              position: 'left',
              title: { display: true, text: 'milhares de toneladas (mil t)', color: TEMA_GRAFICO.subtitulo },
              ticks: { color: TEMA_GRAFICO.ticks },
              grid: { color: TEMA_GRAFICO.grid },
            },
            y1: {
              position: 'right',
              grid: { drawOnChartArea: false },
              title: { display: true, text: 'milhões de dólares FOB (US$ mi)', color: TEMA_GRAFICO.subtitulo },
              ticks: { color: TEMA_GRAFICO.ticks },
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
