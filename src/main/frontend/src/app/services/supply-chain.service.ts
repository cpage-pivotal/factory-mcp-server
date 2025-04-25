import {Injectable, Inject, Injector} from '@angular/core';
import {DOCUMENT} from '@angular/common';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {SupplyChainStatus} from '../models/supply-chain-status.model';
import {DailyTarget} from '../models/daily-target.model';

@Injectable({
  providedIn: 'root'
})
export class SupplyChainService {
  private apiUrl = '/api/supply-chain';
  host = '';
  protocol = '';

  constructor(private http: HttpClient, private injector: Injector,
              @Inject(DOCUMENT) private document: Document) {
    if (this.document.location.hostname == 'localhost') {
      this.host = 'localhost:8080';
    } else this.host = this.document.location.host;
    this.protocol = this.document.location.protocol;
  }

  getCurrentStatus(): Observable<SupplyChainStatus> {
    return this.http.get<SupplyChainStatus>(`${this.protocol}//${this.host}${this.apiUrl}/status`);
  }

  getStatusByDate(date: string): Observable<SupplyChainStatus> {
    return this.http.get<SupplyChainStatus>(`${this.protocol}//${this.host}${this.apiUrl}/status/${date}`);
  }

  getDailyTarget(date: string): Observable<DailyTarget> {
    return this.http.get<DailyTarget>(`${this.protocol}//${this.host}${this.apiUrl}/targets/${date}`);
  }

  setDailyTarget(date: string, targetUnits: number): Observable<DailyTarget> {
    return this.http.post<DailyTarget>(`${this.protocol}//${this.host}${this.apiUrl}/targets`, {
      date,
      targetUnits
    });
  }
}
