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
    .status { padding: 1.5rem; }
    .status h2 { margin: 0 0 0.25rem; font-size: 1.15rem; }
    .status__sub { margin: 0 0 1rem; color: #6b7280; font-size: 0.9rem; }
    .fontes { display: flex; gap: 1rem; flex-wrap: wrap; margin-bottom: 1.5rem; }
    .fonte-card {
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      padding: 1rem 1.25rem;
      background: #fff;
      min-width: 280px;
    }
    .fonte-card dl { margin: 0.75rem 0 0; font-size: 0.85rem; }
    .fonte-card dl div { display: flex; justify-content: space-between; gap: 1rem; padding: 0.15rem 0; }
    .fonte-card dt { color: #6b7280; }
    .fonte-card dd { margin: 0; font-weight: 600; }
    .fonte-card .erro { color: #b91c1c; }
    .status h3 { font-size: 1rem; margin: 0 0 0.5rem; }
    .historico { width: 100%; border-collapse: collapse; font-size: 0.83rem; background: #fff; }
    .historico th, .historico td { padding: 0.4rem 0.6rem; text-align: left; }
    .historico thead th {
      color: #6b7280; font-size: 0.72rem; text-transform: uppercase;
      letter-spacing: 0.03em; border-bottom: 1px solid #e5e7eb;
    }
    .historico tbody tr:nth-child(odd) { background: #f9fafb; }
    .historico .num { text-align: right; font-variant-numeric: tabular-nums; }
    .historico .msg { color: #6b7280; max-width: 26rem; }
    .historico .vazio { text-align: center; color: #9ca3af; padding: 1.5rem; }
    .pill { padding: 0.1rem 0.55rem; border-radius: 999px; font-size: 0.72rem; font-weight: 600; }
    .pill--sucesso { background: #ecfdf5; color: #15803d; }
    .pill--falha { background: #fef2f2; color: #b91c1c; }
    .pill--parcial { background: #fffbeb; color: #b45309; }
    .pill--em_andamento { background: #eff6ff; color: #1d4ed8; }
    .paginacao {
      display: flex; align-items: center; gap: 1rem;
      margin-top: 0.75rem; font-size: 0.85rem; color: #4b5563;
    }
    .paginacao button {
      font: inherit; padding: 0.3rem 0.8rem; border: 1px solid #d1d5db;
      border-radius: 6px; background: #fff; cursor: pointer;
    }
    .paginacao button:disabled { opacity: 0.4; cursor: default; }
    .placeholder {
      padding: 2rem 1.5rem; border: 1px dashed #d1d5db; border-radius: 8px;
      color: #6b7280; text-align: center;
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
