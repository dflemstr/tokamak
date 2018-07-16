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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import io.dflemstr.tokamak.Tokamak;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.LongAdder;
import org.junit.Test;

public class TokamakPublicTest {
  @Test
  public void example() {
    final LongAdder adder = new LongAdder();

    final CompletionStage<Integer> result =
        Tokamak.run(
            () -> {
              int a = Tokamak.await(async(3));
              int b =
                  Tokamak.once(
                      () -> {
                        adder.increment();
                        return adder.intValue();
                      });
              int c = Tokamak.await(async(4));
              Tokamak.once(adder::increment);

              return a + b + c;
            });

    assertThat(result.toCompletableFuture().join(), is(8));
    assertThat(adder.sum(), is(2L));
  }

  private static CompletionStage<Integer> async(final int value) {
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
