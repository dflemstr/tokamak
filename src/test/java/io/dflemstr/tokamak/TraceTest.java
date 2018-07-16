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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TraceTest {
  @Rule public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void noop() {
    final Trace<Object> trace = Trace.create();
    trace.commit();
  }

  @Test
  public void useAfterCommit() {
    expectedException.expect(IllegalStateException.class);
    final Trace<Object> trace = Trace.create();
    trace.commit();
    trace.operation(1, Object::new);
  }

  @Test
  public void singleOp() {
    final Trace<Object> trace = Trace.create();
    f(trace);
    trace.commit();
  }

  @Test
  public void singleOpRetried() {
    final Trace<Object> trace = Trace.create();
    final Trace.Operation<Object> op1 = f(trace);
    trace.rollback();
    final Trace.Operation<Object> op2 = f(trace);
    trace.commit();

    assertThat(op1.value(), is(op2.value()));
  }

  @Test
  public void singleOpRetriedDifferentPath() {
    expectedException.expect(DeterminismException.class);
    expectedException.expectMessage(
        matchesPattern(
            "Code is not deterministic; it now executed io\\.dflemstr\\.tokamak\\.TraceTest\\.g"
                + "\\(TraceTest\\.java:\\d+\\) "
                + "but last time it executed:\n"
                + "\n"
                + "  - io\\.dflemstr\\.tokamak\\.TraceTest\\.f\\(TraceTest\\.java:\\d+\\)\n"
                + "  - io\\.dflemstr\\.tokamak\\.TraceTest\\.g\\(TraceTest\\.java:\\d+\\)\n"
                + "  - io\\.dflemstr\\.tokamak\\.TraceTest\\.f\\(TraceTest\\.java:\\d+\\)\n"
                + "\n"
                + "You need to remove the source of non-determinism; consider using Tokamak\\.once\\(\\)"));

    final Trace<Object> trace = Trace.create();
    f(trace);
    g(trace);
    f(trace);
    trace.rollback();
    g(trace);
    trace.commit();
  }

  @Test
  public void singleOpRetriedTerminatedEarly() {
    expectedException.expect(DeterminismException.class);
    expectedException.expectMessage(
        matchesPattern(
            "Code is not deterministic; it now returned early but last time the following "
                + "operations were executed:\n"
                + "\n"
                + "  - io\\.dflemstr\\.tokamak\\.TraceTest\\.f\\(TraceTest\\.java:\\d+\\)\n"
                + "  - io\\.dflemstr\\.tokamak\\.TraceTest\\.g\\(TraceTest\\.java:\\d+\\)\n"
                + "  - io\\.dflemstr\\.tokamak\\.TraceTest\\.f\\(TraceTest\\.java:\\d+\\)\n"
                + "\n"
                + "You need to remove the source of non-determinism; consider using Tokamak\\.once\\(\\)"));

    final Trace<Object> trace = Trace.create();
    f(trace);
    g(trace);
    f(trace);
    trace.rollback();
    trace.commit();
  }

  private Trace.Operation<Object> f(final Trace<Object> trace) {
    return trace.operation(0, Object::new);
  }

  private Trace.Operation<Object> g(final Trace<Object> trace) {
    return trace.operation(0, Object::new);
  }
}
