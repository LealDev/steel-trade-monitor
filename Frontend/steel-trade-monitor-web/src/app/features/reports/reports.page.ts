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
    .reports { padding: 1.5rem 1.75rem 2rem; }
    .reports h2 {
      margin: 0 0 0.25rem;
      font-size: 1.25rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    .reports__sub { margin: 0 0 1rem; color: var(--muted); font-size: 0.88rem; max-width: 44rem; }
    .filtros {
      display: flex; gap: 1.1rem; flex-wrap: wrap; margin-bottom: 1.4rem;
      padding: 1rem 1.2rem; border: 1px solid var(--border);
      border-radius: var(--radius);
      background: linear-gradient(180deg, var(--surface-2), var(--surface));
      animation: rise-in 0.45s ease backwards;
    }
    .filtros label {
      display: flex; flex-direction: column; gap: 0.3rem;
      font-family: var(--font-display);
      font-size: 0.68rem; font-weight: 600;
      text-transform: uppercase; letter-spacing: 0.08em;
      color: var(--muted);
    }
    .filtros select, .filtros input {
      font: 400 0.9rem var(--font-body);
      color: var(--text);
      padding: 0.42rem 0.6rem; border: 1px solid var(--border);
      border-radius: 6px; background: var(--bg); min-width: 11rem;
      transition: border-color 0.15s ease, box-shadow 0.15s ease;
    }
    .filtros select:focus, .filtros input:focus {
      outline: none;
      border-color: var(--molten);
      box-shadow: 0 0 0 3px var(--molten-soft);
    }
    .botao-download {
      display: inline-block; padding: 0.62rem 1.5rem; border-radius: 8px;
      background: linear-gradient(145deg, var(--molten), #d95c0e);
      color: #140901; text-decoration: none;
      font-family: var(--font-display);
      font-weight: 700; font-size: 0.86rem;
      text-transform: uppercase; letter-spacing: 0.06em;
      box-shadow: 0 0 20px rgba(255, 125, 38, 0.25);
      transition: box-shadow 0.2s ease, transform 0.2s ease;
    }
    .botao-download:hover {
      box-shadow: 0 0 32px rgba(255, 125, 38, 0.45);
      transform: translateY(-1px);
    }
    .conteudo-arquivo {
      margin-top: 1.6rem; color: var(--muted); font-size: 0.88rem;
      border: 1px solid var(--border); border-radius: var(--radius);
      background: var(--surface); padding: 1.1rem 1.3rem; max-width: 44rem;
      animation: rise-in 0.5s ease 0.1s backwards;
    }
    .conteudo-arquivo h3 {
      font-size: 0.82rem; font-weight: 600;
      text-transform: uppercase; letter-spacing: 0.07em;
      margin: 0 0 0.6rem; color: var(--text);
    }
    .conteudo-arquivo ul { margin: 0; padding-left: 1.1rem; }
    .conteudo-arquivo li { padding: 0.12rem 0; }
    .conteudo-arquivo code {
      font-family: var(--font-mono);
      color: var(--ember);
      background: var(--surface-2);
      padding: 0.08rem 0.4rem; border-radius: 4px; font-size: 0.84em;
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
