---
name: docusaurus-markdown-html-probes
description: guide/ Markdown/MDX checkers, built-HTML tab readers and rehype plugins probed through the real pipeline
metadata:
  type: feedback
---
Rule: for a Markdown/MDX structure checker, probe the syntax variants the renderer accepts, then confirm survivors in the built HTML.
**Why:** a column-0 `^#` H1 rule passed `   # Foo` and setext `Foo` / `===`, which Docusaurus rendered as the page `<h1>`; a box parser accepting "any fenced block under the label" accepted a bogus extra block.
**How to apply:**
- Witnesses: ATX with 1 to 3 spaces, setext, four-colon `::::type Title`, second fenced block per label, text after a bare label, CRLF, marker inside a fence, indented opener. JSX `<h1>` in MDX is invisible to Markdown rules: route to the rendered "exactly one H1" check.
- Offline config (hash router) renders no route at build time: bad props, broken anchors and absolute route links pass `build:offline`; only the Pages build guards them. For link probes use an anchor id whose slug differs from the auto slug. Built HTML is minified and Prism splits tokens: grep a word, not a command line.
- Span/comment scanners: an unclosed opener in inline code (`` `{/*` ``) blanks the rest of the page; an inline `` `</TabItem>` `` ends an item for a regex tag walker (fail-open). Mutant families: closer of a longer run, carry reset on blank line, escaped backtick, span across a blank line. Classify survivors by direction: fail-open need pins, fail-closed are recommended. Span oracle without a build: `mdast-util-from-markdown` + gfm from `guide/node_modules` (absolute import from a scratch script).

Rule: for a built-HTML tab reader or command extraction, probe the HTML Docusaurus really emits.
**Why:** `<Tabs lazy>` renders only the selected panel (more tabs than panels), so `!==` -> `<` survives unless pinned; built HTML has unquoted attributes (`id=far-behind`), no `</p>`, code lines as `<div class=token-line>` with Prism spans; a reader written for quoted attributes finds nothing.
**How to apply:** pages in a scratch copy: label case, `values`, `default`, `queryString`, `lazy`, nested Tabs, details, table cell, admonition, `<pre><code>`, entities, NBSP. Scan `<(p|pre|h2)\b` openers, split `pre` on the token-line class, decode entities last, match ids as `id="?x[\s">]`; feed commands to `bash -c` straight from `guide/build`. The offline build writes only index.html: `check:built` on it is vacuous. Invisible characters: offer `/\p{Default_Ignorable_Code_Point}/gu`, verify over all code points raw and as `&#x..;`. Scan tag-stripped HTML plus JS chunks (an unselected tab panel is in `textContent`, not `innerText`).

Rule: for a rehype/remark plugin, probe through the real pipeline in a scratch copy with structural controls.
**Why:** hand-built hast fixtures cannot show what MDX emits (`<kbd>` is an `mdxJsxTextElement`, invisible to a walk over `type === 'element'`).
**How to apply:** `rsync -a --exclude node_modules --exclude build --exclude build-offline --exclude .docusaurus guide <dir>/`, symlink `node_modules`, add a probe page, build both configs, read attributes back with `grep -o`. Negative control: delete the registration line. A first-match `perl -pi` on built HTML must target the element, not the filename (preload links share it).
Related: [[docusaurus-guide-probes]], [[docusaurus-content-ui-probes]], [[docusaurus-marker-ban-and-site-pass]]
