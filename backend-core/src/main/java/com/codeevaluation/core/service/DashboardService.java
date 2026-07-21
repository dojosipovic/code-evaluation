package com.codeevaluation.core.service;

import com.codeevaluation.core.api.dto.dashboard.DashboardChartDto;
import com.codeevaluation.core.api.dto.dashboard.DashboardDto;
import com.codeevaluation.core.api.dto.dashboard.DashboardStatDto;
import com.codeevaluation.core.enumeration.InviteStatus;
import com.codeevaluation.core.enumeration.Role;
import com.codeevaluation.core.model.User;
import com.codeevaluation.core.provider.CurrentUserProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class DashboardService {

    private static final String DOUGHNUT = "doughnut";
    private static final String BAR = "bar";

    private final CurrentUserProvider currentUserProvider;
    private final EntityManager entityManager;

    public DashboardDto getDashboard() {
        User currentUser = currentUserProvider.getCurrentUser();

        return switch (currentUser.getRole()) {
            case STUDENT -> getStudentDashboard(currentUser);
            case PROF -> getProfessorDashboard(currentUser);
            case ADMIN -> getAdminDashboard(currentUser);
            default -> new DashboardDto(currentUser.getRole(), List.of(), List.of());
        };
    }

    private DashboardDto getStudentDashboard(User user) {
        Instant now = Instant.now();
        long totalAssignments = countLong(
                """
                select count(distinct a.id)
                from Assignment a
                join a.group g
                where g.owner.id = :userId
                or exists (
                    select 1 from GroupMember gm
                    where gm.group = g
                    and gm.user.id = :userId
                )
                """,
                "userId",
                user.getId()
        );
        long activeAssignments = countLong(
                """
                select count(distinct a.id)
                from Assignment a
                join a.group g
                where a.startsAt < :now
                and a.endsAt > :now
                and not exists (
                    select 1 from Submission s
                    where s.assignment = a
                    and s.user.id = :userId
                )
                and (
                    g.owner.id = :userId
                    or exists (
                        select 1 from GroupMember gm
                        where gm.group = g
                        and gm.user.id = :userId
                    )
                )
                """,
                List.of("userId", "now"),
                List.of(user.getId(), now)
        );
        long submittedAssignments = countLong(
                "select count(s.id) from Submission s where s.user.id = :userId",
                "userId",
                user.getId()
        );
        long gradedAssignments = countLong(
                """
                select count(s.id)
                from Submission s
                where s.user.id = :userId
                and s.finalScore is not null
                """,
                "userId",
                user.getId()
        );
        long unsubmittedAssignments = Math.max(totalAssignments - submittedAssignments, 0);
        Number averageScore = averageScorePercentage(
                """
                select s.finalScore, s.assignment.points
                from Submission s
                where s.user.id = :userId
                and s.finalScore is not null
                and s.assignment.points > 0
                """,
                "userId",
                user.getId()
        );
        long passedTests = sumLong(
                """
                select coalesce(sum(run.passedTests), 0)
                from SubmissionTestRun run
                join run.submission s
                where s.user.id = :userId
                """,
                "userId",
                user.getId()
        );
        long totalTests = sumLong(
                """
                select coalesce(sum(run.totalTests), 0)
                from SubmissionTestRun run
                join run.submission s
                where s.user.id = :userId
                """,
                "userId",
                user.getId()
        );
        long failedTests = Math.max(totalTests - passedTests, 0);

        List<DashboardStatDto> stats = List.of(
                stat("activeAssignments", "Aktivni assignmenti", activeAssignments),
                percentStat("averageScore", "Prosjecni score", averageScore),
                percentStat("completionRate", "Rijesenost", submittedAssignments, totalAssignments),
                percentStat("testPassRate", "Pass rate testova", passedTests, totalTests)
        );

        List<DashboardChartDto> charts = List.of(
                chart(
                        "assignmentStatus",
                        "Status assignmenta",
                        DOUGHNUT,
                        List.of("Nije predano", "Predano", "Ocijenjeno"),
                        List.of(
                                unsubmittedAssignments,
                                Math.max(submittedAssignments - gradedAssignments, 0),
                                gradedAssignments
                        )
                )
        );

        return new DashboardDto(user.getRole(), stats, charts);
    }

    private DashboardDto getProfessorDashboard(User user) {
        Instant now = Instant.now();
        long groups = countLong(
                "select count(g.id) from Group g where g.owner.id = :userId",
                "userId",
                user.getId()
        );
        long students = countLong(
                """
                select count(distinct gm.user.id)
                from GroupMember gm
                where gm.group.owner.id = :userId
                """,
                "userId",
                user.getId()
        );
        long activeAssignments = professorAssignmentCount(
                user.getId(),
                "a.startsAt < :now and a.endsAt > :now",
                now
        );
        long finishedAssignments = professorAssignmentCount(
                user.getId(),
                "a.endsAt <= :now",
                now
        );
        long futureAssignments = professorAssignmentCount(
                user.getId(),
                "a.startsAt >= :now",
                now
        );
        long totalSubmissions = professorSubmissionCount(user.getId(), null);
        long ungradedAssignments = professorUngradedAssignmentCount(user.getId(), now);
        long ungradedSubmissions = professorSubmissionCount(user.getId(), "s.finalScore is null");
        long gradedSubmissions = professorSubmissionCount(user.getId(), "s.finalScore is not null");
        Number averageScore = averageScorePercentage(
                """
                select s.finalScore, s.assignment.points
                from Submission s
                where s.assignment.group.owner.id = :userId
                and s.finalScore is not null
                and s.assignment.points > 0
                """,
                "userId",
                user.getId()
        );

        List<DashboardStatDto> stats = List.of(
                stat("groups", "Moje grupe", groups),
                stat("students", "Studenti u grupama", students),
                stat("activeAssignments", "Aktivni assignmenti", activeAssignments),
                stat("finishedAssignments", "Zavrseni assignmenti", finishedAssignments),
                stat("ungradedAssignments", "Neocijenjeni assignmenti", ungradedAssignments),
                stat("gradedSubmissions", "Ocijenjene predaje", gradedSubmissions),
                stat("totalSubmissions", "Ukupno predaja", totalSubmissions),
                percentStat("averageScore", "Prosjecni score", averageScore)
        );

        List<DashboardChartDto> charts = new ArrayList<>();
        charts.add(chart(
                "assignmentStatus",
                "Status assignmenta",
                DOUGHNUT,
                List.of("Aktivni", "Zavrseni", "Buduci"),
                List.of(activeAssignments, finishedAssignments, futureAssignments)
        ));
        charts.add(chart(
                "submissionEvaluation",
                "Status predaja",
                DOUGHNUT,
                List.of("Ocijenjene", "Neocijenjene"),
                List.of(gradedSubmissions, ungradedSubmissions)
        ));
        charts.add(scoreDistributionChart(
                """
                select s.finalScore, s.assignment.points
                from Submission s
                where s.assignment.group.owner.id = :userId
                and s.finalScore is not null
                and s.assignment.points > 0
                """,
                "userId",
                user.getId()
        ));

        return new DashboardDto(user.getRole(), stats, charts);
    }

    private DashboardDto getAdminDashboard(User user) {
        Instant now = Instant.now();
        long students = countLong(
                "select count(u.id) from User u where u.role = :role",
                "role",
                Role.STUDENT
        );
        long professors = countLong(
                "select count(u.id) from User u where u.role = :role",
                "role",
                Role.PROF
        );
        long admins = countLong(
                "select count(u.id) from User u where u.role = :role",
                "role",
                Role.ADMIN
        );
        long enabledUsers = countLong("select count(u.id) from User u where u.enabled = true");
        long disabledUsers = countLong("select count(u.id) from User u where u.enabled = false");
        long groups = countLong("select count(g.id) from Group g");
        long pendingInvites = countLong(
                """
                select count(i.id)
                from Invite i
                where i.status = :status
                and i.expiresAt > :now
                """,
                List.of("status", "now"),
                List.of(InviteStatus.PENDING, now)
        );
        long acceptedInvites = countLong(
                "select count(i.id) from Invite i where i.status = :status",
                "status",
                InviteStatus.ACCEPTED
        );
        long expiredInvites = countLong(
                """
                select count(i.id)
                from Invite i
                where i.status = :expiredStatus
                or (i.status = :pendingStatus and i.expiresAt <= :now)
                """,
                List.of("expiredStatus", "pendingStatus", "now"),
                List.of(InviteStatus.EXPIRED, InviteStatus.PENDING, now)
        );
        long activeAssignments = adminAssignmentCount(
                "a.startsAt < :now and a.endsAt > :now",
                now
        );
        long finishedAssignments = adminAssignmentCount("a.endsAt <= :now", now);
        long futureAssignments = adminAssignmentCount("a.startsAt >= :now", now);

        List<DashboardStatDto> stats = new ArrayList<>(List.of(
                stat("groups", "Grupe", groups)
        ));
        stats.addAll(getProfessorStats(user));

        List<DashboardChartDto> charts = new ArrayList<>(List.of(
                chart(
                        "usersByRole",
                        "Korisnici po roli",
                        DOUGHNUT,
                        List.of("Studenti", "Profesori", "Admini"),
                        List.of(students, professors, admins)
                ),
                chart(
                        "userStatus",
                        "Status korisnika",
                        DOUGHNUT,
                        List.of("Enabled", "Disabled"),
                        List.of(enabledUsers, disabledUsers)
                ),
                chart(
                        "inviteStatus",
                        "Invite statusi",
                        DOUGHNUT,
                        List.of("Pending", "Accepted", "Expired"),
                        List.of(pendingInvites, acceptedInvites, expiredInvites)
                ),
                chart(
                        "assignmentStatus",
                        "Status assignmenta",
                        DOUGHNUT,
                        List.of("Aktivni", "Zavrseni", "Buduci"),
                        List.of(activeAssignments, finishedAssignments, futureAssignments)
                )
        ));
        charts.addAll(getProfessorCharts(user));

        return new DashboardDto(user.getRole(), stats, charts);
    }

    private List<DashboardStatDto> getProfessorStats(User user) {
        Instant now = Instant.now();
        long groups = countLong(
                "select count(g.id) from Group g where g.owner.id = :userId",
                "userId",
                user.getId()
        );
        long students = countLong(
                """
                select count(distinct gm.user.id)
                from GroupMember gm
                where gm.group.owner.id = :userId
                """,
                "userId",
                user.getId()
        );
        long activeAssignments = professorAssignmentCount(
                user.getId(),
                "a.startsAt < :now and a.endsAt > :now",
                now
        );
        long finishedAssignments = professorAssignmentCount(
                user.getId(),
                "a.endsAt <= :now",
                now
        );
        long ungradedAssignments = ungradedAssignmentCount(user, now);
        Number averageScore = averageScorePercentage(
                """
                select s.finalScore, s.assignment.points
                from Submission s
                where s.assignment.group.owner.id = :userId
                and s.finalScore is not null
                and s.assignment.points > 0
                """,
                "userId",
                user.getId()
        );

        return List.of(
                stat("profGroups", "Moje grupe", groups),
                stat("profStudents", "Studenti u mojim grupama", students),
                stat("profActiveAssignments", "Moji aktivni assignmenti", activeAssignments),
                stat("profFinishedAssignments", "Moji zavrseni assignmenti", finishedAssignments),
                stat(
                        "profUngradedAssignments",
                        "Neocijenjeni assignmenti",
                        ungradedAssignments
                ),
                percentStat("profAverageScore", "Moj prosjecni score", averageScore)
        );
    }

    private List<DashboardChartDto> getProfessorCharts(User user) {
        Instant now = Instant.now();
        long activeAssignments = professorAssignmentCount(
                user.getId(),
                "a.startsAt < :now and a.endsAt > :now",
                now
        );
        long finishedAssignments = professorAssignmentCount(
                user.getId(),
                "a.endsAt <= :now",
                now
        );
        long futureAssignments = professorAssignmentCount(
                user.getId(),
                "a.startsAt >= :now",
                now
        );
        long ungradedSubmissions = professorSubmissionCount(user.getId(), "s.finalScore is null");
        long gradedSubmissions = professorSubmissionCount(user.getId(), "s.finalScore is not null");

        return List.of(
                chart(
                        "profAssignmentStatus",
                        "Moji statusi assignmenta",
                        DOUGHNUT,
                        List.of("Aktivni", "Zavrseni", "Buduci"),
                        List.of(activeAssignments, finishedAssignments, futureAssignments)
                ),
                chart(
                        "profSubmissionEvaluation",
                        "Moji statusi predaja",
                        DOUGHNUT,
                        List.of("Ocijenjene", "Neocijenjene"),
                        List.of(gradedSubmissions, ungradedSubmissions)
                ),
                scoreDistributionChart(
                        """
                        select s.finalScore, s.assignment.points
                        from Submission s
                        where s.assignment.group.owner.id = :userId
                        and s.finalScore is not null
                        and s.assignment.points > 0
                        """,
                        "userId",
                        user.getId()
                )
        );
    }

    private long professorAssignmentCount(Long userId, String condition, Instant now) {
        return countLong(
                """
                select count(a.id)
                from Assignment a
                where a.group.owner.id = :userId
                and
                """ + condition,
                List.of("userId", "now"),
                List.of(userId, now)
        );
    }

    private long adminAssignmentCount(String condition, Instant now) {
        return countLong(
                "select count(a.id) from Assignment a where " + condition,
                "now",
                now
        );
    }

    private long professorSubmissionCount(Long userId, String condition) {
        String query =
                """
                select count(s.id)
                from Submission s
                where s.assignment.group.owner.id = :userId
                """;
        if (condition != null) {
            query += " and " + condition;
        }
        return countLong(query, "userId", userId);
    }

    private long professorUngradedAssignmentCount(Long userId, Instant now) {
        return countLong(
                """
                select count(distinct a.id)
                from Assignment a
                where a.group.owner.id = :userId
                and (a.startsAt >= :now or a.endsAt <= :now)
                and exists (
                    select 1
                    from Submission s
                    where s.assignment = a
                    and s.finalScore is null
                )
                """,
                List.of("userId", "now"),
                List.of(userId, now)
        );
    }

    private long ungradedAssignmentCount(User user, Instant now) {
        if (user.isAdmin()) {
            return countLong(
                    """
                    select count(distinct a.id)
                    from Assignment a
                    where (a.startsAt >= :now or a.endsAt <= :now)
                    and exists (
                        select 1
                        from Submission s
                        where s.assignment = a
                        and s.finalScore is null
                    )
                    """,
                    "now",
                    now
            );
        }

        return professorUngradedAssignmentCount(user.getId(), now);
    }

    private DashboardChartDto scoreDistributionChart(String query, String parameter, Object value) {
        List<Object[]> rows = entityManager.createQuery(query, Object[].class)
                .setParameter(parameter, value)
                .getResultList();
        long[] buckets = new long[5];

        for (Object[] row : rows) {
            BigDecimal score = toBigDecimal(row[0]);
            BigDecimal points = toBigDecimal(row[1]);
            if (score == null || points == null || points.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            int percentage = score
                    .multiply(BigDecimal.valueOf(100))
                    .divide(points, 0, RoundingMode.HALF_UP)
                    .intValue();
            int bucket = Math.min(Math.max(percentage / 20, 0), 4);
            if (percentage == 100) {
                bucket = 4;
            }
            buckets[bucket]++;
        }

        return chart(
                "scoreDistribution",
                "Distribucija bodova",
                BAR,
                List.of("0-20%", "20-40%", "40-60%", "60-80%", "80-100%"),
                List.of(buckets[0], buckets[1], buckets[2], buckets[3], buckets[4])
        );
    }

    private DashboardStatDto stat(String key, String label, Number value) {
        return new DashboardStatDto(key, label, value == null ? 0 : round(value), null);
    }

    private DashboardStatDto percentStat(String key, String label, long part, long total) {
        long value = total == 0 ? 0 : Math.round((part * 100.0) / total);
        return new DashboardStatDto(key, label, value, "%");
    }

    private DashboardStatDto percentStat(String key, String label, Number value) {
        return new DashboardStatDto(key, label, value == null ? 0 : round(value), "%");
    }

    private DashboardChartDto chart(
            String key,
            String title,
            String type,
            List<String> labels,
            List<Number> values
    ) {
        return new DashboardChartDto(key, title, type, labels, values);
    }

    private long countLong(String query) {
        return ((Number) entityManager.createQuery(query).getSingleResult()).longValue();
    }

    private long countLong(String query, String parameter, Object value) {
        return countLong(query, List.of(parameter), List.of(value));
    }

    private long countLong(String query, List<String> parameters, List<Object> values) {
        var typedQuery = entityManager.createQuery(query);
        for (int i = 0; i < parameters.size(); i++) {
            typedQuery.setParameter(parameters.get(i), values.get(i));
        }
        return ((Number) typedQuery.getSingleResult()).longValue();
    }

    private long sumLong(String query, String parameter, Object value) {
        Number result = (Number) entityManager.createQuery(query)
                .setParameter(parameter, value)
                .getSingleResult();
        return result == null ? 0 : result.longValue();
    }

    private Number averageNumber(String query, String parameter, Object value) {
        Number result = (Number) entityManager.createQuery(query)
                .setParameter(parameter, value)
                .getSingleResult();
        return result == null ? 0 : round(result);
    }

    private Number averageScorePercentage(String query, String parameter, Object value) {
        List<Object[]> rows = entityManager.createQuery(query, Object[].class)
                .setParameter(parameter, value)
                .getResultList();
        if (rows.isEmpty()) {
            return 0;
        }

        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (Object[] row : rows) {
            BigDecimal score = toBigDecimal(row[0]);
            BigDecimal points = toBigDecimal(row[1]);
            if (score == null || points == null || points.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            total = total.add(score
                    .multiply(BigDecimal.valueOf(100))
                    .divide(points, 4, RoundingMode.HALF_UP));
            count++;
        }

        if (count == 0) {
            return 0;
        }
        return total.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
    }

    private Number round(Number value) {
        if (value instanceof Long || value instanceof Integer) {
            return value;
        }
        return BigDecimal.valueOf(value.doubleValue()).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return null;
    }
}
