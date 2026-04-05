package com.codeevaluation.core.api.dto.invite;

import com.codeevaluation.core.enumeration.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteValidateDto {
    private boolean valid;
    private String reason;
    private String email;
    private Role role;

    public static InviteValidateDto valid(String email, Role role) {
        InviteValidateDto r = new InviteValidateDto();
        r.setValid(true);
        r.setEmail(email);
        r.setRole(role);
        return r;
    }

    public static InviteValidateDto invalid(String reason) {
        InviteValidateDto r = new InviteValidateDto();
        r.setValid(false);
        r.setReason(reason);
        return r;
    }
}
