---
name: view-controller-test-traps
description: Spring MVC view controller (WebMvcConfigurer) traps for tests and probes in the bookstore module (handler order, parent resources block form)
metadata:
  type: reference
---

- `ViewControllerRegistry` handler mapping has order 1; `RequestMappingHandlerMapping` (controllers) is 0 and the
  actuator mapping is earlier still. An extra `addRedirectViewController("/books", ...)` is therefore never reached:
  an equivalent mutant, no test can kill it. The observable variant needs `registry.setOrder(-1000)` (then
  `booksIsNotRedirected` and `healthIsNotRedirected` fail with 302). Tell the verifier before it probes.
- `RestTestClient` from `IntegrationTestBase` does not follow redirects (302 is observed as is). Read the request URL
  from `EntityExchangeResult.getUrl()` to resolve a `Location` that Tomcat writes as an absolute URL.
- Spring Boot parent pom `<resources>` uses `${basedir}/src/main/resources`; the effective pom prints the relative
  form. When a check compares the child pom with the raw parent pom text, copy the `${basedir}` form.
