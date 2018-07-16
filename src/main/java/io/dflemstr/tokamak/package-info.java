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

/**
 * The Tokamak library offers asynchronous execution of synchronous-looking code (similar to {@code
 * async/await} syntax in other programming languages). It does so by intelligently retrying the
 * code when asynchronous operations complete. The core idea is that async operations are expensive
 * and synchronous code is cheap.
 *
 * <p>The main entry point is {@link
 * io.dflemstr.tokamak.Tokamak#run(io.dflemstr.tokamak.Tokamak.VoidClosure)} Tokamak.run()}:
 *
 * <pre>{@code
 * Tokamak.run(() -> {
 *   // synchronous code here
 * });
 * }</pre>
 *
 * Within the Tokamak context, you can write synchronous-looking code as usual, and use {@link
 * io.dflemstr.tokamak.Tokamak#await(java.util.concurrent.CompletionStage) Tokamak.await()} when you
 * want to execute asynchronous code. For example:
 *
 * <pre>{@code
 * final int userId = 42;
 * final String userName = Tokamak.await(User.fetchName(userId));
 * final int userAge = Tokamak.await(User.fetchAge(userId));
 *
 * System.out.printf("User %s has age %d%n", userName, userAge);
 * }</pre>
 *
 * Since the code is retried until all async operations succeed, you need to mark all side-effects
 * that should only be performed once using {@link
 * io.dflemstr.tokamak.Tokamak#once(io.dflemstr.tokamak.Tokamak.VoidClosure) Tokamak.once()}:
 *
 * <pre>{@code
 * final int userId = 42;
 * final String userName = Tokamak.await(User.fetchName(userId));
 * Tokamak.once(this::incrementFetchNameMetric);
 * final int userAge = Tokamak.await(User.fetchAge(userId));
 * Tokamak.once(this::incrementFetchAgeMetric);
 *
 * System.out.printf("User %s has age %d%n", userName, userAge);
 * }</pre>
 */
@ParametersAreNonnullByDefault
package io.dflemstr.tokamak;

import javax.annotation.ParametersAreNonnullByDefault;
