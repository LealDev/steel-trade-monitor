import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { DataFreshnessService } from './data-freshness.service';
import { SourceFreshness } from '../models/source-freshness.model';

describe('DataFreshnessService', () => {
  let service: DataFreshnessService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DataFreshnessService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('consulta a rota de freshness', () => {
    const resposta: SourceFreshness[] = [
      {
        fonte: 'COMEXSTAT',
        status: 'SUCESSO',
        iniciadoEm: '2026-09-22T03:00:00Z',
        finalizadoEm: '2026-09-22T03:02:00Z',
        periodoReferencia: '2025-04..2026-09',
        registrosGravados: 5995,
        mensagem: null,
        ultimoSucessoEm: '2026-09-22T03:02:00Z',
      },
    ];
    let recebido: SourceFreshness[] | undefined;

    service.getFreshness().subscribe(r => (recebido = r));

    const req = httpMock.expectOne('/api/v1/status/freshness');
    expect(req.request.method).toBe('GET');
    req.flush(resposta);

    expect(recebido).toEqual(resposta);
  });
});
