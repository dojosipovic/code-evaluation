import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { IUserResponse } from '../models/user/IUserResponse';
import { UserQueryParamEnum } from '../models/enum/UserQueryParamEnum';
import { IUserQueryParams } from '../models/user/IUserQueryParams';
import { IPagedResponse } from '../models/IPagedResponse';
import { SortDirection } from '../config/app-types';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private http = inject(HttpClient);
  private readonly baseUrl = '/api/users';

  getUserByEmail(email: string): Observable<IUserResponse> {
    const httpParams = new HttpParams().set(UserQueryParamEnum.EMAIL, email);
    return this.http.get<IUserResponse>(`${this.baseUrl}/search-email`, { params: httpParams });
  }

  getUserByUsername(username: string): Observable<IUserResponse> {
    const httpParams = new HttpParams().set(UserQueryParamEnum.USERNAME, username);
    return this.http.get<IUserResponse>(`${this.baseUrl}/search-username`, { params: httpParams });
  }

  enableUser(id: number): Observable<null> {
    return this.http.patch<null>(`${this.baseUrl}/${id}/enabled`, true);
  }

  disableUser(id: number): Observable<null> {
    return this.http.patch<null>(`${this.baseUrl}/${id}/enabled`, false);
  }

  getUsers(params: IUserQueryParams): Observable<IPagedResponse<IUserResponse>> {
    const defaultSortDirection: SortDirection = 'desc';
    let httpParams = new HttpParams()
        .set(UserQueryParamEnum.PAGE, params.page)
        .set(UserQueryParamEnum.SIZE, params.size)
        .set(UserQueryParamEnum.SORT_DIRECTION, params.sortDirection ?? defaultSortDirection);

    if (params.sortBy) httpParams = httpParams.set(UserQueryParamEnum.SORT_BY, params.sortBy);
    if (params.role) httpParams = httpParams.set(UserQueryParamEnum.ROLE, params.role);
    if (params.search) httpParams = httpParams.set(UserQueryParamEnum.SEARCH, params.search);
    if (params.enabled !== null && params.enabled !== undefined)
      httpParams = httpParams.set(UserQueryParamEnum.ENABLED, params.enabled);

    return this.http.get<IPagedResponse<IUserResponse>>(this.baseUrl, { params: httpParams });
  }
}
