/*
 * MIT License
 *
 * Copyright (c) 2018 David Flemström
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.dflemstr.tokamak;

import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A trace/path through the execution of a program.
 *
 * <p>The execution graph is as follows: create()/reset() → operation()* → rollback()/commit()
 *
 * <p>After a rollback(), it is only valid to call operation() again in exactly the same order as
 * the last time, optionally with additional operation() calls afterwards. The operation() call will
 * return the same Operation object as before for operations that were already performed, and new
 * Operation objects for new operation() calls.
 *
 * <p>After a commit(), it is only valid to call reset().
 */
final class Trace<A> {
  // We can safely assume that this class will only be used from a single thread.

  private static final Logger LOG = LoggerFactory.getLogger(Trace.class);
  // All operations that have been performed in the longest path of the program since the last
  // rollback()
  private final ArrayList<Operation<A>> operations;
  // The index of the next operation to be performed.  If this is equal to operations.size(), it
  // means that the next operation has not yet been performed.
  private int nextOperationIndex;
  // Whether the context is in committed state.
  private boolean committed;

  private Trace(
      final ArrayList<Operation<A>> operations,
      final int nextOperationIndex,
      final boolean committed) {
    this.operations = operations;
    this.nextOperationIndex = nextOperationIndex;
    this.committed = committed;
  }

  /**
   * Creates a new trace. This is equivalent to calling {@link #reset()} on an existing trace.
   *
   * @param <A> the type of state associated with each operation of the trace
   * @return a new trace instance.
   */
  static <A> Trace<A> create() {
    return new Trace<>(new ArrayList<>(), 0, false);
  }

  /**
   * Records that a new operation was performed.
   *
   * @param depth the stack depth of the operation
   * @param value the value constructor to create values associated with the operation
   * @return the constructed operation, or an existing operation if this method was already called
   *     with the same parameters before the last {@link #rollback()}.
   */
  Operation<A> operation(final int depth, final Supplier<A> value) {
    checkIsNotCommitted();
    final Location location = Location.get(depth + 1);
    if (nextOperationIndex < operations.size()) {
      LOG.trace("following existing trace");
      checkIsNextLocation(location);
    } else {
      LOG.trace("appending new operation");
      operations.add(Operation.create(location, value.get()));
    }
    final Operation<A> operation = operations.get(nextOperationIndex);
    nextOperationIndex++;
    return operation;
  }

  /** Rolls back the trace, expecting the same series of operations to be re-tried on this trace. */
  void rollback() {
    LOG.trace("rolling back");
    checkIsNotCommitted();
    nextOperationIndex = 0;
    LOG.trace("rolled back");
  }

  /** Commits the trace, expecting all interactions to be done, until the next {@link #reset()}. */
  void commit() {
    LOG.trace("committing");
    checkStepsCompleted();
    committed = true;
    LOG.trace("committed");
  }

  /** Resets the trace to the same state it was in at construction time */
  void reset() {
    LOG.trace("resetting");
    operations.clear();
    nextOperationIndex = 0;
    committed = false;
    LOG.trace("reset");
  }

  private void checkIsNextLocation(final Location location) {
    checkLocationsAreEquivalent(
        operations.get(nextOperationIndex).location(),
        location,
        operations.subList(nextOperationIndex, operations.size()));
  }

  private void checkLocationsAreEquivalent(
      final Location expected, final Location actual, final List<Operation<A>> remainder) {
    if (!expected.equals(actual)) {
      throw new DeterminismException(
          "Code is not deterministic; it now executed "
              + actual
              + " but last time it executed:"
              + formatOperations(remainder)
              + "You need to remove the source of non-determinism; consider using Tokamak.once()");
    }
  }

  private void checkStepsCompleted() {
    if (nextOperationIndex != operations.size()) {
      throw new DeterminismException(
          "Code is not deterministic; it now returned early but last time the following "
              + "operations were executed:"
              + formatOperations(operations.subList(nextOperationIndex, operations.size()))
              + "You need to remove the source of non-determinism; consider using Tokamak.once()");
    }
  }

  private void checkIsNotCommitted() {
    if (committed) {
      throw new IllegalStateException("Context is committed");
    }
  }

  private static <A> String formatOperations(final List<Operation<A>> operations) {
    return operations
        .stream()
        .map(Operation::location)
        .map(Location::toString)
        .collect(joining("\n  - ", "\n\n  - ", "\n\n"));
  }

  @AutoValue
  abstract static class Operation<A> {
    abstract Location location();

    abstract A value();

    static <A> Operation<A> create(final Location location, final A value) {
      return new AutoValue_Trace_Operation<>(location, value);
    }
  }
}
