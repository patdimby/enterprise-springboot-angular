package com.enterprise.app.user;

/**
 * Les rôles possibles dans l'application — c'est une "enum" :
 * une liste fermée de valeurs, impossible d'en inventer d'autres
 * (la compilation échoue si on écrit {@code RoleName.SUPERGIRL}).
 *
 * <ul>
 *   <li><b>ADMIN</b> : tout faire, y compris gérer les comptes (/api/users) ;</li>
 *   <li><b>MANAGER</b> : (phase 2) gérer les projets de son équipe ;</li>
 *   <li><b>USER</b> : rôle de base attribué à toute inscription.</li>
 * </ul>
 */
public enum RoleName {
    ADMIN,
    MANAGER,
    USER
}
