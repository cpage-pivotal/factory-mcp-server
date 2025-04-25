export interface DeviceHealth {
  id: number;
  deviceId: string;
  name: string;
  deviceType: string;
  operational: boolean;
  healthScore: number;
}
