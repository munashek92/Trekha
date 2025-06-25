package com.app.trekha.config.security;

import com.app.trekha.user.model.ERole;
import com.app.trekha.user.model.Role;
import com.app.trekha.user.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.HashSet;
import java.util.Set;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {
    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser customUser) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        User principal = new User();
        principal.setId(customUser.id());
        principal.setEmail(customUser.username());

        Set<Role> roles = new HashSet<>();
        for (String role : customUser.roles()) {
            roles.add(new Role(ERole.valueOf("ROLE_" + role)));
        }
        principal.setRoles(roles);

        Authentication auth = new UsernamePasswordAuthenticationToken(principal, "password", principal.getAuthorities());
        context.setAuthentication(auth);
        return context;
    }
}