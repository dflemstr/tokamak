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
package io.dflemstr.tokamak.test;

import static io.dflemstr.tokamak.Tokamak.await;
import static io.dflemstr.tokamak.Tokamak.computeOnce;
import static io.dflemstr.tokamak.Tokamak.once;
import static io.dflemstr.tokamak.Tokamak.runOnce;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.common.collect.Sets;
import io.dflemstr.tokamak.Tokamak;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.LongAdder;
import org.junit.Test;

public class TokamakPublicTest {
  @Test
  public void example() {
    final LongAdder adder = new LongAdder();

    final CompletionStage<Integer> result = Tokamak.run(() -> syncOperation(adder));

    assertThat(result.toCompletableFuture().join(), is(2 * 3 * 5 * 7));
    assertThat(adder.sum(), is(2L));
  }

  private static int syncOperation(final LongAdder adder) {
    final HashSet<Integer> set = computeOnce(Sets::newHashSet);
    runOnce(() -> set.add(42));

    // a=2
    final int a = await(longAsyncComputation(2));
    // b=3
    final int b = await(immediate(3));

    once(adder::increment);

    // c=5
    final int setSize = set.size();
    final int c = await(longAsyncComputation(setSize + 4));

    once(adder::increment);

    // d=7
    final int d = adder.intValue() + 5;

    // unique result since a, b, c, d are prime
    return a * b * c * d;
  }

  private static CompletionStage<Integer> immediate(final int value) {
    return CompletableFuture.completedFuture(value);
  }

  private static CompletionStage<Integer> longAsyncComputation(final int value) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            Thread.sleep(10);
          } catch (InterruptedException e) {
            // ignore
          }
          return value;
        });
  }
}
