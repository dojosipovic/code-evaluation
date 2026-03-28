import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { IUserResponse } from '../models/user/IUserResponse';
import { UserQueryParamEnum } from '../models/enum/UserQueryParamEnum';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private http = inject(HttpClient);
  private readonly baseUrl = '/api/users';

  getUserByEmail(email: string): Observable<IUserResponse> {
    const httpParams = new HttpParams().set(UserQueryParamEnum.EMAIL, email);
    return this.http.get<IUserResponse>(this.baseUrl, { params: httpParams });
  }
}
