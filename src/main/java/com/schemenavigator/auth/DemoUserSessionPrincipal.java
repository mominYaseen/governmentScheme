package com.schemenavigator.auth;

import java.io.Serializable;

/**
 * Serializable session principal for demo login — do not put JPA {@link com.schemenavigator.model.AppUser}
 * in the HTTP session (serialization / lazy-loading issues break the next request).
 */
public record DemoUserSessionPrincipal(long id, String email, String displayName, String provider)
        implements Serializable {

    private static final long serialVersionUID = 1L;
}
