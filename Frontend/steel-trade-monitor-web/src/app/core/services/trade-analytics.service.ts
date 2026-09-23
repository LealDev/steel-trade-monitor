import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { TimeSeriesPoint } from '../models/time-series-point.model';

export interface TimeSeriesFilter {
  flow?: 'EXPORT' | 'IMPORT';
  ncmChapter?: number;
  from?: string;
  to?: string;
}

@Injectable({ providedIn: 'root' })
export class TradeAnalyticsService {
  private readonly http = inject(HttpClient);

  getTimeSeries(filter: TimeSeriesFilter = {}): Observable<TimeSeriesPoint[]> {
    let params = new HttpParams();
    if (filter.flow) params = params.set('flow', filter.flow);
    if (filter.ncmChapter != null) params = params.set('ncmChapter', filter.ncmChapter);
    if (filter.from) params = params.set('from', filter.from);
    if (filter.to) params = params.set('to', filter.to);
    return this.http.get<TimeSeriesPoint[]>('/api/v1/trade/time-series', { params });
  }
}
