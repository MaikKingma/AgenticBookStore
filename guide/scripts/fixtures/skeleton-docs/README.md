# Skeleton docs snapshot for the self-tests

`docs/` here holds the twelve files of `guide/docs/` (the eleven skeleton pages and
`reference/_category_.json`) as they are at commit 05e58a2 of the spec branch, copied byte for byte.
Every page still carries the skeleton marker `{/* guide-skeleton */}`.

The self-tests that need a whole guide in a known state (every page a skeleton, the pending entries
per page, a marker removed on purpose) read this copy instead of the live `guide/docs/`, because the
content tasks fill in the live pages and remove their markers. The tests that describe the live guide
itself (the page set, and what the command line reports on the default root) still read `guide/docs/`.

Never edit these files. The checker does not count them as pages: the structure, coverage and
content rules read `<root>/docs/` only, and the site build reads `guide/docs/` only.
