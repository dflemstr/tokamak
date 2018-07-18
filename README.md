# `tokamak` - An experimental async runtime library

The Tokamak library offers asynchronous execution of synchronous-looking code (similar to
`async/await` syntax in other programming languages). It does so by intelligently retrying the
code when asynchronous operations complete. The core idea is that async operations are expensive
and synchronous code is cheap.

The main entry point is `Tokamak.run()`:

```java
Tokamak.run(() -> {
  // synchronous code here
});
```

Within the Tokamak context, you can write synchronous-looking code as usual, and use
`Tokamak.await()` when you want to execute asynchronous code. For example:

```java
final int userId = 42;
// User.fetchName returns a CompletionStage<String>
final String userName = await(User.fetchName(userId));
// User.fetchAge returns a CompletionStage<Integer>
final int userAge = await(User.fetchAge(userId));

System.out.printf("User %s has age %d%n", userName, userAge);
```

Since the code is retried until all async operations succeed, you need to mark all side-effects
that should only be performed once using `Tokamak.once()`:

```java
final int userId = 42;
final String userName = await(User.fetchName(userId));
once(this::incrementFetchNameMetric);
final int userAge = await(User.fetchAge(userId));
once(this::incrementFetchAgeMetric);

System.out.printf("User %s has age %d%n", userName, userAge);
```
