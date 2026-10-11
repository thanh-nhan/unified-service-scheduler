package com.keyloop.scheduler.common.persistence;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import java.util.Optional;

/** Identifies database constraint violations by SQLSTATE and constraint name, never by message text. */
public final class ConstraintViolations {

    private static final String EXCLUSION_VIOLATION = "23P01";
    private static final String UNIQUE_VIOLATION = "23505";

    private ConstraintViolations() {
    }

    public static Optional<String> constraintName(Throwable t) {
        return findPsql(t)
                .map(PSQLException::getServerErrorMessage)
                .map(ServerErrorMessage::getConstraint);
    }

    public static boolean isExclusionViolation(Throwable t) {
        return hasSqlState(t, EXCLUSION_VIOLATION);
    }

    public static boolean isUniqueViolation(Throwable t) {
        return hasSqlState(t, UNIQUE_VIOLATION);
    }

    private static boolean hasSqlState(Throwable t, String sqlState) {
        return findPsql(t).map(e -> sqlState.equals(e.getSQLState())).orElse(false);
    }

    private static Optional<PSQLException> findPsql(Throwable t) {
        for (Throwable cause = t; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
            if (cause instanceof PSQLException psql) {
                return Optional.of(psql);
            }
        }
        return Optional.empty();
    }
}
