import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { CountryRanking } from '../models/country-ranking.model';
import { TimeSeriesPoint } from '../models/time-series-point.model';
import { TransportBreakdown } from '../models/transport-breakdown.model';

export interface TradeFilter {
  flow?: 'EXPORT' | 'IMPORT';
  ncmChapter?: number;
  from?: string;
  to?: string;
}

@Injectable({ providedIn: 'root' })
export class TradeAnalyticsService {
  private readonly http = inject(HttpClient);

  getTimeSeries(filter: TradeFilter = {}): Observable<TimeSeriesPoint[]> {
    return this.http.get<TimeSeriesPoint[]>('/api/v1/trade/time-series', {
      params: this.params(filter),
    });
  }

  getTopCountries(filter: TradeFilter = {}, limit = 10): Observable<CountryRanking[]> {
    return this.http.get<CountryRanking[]>('/api/v1/trade/top-countries', {
      params: this.params(filter).set('limit', limit),
    });
  }

  getByTransport(filter: TradeFilter = {}): Observable<TransportBreakdown[]> {
    return this.http.get<TransportBreakdown[]>('/api/v1/trade/by-transport', {
      params: this.params(filter),
    });
  }

  private params(filter: TradeFilter): HttpParams {
    let params = new HttpParams();
    if (filter.flow) params = params.set('flow', filter.flow);
    if (filter.ncmChapter != null) params = params.set('ncmChapter', filter.ncmChapter);
    if (filter.from) params = params.set('from', filter.from);
    if (filter.to) params = params.set('to', filter.to);
    return params;
  }
}
