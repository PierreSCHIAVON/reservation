package config;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
public class KeycloakRolesExtractor {
    private KeycloakRolesExtractor() {}

    @SuppressWarnings("unchecked")
    public static Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return authorities;

        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof Collection<?> roles)) return authorities;

        for (Object roleObj : roles) {
            if (roleObj instanceof String role && !role.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
        }

        return authorities;
    }
}
