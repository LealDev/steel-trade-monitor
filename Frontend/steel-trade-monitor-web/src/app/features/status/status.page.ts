import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';

import { Execution, Page } from '../../core/models/execution.model';
import { SourceFreshness } from '../../core/models/source-freshness.model';
import { DataFreshnessService } from '../../core/services/data-freshness.service';
import { FreshnessBadge } from '../../shared/components/freshness-badge/freshness-badge';

/**
 * Status das fontes: responde "a última carga funcionou?" sem abrir o
 * banco — freshness por fonte + histórico paginado de execuções.
 */
@Component({
  selector: 'app-status-page',
  imports: [DatePipe, DecimalPipe, FreshnessBadge],
  template: `
    <section class="status">
      <header>
        <h2>Status das fontes</h2>
        <p class="status__sub">
          Cada carga de dados fica registrada: quando rodou, qual janela cobriu,
          quantos registros gravou e como terminou.
        </p>
      </header>

      <div class="fontes">
        @for (fonte of fontes(); track fonte.fonte) {
          <div class="fonte-card">
            <app-freshness-badge [freshness]="fonte" />
            <dl>
              <div><dt>Última janela</dt><dd>{{ fonte.periodoReferencia ?? '—' }}</dd></div>
              <div><dt>Registros gravados</dt><dd>{{ fonte.registrosGravados ?? '—' }}</dd></div>
              <div>
                <dt>Último sucesso</dt>
                <dd>{{ fonte.ultimoSucessoEm ? (fonte.ultimoSucessoEm | date: 'dd/MM/yyyy HH:mm') : 'nunca' }}</dd>
              </div>
              @if (fonte.mensagem) {
                <div><dt>Mensagem</dt><dd class="erro">{{ fonte.mensagem }}</dd></div>
              }
            </dl>
          </div>
        } @empty {
          <div class="placeholder">Nenhuma fonte executada ainda.</div>
        }
      </div>

      <h3>Histórico de execuções</h3>
      @if (execucoes(); as pagina) {
        <table class="historico">
          <thead>
            <tr>
              <th>#</th><th>Fonte</th><th>Janela</th><th>Status</th>
              <th class="num">Registros</th><th>Início</th><th>Duração</th><th>Mensagem</th>
            </tr>
          </thead>
          <tbody>
            @for (ex of pagina.content; track ex.id) {
              <tr>
                <td>{{ ex.id }}</td>
                <td>{{ ex.fonte }}</td>
                <td>{{ ex.periodoReferencia ?? '—' }}</td>
                <td><span class="pill" [class]="'pill pill--' + ex.status.toLowerCase()">{{ ex.status }}</span></td>
                <td class="num">{{ ex.registrosGravados != null ? (ex.registrosGravados | number) : '—' }}</td>
                <td>{{ ex.iniciadoEm | date: 'dd/MM HH:mm:ss' }}</td>
                <td>{{ duracao(ex) }}</td>
                <td class="msg">{{ ex.mensagem ?? '' }}</td>
              </tr>
            } @empty {
              <tr><td colspan="8" class="vazio">Nenhuma execução registrada.</td></tr>
            }
          </tbody>
        </table>

        @if (pagina.totalPages > 1) {
          <div class="paginacao">
            <button (click)="irPara(pagina.page - 1)" [disabled]="pagina.page === 0">← Anterior</button>
            <span>página {{ pagina.page + 1 }} de {{ pagina.totalPages }} · {{ pagina.totalElements }} execuções</span>
            <button (click)="irPara(pagina.page + 1)" [disabled]="pagina.page + 1 >= pagina.totalPages">Próxima →</button>
          </div>
        }
      } @else {
        <div class="placeholder">Carregando histórico…</div>
      }
    </section>
  `,
  styles: `
    .status { padding: 1.5rem 1.75rem 2rem; }
    .status h2 {
      margin: 0 0 0.25rem;
      font-size: 1.25rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    .status__sub { margin: 0 0 1rem; color: var(--muted); font-size: 0.88rem; max-width: 46rem; }
    .fontes { display: flex; gap: 1rem; flex-wrap: wrap; margin-bottom: 1.6rem; }
    .fonte-card {
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 1.1rem 1.3rem;
      background: linear-gradient(180deg, var(--surface-2), var(--surface));
      min-width: 300px;
      animation: rise-in 0.45s ease backwards;
    }
    .fonte-card dl { margin: 0.85rem 0 0; font-size: 0.83rem; }
    .fonte-card dl div {
      display: flex; justify-content: space-between; gap: 1.25rem;
      padding: 0.22rem 0;
      border-bottom: 1px solid color-mix(in srgb, var(--border) 45%, transparent);
    }
    .fonte-card dl div:last-child { border-bottom: none; }
    .fonte-card dt { color: var(--faint); }
    .fonte-card dd { margin: 0; font-family: var(--font-mono); font-weight: 500; color: var(--text); }
    .fonte-card .erro { color: var(--err); }
    .status h3 {
      font-size: 0.9rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.07em;
      margin: 0 0 0.6rem;
    }
    .historico {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.82rem;
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      overflow: hidden;
      animation: rise-in 0.5s ease 0.1s backwards;
    }
    .historico th, .historico td { padding: 0.5rem 0.7rem; text-align: left; }
    .historico thead th {
      font-family: var(--font-display);
      color: var(--faint); font-size: 0.66rem; font-weight: 600;
      text-transform: uppercase; letter-spacing: 0.08em;
      border-bottom: 1px solid var(--border);
      background: var(--surface-2);
    }
    .historico tbody tr { border-bottom: 1px solid color-mix(in srgb, var(--border) 40%, transparent); }
    .historico tbody tr:hover { background: var(--molten-soft); }
    .historico td:first-child { font-family: var(--font-mono); color: var(--faint); }
    .historico .num { text-align: right; font-family: var(--font-mono); font-variant-numeric: tabular-nums; }
    .historico .msg { color: var(--faint); max-width: 26rem; }
    .historico .vazio { text-align: center; color: var(--faint); padding: 1.6rem; }
    .pill {
      font-family: var(--font-mono);
      padding: 0.12rem 0.6rem; border-radius: 999px;
      font-size: 0.68rem; font-weight: 600; letter-spacing: 0.03em;
    }
    .pill--sucesso { background: var(--ok-soft); color: var(--ok); }
    .pill--falha { background: var(--err-soft); color: var(--err); }
    .pill--parcial { background: var(--warn-soft); color: var(--warn); }
    .pill--em_andamento { background: rgba(106, 169, 233, 0.14); color: var(--steel); }
    .paginacao {
      display: flex; align-items: center; gap: 1rem;
      margin-top: 0.85rem; font-size: 0.82rem; color: var(--muted);
    }
    .paginacao button {
      font: 600 0.78rem var(--font-display);
      text-transform: uppercase; letter-spacing: 0.05em;
      color: var(--muted);
      padding: 0.38rem 0.9rem; border: 1px solid var(--border);
      border-radius: 6px; background: var(--surface-2); cursor: pointer;
      transition: color 0.15s ease, border-color 0.15s ease;
    }
    .paginacao button:hover:not(:disabled) { color: var(--molten); border-color: var(--molten); }
    .paginacao button:disabled { opacity: 0.35; cursor: default; }
    .placeholder {
      padding: 2.2rem 1.5rem; border: 1px dashed var(--border-strong);
      border-radius: var(--radius); background: var(--surface);
      color: var(--muted); text-align: center;
    }
  `,
})
export class StatusPage {
  private readonly dataFreshness = inject(DataFreshnessService);

  readonly fontes = signal<SourceFreshness[]>([]);
  readonly execucoes = signal<Page<Execution> | null>(null);

  constructor() {
    this.dataFreshness.getFreshness().subscribe({
      next: fontes => this.fontes.set(fontes),
      error: () => this.fontes.set([]),
    });
    this.irPara(0);
  }

  irPara(pagina: number): void {
    this.dataFreshness.getExecutions(pagina, 15).subscribe({
      next: resultado => this.execucoes.set(resultado),
      error: () => this.execucoes.set({ content: [], page: 0, size: 15, totalElements: 0, totalPages: 0 }),
    });
  }

  duracao(ex: Execution): string {
    if (!ex.finalizadoEm) {
      return 'em andamento';
    }
    const segundos = Math.round(
      (new Date(ex.finalizadoEm).getTime() - new Date(ex.iniciadoEm).getTime()) / 1000,
    );
    return segundos < 60 ? `${segundos}s` : `${Math.floor(segundos / 60)}m ${segundos % 60}s`;
  }
}
