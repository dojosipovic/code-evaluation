import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
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
    providePrimeNG({
      ripple: true,
      theme: {
        preset: Lara
      }
    }),
    MessageService
  ]
};
