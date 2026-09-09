# Tests to implement

1. Primary assignment always produces one reporter when catalog is non-empty.
2. Disabled reporters are excluded.
3. Weight zero reporter is never picked if any positive weight exists.
4. Second reporter is distinct.
5. Statistical test over a fixed RNG abstraction: second-report rate is ~0.10.
6. Existing assignment is reused.
7. Concurrent unique-index collision reloads existing match assignment.
8. Cooldown/daily caps alter candidate availability once runtime-state logic is added.

Note: inject a `RandomGenerator`/random source before implementing deterministic unit tests.
