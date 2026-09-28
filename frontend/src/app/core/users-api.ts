import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { PageResponse, UpdateEnabledRequest, UpdateRolesRequest, UserResponse } from './models';

/**
 * Client HTTP d'administration des utilisateurs (réservé ADMIN côté Java).
 *
 * PATCH = "modifier UNE partie seulement" (rôles OU enabled),
 * au lieu de PUT qui remplacerait tout l'objet.
 */
@Injectable({ providedIn: 'root' })
export class UsersApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/users`;

  /** GET /api/users?page=0&size=20 → une page (pas toute la base). */
  list(page = 0, size = 20): Observable<PageResponse<UserResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<UserResponse>>(this.baseUrl, { params });
  }

  get(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/${id}`);
  }

  updateRoles(id: number, body: UpdateRolesRequest): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.baseUrl}/${id}/roles`, body);
  }

  updateEnabled(id: number, body: UpdateEnabledRequest): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.baseUrl}/${id}/enabled`, body);
  }

  /** 204 No Content : succès sans JSON. */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
