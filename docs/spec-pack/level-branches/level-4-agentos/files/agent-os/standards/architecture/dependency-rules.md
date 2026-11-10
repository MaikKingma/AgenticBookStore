# Dependency Rules

Enforced by ArchUnit in `ArchitectureTest`; a violation breaks the build.

1. `domain` may only be accessed by `domaininteraction`.
2. `domaininteraction` may only be accessed by `command`, `query`, `process`, `assistant`, `data`, `acl`.
3. No outer adapter (`command`, `query`, `process`, `assistant`, `data`, `acl`) may be accessed by any other layer.
4. `domain` depends on `java..` only. No Spring, no Jakarta Persistence, no Jackson, no Lombok, no Spring AI.
5. `jakarta.persistence..` and `org.springframework.data..` are only allowed in `data`.
6. `org.springframework.web.client..` (`RestClient`) is only allowed in `acl`.
7. `org.springframework.ai..` is only allowed in `assistant`.
8. `@RestController` classes only live in `command`, `query` and `assistant`.

One exception to rule 1: `process` may reference `domain` classes whose simple name ends with `Event`, because the event type is the contract of the listener.

```java
.ignoreDependency(resideInAPackage(PROCESS), resideInAPackage(DOMAIN).and(simpleNameEndingWith("Event")))
```

- Adding another exception means changing the rule and `docs/architecture.md` in the same change, after the team agreed.
- `APP_SERVICE_IMPL_NESTED_CLASSES_ONLY_IN_ACL`: the private payload records nested in an `*AppServiceImpl` describe the distributor's JSON and never leave `acl`.
- Layer patterns are anchored at `nl.openvalue..`, so `org.springframework.data.domain` never matches.
