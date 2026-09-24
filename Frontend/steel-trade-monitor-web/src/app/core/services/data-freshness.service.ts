import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Execution, Page } from '../models/execution.model';
import { SourceFreshness } from '../models/source-freshness.model';

@Injectable({ providedIn: 'root' })
export class DataFreshnessService {
  private readonly http = inject(HttpClient);

  getFreshness(): Observable<SourceFreshness[]> {
    return this.http.get<SourceFreshness[]>('/api/v1/status/freshness');
  }

  getExecutions(page = 0, size = 20): Observable<Page<Execution>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<Execution>>('/api/v1/status/executions', { params });
  }
}
