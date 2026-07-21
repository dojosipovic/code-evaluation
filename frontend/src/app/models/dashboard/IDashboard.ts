import { AppRole } from '../../config/app-types';

export interface IDashboard {
  role: AppRole;
  stats: IDashboardStat[];
  charts: IDashboardChart[];
}

export interface IDashboardStat {
  key: string;
  label: string;
  value: number;
  suffix: string | null;
}

export interface IDashboardChart {
  key: string;
  title: string;
  type: 'doughnut' | 'bar';
  labels: string[];
  values: number[];
}
