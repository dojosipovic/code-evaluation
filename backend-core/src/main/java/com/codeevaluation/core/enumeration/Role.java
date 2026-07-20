package com.codeevaluation.core.enumeration;

public enum Role {
    ADMIN, STUDENT, PROF, PLAGSCAN;

    @Override
    public String toString() {
        return name().toUpperCase();
    }
}
