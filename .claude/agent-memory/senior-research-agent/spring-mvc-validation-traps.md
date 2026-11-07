---
name: spring-mvc-validation-traps
description: Boot 4 / Spring 7 / Jackson 3 request binding and validation behaviour that contradicts Boot 3-era intuition (primitive JSON fields, @Validated, boolean params, params conditions, scoped advice)
metadata:
  type: reference
---

Verified on Boot 4.1.2 (Spring 7.0.10, Jackson 3.1.7, Hibernate Validator 9.1.5) with a scratch `@WebMvcTest`.

- Jackson 3 enables `FAIL_ON_NULL_FOR_PRIMITIVES` by default and Boot keeps it: a missing or null `int` record
  component is `HttpMessageNotReadableException` (400 without the app's `validation-failed` body), not 0.
  Use `Integer` + `@NotNull` when a missing field must be named. Jackson still coerces `"5"` and truncates `1.5`.
- Class-level `@Validated` on a controller switches to AOP method validation: `ConstraintViolationException`
  (path `method.param`), not `HandlerMethodValidationException`. The built-in one needs no class-level `@Validated`;
  its field name comes from `getParameterValidationResults().get(i).getMethodParameter().getParameterName()`.
- `@RequestParam boolean` accepts `TRUE`, `1`, `yes`, `on`; an empty value is `MethodArgumentTypeMismatchException`.
- `@GetMapping(params = "x=true")` mismatches throw during handler lookup with no handler, so a
  `@RestControllerAdvice(basePackageClasses = ...)` never sees them.
- Check `docs/spec-pack/product/tech-stack.md` before proposing constraints on controller parameters: it limits
  Jakarta Validation to `*Payload` records.

Related: [[spring-tx-event-traps]]
