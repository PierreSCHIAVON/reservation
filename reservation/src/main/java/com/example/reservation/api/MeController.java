package com.example.reservation.api;

import com.example.reservation.dto.generated.UserInfo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class MeController {

    @GetMapping("/me")
    public UserInfo me(@AuthenticationPrincipal Jwt jwt) {
        UserInfo info = new UserInfo();
        info.setSub(jwt.getSubject());
        info.setUsername(jwt.getClaimAsString("preferred_username"));
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        info.setRealmAccess(realmAccess != null ? realmAccess : Map.of());
        info.setIssuer(jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
        return info;
    }
}
