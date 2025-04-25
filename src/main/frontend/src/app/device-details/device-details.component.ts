import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { FactoryService } from '../services/factory.service';
import { DeviceHealth } from '../models/device-health.model';

@Component({
  selector: 'app-device-details',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressBarModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatInputModule,
    MatFormFieldModule,
    FormsModule,
    ReactiveFormsModule
  ],
  templateUrl: './device-details.component.html',
  styleUrls: ['./device-details.component.scss']
})
export class DeviceDetailsComponent implements OnInit {
  deviceId: number = 0;
  stageId: number = 0;
  device: DeviceHealth | null = null;
  deviceForm: FormGroup;
  metricsForm: FormGroup;
  loading = true;
  error = false;
  updateSuccess = false;
  metricsSuccess = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private factoryService: FactoryService,
    private fb: FormBuilder
  ) {
    this.deviceForm = this.fb.group({
      operational: [true],
      healthScore: [100, [Validators.required, Validators.min(0), Validators.max(100)]]
    });

    this.metricsForm = this.fb.group({
      unitsProduced: [0, [Validators.required, Validators.min(0)]],
      defectiveUnits: [0, [Validators.required, Validators.min(0)]],
      cycleTimeMinutes: [5, [Validators.required, Validators.min(0.1)]]
    });
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.deviceId = +params['id'];
      this.stageId = +params['stageId'];
      this.loadDeviceData();
    });
  }

  loadDeviceData(): void {
    this.loading = true;
    this.error = false;

    // Get the stage data to find the device
    this.factoryService.getStageHealth(this.stageId).subscribe({
      next: (data) => {
        const deviceFound = data.devices.find(d => d.id === this.deviceId) || null;

        if (deviceFound) {
          this.device = deviceFound;

          // Update form with current values
          this.deviceForm.patchValue({
            operational: deviceFound.operational,
            healthScore: deviceFound.healthScore
          });

          this.loading = false;
        } else {
          this.error = true;
          this.loading = false;
        }
      },
      error: (err) => {
        console.error('Error loading device data', err);
        this.error = true;
        this.loading = false;
      }
    });
  }

  updateDeviceHealth(): void {
    if (this.deviceForm.invalid || !this.device) return;

    const operational = this.deviceForm.get('operational')?.value;
    const healthScore = this.deviceForm.get('healthScore')?.value;

    this.factoryService.updateDeviceHealth(this.deviceId, operational, healthScore).subscribe({
      next: () => {
        // Update local device data
        if (this.device) {
          this.device.operational = operational;
          this.device.healthScore = healthScore;
        }

        this.updateSuccess = true;
        setTimeout(() => this.updateSuccess = false, 3000);
      },
      error: (err) => {
        console.error('Error updating device health', err);
        this.error = true;
      }
    });
  }

  recordMetrics(): void {
    if (this.metricsForm.invalid || !this.device) return;

    const unitsProduced = this.metricsForm.get('unitsProduced')?.value;
    const defectiveUnits = this.metricsForm.get('defectiveUnits')?.value;
    const cycleTimeMinutes = this.metricsForm.get('cycleTimeMinutes')?.value;

    this.factoryService.recordProductionMetrics(
      this.deviceId,
      unitsProduced,
      defectiveUnits,
      cycleTimeMinutes
    ).subscribe({
      next: () => {
        this.metricsSuccess = true;
        setTimeout(() => this.metricsSuccess = false, 3000);

        // Reset the form
        this.metricsForm.patchValue({
          unitsProduced: 0,
          defectiveUnits: 0,
          cycleTimeMinutes: 5
        });
      },
      error: (err) => {
        console.error('Error recording metrics', err);
        this.error = true;
      }
    });
  }

  getHealthColor(score: number): string {
    if (score >= 90) return 'primary';
    if (score >= 70) return 'accent';
    return 'warn';
  }

  goBackToStage(): void {
    this.router.navigate(['/stages', this.stageId]);
  }
}
