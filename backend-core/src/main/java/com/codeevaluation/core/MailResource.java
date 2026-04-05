package com.codeevaluation.core;

import com.codeevaluation.core.service.MailService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import lombok.RequiredArgsConstructor;

@Path("/api/test-mail")
@RequiredArgsConstructor
public class MailResource {

    private final MailService mailService;

    @GET
    public String send(@QueryParam("to") String to) {
        mailService.sendTestMail(to);
        return "Mail poslan na " + to;
    }
}
