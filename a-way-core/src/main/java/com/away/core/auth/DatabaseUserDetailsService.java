package com.away.core.auth;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public DatabaseUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        AppUser user = users.findWithMembershipsByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (user.isMaster()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_MASTER"));
        }
        user.getMemberships().forEach(membership ->
                authorities.add(new SimpleGrantedAuthority("ROLE_" + membership.getRole())));
        boolean activo = "ACTIVO".equals(user.getEstado());
        return new User(user.getUsername(), user.getPasswordHash() == null ? "" : user.getPasswordHash(),
                activo, true, true, true, authorities);
    }
}
