import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideAnimations } from '@angular/platform-browser/animations';
import { providePrimeNG } from 'primeng/config';
import Lara from '@primeuix/themes/lara';
import { MessageService } from 'primeng/api';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { bearerInterceptor } from './interceptor/bearer.interceptor';
import { credentialsInterceptor } from './interceptor/credentials.interceptor';
import { refreshInterceptor } from './interceptor/refresh.interceptor';
import { NGX_MONACO_EDITOR_CONFIG } from 'ngx-monaco-editor-v2';
import { ConfigService } from './services/config.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideAnimations(),
    provideHttpClient(withInterceptors([
      credentialsInterceptor,
      bearerInterceptor,
      refreshInterceptor
    ])),
    provideAppInitializer(() => {
      const configService = inject(ConfigService);
      return configService.load();
    }),
    providePrimeNG({
      ripple: true,
      theme: {
        preset: Lara,
        options: {
          darkModeSelector: '.app-dark'
        }
      }
    }),
    MessageService,
    {
      provide: NGX_MONACO_EDITOR_CONFIG,
      useValue: {
        baseUrl: 'assets/monaco',
        defaultOptions: {
          scrollBeyondLastLine: false,
          minimap: {
            enabled: false
          }
        }
      }
    }
  ]
};
