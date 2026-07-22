import { computed, Injectable, signal } from '@angular/core';

export type AppTheme = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly storageKey = 'app_theme';
  private readonly darkClass = 'app-dark';
  private readonly _theme = signal<AppTheme>(this.getInitialTheme());

  readonly theme = computed(() => this._theme());
  readonly isDark = computed(() => this._theme() === 'dark');
  readonly icon = computed(() => this.isDark() ? 'pi pi-sun' : 'pi pi-moon');
  readonly label = computed(() => this.isDark() ? 'Svijetla tema' : 'Tamna tema');

  constructor() {
    this.applyTheme(this._theme());
  }

  toggleTheme(): void {
    this.setTheme(this.isDark() ? 'light' : 'dark');
  }

  setTheme(theme: AppTheme): void {
    this._theme.set(theme);
    this.applyTheme(theme);

    if (this.hasBrowserStorage()) {
      localStorage.setItem(this.storageKey, theme);
    }
  }

  private getInitialTheme(): AppTheme {
    if (this.hasBrowserStorage()) {
      const savedTheme = localStorage.getItem(this.storageKey);

      if (savedTheme === 'light' || savedTheme === 'dark') {
        return savedTheme;
      }
    }

    return 'dark';
  }

  private applyTheme(theme: AppTheme): void {
    if (typeof document === 'undefined') {
      return;
    }

    document.documentElement.classList.toggle(this.darkClass, theme === 'dark');
    document.documentElement.style.colorScheme = theme;
  }

  private hasBrowserStorage(): boolean {
    return typeof localStorage !== 'undefined';
  }
}
