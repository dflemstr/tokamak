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

import java.util.ArrayDeque;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A tokamak per-thread context that keeps all necessary operation state. */
final class Context {
  // We can safely assume that this class will only be used from a single thread.

  private static final Logger LOG = LoggerFactory.getLogger(Context.class);
  // The trace of operation results so far.
  private final Trace<AtomicReference<Object>> trace;
  // Async operations that we are blocked on completing.
  private final ArrayDeque<CompletionStage<?>> blockedOn;

  private Context(
      final Trace<AtomicReference<Object>> trace, final ArrayDeque<CompletionStage<?>> blockedOn) {
    this.trace = trace;
    this.blockedOn = blockedOn;
  }

  /** Creates a new context instance. This should happen at most once per thread. */
  static Context create() {
    LOG.trace("creating new tokamak context for this thread");
    return new Context(Trace.create(), new ArrayDeque<>());
  }

  /** Resets this context to its initial state. */
  void reset() {
    LOG.trace("resetting");
    trace.reset();
    blockedOn.clear();
    LOG.trace("reset");
  }

  /**
   * Records that an operation was performed on this thread.
   *
   * @param depth the stack depth above which the operation was performed
   * @return the arbitrary state associated with this operation
   */
  AtomicReference<Object> operation(final int depth) {
    LOG.trace("recording an operation");
    final AtomicReference<Object> result = trace.operation(depth + 1, AtomicReference::new).value();
    LOG.trace("recorded an operation");
    return result;
  }

  /**
   * Registers a completion stage that the current computation is blocked on before it may continue.
   *
   * @param stage the stage that computation is blocked on
   */
  void addBlockedOn(final CompletionStage<?> stage) {
    LOG.trace("now blocked on {}", stage);
    blockedOn.addLast(stage);
  }

  /**
   * Checks whether computation is currently blocked on anything.
   *
   * @return {@code true} if blocked, {@code false} otherwise
   */
  boolean isBlockedOn() {
    if (blockedOn.isEmpty()) {
      LOG.trace("not blocked on anything");
      return false;
    } else {
      LOG.trace("blocked on {} tasks", blockedOn.size());
      return true;
    }
  }

  /**
   * Runs a callback for each blocked-on stage. This consumes the stages; after the method call
   * completes, {@link #isBlockedOn()} should return false.
   *
   * @param callback the callback to run for each blocked-on stage
   */
  void forEachBlockedOn(final Consumer<CompletionStage<?>> callback) {
    LOG.trace("flushing blocked stages");
    CompletionStage<?> completionStage;
    while (null != (completionStage = blockedOn.pollFirst())) {
      callback.accept(completionStage);
    }
    LOG.trace("flushed blocked stages");
  }

  /** Marks this context as committed and awaiting a {@link #reset()}. */
  void commit() {
    LOG.trace("committing");
    trace.commit();
    LOG.trace("committed");
  }

  /** Marks this context as rolled-back. */
  void rollback() {
    LOG.trace("rolling back");
    trace.rollback();
    LOG.trace("rolled back");
  }
}
