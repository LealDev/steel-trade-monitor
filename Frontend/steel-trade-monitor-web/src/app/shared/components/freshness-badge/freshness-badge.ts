import { DatePipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';

import { SourceFreshness } from '../../../core/models/source-freshness.model';

/**
 * Badge de frescor de uma fonte: quando atualizou e em que estado.
 * Componente burro: recebe o freshness pronto por input.
 */
@Component({
  selector: 'app-freshness-badge',
  imports: [DatePipe],
  template: `
    <span class="badge" [class]="'badge badge--' + tom()">
      <span class="badge__dot"></span>
      {{ freshness().fonte }} ·
      @switch (freshness().status) {
        @case ('SUCESSO') {
          atualizado em {{ freshness().finalizadoEm | date: 'dd/MM HH:mm' }}
        }
        @case ('PARCIAL') {
          carga parcial em {{ freshness().finalizadoEm | date: 'dd/MM HH:mm' }}
        }
        @case ('FALHA') {
          falha na última carga
          @if (freshness().ultimoSucessoEm) {
            (dados de {{ freshness().ultimoSucessoEm | date: 'dd/MM HH:mm' }})
          }
        }
        @case ('EM_ANDAMENTO') {
          carga em andamento…
        }
      }
    </span>
  `,
  styles: `
    .badge {
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      padding: 0.22rem 0.7rem;
      border-radius: 999px;
      font-size: 0.74rem;
      font-family: var(--font-mono);
      border: 1px solid var(--border);
      background: var(--surface-2);
      color: var(--muted);
    }
    .badge__dot {
      width: 0.45rem;
      height: 0.45rem;
      border-radius: 50%;
      background: currentColor;
      box-shadow: 0 0 6px currentColor;
    }
    .badge--ok { color: var(--ok); background: var(--ok-soft); border-color: transparent; }
    .badge--alerta { color: var(--warn); background: var(--warn-soft); border-color: transparent; }
    .badge--erro { color: var(--err); background: var(--err-soft); border-color: transparent; }
  `,
})
export class FreshnessBadge {
  readonly freshness = input.required<SourceFreshness>();

  readonly tom = computed(() => {
    switch (this.freshness().status) {
      case 'SUCESSO':
        return 'ok';
      case 'FALHA':
        return 'erro';
      default:
        return 'alerta';
    }
  });
}
