import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { TradeAnalyticsService } from './trade-analytics.service';
import { TimeSeriesPoint } from '../models/time-series-point.model';

describe('TradeAnalyticsService', () => {
  let service: TradeAnalyticsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TradeAnalyticsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('consulta a rota de série temporal com os filtros informados', () => {
    const resposta: TimeSeriesPoint[] = [{ period: '2025-06', kgLiquido: 1000, valorFobUsd: 2000 }];
    let recebido: TimeSeriesPoint[] | undefined;

    service
      .getTimeSeries({ flow: 'EXPORT', ncmChapter: 72, from: '2025-01', to: '2025-06' })
      .subscribe(r => (recebido = r));

    const req = httpMock.expectOne(
      r => r.url === '/api/v1/trade/time-series' && r.params.get('ncmChapter') === '72',
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('flow')).toBe('EXPORT');
    expect(req.request.params.get('from')).toBe('2025-01');
    req.flush(resposta);

    expect(recebido).toEqual(resposta);
  });

  it('omite parâmetros não informados', () => {
    service.getTimeSeries().subscribe();

    const req = httpMock.expectOne(r => r.url === '/api/v1/trade/time-series');
    expect(req.request.params.keys().length).toBe(0);
    req.flush([]);
  });
});
