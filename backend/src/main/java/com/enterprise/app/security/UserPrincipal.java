package com.enterprise.app.security;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

/**
 * Principal applicatif installé dans le SecurityContext après validation du JWT.
 */
@Value
@Builder
public class UserPrincipal implements Serializable {

    Long id;
    String email;
    String fullName;
}
