package com.codeevaluation.core.code;

import com.codeevaluation.core.enumeration.ProgrammingLanguage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.BadRequestException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class CodeRunnerFactory {

    private final Map<ProgrammingLanguage, CodeRunner> runners;

    public CodeRunnerFactory(Instance<CodeRunner> runnerInstances) {
        this.runners = runnerInstances
                .stream()
                .collect(
                        Collectors.toUnmodifiableMap(
                                CodeRunner::language,
                                Function.identity()
                        )
                );
    }

    public CodeRunner getRunner(ProgrammingLanguage language) {
        CodeRunner runner = runners.get(language);

        if (runner == null) {
            throw new BadRequestException("Unsupported programming language: " + language);
        }

        return runner;
    }
}
