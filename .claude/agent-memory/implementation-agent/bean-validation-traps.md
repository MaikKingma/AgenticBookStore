---
name: bean-validation-traps
description: Payload constraint techniques in the command layer (validating a trimmed value, keeping one violation per rule, test data for JSON control characters)
applies-to:
  - bookstore/src/main/java/nl/openvalue/bookstore/command/**
  - bookstore/src/test/java/nl/openvalue/bookstore/command/**
metadata:
  type: reference
---
- Bean validation sees the raw string. A length rule on the trimmed value must NOT be a regex: `Pattern` counts code
  points, so `.{0,252}` lets a surrogate-pair address past the UTF-16 limit that `String.length()` enforces (domain 422
  instead of payload 400). Use a nested custom constraint (annotation + `ConstraintValidator` as public static nested
  types in the payload file, `value.trim().length() <= max`, null valid). Targets must copy `@Pattern`'s
  (METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE); `RECORD_COMPONENT` alone is never validated.
  ArchUnit's suffix rules ignore nested types. Keep the shape `@Pattern` separate so a both-broken value lists the field
  twice. `[\x00-\x20]` is exactly the set `String.trim()` strips.
- `OrderCommandsTest.placeBody` pastes the email raw into JSON: write a control character as the JSON escape (`"\\t"` in Java
  source) and compare with `email.replace("\\t", "\t")`.
- U+2003 is a legal local-part character for these patterns (`[^@\x00-\x20]`), so it is accepted and can never be a "padding
  that stays 400" witness. Hibernate Validator's `@NotBlank` does reject a U+2003-only string (400), unlike `String.trim()`.
  In a test that pastes the value raw into JSON, write it as `"\\u2003"` in Java source: ASCII only, and the doubled backslash
  keeps javac's unicode-escape preprocessing out (never put a bare backslash-u escape in a comment either).
