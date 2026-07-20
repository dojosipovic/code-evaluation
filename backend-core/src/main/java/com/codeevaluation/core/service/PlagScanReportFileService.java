package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.plagscan.PlagScanReportFileDownload;
import com.codeevaluation.core.helper.AssignmentAccessPolicy;
import com.codeevaluation.core.model.Assignment;
import com.codeevaluation.core.model.SubmissionPlagiarismRun;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.provider.CurrentUserProvider;
import com.codeevaluation.core.repository.AssignmentRepository;
import com.codeevaluation.core.repository.SubmissionPlagiarismRunRepository;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
@RequiredArgsConstructor
public class PlagScanReportFileService {

    private final SubmissionPlagiarismRunRepository submissionPlagiarismRunRepository;
    private final AssignmentRepository assignmentRepository;
    private final CurrentUserProvider currentUserProvider;
    private final JWTParser jwtParser;

    public boolean reportExists(Long assignmentId) {
        Assignment assignment = assignmentRepository.findByIdWithTaskAndTests(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));
        User currentUser = currentUserProvider.getCurrentUser();

        if (!AssignmentAccessPolicy.canIssuePlagScanToken(assignment, currentUser)) {
            throw new ForbiddenException("You cannot check the PlagScan report for this assignment");
        }

        return submissionPlagiarismRunRepository.findLatestByAssignmentId(assignmentId)
                .map(SubmissionPlagiarismRun::getReportFileBase64)
                .filter(StringUtils::isNotBlank)
                .isPresent();
    }

    public PlagScanReportFileDownload getReportFile(String token) {
        JsonWebToken jsonWebToken = parseToken(token);
        if (!jsonWebToken.getGroups().contains("PLAGSCAN")) {
            throw new ForbiddenException("Invalid token role");
        }

        Long assignmentId = readAssignmentIdClaim(jsonWebToken);
        SubmissionPlagiarismRun plagiarismRun = submissionPlagiarismRunRepository
                .findLatestByAssignmentId(assignmentId)
                .orElseThrow(() -> new NotFoundException("Plagiarism report file not found"));

        String normalizedBase64 = plagiarismRun.getReportFileBase64().replaceAll("\\s+", "");
        byte[] reportBytes = Base64.getDecoder().decode(normalizedBase64);

        return new PlagScanReportFileDownload(
                reportBytes,
                "plagiarism-report-%s.jplag".formatted(assignmentId)
        );
    }

    private JsonWebToken parseToken(String token) {
        if (token == null || token.isBlank()) {
            throw new NotAuthorizedException("Missing token", "Bearer");
        }

        try {
            return jwtParser.parse(token);
        } catch (ParseException e) {
            throw new NotAuthorizedException("Invalid token", "Bearer");
        }
    }

    private Long readAssignmentIdClaim(JsonWebToken jsonWebToken) {
        Object assignmentIdClaim = jsonWebToken.getClaim("assignmentId");
        if (assignmentIdClaim instanceof JsonNumber number) {
            return number.longValue();
        }
        if (assignmentIdClaim instanceof JsonString value) {
            return parseAssignmentId(value.getString());
        }
        if (assignmentIdClaim instanceof Number number) {
            return number.longValue();
        }
        if (assignmentIdClaim instanceof String value) {
            return parseAssignmentId(value);
        }

        throw new ForbiddenException("Missing assignment scope");
    }

    private Long parseAssignmentId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ForbiddenException("Invalid assignment scope");
        }
    }
}
