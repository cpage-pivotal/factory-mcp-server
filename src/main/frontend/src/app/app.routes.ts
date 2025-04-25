import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard.component';
import { StageDetailsComponent } from './stage-details/stage-details.component';
import { DeviceDetailsComponent } from './device-details/device-details.component';
import { SupplyChainComponent } from './supply-chain/supply-chain.component';
import { ProductionMetricsComponent } from './production-metrics/production-metrics.component';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'stages/:id', component: StageDetailsComponent },
  { path: 'stages/:stageId/devices/:id', component: DeviceDetailsComponent },
  { path: 'supply-chain', component: SupplyChainComponent },
  { path: 'metrics', component: ProductionMetricsComponent }
];
