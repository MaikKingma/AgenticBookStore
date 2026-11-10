# Tests per Layer

Write the test in the layer where the behaviour lives.

| Layer | Test type | Tooling | Example name |
|---|---|---|---|
| `domain` | Pure unit test, no Spring | JUnit, AssertJ | `BookTest` |
| `domaininteraction` | Unit test with mocked ports | JUnit, Mockito, AssertJ | `CatalogueFlowTest` |
| `command`, `query` | Web slice test with mocked flows | `@WebMvcTest`, `@MockitoBean` | `BookCommandsTest` |
| `assistant` | Web slice test with a mocked chat client; unit test of the tools with mocked flows | `@WebMvcTest`, `@MockitoBean`, Mockito | `AssistantCommandsTest`, `BookstoreToolsTest` |
| `data` | JPA slice test on PostgreSQL | `@DataJpaTest`, Testcontainers PostgreSQL, `@AutoConfigureTestDatabase(replace = NONE)` | `BookDataServiceImplTest` |
| `acl` | HTTP contract test against a mocked server | `MockRestServiceServer` bound to the `RestClient.Builder` | `DistributorAppServiceImplTest` |
| `process` | Unit test of processor and delegate | JUnit, Mockito | `EventProcessorTest` |
| End to end | Full context, PostgreSQL in Docker, real HTTP | `@SpringBootTest` with a random port and `RestTestClient` | `CatalogueIntegrationTest` |
| Architecture | Layer, dependency and naming rules | ArchUnit `@ArchTest` | `ArchitectureTest` |

- Use `@MockitoBean`, not the removed `@MockBean`.
- Name an end-to-end test `*IntegrationTest`, for example `CatalogueIntegrationTest`.
