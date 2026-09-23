import { TimeSeriesPoint } from '../../core/models/time-series-point.model';

/**
 * Indicadores derivados da série temporal.
 * O preço médio (US$/t) é medida derivada: calculado sempre, nunca armazenado —
 * FOB total dividido pelas toneladas totais, e não a média dos preços mensais
 * (média de razões daria peso igual a meses de volumes diferentes).
 */
export interface ResumoSerie {
  precoMedioPeriodoUsdPorTonelada: number;
  precoUltimoMesUsdPorTonelada: number;
  variacaoPrecoPercentual: number | null;
  volumeTotalToneladas: number;
  valorTotalFobUsd: number;
  ultimoPeriodo: string;
}

export function resumirSerie(pontos: TimeSeriesPoint[]): ResumoSerie | null {
  if (pontos.length === 0) {
    return null;
  }

  const kgTotal = pontos.reduce((soma, p) => soma + p.kgLiquido, 0);
  const fobTotal = pontos.reduce((soma, p) => soma + p.valorFobUsd, 0);
  if (kgTotal === 0) {
    return null;
  }

  const ultimo = pontos[pontos.length - 1];
  const penultimo = pontos.length > 1 ? pontos[pontos.length - 2] : null;

  const precoUltimo = precoPorTonelada(ultimo);
  const precoPenultimo = penultimo ? precoPorTonelada(penultimo) : null;

  return {
    precoMedioPeriodoUsdPorTonelada: fobTotal / (kgTotal / 1000),
    precoUltimoMesUsdPorTonelada: precoUltimo,
    variacaoPrecoPercentual:
      precoPenultimo && precoPenultimo > 0 ? ((precoUltimo - precoPenultimo) / precoPenultimo) * 100 : null,
    volumeTotalToneladas: kgTotal / 1000,
    valorTotalFobUsd: fobTotal,
    ultimoPeriodo: ultimo.period,
  };
}

function precoPorTonelada(ponto: TimeSeriesPoint): number {
  return ponto.kgLiquido > 0 ? ponto.valorFobUsd / (ponto.kgLiquido / 1000) : 0;
}

const MESES_ABREVIADOS = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];

/** "2025-01" → "jan/2025". */
export function formatarPeriodo(period: string): string {
  const [ano, mes] = period.split('-');
  return `${MESES_ABREVIADOS[Number(mes) - 1]}/${ano}`;
}

/** Rótulo do intervalo exibido: "jan/2025 – jun/2025" (ou o mês único). */
export function rotuloPeriodo(pontos: TimeSeriesPoint[]): string | null {
  if (pontos.length === 0) {
    return null;
  }
  const primeiro = formatarPeriodo(pontos[0].period);
  const ultimo = formatarPeriodo(pontos[pontos.length - 1].period);
  return primeiro === ultimo ? primeiro : `${primeiro} – ${ultimo}`;
}
