import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { KnobModule } from 'primeng/knob';
import { SkeletonModule } from 'primeng/skeleton';
import { finalize } from 'rxjs';
import { IDashboard, IDashboardChart, IDashboardStat } from '../../models/dashboard/IDashboard';
import { DashboardService } from '../../services/dashboard.service';

interface DashboardChartView {
  key: string;
  title: string;
  type: 'doughnut' | 'bar';
  data: unknown;
  options: unknown;
  hasData: boolean;
}

@Component({
  selector: 'app-dashboard',
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    CardModule,
    ChartModule,
    KnobModule,
    SkeletonModule
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  dashboard = signal<IDashboard | null>(null);
  chartViews = signal<DashboardChartView[]>([]);
  compactViewport = signal(false);
  loading = signal(true);
  error = signal(false);
  skeletonStats = Array.from({ length: 6 });
  skeletonCharts = Array.from({ length: 4 });

  private dashboardService = inject(DashboardService);

  private readonly palette = [
    '#2563eb',
    '#16a34a',
    '#f59e0b',
    '#dc2626',
    '#7c3aed',
    '#0891b2',
    '#475569',
    '#db2777',
  ];

  ngOnInit(): void {
    this.updateViewportState();
    this.loading.set(true);
    this.dashboardService.getDashboard().pipe(
      finalize(() => {
        this.loading.set(false);
      })
    ).subscribe({
      next: dashboard => {
        const nextDashboard = {
          ...dashboard,
          stats: dashboard.stats ?? [],
          charts: dashboard.charts ?? [],
        };
        this.dashboard.set(nextDashboard);
        this.chartViews.set(nextDashboard.charts.map(chart => this.toChartView(chart)));
        this.error.set(false);
      },
      error: () => {
        this.error.set(true);
      },
    });
  }

  @HostListener('window:resize')
  onResize(): void {
    const wasCompact = this.compactViewport();
    this.updateViewportState();

    if (wasCompact !== this.compactViewport() && this.dashboard()) {
      this.chartViews.set(this.dashboard()!.charts.map(chart => this.toChartView(chart)));
    }
  }

  isActiveAssignmentStat(dashboard: IDashboard, stat: IDashboardStat): boolean {
    return dashboard.role === 'STUDENT' && stat.key === 'activeAssignments' && stat.value > 0;
  }

  isAnimatedStat(stat: IDashboardStat): boolean {
    return ['ungradedAssignments', 'profUngradedAssignments'].includes(stat.key) && stat.value > 0;
  }

  isKnobStat(stat: IDashboardStat): boolean {
    return ['averageScore', 'completionRate', 'testPassRate', 'profAverageScore']
      .includes(stat.key);
  }

  knobValue(value: number): number {
    return Math.max(0, Math.min(100, Math.round(value)));
  }

  knobSize(): number {
    return this.compactViewport() ? 96 : 112;
  }

  private updateViewportState(): void {
    this.compactViewport.set(window.innerWidth <= 640);
  }

  private toChartView(chart: IDashboardChart): DashboardChartView {
    return {
      key: chart.key,
      title: chart.title,
      type: chart.type,
      data: this.chartData(chart),
      options: this.chartOptions(chart),
      hasData: (chart.values ?? []).some(value => value > 0),
    };
  }

  private chartData(chart: IDashboardChart): unknown {
    return {
      labels: chart.labels,
      datasets: [
        {
          data: chart.values,
          backgroundColor: chart.labels.map((_, index) => this.palette[index % this.palette.length]),
          borderColor: chart.type === 'bar' ? '#1f2937' : '#ffffff',
          borderWidth: chart.type === 'bar' ? 0 : 2,
          borderRadius: chart.type === 'bar' ? 6 : 0,
        },
      ],
    };
  }

  private chartOptions(chart: IDashboardChart): unknown {
    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: this.compactViewport() || chart.type === 'bar' ? 'bottom' : 'right',
          labels: {
            boxWidth: 12,
            boxHeight: 12,
            usePointStyle: true,
            padding: this.compactViewport() ? 10 : 14,
          },
        },
      },
      scales:
        chart.type === 'bar'
          ? {
              y: {
                beginAtZero: true,
                ticks: {
                  precision: 0,
                },
              },
            }
          : undefined,
    };
  }
}
