package org.drone_remote_unit.model;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Rust-like Result.
 *
 * @param <T> any possible class
 */
public sealed interface Result<T> permits Result.Ok, Result.Err {
    /**
     * Ok variant.
     *
     * @param <T> any possible class
     * @param value encapsulated value of type {@code <T>}
     */
    record Ok<T>(@Nullable T value) implements Result<T> { }

    /**
     * Err variant.
     *
     * @param <T> any possible class
     * @param error string representing an error
     */
    record Err<T>(@Nullable String error) implements Result<T> { }
}
