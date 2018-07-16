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
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** A tokamak per-thread context that traces all operations performed on that thread. */
final class Context {
  // We can safely assume that this class will be used from a single thread.

  // All operations that have been performed in the longest path of the program since the last
  // begin()
  private final ArrayList<Operation> operations;
  // Async operations that we are blocked on completing.
  private final ArrayList<CompletionStage<?>> blockedOn;
  // The index of the next operation to be performed.  If this is equal to operations.size(), it
  // means that the next operation has not yet been performed.
  private int nextOperationIndex;
  // Whether the context is in committed state.
  private boolean committed;

  private Context(
      final ArrayList<Operation> operations,
      final ArrayList<CompletionStage<?>> blockedOn,
      final int nextOperationIndex,
      final boolean committed) {
    this.operations = operations;
    this.blockedOn = blockedOn;
    this.nextOperationIndex = nextOperationIndex;
    this.committed = committed;
  }

  static Context create() {
    return new Context(new ArrayList<>(), new ArrayList<>(), 0, false);
  }

  Operation operation(final int depth) {
    checkIsNotCommitted();
    final Location location = Location.get(depth);
    if (nextOperationIndex < operations.size()) {
      checkIsNextLocation(location);
    } else {
      operations.add(Operation.create(location, new AtomicReference<>()));
    }
    final Operation operation = operations.get(nextOperationIndex);
    nextOperationIndex++;
    return operation;
  }

  void addBlockedOn(final CompletionStage<?> stage) {
    blockedOn.add(stage);
  }

  boolean isBlockedOn() {
    return !blockedOn.isEmpty();
  }

  void forEachBlockedOn(final Consumer<CompletionStage<?>> callback) {
    for (final CompletionStage<?> completionStage : blockedOn) {
      callback.accept(completionStage);
    }
    blockedOn.clear();
  }

  void rollback() {
    checkIsNotCommitted();
    nextOperationIndex = 0;
  }

  void commit() {
    checkStepsCompleted();
    committed = true;
  }

  void reset() {
    operations.clear();
    blockedOn.clear();
    nextOperationIndex = 0;
    committed = false;
  }

  private void checkIsNextLocation(final Location location) {
    checkLocationsAreEquivalent(
        operations.get(nextOperationIndex).location(),
        location,
        operations.subList(nextOperationIndex, operations.size()));
  }

  private void checkLocationsAreEquivalent(
      final Location expected, final Location actual, final List<Operation> remainder) {
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

  private String formatOperations(final List<Operation> operations) {
    return operations
        .stream()
        .map(Operation::location)
        .map(Location::toString)
        .collect(joining("\n  - ", "\n\n  - ", "\n\n"));
  }

  @AutoValue
  abstract static class Operation {
    abstract Location location();

    abstract AtomicReference<Object> value();

    static Operation create(final Location location, final AtomicReference<Object> value) {
      return new AutoValue_Context_Operation(location, value);
    }
  }
}
