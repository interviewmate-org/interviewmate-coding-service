package com.interviewmate.codingservice.securityconfig;

import java.util.Set;

public final class SecurityConstants {
    private SecurityConstants() {}

    public static final String HEADER_ROLE    = "X-Role";
    public static final String HEADER_USER_ID = "X-User-Id";      // optional, if gateway sends it

    public static final Set<String> VALID_ROLES = Set.of("ADMIN", "USER");
}
