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
      position: relative;
      display: flex;
      flex-direction: column;
      gap: 0.3rem;
      padding: 1rem 1.25rem 0.9rem;
      border: 1px solid var(--border);
      border-radius: var(--radius);
      background: linear-gradient(180deg, var(--surface-2), var(--surface));
      overflow: hidden;
      transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
    }
    .kpi::before {
      content: '';
      position: absolute;
      inset: 0 auto 0 0;
      width: 3px;
      background: linear-gradient(180deg, var(--molten), transparent 85%);
      opacity: 0.55;
    }
    .kpi:hover {
      border-color: var(--border-strong);
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
    }
    .kpi__label {
      font-family: var(--font-display);
      font-size: 0.68rem;
      font-weight: 600;
      color: var(--muted);
      text-transform: uppercase;
      letter-spacing: 0.09em;
    }
    .kpi__value {
      font-family: var(--font-mono);
      font-size: 1.55rem;
      font-weight: 600;
      color: var(--text);
      font-variant-numeric: tabular-nums;
    }
    .kpi__footer {
      display: flex;
      gap: 0.6rem;
      align-items: baseline;
      font-size: 0.74rem;
      min-height: 1rem;
    }
    .kpi__trend {
      font-family: var(--font-mono);
      color: var(--muted);
      font-weight: 600;
    }
    .kpi__trend--up { color: var(--ok); }
    .kpi__trend--down { color: var(--err); }
    .kpi__hint { color: var(--faint); }
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
