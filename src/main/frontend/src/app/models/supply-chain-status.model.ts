import { ProductionOutput } from './production-output.model';

export interface SupplyChainStatus {
  date: string;
  dailyTarget: number;
  currentOutput: number;
  projectedEndOfDayOutput: number;
  targetCompletionPercentage: number;
  onTrack: boolean;
  stageOutputs: ProductionOutput[];
}
