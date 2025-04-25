import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { ActivatedRoute, Router } from '@angular/router';
import { FactoryService } from '../services/factory.service';
import { StageHealth } from '../models/stage-health.model';
import { ProductionOutput } from '../models/production-output.model';
import { DeviceHealth } from '../models/device-health.model';

@Component({
  selector: 'app-stage-details',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressBarModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule
  ],
  templateUrl: './stage-details.component.html',
  styleUrls: ['./stage-details.component.scss']
})
export class StageDetailsComponent implements OnInit {
  stageId: number = 0;
  stageHealth: StageHealth | null = null;
  productionOutput: ProductionOutput | null = null;
  loading = true;
  error = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private factoryService: FactoryService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.stageId = +params['id'];
      this.loadStageData();
    });
  }

  loadStageData(): void {
    this.loading = true;
    this.error = false;

    this.factoryService.getStageHealth(this.stageId).subscribe({
      next: (data) => {
        this.stageHealth = data;

        // After loading stage, get production data for today
        const today = new Date();
        const startTime = new Date(today.setHours(0, 0, 0, 0)).toISOString();
        const endTime = new Date().toISOString();

        this.factoryService.getStageOutput(data.sequenceOrder, startTime, endTime).subscribe({
          next: (output) => {
            this.productionOutput = output;
            this.loading = false;
          },
          error: (err) => {
            console.error('Error loading production output', err);
            this.loading = false;
          }
        });
      },
      error: (err) => {
        console.error('Error loading stage health', err);
        this.error = true;
        this.loading = false;
      }
    });
  }

  getHealthColor(score: number): string {
    if (score >= 90) return 'primary';
    if (score >= 70) return 'accent';
    return 'warn';
  }

  getDeviceStatusClass(device: DeviceHealth): string {
    if (!device.operational) return 'status-offline';
    if (device.healthScore < 70) return 'status-warning';
    return 'status-online';
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  refreshData(): void {
    this.loadStageData();
  }

  viewDeviceDetails(deviceId: number): void {
    this.router.navigate(['/stages', this.stageId, 'devices', deviceId]);
  }
}
