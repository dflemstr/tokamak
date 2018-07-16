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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class LocationTest {
  @Rule public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void simple() {
    final Location f = f(0);
    assertThat(f.fileName(), is("LocationTest.java"));
    assertThat(f.className(), is("io.dflemstr.tokamak.LocationTest"));
    assertThat(f.methodName(), is("f"));
    assertThat(f.lineNumber(), is(greaterThan(1)));
  }

  @Test
  public void string() {
    final Location f = f(0);
    assertThat(
        f.toString(),
        matchesPattern("io.dflemstr\\.tokamak\\.LocationTest\\.f\\(LocationTest\\.java:\\d+\\)"));
  }

  @Test
  public void illegal() {
    expectedException.expect(IllegalArgumentException.class);
    Location.get(-1);
  }

  @Test
  public void relativeLineNumbers() {
    final Location f = f(0);
    final Location g = g(0);

    assertThat(f.lineNumber(), is(lessThan(g.lineNumber())));
  }

  @Test
  public void deep() {
    final Location callsF = callsF(1);
    assertThat(callsF.fileName(), is("LocationTest.java"));
    assertThat(callsF.className(), is("io.dflemstr.tokamak.LocationTest"));
    assertThat(callsF.methodName(), is("callsF"));
    assertThat(callsF.lineNumber(), is(greaterThan(1)));
  }

  @Test
  public void equality() {
    final Location f1 = f(0);
    final Location f2 = f(0);

    assertThat(f1, is(f2));
  }

  @Test
  public void nonEquality() {
    final Location f = f(0);
    final Location g = g(0);

    assertThat(f, is(not(g)));
  }

  private Location callsF(final int depth) {
    return f(depth);
  }

  private Location f(final int depth) {
    return Location.get(depth);
  }

  private Location g(final int depth) {
    return Location.get(depth);
  }
}
