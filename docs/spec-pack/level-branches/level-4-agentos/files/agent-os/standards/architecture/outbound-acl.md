# Anti-Corruption Layer

Outbound anti-corruption layer towards the distributor: the `acl` package.

- An `*AppServiceImpl` implements the port with Spring `RestClient`.
- External payloads are private nested records. They describe the distributor's JSON and never leave `acl`.
- Every external failure becomes a port exception.
- The base URL comes from `distributor.base-url`.
