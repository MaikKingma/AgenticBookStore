# Persistence Adapter

Outbound persistence adapter: the `data` package.

- A `*DataServiceImpl` implements a `*DataService` port with a Spring Data `*Repository` and a `*JPAMapper`.
- JPA entities (`*JPA`) never leave `data`. The port speaks DTOs.
- `jakarta.persistence..` and `org.springframework.data..` are allowed only in `data` (ArchUnit rule `PERSISTENCE_TYPES_ONLY_IN_DATA`).

```java
public class BookDataServiceImpl implements BookDataService
```
