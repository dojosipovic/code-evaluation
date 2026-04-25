package com.codeevaluation.core.api.dto.user;

import com.codeevaluation.core.enumeration.Role;
import com.codeevaluation.core.model.User;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String firstname;
    private String lastname;
    private String email;
    private Role role;
    private Boolean enabled;

    public static UserDto from(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setFirstname(user.getFirstname());
        userDto.setLastname(user.getLastname());
        userDto.setEmail(user.getEmail());
        userDto.setRole(user.getRole());
        userDto.setEnabled(user.getEnabled());

        return userDto;
    }

    public static List<UserDto> from(List<User> users) {
        return users.stream().map(UserDto::from).toList();
    }
}
