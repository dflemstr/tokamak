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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Tokamak {

  private static final Logger LOG = LoggerFactory.getLogger(Tokamak.class);
  private static final ThreadLocal<Context> CONTEXT_THREAD_LOCAL =
      ThreadLocal.withInitial(Context::create);
  private static final TokamakBreakError THE_BREAK_ERROR = new TokamakBreakError();

  private Tokamak() {}

  public static <A, E extends Exception> CompletionStage<A> run(final Closure<A, E> closure) {
    LOG.debug("running new tokamak closure {}", closure);
    final Context context = CONTEXT_THREAD_LOCAL.get();
    final CompletableFuture<A> result = new CompletableFuture<>();

    context.reset();
    tryComplete(context, closure::apply, result);

    return result;
  }

  public static <E extends Exception> CompletionStage<Void> run(final VoidClosure<E> closure) {
    LOG.debug("running new tokamak closure {}", closure);
    final Context context = CONTEXT_THREAD_LOCAL.get();
    final CompletableFuture<Void> result = new CompletableFuture<>();

    context.reset();
    tryComplete(
        context,
        () -> {
          closure.apply();
          return null;
        },
        result);

    return result;
  }

  public static <A> Operation.Builder<A> opBuilder() {
    return Operation.builder();
  }

  public static <A> Operation<A> op() {
    return Tokamak.<A>opBuilder().build();
  }

  public static <A> A await(final CompletionStage<A> stage) {
    return Tokamak.<A>op().await(stage);
  }

  public static <A, E extends Exception> A once(final Closure<A, E> closure) throws E {
    return Tokamak.<A>op().perform(closure);
  }

  public static <E extends Exception> void once(final VoidClosure<E> closure) throws E {
    Tokamak.<Void>op().perform(closure);
  }

  @FunctionalInterface
  public interface Closure<A, E extends Exception> {
    A apply() throws E;
  }

  @FunctionalInterface
  public interface VoidClosure<E extends Exception> {
    void apply() throws E;
  }

  @AutoValue
  public abstract static class Operation<A> {

    abstract ImmutableSet<Class<? extends Throwable>> retryOn();

    static <A> Builder<A> builder() {
      return new AutoValue_Tokamak_Operation.Builder<>();
    }

    public A await(final CompletionStage<A> stage) {
      final Context context = CONTEXT_THREAD_LOCAL.get();
      final Context.Operation operation = context.operation(2);

      @SuppressWarnings("unchecked")
      final CompletableFuture<A> future =
          (CompletableFuture<A>)
              operation.value().updateAndGet(v -> v == null ? stage.toCompletableFuture() : v);

      if (future.isDone()) {
        try {
          return future.join();
        } catch (final CompletionException e) {
          return handleThrowable(e.getCause());
        }
      } else {
        context.addBlockedOn(future);
        throw THE_BREAK_ERROR;
      }
    }

    public <E extends Exception> A perform(final Closure<A, E> closure) throws E {
      final Context context = CONTEXT_THREAD_LOCAL.get();
      final Context.Operation operation = context.operation(2);

      @SuppressWarnings("unchecked")
      A result = (A) operation.value().get();

      if (result == null) {
        try {
          result = closure.apply();
          operation.value().set(result);
          return result;
        } catch (final Exception e) {
          return handleThrowable(e);
        }
      }

      return result;
    }

    public <E extends Exception> void perform(final VoidClosure<E> closure) throws E {
      final Context context = CONTEXT_THREAD_LOCAL.get();
      final Context.Operation operation = context.operation(2);

      @SuppressWarnings("unchecked")
      A result = (A) operation.value().get();
      // Check for some placeholder value
      if (result == null) {
        try {
          closure.apply();
          operation.value().set(Boolean.TRUE);
        } catch (final Exception e) {
          handleThrowable(e);
        }
      }
    }

    private <E extends Exception> A handleThrowable(final Throwable throwable) throws E {
      if (retryOn().contains(throwable.getClass())) {
        throw THE_BREAK_ERROR;
      } else if (throwable instanceof RuntimeException) {
        throw (RuntimeException) throwable;
      } else {
        @SuppressWarnings("unchecked")
        final E typed = (E) throwable;
        throw typed;
      }
    }

    @AutoValue.Builder
    public abstract static class Builder<A> {
      public abstract ImmutableSet.Builder<Class<? extends Throwable>> retryOnBuilder();

      public Builder<A> retryOn(final Class<? extends Throwable> exceptionClass) {
        retryOnBuilder().add(exceptionClass);
        return this;
      }

      public abstract Operation<A> build();
    }
  }

  private static <A> void tryComplete(
      final Context context,
      final Callable<A> callable,
      final CompletableFuture<A> completableFuture) {
    LOG.debug("trying to complete");
    while (true) {
      try {
        final A result = callable.call();
        LOG.debug("closure called");
        context.commit();
        completableFuture.complete(result);
      } catch (final TokamakBreakError breakError) {
        if (breakError != THE_BREAK_ERROR) {
          throw new IllegalStateException(
              "There is more than one instance of " + TokamakBreakError.class);
        }

        context.rollback();

        if (context.isBlockedOn()) {
          final ArrayList<CompletionStage<A>> spawnedStages = new ArrayList<>();
          final AtomicBoolean handlerSpawned = new AtomicBoolean();

          final BiConsumer<A, Throwable> handler =
              (a, e) -> {
                if (handlerSpawned.compareAndSet(false, true)) {
                  for (final CompletionStage<A> spawnedStage : spawnedStages) {
                    spawnedStage.toCompletableFuture().cancel(false);
                  }
                  CONTEXT_THREAD_LOCAL.set(context);
                  tryComplete(context, callable, completableFuture);
                }
              };

          context.forEachBlockedOn(
              stage -> {
                @SuppressWarnings("unchecked")
                final CompletionStage<A> typedStage = (CompletionStage<A>) stage;
                final CompletionStage<A> handlerStage = typedStage.whenComplete(handler);
                spawnedStages.add(handlerStage);
              });

          return;
        }
      } catch (final Exception e) {
        completableFuture.completeExceptionally(e);
      }
    }
  }

  private static final class TokamakBreakError extends Error {

    private static final String MESSAGE =
        "Internal Tokamak break control flow error. Catching this error is a bug.";

    private TokamakBreakError() {
      super(MESSAGE, null, false, false);
    }
  }
}
