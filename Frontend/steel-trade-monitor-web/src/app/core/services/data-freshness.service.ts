import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { SourceFreshness } from '../models/source-freshness.model';

@Injectable({ providedIn: 'root' })
export class DataFreshnessService {
  private readonly http = inject(HttpClient);

  getFreshness(): Observable<SourceFreshness[]> {
    return this.http.get<SourceFreshness[]>('/api/v1/status/freshness');
  }
}
