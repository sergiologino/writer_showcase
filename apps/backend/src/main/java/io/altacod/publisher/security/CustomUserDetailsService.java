package io.altacod.publisher.security;

import io.altacod.publisher.config.PublisherSecurityProperties;
import io.altacod.publisher.user.UserEntity;
import io.altacod.publisher.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PublisherSecurityProperties securityProperties;

    public CustomUserDetailsService(
            UserRepository userRepository,
            PublisherSecurityProperties securityProperties
    ) {
        this.userRepository = userRepository;
        this.securityProperties = securityProperties;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(username)
                .map(this::toPrincipal)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private SecurityUserPrincipal toPrincipal(UserEntity user) {
        boolean admin = user.isAdmin() || securityProperties.isListedAdmin(user.getEmail());
        return new SecurityUserPrincipal(user, admin);
    }
}
