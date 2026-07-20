import { Location } from '@angular/common';
import { Component, computed, HostListener, inject, OnInit } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { MenuItem } from 'primeng/api';
import { filter } from 'rxjs';
import { AuthService } from '../services/auth/auth.service';
import { APP_NAV_ITEMS } from '../config/app-navigation.config';
import { BreadcrumbService } from '../services/breadcrumb.service';
import { ThemeService } from '../services/theme.service';

@Component({
  selector: 'app-layout',
  imports: [RouterModule, ButtonModule, BreadcrumbModule],
  templateUrl: './app-layout.html',
  styleUrl: './app-layout.scss',
})
export class AppLayout implements OnInit {
  collapsed = false;
  mobileMenuVisible = false;
  isMobile = typeof window !== 'undefined' ? window.innerWidth < 768 : false;

  private authService = inject(AuthService);
  private breadcrumbService = inject(BreadcrumbService);
  protected themeService = inject(ThemeService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private location = inject(Location);

  breadcrumbs = this.breadcrumbService.items;
  private currentUrl = '';
  private navbarNavigation = false;
  backTarget: string | null = null;

  menuItems = computed(() => {
    return APP_NAV_ITEMS.filter(item => {
      if (!item.showInNavbar) return false;
      if (!item.roles || item.roles.length === 0) return true;
      return this.authService.hasAnyRole(item.roles);
    });
  });

  ngOnInit(): void {
    this.currentUrl = this.router.url;
    this.setBreadcrumbsFromRoute();

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(event => {
        const nextUrl = event.urlAfterRedirects;
        this.updateBackTarget(this.currentUrl, nextUrl);

        this.navbarNavigation = false;
        this.currentUrl = nextUrl;
        this.setBreadcrumbsFromRoute();
      });
  }

  @HostListener('window:resize')
  onResize() {
    this.isMobile = window.innerWidth < 768;

    if (!this.isMobile) {
      this.mobileMenuVisible = false;
    }
  }

  toggleSidebar() {
    if (this.isMobile) {
      this.mobileMenuVisible = !this.mobileMenuVisible;
    } else {
      this.collapsed = !this.collapsed;
    }
  }

  closeMobileMenu() {
    this.mobileMenuVisible = false;
  }

  onNavClick(): void {
    this.navbarNavigation = true;
    this.closeMobileMenu();
  }

  goBack(): void {
    if (!this.backTarget) {
      return;
    }

    this.location.back();
  }

  private updateBackTarget(previousUrl: string, nextUrl: string): void {
    if (this.navbarNavigation) {
      this.backTarget = null;
      return;
    }

    const configuredBackTarget = this.getConfiguredBackTarget();

    if (!configuredBackTarget) {
      this.backTarget = null;
      return;
    }

    if (this.normalizeUrl(nextUrl) !== configuredBackTarget) {
      this.backTarget = configuredBackTarget;
      return;
    }

    if (this.normalizeUrl(previousUrl) === configuredBackTarget) {
      this.backTarget = configuredBackTarget;
      return;
    }

    if (this.backTarget === configuredBackTarget) {
      return;
    }

    if (this.normalizeUrl(nextUrl) !== this.normalizeUrl(previousUrl)) {
      this.backTarget = null;
    }
  }

  private getConfiguredBackTarget(): string | null {
    let route = this.route.firstChild;
    let backTarget: string | null = null;

    while (route) {
      const routePath = route.snapshot.routeConfig?.path;
      const groupId = Number(route.snapshot.queryParamMap.get('groupId'));

      if (routePath === 'submissions/:id' && Number.isFinite(groupId)) {
        backTarget = this.normalizeUrl(`/groups/${groupId}/tasks`);
      }

      const configuredValue = route.snapshot.data['backTo'];

      if (typeof configuredValue === 'string') {
        backTarget = this.normalizeUrl(configuredValue);
      }

      route = route.firstChild;
    }

    return backTarget;
  }

  private normalizeUrl(url: string): string {
    return url.split('?')[0].split('#')[0];
  }

  private setBreadcrumbsFromRoute(): void {
    const items: MenuItem[] = [];
    let route = this.route.firstChild;

    while (route) {
      const label = route.snapshot.data['breadcrumb'];

      if (label) {
        items.push({
          label,
          routerLink: this.buildRouterLink(route)
        });
      }

      route = route.firstChild;
    }

    this.breadcrumbService.set(items);
  }

  private buildRouterLink(route: ActivatedRoute): string {
    const segments = [];
    let current: ActivatedRoute | null = route;

    while (current) {
      const path = current.snapshot.url.map(segment => segment.path).join('/');

      if (path) {
        segments.unshift(path);
      }

      current = current.parent;
    }

    return `/${segments.join('/')}`;
  }
}
