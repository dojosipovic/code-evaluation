package com.codeevaluation.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeevaluation.core.api.dto.user.UserDto;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.repository.UserRepository;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    @Test
    void shouldReturnUserDto_whenEmailExists() {
        // given
        String email = "test@example.com";

        User user = new User();
        user.setEmail(email);
        user.setUsername("testuser");

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        // when
        UserDto result = userService.findByEmail(email);

        // then
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void shouldThrowBadRequest_whenEmailIsBlank() {
        // expect
        assertThrows(BadRequestException.class,
                () -> userService.findByEmail(""));

        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldThrowNotFound_whenUserDoesNotExist() {
        // given
        String email = "missing@example.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        // expect
        assertThrows(NotFoundException.class,
                () -> userService.findByEmail(email));

        verify(userRepository).findByEmail(email);
    }
}
