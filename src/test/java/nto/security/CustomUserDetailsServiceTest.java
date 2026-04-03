package nto.security;

import nto.core.entities.UserEntity;
import nto.infrastructure.repositories.JpaUserRepository;
import nto.infrastructure.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private JpaUserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByUsernameShouldReturnUserDetails() {
        UserEntity user = UserEntity.builder().username("alice").password("enc").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("alice");

        assertEquals("alice", result.getUsername());
        assertEquals("enc", result.getPassword());
        assertEquals(0, result.getAuthorities().size());
    }

    @Test
    void loadUserByUsernameShouldThrowWhenUserMissing() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("alice"));
    }
}
