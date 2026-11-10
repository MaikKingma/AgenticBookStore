# Class Naming

Top-level classes of a package must end with one of these suffixes (`ArchitectureTest`). Nested, anonymous and synthetic classes and `package-info` are not checked.

| Package | Class names must end with |
|---|---|
| `command` | `Commands`, `Payload`, `CommandResult`, `ExceptionHandler` |
| `query` | `Queries`, `View`, `ExceptionHandler` |
| `process` | `EventProcessor`, `Delegate` |
| `assistant` | `Commands`, `Tools`, `Config`, `Properties`, `Payload`, `View`, `ExceptionHandler`, `RateLimiter`, `ChatModel` |
| `data` | `DataServiceImpl`, `JPA`, `JPAMapper`, `Repository` |
| `acl` | `AppServiceImpl`, `Properties` |
| `domaininteraction` | `Flow`, `DataService`, `AppService`, `DTO`, `DomainMapper`, `Exception` |

- `domain` has no suffix rule: name aggregates, value objects, events and exceptions after the business.
- Prefer a top-level record for a nested JSON shape (`OrderLinePayload`, `PendingRestockView`) so it carries a suffix.
- Private nested payload records inside an `*AppServiceImpl` are the exception: they describe the distributor's JSON and never leave `acl` (`APP_SERVICE_IMPL_NESTED_CLASSES_ONLY_IN_ACL`).
