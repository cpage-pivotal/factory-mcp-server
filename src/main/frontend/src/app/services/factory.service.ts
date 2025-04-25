import {Injectable, Inject, Injector} from '@angular/core';
import {DOCUMENT} from '@angular/common';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {StageHealth} from '../models/stage-health.model';
import {ProductionOutput} from '../models/production-output.model';

@Injectable({
  providedIn: 'root'
})
export class FactoryService {
  private apiUrl = '/api/factory';
  host = '';
  protocol = '';

  constructor(private http: HttpClient, private injector: Injector,
              @Inject(DOCUMENT) private document: Document) {
    if (this.document.location.hostname == 'localhost') {
      this.host = 'localhost:8080';
    } else this.host = this.document.location.host;
    this.protocol = this.document.location.protocol;
  }

  getStagesHealth(): Observable<StageHealth[]> {
    return this.http.get<StageHealth[]>(`${this.protocol}//${this.host}${this.apiUrl}/stages/health`);
  }

  getStageHealth(stageId: number): Observable<StageHealth> {
    return this.http.get<StageHealth>(`${this.protocol}//${this.host}${this.apiUrl}/stages/${stageId}/health`);
  }

  updateDeviceHealth(deviceId: number, operational: boolean, healthScore: number): Observable<void> {
    return this.http.put<void>(`${this.protocol}//${this.host}${this.apiUrl}/devices/${deviceId}/health`, {
      operational,
      healthScore
    });
  }

  getStageOutput(stageOrder: number, startTime: string, endTime: string): Observable<ProductionOutput> {
    return this.http.get<ProductionOutput>(
      `${this.protocol}//${this.host}${this.apiUrl}/stages/${stageOrder}/output`,
      {params: {startTime, endTime}}
    );
  }

  getAllStagesOutput(startTime: string, endTime: string): Observable<ProductionOutput[]> {
    return this.http.get<ProductionOutput[]>(
      `${this.protocol}//${this.host}${this.apiUrl}/output`,
      {params: {startTime, endTime}}
    );
  }

  recordProductionMetrics(
    deviceId: number,
    unitsProduced: number,
    defectiveUnits: number,
    cycleTimeMinutes: number
  ): Observable<void> {
    return this.http.post<void>(`${this.protocol}//${this.host}${this.apiUrl}/devices/${deviceId}/metrics`, {
      unitsProduced,
      defectiveUnits,
      cycleTimeMinutes
    });
  }
}
