# Built-page fixtures for the PowerShell rule of check:built

Each `.html` file here is captured from a real `npm run build` (Docusaurus 3.10.2, Pages config) and
copied byte for byte: the `<article>` element of one probe page, with a newline added at the end.
The probe pages were built in a scratch copy of the guide, never in the guide itself. Each probe page
was `docs/zz-NN-probe-<name>.mdx` with the frontmatter `title: Probe <name>`, the two theme imports
(`import Tabs from '@theme/Tabs';` and `import TabItem from '@theme/TabItem';`) and the body below.

`real-level-0-at-a-glance.html` is the exception: the first admonition (the at a glance box) of the
real Level 0 page from `guide/build`, copied byte for byte.

## The group of the T5 r4 cases

The 11 T5 r4 cases use the group of the QA probes: a `<Tabs groupId="os">` with a `macOS/Linux` item
holding a bash fence with `ls`, and a `Windows PowerShell` item holding the case text (WIN below),
an empty line, and a bash fence with the line `Remove-Item -Recurse inventedfolder`. Every tag sits
alone on its line, with empty lines around the bodies. B is one backtick.

| fixture | body |
|---|---|
| oa1c | group with WIN = `Close with B x`, next line `2. </TabItem> yB later.` |
| oa8c | group with WIN = `Close with Bx`, next line `*`, next line `more </TabItem> yB later.` |
| oa3c | group with WIN = `Close with Bx`, next line `<b>y</b> </TabItem> zB later.` |
| oa5c | group with WIN = `<b>A</b> close with Bx`, next line `more </TabItem> yB later.` |
| oa4c | group with WIN = `Close with Bx`, next line `{'y'} </TabItem> zB later.` |
| oa6c | group with WIN = `\| Close Ba \| </TabItem>B b` |
| oa6b | `\| Type Ba \| {/*B b`, an empty line, then the group with WIN = `Some text.` |
| oa7c | group with WIN = `- > Close with Bx`, next line `  > more </TabItem> yB later.` |
| oa1b | `Type Bx`, next line `2. {/* yB to start a comment.`, an empty line, then the group with WIN = `Some text.` |
| oa1b2 | as oa1b, then an empty line and `End {/* note */} here.` |
| oa1b-f | as oa1b, but `See <img src="https://example.com/x.png" alt="x" /> here.` and an empty line before the group |

(oa1c has no space between B and x in the source: `Close with Bx`.)

## The other cases

Each is a `Tabs` element with one `TabItem` per row item (value v0, v1, ...), label and body as given.

| fixture | tabs (label: body) |
|---|---|
| plain | groupId os. macOS/Linux: bash fence `brew install inventedthing`. Windows PowerShell: powershell fence `Get-Command jdtls`; `<pre>Get-Command jdtls</pre>`; `Run BGet-Command jdtlsB first.` |
| invented | groupId os. macOS/Linux: bash fence `ls`. Windows PowerShell: powershell fence with `Get-Command jdtls`, an empty line, `Invoke-Invented -Now`; `Then BInvented-InlineB too.` |
| nested | groupId os. macOS/Linux: Tabs [macOS: bash fence `brew install innerok`; Windows PowerShell: powershell fence `Inner-Invented-One`]. Windows PowerShell: Tabs [Arch Linux: bash fence `Inner-Invented-Two`; Other Linux: bash fence `Get-Command jdtls`] |
| rawpre | macOS/Linux: `Nothing.` PowerShell: `<pre>`, `Get-Command jdtls`, `Remove-Item -Recurse inventedfolder`, `</pre>` on four lines |
| rawpre-blank | as rawpre with an empty line between the two command lines |
| rawpre-expr | macOS/Linux: `Nothing.` PowerShell: `<pre>{B...B}</pre>`, a template literal with `Get-Command jdtls`, an empty line and `Remove-Item -Recurse inventedfolder` |
| entities | groupId os. macOS/Linux: `Nothing.` Windows PowerShell: powershell fence `$env:BOOKSTORE_AI_API_KEY="<key on screen>"`; `Or Bgit clone https://github.com/MaikKingma/AgenticBookStore.git && cd AgenticBookStoreB and B/compact focus on <topic>B.`; powershell fence `Write-Host "a<b" & 'c'` |
| labels | three groups: groupId jdtls [macOS: bash fence `Invented-Mac`; Windows (WSL): bash fence `Invented-Wsl`]; no groupId [macOS/Linux: `Nothing.`; PowerShell: powershell fence `Invented-Short-Label`]; groupId os [macOS/Linux: `Nothing.`; Windows PowerShell: powershell fence `Invented-Long-Label`] |
| inlist | `1. Install the tool:`, then the os group [macOS/Linux: bash fence `ls`; Windows PowerShell: powershell fence `Get-Command jdtls` and `Invented-In-List`] indented by three spaces, then `2. Next step.` |
| inlist-ol | `- Install the tool:`, then the os group [macOS/Linux: bash fence `ls`; Windows PowerShell: `1. Run BGet-Command jdtlsB first.` and `2. Then BInvented-In-Ordered-ItemB.`] indented by two spaces, then `- Next step.` |

## The rework cases

The same page shape, every tag alone on its line with empty lines around the bodies.

| fixture | body |
|---|---|
| lazy-default | `<Tabs groupId="os" lazy>` with `<TabItem value="mac" label="macOS/Linux">` (`Nothing.`) and `<TabItem value="win" label="Windows PowerShell" default>` (bash fence `Invoke-Invented -Lazy`). Only the selected panel is rendered: 2 tabs, 1 panel. |
| pre-tokenline-extra | `<Tabs groupId="os">` with `<TabItem value="mac" label="macOS/Linux">` (`Nothing.`) and `<TabItem value="win" label="Windows PowerShell">` holding the one line `<pre><span className="token-line">Get-Command jdtls</span>{'\n'}Invoke-Invented -Extra</pre>` |
| pslang | no tabs: fences `powershell` (`Get-Command jdtls`, `Invented-Powershell`), `pwsh` (`Invented-Pwsh`), `ps1` (`Invented-Ps1`), `ps` (`Invented-Ps`), `cmd` (`Invented-Cmd`), `bat` (`Invented-Bat`), `PowerShell` (`Invented-Mixed-Case`), `bash` (`Invented-Bash`) and one without a language (`Invented-Plain`), with an empty line between them |
| pslang-opener | no tabs: `Type Bx`, next line `2. {/* yB to start a comment.`, an empty line, then a powershell fence `Invoke-Invented -Opener` |
