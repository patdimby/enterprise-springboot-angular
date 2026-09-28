/**
 * Modèles = la forme du JSON échangé avec Java.
 *
 * TypeScript n'existe que pendant l'écriture du code : au runtime, c'est du JS.
 * Ces "interface" servent à l'éditeur (auto-complétion) et à éviter les fautes
 * (user.emial serait souligné en rouge).
 *
 * Elles doivent coller aux DTO Java (LoginResponse, UserResponse, etc.).
 */

/** Réponse de POST /api/auth/login. */
export interface LoginResponse {
  token: string;
  type: string;
  id: number;
  email: string;
  fullName: string;
  roles: string[];
}

/** Compte public : JAMAIS de mot de passe ici. */
export interface UserResponse {
  id: number;
  email: string;
  fullName: string;
  enabled: boolean;
  roles: string[];
  createdAt: string;
}

/**
 * Page Spring Data : content = les lignes de CETTE page,
 * totalElements = combien il y en a en tout (pour le paginator).
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UpdateRolesRequest {
  roles: string[];
}

export interface UpdateEnabledRequest {
  enabled: boolean;
}

/** Erreur standard RFC 7807 renvoyée par GlobalExceptionHandler (Java). */
export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}

/** Miroir de l'enum Java RoleName. */
export const ROLE_NAMES = ['ADMIN', 'MANAGER', 'USER'] as const;
