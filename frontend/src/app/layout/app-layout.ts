import { Component, computed, HostListener, inject, OnInit } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { MenuItem } from 'primeng/api';
import { filter } from 'rxjs';
import { AuthService } from '../services/auth/auth.service';
import { APP_NAV_ITEMS } from '../config/app-navigation.config';
import { BreadcrumbService } from '../services/breadcrumb.service';

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
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  breadcrumbs = this.breadcrumbService.items;

  menuItems = computed(() => {
    return APP_NAV_ITEMS.filter(item => {
      if (!item.showInNavbar) return false;
      if (!item.roles || item.roles.length === 0) return true;
      return this.authService.hasAnyRole(item.roles);
    });
  });

  ngOnInit(): void {
    this.setBreadcrumbsFromRoute();

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => this.setBreadcrumbsFromRoute());
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
