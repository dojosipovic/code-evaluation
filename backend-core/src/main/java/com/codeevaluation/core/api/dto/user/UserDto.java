package com.codeevaluation.core.api.dto.user;

import com.codeevaluation.core.enumeration.Role;
import com.codeevaluation.core.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private Role role;
    private Boolean enabled;

    public static UserDto from(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        userDto.setRole(user.getRole());
        userDto.setEnabled(user.getEnabled());

        return userDto;
    }
}
