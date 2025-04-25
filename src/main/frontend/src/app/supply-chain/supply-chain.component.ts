import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { SupplyChainService } from '../services/supply-chain.service';
import { SupplyChainStatus } from '../models/supply-chain-status.model';

@Component({
  selector: 'app-supply-chain',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressBarModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatFormFieldModule,
    FormsModule,
    ReactiveFormsModule
  ],
  templateUrl: './supply-chain.component.html',
  styleUrls: ['./supply-chain.component.scss']
})
export class SupplyChainComponent implements OnInit {
  supplyChainStatus: SupplyChainStatus | null = null;
  dateForm: FormGroup;
  targetForm: FormGroup;
  selectedDate: Date = new Date();
  loading = true;
  error = false;

  constructor(
    private supplyChainService: SupplyChainService,
    private router: Router,
    private fb: FormBuilder
  ) {
    this.dateForm = this.fb.group({
      date: [new Date(), Validators.required]
    });

    this.targetForm = this.fb.group({
      targetUnits: ['', [Validators.required, Validators.min(1)]]
    });
  }

  ngOnInit(): void {
    this.loadCurrentStatus();
  }

  loadCurrentStatus(): void {
    this.loading = true;
    this.error = false;

    this.supplyChainService.getCurrentStatus().subscribe({
      next: (data) => {
        this.supplyChainStatus = data;
        this.loading = false;

        // Update the target form with current target
        this.targetForm.patchValue({
          targetUnits: data.dailyTarget
        });
      },
      error: (err) => {
        console.error('Error loading supply chain status', err);
        this.error = true;
        this.loading = false;
      }
    });
  }

  loadStatusByDate(): void {
    if (this.dateForm.invalid) return;

    const date = this.dateForm.get('date')?.value;
    if (!date) return;

    this.selectedDate = date;
    const formattedDate = this.formatDate(date);

    this.loading = true;
    this.error = false;

    this.supplyChainService.getStatusByDate(formattedDate).subscribe({
      next: (data) => {
        this.supplyChainStatus = data;
        this.loading = false;

        // Update the target form with the date's target
        this.targetForm.patchValue({
          targetUnits: data.dailyTarget
        });
      },
      error: (err) => {
        console.error('Error loading supply chain status for date', err);
        this.error = true;
        this.loading = false;
      }
    });
  }

  updateDailyTarget(): void {
    if (this.targetForm.invalid) return;

    const targetUnits = this.targetForm.get('targetUnits')?.value;
    const formattedDate = this.formatDate(this.selectedDate);

    this.loading = true;

    this.supplyChainService.setDailyTarget(formattedDate, targetUnits).subscribe({
      next: () => {
        // Reload status to reflect updated target
        this.loadStatusByDate();
      },
      error: (err) => {
        console.error('Error updating daily target', err);
        this.error = true;
        this.loading = false;
      }
    });
  }

  getStatusColor(percentage: number): string {
    if (percentage >= 90) return 'primary';
    if (percentage >= 75) return 'accent';
    return 'warn';
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
