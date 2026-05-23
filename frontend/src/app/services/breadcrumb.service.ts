import { Injectable, signal } from '@angular/core';
import { MenuItem } from 'primeng/api';

@Injectable({ providedIn: 'root' })
export class BreadcrumbService {
  private readonly itemsSignal = signal<MenuItem[]>([]);

  readonly items = this.itemsSignal.asReadonly();

  set(items: MenuItem[]): void {
    this.itemsSignal.set(items);
  }

  reset(): void {
    this.itemsSignal.set([]);
  }

  shorten(label: string, maxLength = 20): string {
    const trimmed = label.trim();

    if (trimmed.length <= maxLength) {
      return trimmed;
    }

    return `${trimmed.slice(0, maxLength).trim()}...`;
  }
}
