import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink, Router } from '@angular/router';
import { SupplyChainService } from '../services/supply-chain.service';
import { FactoryService } from '../services/factory.service';
import { StageHealth } from '../models/stage-health.model';
import { SupplyChainStatus } from '../models/supply-chain-status.model';
import { trigger, transition, style, animate, state } from '@angular/animations';
import { interval, Subject, Subscription } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressBarModule,
    MatButtonModule,
    MatIconModule,
    MatBadgeModule,
    MatDividerModule,
    MatTooltipModule,
    RouterLink
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  animations: [
    trigger('fadeIn', [
      transition(':enter', [
        style({ opacity: 0 }),
        animate('600ms ease-in', style({ opacity: 1 }))
      ])
    ]),
    trigger('counterAnimation', [
      transition(':increment', [
        style({ color: '#4caf50', transform: 'scale(1.2)' }),
        animate('300ms ease-out', style({ color: '*', transform: 'scale(1)' }))
      ]),
      transition(':decrement', [
        style({ color: '#f44336', transform: 'scale(1.2)' }),
        animate('300ms ease-out', style({ color: '*', transform: 'scale(1)' }))
      ])
    ]),
    trigger('refreshRotate', [
      state('idle', style({ transform: 'rotate(0)' })),
      state('loading', style({ transform: 'rotate(360deg)' })),
      transition('idle => loading', animate('750ms linear')),
      transition('loading => idle', animate('0ms'))
    ])
  ]
})
export class DashboardComponent implements OnInit, OnDestroy {
  supplyChainStatus: SupplyChainStatus | null = null;
  previousSupplyChainStatus: SupplyChainStatus | null = null;
  stagesHealth: StageHealth[] = [];
  loading = true;
  error = false;
  refreshState = 'idle';

  // For auto-refresh
  autoRefresh = false;
  refreshInterval: Subscription | null = null;
  private destroy$ = new Subject<void>();

  // Animated counters
  displayCurrentOutput = 0;
  displayProjectedOutput = 0;
  displayTargetCompletion = 0;
  displayHealthScores: { [key: number]: number } = {};

  // KPI indicators
  productionTrend = 0; // 1 = up, 0 = stable, -1 = down
  healthTrend = 0;
  defectRate = 0;

  constructor(
    private supplyChainService: SupplyChainService,
    private factoryService: FactoryService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();

    if (this.refreshInterval) {
      this.refreshInterval.unsubscribe();
    }
  }

  loadDashboardData(): void {
    this.loading = true;
    this.error = false;
    this.refreshState = 'loading';

    // Store previous values for trends
    this.previousSupplyChainStatus = this.supplyChainStatus ? {...this.supplyChainStatus} : null;

    // Load supply chain status
    this.supplyChainService.getCurrentStatus().subscribe({
      next: (data) => {
        this.supplyChainStatus = data;
        this.calculateTrends();
        this.animateCounters();
        this.loading = false;
        this.refreshState = 'idle';
      },
      error: (err) => {
        console.error('Error loading supply chain status', err);
        this.error = true;
        this.loading = false;
        this.refreshState = 'idle';
      }
    });

    // Load stages health
    this.factoryService.getStagesHealth().subscribe({
      next: (data) => {
        this.stagesHealth = data;
        this.animateHealthScores();
        this.calculateDefectRate();
      },
      error: (err) => {
        console.error('Error loading stages health', err);
        this.error = true;
        this.loading = false;
        this.refreshState = 'idle';
      }
    });
  }

  toggleAutoRefresh(): void {
    this.autoRefresh = !this.autoRefresh;

    if (this.autoRefresh) {
      this.refreshInterval = interval(30000) // Refresh every 30 seconds
        .pipe(takeUntil(this.destroy$))
        .subscribe(() => this.loadDashboardData());
    } else if (this.refreshInterval) {
      this.refreshInterval.unsubscribe();
      this.refreshInterval = null;
    }
  }

  animateCounters(): void {
    if (!this.supplyChainStatus) return;

    // For simple animation, animate from current value to target over time
    const steps = 20;
    const duration = 1000;
    const stepTime = duration / steps;

    const currentOutputTarget = this.supplyChainStatus.currentOutput;
    const projectedOutputTarget = this.supplyChainStatus.projectedEndOfDayOutput;
    const completionTarget = this.supplyChainStatus.targetCompletionPercentage;

    const currentOutputStep = (currentOutputTarget - this.displayCurrentOutput) / steps;
    const projectedOutputStep = (projectedOutputTarget - this.displayProjectedOutput) / steps;
    const completionStep = (completionTarget - this.displayTargetCompletion) / steps;

    let currentStep = 0;

    const timer = setInterval(() => {
      currentStep++;

      this.displayCurrentOutput = Math.round(this.displayCurrentOutput + currentOutputStep);
      this.displayProjectedOutput = Math.round(this.displayProjectedOutput + projectedOutputStep);
      this.displayTargetCompletion = +(this.displayTargetCompletion + completionStep).toFixed(1);

      if (currentStep >= steps) {
        this.displayCurrentOutput = currentOutputTarget;
        this.displayProjectedOutput = projectedOutputTarget;
        this.displayTargetCompletion = completionTarget;
        clearInterval(timer);
      }
    }, stepTime);
  }

  animateHealthScores(): void {
    const steps = 20;
    const duration = 1000;
    const stepTime = duration / steps;

    // Initialize display scores if needed
    this.stagesHealth.forEach(stage => {
      if (this.displayHealthScores[stage.stageId] === undefined) {
        this.displayHealthScores[stage.stageId] = 0;
      }
    });

    // Calculate steps for each health score
    const healthSteps: { [key: number]: number } = {};
    this.stagesHealth.forEach(stage => {
      healthSteps[stage.stageId] =
        (stage.overallHealthScore - this.displayHealthScores[stage.stageId]) / steps;
    });

    let currentStep = 0;

    const timer = setInterval(() => {
      currentStep++;

      this.stagesHealth.forEach(stage => {
        this.displayHealthScores[stage.stageId] =
          +(this.displayHealthScores[stage.stageId] + healthSteps[stage.stageId]).toFixed(1);
      });

      if (currentStep >= steps) {
        this.stagesHealth.forEach(stage => {
          this.displayHealthScores[stage.stageId] = stage.overallHealthScore;
        });
        clearInterval(timer);
      }
    }, stepTime);
  }

  calculateTrends(): void {
    if (!this.previousSupplyChainStatus || !this.supplyChainStatus) {
      this.productionTrend = 0;
      return;
    }

    // Production trend
    if (this.supplyChainStatus.currentOutput > this.previousSupplyChainStatus.currentOutput) {
      this.productionTrend = 1; // Up
    } else if (this.supplyChainStatus.currentOutput < this.previousSupplyChainStatus.currentOutput) {
      this.productionTrend = -1; // Down
    } else {
      this.productionTrend = 0; // Stable
    }
  }

  calculateDefectRate(): void {
    if (!this.supplyChainStatus || !this.stagesHealth.length) return;

    let totalDefects = 0;
    let totalProduced = 0;

    // Sum up all defects and produced units across all stages
    this.supplyChainStatus.stageOutputs.forEach(stage => {
      totalDefects += stage.defectiveUnits;
      totalProduced += stage.unitsProduced;
    });

    this.defectRate = totalProduced > 0 ? (totalDefects / totalProduced) * 100 : 0;
  }

  getStatusColor(percentage: number): string {
    if (percentage >= 90) return 'primary';
    if (percentage >= 75) return 'accent';
    return 'warn';
  }

  getHealthColor(score: number): string {
    if (score >= 90) return 'primary';
    if (score >= 70) return 'accent';
    return 'warn';
  }

  getOperationalRatio(stage: StageHealth): string {
    return `${stage.operationalDevices}/${stage.totalDevices}`;
  }

  getTrendIcon(trend: number): string {
    if (trend > 0) return 'trending_up';
    if (trend < 0) return 'trending_down';
    return 'trending_flat';
  }

  getTrendClass(trend: number, inverse: boolean = false): string {
    if (trend === 0) return 'trend-stable';

    if (inverse) {
      return trend > 0 ? 'trend-negative' : 'trend-positive';
    } else {
      return trend > 0 ? 'trend-positive' : 'trend-negative';
    }
  }

  viewStageDetails(stageId: number): void {
    this.router.navigate(['/stages', stageId]);
  }

  refreshData(): void {
    this.loadDashboardData();
  }
}
