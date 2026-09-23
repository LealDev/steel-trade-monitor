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
      gap: 0.4rem;
      padding: 0.2rem 0.6rem;
      border-radius: 999px;
      font-size: 0.78rem;
      background: #f3f4f6;
      color: #374151;
    }
    .badge__dot {
      width: 0.5rem;
      height: 0.5rem;
      border-radius: 50%;
      background: currentColor;
    }
    .badge--ok { color: #15803d; background: #ecfdf5; }
    .badge--alerta { color: #b45309; background: #fffbeb; }
    .badge--erro { color: #b91c1c; background: #fef2f2; }
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
