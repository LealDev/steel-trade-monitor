import { Component, input } from '@angular/core';

/**
 * Card de indicador. Burro de propósito: não sabe se mostra tonelada ou
 * dólar — recebe tudo formatado por input.
 */
@Component({
  selector: 'app-kpi-card',
  template: `
    <div class="kpi">
      <span class="kpi__label">{{ label() }}</span>
      <span class="kpi__value">{{ value() }}</span>
      <span class="kpi__footer">
        @if (trendPercent() !== null) {
          <span
            class="kpi__trend"
            [class.kpi__trend--up]="trendPercent()! > 0"
            [class.kpi__trend--down]="trendPercent()! < 0"
          >
            {{ trendPercent()! > 0 ? '▲' : trendPercent()! < 0 ? '▼' : '•' }}
            {{ formatTrend(trendPercent()!) }}
          </span>
        }
        @if (hint()) {
          <span class="kpi__hint">{{ hint() }}</span>
        }
      </span>
    </div>
  `,
  styles: `
    .kpi {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
      padding: 1rem 1.25rem;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      background: #fff;
    }
    .kpi__label {
      font-size: 0.78rem;
      color: #6b7280;
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }
    .kpi__value {
      font-size: 1.5rem;
      font-weight: 700;
      color: #111827;
    }
    .kpi__footer {
      display: flex;
      gap: 0.5rem;
      align-items: baseline;
      font-size: 0.78rem;
      min-height: 1rem;
    }
    .kpi__trend { color: #6b7280; font-weight: 600; }
    .kpi__trend--up { color: #15803d; }
    .kpi__trend--down { color: #b91c1c; }
    .kpi__hint { color: #9ca3af; }
  `,
})
export class KpiCard {
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  readonly hint = input<string>('');
  readonly trendPercent = input<number | null>(null);

  formatTrend(percent: number): string {
    return `${Math.abs(percent).toFixed(1)}%`;
  }
}
