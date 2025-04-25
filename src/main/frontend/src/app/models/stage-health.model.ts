import { DeviceHealth } from './device-health.model';

export interface StageHealth {
  stageId: number;
  stageName: string;
  sequenceOrder: number;
  overallHealthScore: number;
  totalDevices: number;
  operationalDevices: number;
  devices: DeviceHealth[];
}
