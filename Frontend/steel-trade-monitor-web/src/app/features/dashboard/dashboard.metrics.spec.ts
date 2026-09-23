import { formatarPeriodo, resumirSerie, rotuloPeriodo, serieDePreco } from './dashboard.metrics';
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

describe('serieDePreco', () => {
  it('calcula o preço mensal em US$/t', () => {
    const precos = serieDePreco([
      { period: '2025-05', kgLiquido: 1_000_000, valorFobUsd: 500_000 },
      { period: '2025-06', kgLiquido: 2_000_000, valorFobUsd: 1_600_000 },
    ]);
    expect(precos).toEqual([
      { period: '2025-05', usdPorTonelada: 500 },
      { period: '2025-06', usdPorTonelada: 800 },
    ]);
  });

  it('omite meses sem volume', () => {
    const precos = serieDePreco([{ period: '2025-06', kgLiquido: 0, valorFobUsd: 10 }]);
    expect(precos).toEqual([]);
  });
});

describe('rotuloPeriodo', () => {
  it('formata o intervalo entre o primeiro e o último mês da série', () => {
    expect(
      rotuloPeriodo([
        { period: '2025-01', kgLiquido: 1, valorFobUsd: 1 },
        { period: '2025-06', kgLiquido: 1, valorFobUsd: 1 },
      ]),
    ).toBe('jan/2025 – jun/2025');
  });

  it('mês único aparece sem intervalo', () => {
    expect(rotuloPeriodo([{ period: '2025-12', kgLiquido: 1, valorFobUsd: 1 }])).toBe('dez/2025');
  });

  it('série vazia devolve null', () => {
    expect(rotuloPeriodo([])).toBeNull();
  });

  it('formata um período isolado', () => {
    expect(formatarPeriodo('2026-02')).toBe('fev/2026');
  });
});
