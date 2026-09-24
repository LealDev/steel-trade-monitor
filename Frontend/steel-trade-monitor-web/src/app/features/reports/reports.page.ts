import { Component, computed, signal } from '@angular/core';

/**
 * Relatórios: exportação do recorte escolhido em CSV.
 * O arquivo é gerado pelo backend (/api/v1/export/trade.csv) — o link
 * carrega os filtros como query params.
 */
@Component({
  selector: 'app-reports-page',
  template: `
    <section class="reports">
      <header>
        <h2>Relatórios</h2>
        <p class="reports__sub">
          Exporte a série mensal do recorte escolhido em CSV — abre direto no
          Excel (separador ";", decimais com vírgula, UTF-8 com BOM).
        </p>
      </header>

      <form class="filtros" aria-label="Filtros do relatório">
        <label>
          Fluxo
          <select [value]="fluxo()" (change)="aoMudarFluxo($event)">
            <option value="EXPORT">Exportação</option>
            <option value="IMPORT">Importação</option>
          </select>
        </label>
        <label>
          Capítulo NCM
          <select [value]="capitulo()" (change)="aoMudarCapitulo($event)">
            <option value="72">72 — Ferro fundido, ferro e aço</option>
            <option value="73">73 — Obras de ferro ou aço</option>
          </select>
        </label>
        <label>
          De
          <input type="month" [value]="de()" (change)="aoMudarDe($event)" />
        </label>
        <label>
          Até
          <input type="month" [value]="ate()" (change)="aoMudarAte($event)" />
        </label>
      </form>

      <a class="botao-download" [href]="urlDownload()" download>
        ⬇ Baixar CSV
      </a>

      <div class="conteudo-arquivo">
        <h3>O que vem no arquivo</h3>
        <ul>
          <li><code>periodo</code> — o mês (AAAA-MM)</li>
          <li><code>fluxo</code> e <code>capitulo_ncm</code> — o recorte exportado</li>
          <li><code>kg_liquido</code> — quilos embarcados no mês</li>
          <li><code>valor_fob_usd</code> — valor FOB (sem frete e seguro), em dólares</li>
          <li><code>preco_medio_usd_t</code> — US$/t do mês (FOB ÷ toneladas, calculado na geração)</li>
        </ul>
      </div>
    </section>
  `,
  styles: `
    .reports { padding: 1.5rem; }
    .reports h2 { margin: 0 0 0.25rem; font-size: 1.15rem; }
    .reports__sub { margin: 0 0 1rem; color: #6b7280; font-size: 0.9rem; max-width: 42rem; }
    .filtros {
      display: flex; gap: 1rem; flex-wrap: wrap; margin-bottom: 1.25rem;
      padding: 0.9rem 1rem; border: 1px solid #e5e7eb; border-radius: 8px; background: #fafafa;
    }
    .filtros label {
      display: flex; flex-direction: column; gap: 0.25rem;
      font-size: 0.78rem; color: #4b5563; font-weight: 600;
    }
    .filtros select, .filtros input {
      font: inherit; padding: 0.35rem 0.5rem; border: 1px solid #d1d5db;
      border-radius: 6px; background: #fff; min-width: 10rem;
    }
    .botao-download {
      display: inline-block; padding: 0.55rem 1.2rem; border-radius: 6px;
      background: #1d4ed8; color: #fff; text-decoration: none; font-weight: 600;
    }
    .botao-download:hover { background: #1e40af; }
    .conteudo-arquivo { margin-top: 1.5rem; color: #4b5563; font-size: 0.9rem; }
    .conteudo-arquivo h3 { font-size: 0.95rem; margin: 0 0 0.5rem; color: #111827; }
    .conteudo-arquivo code {
      background: #f3f4f6; padding: 0.05rem 0.35rem; border-radius: 4px; font-size: 0.85em;
    }
  `,
})
export class ReportsPage {
  readonly fluxo = signal<'EXPORT' | 'IMPORT'>('EXPORT');
  readonly capitulo = signal(72);
  readonly de = signal('');
  readonly ate = signal('');

  readonly urlDownload = computed(() => {
    const params = new URLSearchParams({
      flow: this.fluxo(),
      ncmChapter: String(this.capitulo()),
    });
    if (this.de()) params.set('from', this.de());
    if (this.ate()) params.set('to', this.ate());
    return `/api/v1/export/trade.csv?${params.toString()}`;
  });

  aoMudarFluxo(evento: Event): void {
    this.fluxo.set((evento.target as HTMLSelectElement).value as 'EXPORT' | 'IMPORT');
  }

  aoMudarCapitulo(evento: Event): void {
    this.capitulo.set(Number((evento.target as HTMLSelectElement).value));
  }

  aoMudarDe(evento: Event): void {
    this.de.set((evento.target as HTMLInputElement).value);
  }

  aoMudarAte(evento: Event): void {
    this.ate.set((evento.target as HTMLInputElement).value);
  }
}
