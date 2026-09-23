import { resumirSerie } from './dashboard.metrics';
import { TimeSeriesPoint } from '../../core/models/time-series-point.model';

describe('resumirSerie', () => {
  const serie: TimeSeriesPoint[] = [
    // 1.000 t a US$ 500/t
    { period: '2025-05', kgLiquido: 1_000_000, valorFobUsd: 500_000 },
    // 2.000 t a US$ 800/t
    { period: '2025-06', kgLiquido: 2_000_000, valorFobUsd: 1_600_000 },
  ];

  it('calcula o preço médio ponderado pelo volume, não a média dos preços mensais', () => {
    const resumo = resumirSerie(serie)!;
    // (500k + 1.600k) / 3.000 t = 700 — e não (500+800)/2 = 650
    expect(resumo.precoMedioPeriodoUsdPorTonelada).toBeCloseTo(700, 6);
  });

  it('calcula o preço do último mês e a variação contra o anterior', () => {
    const resumo = resumirSerie(serie)!;
    expect(resumo.precoUltimoMesUsdPorTonelada).toBeCloseTo(800, 6);
    expect(resumo.variacaoPrecoPercentual).toBeCloseTo(60, 6); // 500 → 800
    expect(resumo.ultimoPeriodo).toBe('2025-06');
  });

  it('acumula volume em toneladas e FOB total', () => {
    const resumo = resumirSerie(serie)!;
    expect(resumo.volumeTotalToneladas).toBeCloseTo(3000, 6);
    expect(resumo.valorTotalFobUsd).toBeCloseTo(2_100_000, 6);
  });

  it('sem mês anterior não há variação', () => {
    const resumo = resumirSerie([serie[1]])!;
    expect(resumo.variacaoPrecoPercentual).toBeNull();
  });

  it('série vazia ou sem volume devolve null', () => {
    expect(resumirSerie([])).toBeNull();
    expect(resumirSerie([{ period: '2025-06', kgLiquido: 0, valorFobUsd: 10 }])).toBeNull();
  });
});
