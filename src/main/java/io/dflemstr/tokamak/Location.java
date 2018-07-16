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
import java.util.Locale;

/** A location in code. This can be used as a key in various tracing contexts. */
@AutoValue
abstract class Location {
  Location() {}

  /** The file name of the source file. */
  abstract String fileName();

  /** The line number of the executed source code in the source file. */
  abstract int lineNumber();

  /** The class containing the executed source code. */
  abstract String className();

  /** The method containing the executed source code. */
  abstract String methodName();

  static Location get(final int depth) {
    if (depth < 0) {
      throw new IllegalArgumentException("depth must not be negative, but was " + depth);
    }

    final StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[depth + 2];

    if (stackTraceElement.getFileName() == null || stackTraceElement.getLineNumber() < 0) {
      throw new IllegalStateException(
          "tokamak can only be used for programs where source file information is present.");
    }

    return new AutoValue_Location(
        stackTraceElement.getFileName(),
        stackTraceElement.getLineNumber(),
        stackTraceElement.getClassName(),
        stackTraceElement.getMethodName());
  }

  @Override
  public String toString() {
    // Using this format is the same as a stack trace, so most IDEs make the links clickable.
    // We don't want to use StackTraceElement directly because we don't want to support native
    // methods or missing source information.
    return String.format(
        Locale.ROOT, "%s.%s(%s:%d)", className(), methodName(), fileName(), lineNumber());
  }
}
