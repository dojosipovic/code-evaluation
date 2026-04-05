import { AsyncValidatorFn, AbstractControl, ValidationErrors } from '@angular/forms';
import { catchError, map, of, timer, switchMap } from 'rxjs';
import { UserService } from '../services/user.service';

export function usernameAvailableValidator(userService: UserService): AsyncValidatorFn {
  return (control: AbstractControl) => {
    const rawValue = control.value;
    const username = typeof rawValue === 'string' ? rawValue.trim() : '';

    if (!username) {
      return of(null);
    }

    if (username.length < 3) {
      return of(null);
    }

    return timer(1500).pipe(
      switchMap(() => userService.getUserByUsername(username)),
      map((): ValidationErrors => ({ usernameTaken: true })),
      catchError((error) => {
        if (error.status === 404) {
          return of(null);
        }

        return of({ usernameCheckFailed: true });
      })
    );
  };
}