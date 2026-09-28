import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { PageResponse, UpdateEnabledRequest, UpdateRolesRequest, UserResponse } from './models';

/**
 * CLIENT HTTP d'ADMINISTRATION des utilisateurs (/api/users, ADMIN).
 * Construit les requêtes paginées et les PATCH partiels attendus par
 * UserController côté Java.
 */
@Injectable({ providedIn: 'root' })
export class UsersApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/users`;

  /** GET /api/users?page=0&size=20 → une page d'utilisateurs. */
  list(page = 0, size = 20): Observable<PageResponse<UserResponse>> {
    // HttpParams encode proprement les paramètres d'URL.
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<UserResponse>>(this.baseUrl, { params });
  }

  /** GET /api/users/{id} → détail d'un compte. */
  get(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/${id}`);
  }

  /** PATCH /api/users/{id}/roles → remplace les rôles. */
  updateRoles(id: number, body: UpdateRolesRequest): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.baseUrl}/${id}/roles`, body);
  }

  /** PATCH /api/users/{id}/enabled → active/désactive le compte. */
  updateEnabled(id: number, body: UpdateEnabledRequest): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.baseUrl}/${id}/enabled`, body);
  }

  /** DELETE /api/users/{id} → supprime le compte (204 No Content). */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
