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

import static org.hamcrest.Matchers.matchesPattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ContextTest {
  @Rule public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void noop() {
    final Context context = Context.create();
    context.commit();
  }

  @Test
  public void useAfterCommit() {
    expectedException.expect(IllegalStateException.class);
    final Context context = Context.create();
    context.commit();
    context.operation(1);
  }

  @Test
  public void singleOp() {
    final Context context = Context.create();
    f(context);
    context.commit();
  }

  @Test
  public void singleOpRetried() {
    final Context context = Context.create();
    f(context);
    context.rollback();
    f(context);
    context.commit();
  }

  @Test
  public void singleOpRetriedDifferentPath() {
    expectedException.expect(DeterminismException.class);
    expectedException.expectMessage(
        matchesPattern(
            "Code is not deterministic; it now executed io\\.dflemstr\\.tokamak\\.ContextTest\\.g"
                + "\\(ContextTest\\.java:\\d+\\) "
                + "but last time it executed:\n"
                + "\n"
                + "  - io\\.dflemstr\\.tokamak\\.ContextTest\\.f\\(ContextTest\\.java:\\d+\\)\n"
                + "  - io\\.dflemstr\\.tokamak\\.ContextTest\\.g\\(ContextTest\\.java:\\d+\\)\n"
                + "  - io\\.dflemstr\\.tokamak\\.ContextTest\\.f\\(ContextTest\\.java:\\d+\\)\n"
                + "\n"
                + "You need to remove the source of non-determinism; consider using Tokamak\\.once\\(\\)"));

    final Context context = Context.create();
    f(context);
    g(context);
    f(context);
    context.rollback();
    g(context);
    context.commit();
  }

  @Test
  public void singleOpRetriedTerminatedEarly() {
    expectedException.expect(DeterminismException.class);
    expectedException.expectMessage(
        matchesPattern(
            "Code is not deterministic; it now returned early but last time the following "
                + "operations were executed:\n"
                + "\n"
                + "  - io\\.dflemstr\\.tokamak\\.ContextTest\\.f\\(ContextTest\\.java:\\d+\\)\n"
                + "  - io\\.dflemstr\\.tokamak\\.ContextTest\\.g\\(ContextTest\\.java:\\d+\\)\n"
                + "  - io\\.dflemstr\\.tokamak\\.ContextTest\\.f\\(ContextTest\\.java:\\d+\\)\n"
                + "\n"
                + "You need to remove the source of non-determinism; consider using Tokamak\\.once\\(\\)"));

    final Context context = Context.create();
    f(context);
    g(context);
    f(context);
    context.rollback();
    context.commit();
  }

  private void f(final Context context) {
    context.operation(1);
  }

  private void g(final Context context) {
    context.operation(1);
  }
}
