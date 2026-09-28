/**
 * MODÈLES DE DONNÉES — l'écho exact des DTO Java du backend
 * (com.enterprise.app.auth.dto.*). Les deux mondes se parlent en JSON :
 * ces interfaces TypeScript décrivent ce JSON côté frontend.
 */

/** Réponse de POST /api/auth/login : le JWT + l'identité. */
export interface LoginResponse {
  token: string;
  type: string; // "Bearer"
  id: number;
  email: string;
  fullName: string;
  roles: string[];
}

/** Utilisateur public (jamais de mot de passe dedans !). */
export interface UserResponse {
  id: number;
  email: string;
  fullName: string;
  enabled: boolean;
  roles: string[];
  createdAt: string; // ISO 8601, ex. "2026-09-28T14:30:00Z"
}

/** Page renvoyée par GET /api/users (Page<UserResponse> côté Spring). */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // page courante (0 = première)
  size: number;
}

/** Corps de POST /api/auth/register. */
export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}

/** Corps de POST /api/auth/login. */
export interface LoginRequest {
  email: string;
  password: string;
}

/** Corps de PATCH /api/users/{id}/roles. */
export interface UpdateRolesRequest {
  roles: string[];
}

/** Corps de PATCH /api/users/{id}/enabled. */
export interface UpdateEnabledRequest {
  enabled: boolean;
}

/** Erreur RFC 7807 renvoyée par le backend (ProblemDetail). */
export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}

/** Les rôles possibles, miroir de l'enum Java RoleName. */
export const ROLE_NAMES = ['ADMIN', 'MANAGER', 'USER'] as const;
