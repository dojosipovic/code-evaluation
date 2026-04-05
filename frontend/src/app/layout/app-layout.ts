import { Component, computed, HostListener, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../services/auth/auth.service';
import { APP_NAV_ITEMS } from '../config/app-navigation.config';

@Component({
  selector: 'app-layout',
  imports: [RouterModule, ButtonModule],
  templateUrl: './app-layout.html',
  styleUrl: './app-layout.scss',
})
export class AppLayout {
  collapsed = false;
  mobileMenuVisible = false;
  isMobile = typeof window !== 'undefined' ? window.innerWidth < 768 : false;

  private authService = inject(AuthService);

  menuItems = computed(() => {
    return APP_NAV_ITEMS.filter(item => {
      if (!item.showInNavbar) return false;
      if (!item.roles || item.roles.length === 0) return true;
      return this.authService.hasAnyRole(item.roles);
    });
  });

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
}
