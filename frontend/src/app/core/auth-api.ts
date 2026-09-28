import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { LoginRequest, LoginResponse, RegisterRequest, UserResponse } from './models';

/**
 * CLIENT HTTP de l'AUTHENTIFICATION — le seul endroit qui connaît les URLs
 * de /api/auth/*. Les composants appellent ce service ; si le backend
 * change ses routes, on ne modifie QUE ce fichier.
 */
@Injectable({ providedIn: 'root' })
export class AuthApi {
  /** HttpClient est fourni par provideHttpClient() dans app.config.ts. */
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  /** POST /api/auth/register → le compte créé. */
  register(body: RegisterRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.baseUrl}/register`, body);
  }

  /** POST /api/auth/login → token JWT + identité. */
  login(body: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, body);
  }

  /** GET /api/auth/me → profil de l'utilisateur du token. */
  me(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/me`);
  }
}
