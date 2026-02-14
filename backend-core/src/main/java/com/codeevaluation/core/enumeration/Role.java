package com.codeevaluation.core.enumeration;

public enum Role {
    ADMIN, STUDENT, PROF;

    @Override
    public String toString() {
        return name().toUpperCase();
    }
}
