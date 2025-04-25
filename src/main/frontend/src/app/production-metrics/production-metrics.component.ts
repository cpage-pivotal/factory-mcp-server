import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { FactoryService } from '../services/factory.service';
import { ProductionOutput } from '../models/production-output.model';
import { StageHealth } from '../models/stage-health.model';
import {MatProgressBar} from '@angular/material/progress-bar';

@Component({
  selector: 'app-production-metrics',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    MatNativeDateModule,
    MatSelectModule,
    FormsModule,
    ReactiveFormsModule,
    MatProgressBar
  ],
  templateUrl: './production-metrics.component.html',
  styleUrls: ['./production-metrics.component.scss']
})
export class ProductionMetricsComponent implements OnInit {
  metricsForm: FormGroup;
  stages: StageHealth[] = [];
  productionOutputs: ProductionOutput[] = [];
  loading = true;
  error = false;

  constructor(
    private factoryService: FactoryService,
    private router: Router,
    private fb: FormBuilder
  ) {
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    this.metricsForm = this.fb.group({
      startDate: [yesterday, Validators.required],
      endDate: [today, Validators.required],
      stageOrder: [0] // 0 means all stages
    });
  }

  ngOnInit(): void {
    this.loadStages();
  }

  loadStages(): void {
    this.loading = true;
    this.error = false;

    this.factoryService.getStagesHealth().subscribe({
      next: (data) => {
        this.stages = data;
        this.loading = false;
        this.loadMetrics();
      },
      error: (err) => {
        console.error('Error loading stages', err);
        this.error = true;
        this.loading = false;
      }
    });
  }

  loadMetrics(): void {
    if (this.metricsForm.invalid) return;

    const startDate = this.metricsForm.get('startDate')?.value;
    const endDate = this.metricsForm.get('endDate')?.value;
    const stageOrder = this.metricsForm.get('stageOrder')?.value;

    if (!startDate || !endDate) return;

    // Set time to start of day for startDate and end of day for endDate
    const startDateTime = new Date(startDate);
    startDateTime.setHours(0, 0, 0, 0);

    const endDateTime = new Date(endDate);
    endDateTime.setHours(23, 59, 59, 999);

    this.loading = true;
    this.error = false;

    this.factoryService.getAllStagesOutput(
      startDateTime.toISOString(),
      endDateTime.toISOString()
    ).subscribe({
      next: (data) => {
        // Filter by stage if selected
        if (stageOrder > 0) {
          this.productionOutputs = data.filter(output => output.stageOrder === stageOrder);
        } else {
          this.productionOutputs = data;
        }

        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading metrics', err);
        this.error = true;
        this.loading = false;
      }
    });
  }

  getTotalProduction(): number {
    if (!this.productionOutputs.length) return 0;

    // Get the highest stage order (final assembly)
    const maxStageOrder = Math.max(...this.productionOutputs.map(o => o.stageOrder));

    // Return units produced from the final stage
    const finalStage = this.productionOutputs.find(o => o.stageOrder === maxStageOrder);
    return finalStage ? finalStage.unitsProduced : 0;
  }

  getTotalGoodUnits(): number {
    if (!this.productionOutputs.length) return 0;

    // Get the highest stage order (final assembly)
    const maxStageOrder = Math.max(...this.productionOutputs.map(o => o.stageOrder));

    // Return good units from the final stage
    const finalStage = this.productionOutputs.find(o => o.stageOrder === maxStageOrder);
    return finalStage ? (finalStage.unitsProduced - finalStage.defectiveUnits) : 0;
  }

  getTotalDefects(): number {
    if (!this.productionOutputs.length) return 0;

    return this.productionOutputs.reduce((total, output) => total + output.defectiveUnits, 0);
  }

  getOverallYield(): number {
    const totalProduction = this.getTotalProduction();
    if (totalProduction === 0) return 0;

    const goodUnits = this.getTotalGoodUnits();
    return (goodUnits / totalProduction) * 100;
  }

  getYieldColor(percentage: number): string {
    if (percentage >= 95) return 'primary';
    if (percentage >= 85) return 'accent';
    return 'warn';
  }

  formatDateRange(): string {
    const startDate = this.metricsForm.get('startDate')?.value;
    const endDate = this.metricsForm.get('endDate')?.value;

    if (!startDate || !endDate) return '';

    return `${new Date(startDate).toLocaleDateString()} - ${new Date(endDate).toLocaleDateString()}`;
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }
}
