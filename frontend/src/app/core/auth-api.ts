import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { LoginRequest, LoginResponse, RegisterRequest, UserResponse } from './models';

/**
 * Client HTTP de l'authentification.
 *
 * Règle d'or : SEUL ce fichier connaît les URLs /api/auth/*.
 * Les pages (Login, Register) appellent this.authApi.login(...) ;
 * si le backend change une route, on ne touche qu'ici.
 *
 * Observable = "promesse qui peut émettre plusieurs valeurs".
 * Ici il n'en émet qu'une (la réponse HTTP), puis se termine.
 * On s'abonne avec .subscribe({ next, error }) dans le composant.
 */
@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  /** POST /api/auth/register → le compte créé (sans mot de passe). */
  register(body: RegisterRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.baseUrl}/register`, body);
  }

  /** POST /api/auth/login → JWT + identité. */
  login(body: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, body);
  }

  /** GET /api/auth/me → "qui suis-je ?" selon le token. */
  me(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/me`);
  }
}
