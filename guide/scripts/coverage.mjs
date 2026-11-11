// The coverage manifest of the guide: what the participant pages must carry from the pack,
// each item anchored in a pack section. Data only; the rules that read it are in
// rules/coverage.mjs (anchoring and page coverage) and the check runs them.
//
// An entry is {id, row, source, section, page, kind, snippet, pageSnippet?}:
//   row      the label of the row of the research table "source line -> item -> page" (res-3
//            section 9) or of its sections 2, 5 and 6 that the entry accounts for;
//   source   the pack file, a key of SOURCE_FILES (plan, sdd, briefs, spec10, spec09);
//   section  the start of one heading of that file; the entry is searched in that section only;
//   page     the page, a key of PAGE_FILES;
//   kind     command: the page shows it in a fenced block; text: it is on the page, anywhere
//            outside comments, the at a glance box and the checklist;
//   snippet  text that is in the pack section (whitespace and brackets folded, otherwise exact);
//   pageSnippet  what the page carries when it differs from the pack (a placeholder path in
//            the pack, a corrected course title).
// A row the script cannot match (prose, or a rule of its own) is a manual row with its reason,
// for the verifiers. A page that still carries the skeleton marker lists its entries as pending.

// Paths relative to docs/spec-pack/ in the repository.
export const SOURCE_FILES = {
  plan: 'workshop/workshop-plan.md',
  sdd: 'workshop/sdd-framework-guide.md',
  briefs: 'workshop/level-4-feature-briefs.md',
  spec10: 'specs/10-harness-reference-solutions.md',
  spec09: 'specs/09-workshop-baseline.md',
};

// Paths relative to guide/.
export const PAGE_FILES = {
  IX: 'docs/index.md',
  SU: 'docs/00-setup-check.md',
  L0: 'docs/01-level-0-break-the-agent.mdx',
  L1: 'docs/02-level-1-context-engineering.mdx',
  L2: 'docs/03-level-2-give-your-agent-eyes.mdx',
  L3: 'docs/04-level-3-behavioral-control.mdx',
  L4: 'docs/05-level-4-spec-driven-development.mdx',
  WU: 'docs/06-wrap-up.md',
  CS: 'docs/reference/cheat-sheet.md',
  AR: 'docs/reference/architecture.md',
  TS: 'docs/reference/troubleshooting.md',
};

// A builder per source, section and page: ({id, row, kind, snippet, pageSnippet}) in the order
// of the arguments. Unused fields are left out, so an entry has no undefined key.
const entries = (source, section, page) => (id, row, kind, snippet, pageSnippet) => ({
  id,
  row,
  source,
  section,
  page,
  kind,
  snippet,
  ...(pageSnippet === undefined ? {} : {pageSnippet}),
});
const plan = (section, page) => entries('plan', section, page);

const SETUP = '3. Setup check';
const LEVEL0 = '4. Level 0';
const LEVEL1 = '5. Level 1';
const LEVEL2 = '7. Level 2';
const PART_A_LSP = 'Part A: LSP';
const PART_B_MCP = 'Part B: MCP';
const LEVEL3 = '8. Level 3';
const PART_A_PERMISSIONS = 'Part A: Permissions';
const PART_B_HOOKS = 'Part B: Hooks';
const PART_C = 'Part C: A skill and a command';
const LEVEL4 = '9. Level 4';
const SDD_PART2 = 'Part 2: Level 4 tracks for participants';
const WRAP_UP = '10. Wrap-up';
const RISKS = '11. Risks and mitigations';

const setup = plan(SETUP, 'SU');
const level0 = plan(LEVEL0, 'L0');
const level1 = plan(LEVEL1, 'L1');
const level2 = plan(LEVEL2, 'L2');
const partA = plan(PART_A_LSP, 'L2');
const partB = plan(PART_B_MCP, 'L2');
const level3 = plan(LEVEL3, 'L3');
const permissions = plan(PART_A_PERMISSIONS, 'L3');
const hooks = plan(PART_B_HOOKS, 'L3');
const skill = plan(PART_C, 'L3');
const level4 = plan(LEVEL4, 'L4');
const trackA = entries('sdd', 'Track A: OpenSpec', 'L4');
const trackB = entries('sdd', 'Track B: Spec Kit', 'L4');
const trackC = entries('sdd', 'Track C: Agent OS', 'L4');
const common = entries('sdd', SDD_PART2, 'L4');
const wrapUp = plan(WRAP_UP, 'WU');
const install = entries('sdd', 'Installing a framework yourself', 'WU');
const agenda = plan('2. Agenda', 'IX');
const risks = plan(RISKS, 'TS');
const cheat = (section) => plan(section, 'CS');

const BRIEF_A = entries('briefs', 'Brief A', 'L4');
const BRIEF_B = entries('briefs', 'Brief B', 'L4');
const BRIEF_C = entries('briefs', 'Brief C', 'L4');
const FB_ROW = 'FB:6-12, 14-20, 22-36';

// The JSON of Level 3, as the workshop plan prints it (folded when compared, so the page may
// break its lines differently, but must hold the same keys, values and order).
const PERMISSIONS_JSON = `{
  "permissions": {
    "allow": ["Bash(./mvnw *)", "Bash(git status *)", "Bash(git diff *)", "Bash(git add *)", "Bash(git commit *)"],
    "deny":  ["Read(./.env)", "Read(./.env.*)", "Bash(git push)", "Bash(git push *)", "Bash(* --no-verify*)", "Bash(git commit -n *)"]
  }
}`;

const HOOKS_JSON = String.raw`"hooks": {
  "PreToolUse": [
    { "matcher": "Bash",
      "hooks": [ { "type": "command", "if": "Bash(git commit *)", "timeout": 180,
                   "command": "\"$CLAUDE_PROJECT_DIR\"/.claude/hooks/tests-before-commit.sh" } ] }
  ]
}`;

// The hook script of spec 10, quoted on the Level 3 page as the reference to compare with.
const TESTS_BEFORE_COMMIT = `#!/usr/bin/env bash
# PreToolUse hook: block \`git commit\` unless the full build is green.
# Exit code 2 blocks the tool call and feeds stderr back to Claude. Any other non-zero code does NOT block.
input="$(cat)"
# The "if" filter in settings.json is best-effort, so check the command ourselves too.
if ! printf '%s' "$input" | grep -Eq '"command"[[:space:]]*:[[:space:]]*"[^"]*git[[:space:]]+commit'; then
  exit 0
fi
cd "\${CLAUDE_PROJECT_DIR:-.}" || exit 1
log="$(mktemp)"
if ./mvnw -q verify >"$log" 2>&1; then
  rm -f "$log"
  exit 0
fi
{
  echo "Commit blocked: ./mvnw verify failed. Fix the cause (do not weaken tests), then commit again."
  tail -n 40 "$log"
} >&2
rm -f "$log"
exit 2`;

// Level 1 (spec 15): the rule file of step 3, byte-equal to the level-1 solution branch (level-1-rule.test.mjs compares
// the page with the staged file), and the illustrative /context mock of step 5.
const LEVEL1_DOMAIN_RULE = `---
paths:
  - "bookstore/src/main/java/**/domain/**"
---
# Domain layer
- Aggregates: private constructor, an intention-revealing static factory for creation, \`restore(...)\` for rehydration.
- Change state through behaviour methods named after the business action. No setters.
- Check invariants inside the aggregate and throw an unchecked exception from the \`domain\` package.
- Register domain events in the aggregate; flows publish them after saving via \`pullDomainEvents()\`.
- Value objects are records that validate in the compact constructor.
- Domain tests are plain JUnit and AssertJ, without a Spring context.`;

const LEVEL1_CONTEXT_MOCK = `Context usage: 100k of 200k tokens (50%)

S S S S S S S C K M      S  System prompt                  14k   (7%)
M M M M M M M M M M      C  CLAUDE.md and auto memory       2k   (1%)
M M M M M M M M M M      K  Skill descriptions              2k   (1%)
M M M M M M M M M M      M  Messages and tool results      82k  (41%)
M M M M M M M M M M      .  Free space                    100k  (50%)
. . . . . . . . . .
. . . . . . . . . .
. . . . . . . . . .
. . . . . . . . . .
. . . . . . . . . .`;

export const ENTRIES = [
  // Setup check (WP section 3)
  setup('SU-01', 'WP:50', 'command', 'claude --version'),
  setup('SU-02', 'WP:51', 'text', '/status'),
  setup('SU-03', 'WP:51', 'text', 'Pro/Max/Team/Enterprise'),
  setup('SU-04', 'WP:52', 'command', 'java -version'),
  setup('SU-05', 'WP:53', 'command', 'docker info'),
  setup('SU-06', 'WP:54', 'command', 'which jdtls'),
  setup('SU-07', 'WP:54', 'command', 'Get-Command jdtls'),
  setup('SU-08', 'WP:55', 'command', 'git clone https://github.com/MaikKingma/AgenticBookStore.git && cd AgenticBookStore'),
  setup('SU-09', 'WP:56', 'command', './mvnw verify'),
  setup('SU-10', 'WP:61-66', 'command', 'brew install jdtls'),
  setup('SU-11', 'WP:61-66', 'command', 'yay -S jdtls'),
  setup('SU-12', 'WP:61-66', 'text', 'JDT LS release'),
  setup('SU-13', 'WP:61-66', 'text', 'Use WSL and install as on Linux there.'),
  setup('SU-14', 'WP:61-66', 'text', 'Native Windows is not verified yet.'),
  setup('SU-15', 'WP:68-69', 'text', 'Level 2 Part A'),
  setup('SU-16', 'WP:74-75', 'command', 'export BOOKSTORE_AI_API_KEY=<key on screen>'),
  setup('SU-17', 'WP:74-75', 'command', '$env:BOOKSTORE_AI_API_KEY="<key on screen>"'),
  setup('SU-18', 'WP:78-79', 'text', '/model sonnet'),
  setup('SU-19', 'WP:45-47', 'text', 'Temurin 25'),
  setup('SU-20', 'WP:45-47', 'text', 'Docker Desktop'),
  setup('SU-21', 'WP:45-47', 'text', 'Node.js is not needed'),

  // Level 0 (WP section 4)
  level0('L0-01', 'WP:92-93', 'text', 'look at the onion diagram'),
  level0('L0-02', 'WP:95-98', 'text', 'Add discounts to the bookstore. A book can get a discount percentage. Show the discounted price when people search the catalogue.'),
  level0('L0-03', 'WP:101', 'command', 'claude'),
  level0('L0-04', 'WP:101', 'text', 'permission mode'),
  level0('L0-05', 'WP:102-103', 'text', 'you decide'),
  level0('L0-06', 'WP:104', 'command', './mvnw verify'),
  level0('L0-07', 'WP:105-110', 'command', 'git status'),
  level0('L0-08', 'WP:105-110', 'command', 'git diff --stat'),
  level0('L0-09', 'WP:105-110', 'text', 'Which tests fail'),
  level0('L0-10', 'WP:105-110', 'text', 'Flyway migration'),
  level0('L0-11', 'WP:105-110', 'text', 'for money'),
  level0('L0-12', 'WP:105-110', 'text', 'discount rule live'),
  level0('L0-13', 'WP:105-110', 'text', 'Did it write tests'),
  level0('L0-14', 'WP:105-110', 'text', 'did not verify'),
  level0('L0-15', 'WP:105-110', 'text', 'outside the catalogue'),
  level0('L0-16', 'WP:111-113', 'text', 'observation card'),
  level0('L0-17', 'WP:111-113', 'text', 'went right'),
  level0('L0-18', 'WP:114-116', 'command', 'git restore . && git clean -fd'),
  level0('L0-19', 'WP:114-116', 'text', '`Esc` twice'),
  level0('L0-20', 'WP:114-116', 'text', '/rewind'),
  level0('L0-21', 'WP:114-116', 'text', 'not tracked by checkpoints'),
  level0('L0-22', 'WP:89-90', 'text', 'frontend/'),
  level0('L0-23', 'WP:92-93', 'text', 'ArchitectureTest'),

  // Level 1 (WP section 5, spec 10 scenario)
  level1('L1-01', 'WP:134-135', 'text', 'cross out everything Claude could have read from the code itself'),
  level1('L1-02', 'WP:138-139', 'text', '/init'),
  level1('L1-03', 'WP:140-147', 'command', './mvnw verify'),
  level1('L1-04', 'WP:140-147', 'command', './mvnw -pl bookstore test -Dtest=BookTest'),
  level1('L1-05', 'WP:140-147', 'text', '60 lines'),
  level1('L1-06', 'WP:140-147', 'text', 'without showing output'),
  level1('L1-07', 'WP:140-147', 'text', 'domain is pure Java'),
  level1('L1-08', 'WP:140-147', 'text', 'never double'),
  level1('L1-09', 'WP:140-147', 'text', 'schema changes only via a'),
  level1('L1-10', 'WP:140-147', 'text', 'inbound adapters only talk to flows'),
  level1('L1-11', 'WP:140-147', 'text', 'no Lombok'),
  level1('L1-12', 'WP:140-147', 'text', 'Read docs/architecture.md before adding or moving classes.'),
  level1('L1-13', 'WP:140-147', 'text', '@docs/architecture.md'),
  level1('L1-14', 'WP:148-151', 'text', '.claude/rules/'),
  // Spec 15: the domain rule header is shown as the YAML it is in the file (a list), not as an inline array.
  level1('L1-15', 'WP:148-151', 'text', 'paths:\n  - "bookstore/src/main/java/**/domain/**"'),
  level1('L1-16', 'WP:148-151', 'text', 'paths: ["bookstore/src/main/java/**/data/**", "bookstore/src/main/resources/db/**"]'),
  level1('L1-17', 'WP:148-151', 'text', 'paths: ["**/src/test/**"]'),
  level1('L1-18', 'WP:148-151', 'text', 'domain.md'),
  level1('L1-19', 'WP:148-151', 'text', 'data-and-migrations.md'),
  level1('L1-20', 'WP:148-151', 'text', 'testing.md'),
  level1('L1-21', 'WP:152-155', 'text', '~/.claude/CLAUDE.md'),
  level1('L1-22', 'WP:152-155', 'text', 'CLAUDE.local.md'),
  level1('L1-23', 'WP:152-155', 'text', 'Always use Lombok'),
  level1('L1-24', 'WP:152-155', 'text', '/memory'),
  level1('L1-25', 'WP:152-155', 'text', 'concatenated, not overridden'),
  level1('L1-26', 'WP:156-159', 'text', '/context'),
  level1('L1-27', 'WP:156-159', 'text', '/clear'),
  level1('L1-28', 'WP:156-159', 'text', '/compact focus on <topic>'),
  level1('L1-29', 'WP:156-159', 'text', 'use a subagent to find every place that formats money'),
  level1('L1-30', 'WP:156-159', 'text', '1M-token window'),
  level1('L1-31', 'WP:160', 'text', 'Level 0 task in a fresh session'),
  level1('L1-33', 'WP:140-147', 'text', 'ArchitectureTest'),
  entries('spec10', 'Scenario: Rules load on demand', 'L1')('L1-32', 'S10:113-115', 'text', 'the rule is loaded'),
  // Spec 15 (decision D19), Level 1: the whole rule file, the header sentence, the /context mock with its annotations and
  // the "Make it yours" tip. The two blocks are pinned whole, so that every line of them is covered by the literal sweep.
  level1('L1-34', 'WP:148-151', 'command', LEVEL1_DOMAIN_RULE),
  level1('L1-35', 'WP:148-151', 'text', 'The header between the two `---` lines is YAML frontmatter: `paths` lists globs relative to the project root, and the rule loads only when Claude reads or edits a file that matches one, not at session start.'),
  level1('L1-41', 'WP:148-151', 'text', 'Create `.claude/rules/domain.md` with this exact content:'),
  level1('L1-36', 'WP:156-159', 'text', 'The grid below is an illustration, not a screenshot: your symbols, names and numbers differ.'),
  level1('L1-37', 'WP:156-159', 'command', LEVEL1_CONTEXT_MOCK),
  level1('L1-38', 'WP:156-159', 'text', 'M grows with every prompt, answer, file read and tool result. It is the part you control.'),
  level1('L1-39', 'WP:156-159', 'text', 'Free space shrinks as M grows. Long before it reaches zero, answers drift: that is context rot.'),
  level1('L1-40', 'WP:160', 'text', 'The best harness has a creative engineer who keeps tweaking it. Pick one thing Claude got wrong in Level 0 and write a rule of your own for it, for example `.claude/rules/acl.md` for the distributor client. Skills, hooks and commands of your own can follow in later levels. This is optional.'),

  // Level 2 (WP section 7)
  level2('L2-01', 'WP:182-184', 'text', 'Part A starts with the LSP install check'),
  partA('L2-02', 'WP:190', 'command', 'which jdtls'),
  partA('L2-03', 'WP:190', 'text', 'Windows: inside WSL'),
  partA('L2-04', 'WP:191-194', 'command', '/plugin install jdtls-lsp@claude-plugins-official'),
  partA('L2-05', 'WP:191-194', 'command', '/plugin marketplace add anthropics/claude-plugins-official'),
  partA('L2-06', 'WP:191-194', 'text', 'user scope'),
  partA('L2-07', 'WP:191-194', 'text', 'project scope'),
  partA('L2-08', 'WP:191-194', 'text', '.claude/settings.json'),
  partA('L2-09', 'WP:191-194', 'text', 'Installed'),
  partA('L2-10', 'WP:195-196', 'text', 'Find all references to `Money.of`.'),
  partA('L2-11', 'WP:197-199', 'text', 'In `Book.java`, make `price()` return `String` instead of `Money`. Do not touch the callers yet.'),
  partA('L2-12', 'WP:197-199', 'text', 'Found N new diagnostic issues'),
  partA('L2-13', 'WP:197-199', 'text', 'Fix the new diagnostics.'),
  partA('L2-14', 'WP:200-202', 'text', 'Errors'),
  partA('L2-15', 'WP:200-202', 'command', 'claude --debug'),
  partA('L2-16', 'WP:200-202', 'text', 'Executable not found in $PATH: "jdtls"'),
  partA('L2-17', 'WP:200-202', 'text', 'two minutes'),
  partB('L2-18', 'WP:205', 'command', './mvnw -pl distributor spring-boot:run'),
  partB('L2-19', 'WP:205', 'text', 'java -jar distributor.jar'),
  partB('L2-20', 'WP:207-208', 'command', 'claude mcp add --transport http --scope project distributor http://localhost:8090/mcp'),
  partB('L2-21', 'WP:207-208', 'text', '.mcp.json'),
  partB('L2-22', 'WP:207-208', 'text', '/mcp'),
  partB('L2-23', 'WP:209-210', 'text', 'Which science-fiction titles does the distributor have that we do not sell yet?'),
  partB('L2-24', 'WP:209-210', 'text', 'mcp__distributor__*'),
  partB(
    'L2-25',
    'WP:211-213',
    'text',
    'Using the distributor MCP, find a title that has a translator or illustrator among its contributors. Write an ACL contract test for `DistributorAppServiceImpl` with that exact payload shape and prove that only authors end up in our catalogue.',
  ),
  partB(
    'L2-26',
    'WP:214',
    'text',
    'distributor/src/main/java/.../mcp/CatalogMcpTools.java',
    'distributor/src/main/java/nl/openvalue/distributor/mcp/CatalogMcpTools.java',
  ),
  partB('L2-27', 'WP:216', 'text', 'list_new_releases(days)'),
  entries('spec10', 'Scenario: MCP is available', 'L2')('L2-28', 'S10:209', 'text', 'connected with 4 tools'),

  // Level 3 (WP section 8, spec 10)
  level3('L3-01', 'WP:232-233', 'text', 'the agent reads the stderr and fixes the test'),
  permissions('L3-02', 'WP:236-245', 'command', PERMISSIONS_JSON),
  permissions('L3-32', 'WP:236-245', 'text', '.claude/settings.json'),
  permissions('L3-03', 'WP:246-247', 'text', 'deny, then ask, then allow'),
  permissions('L3-04', 'WP:246-247', 'text', 'Shift+Tab'),
  permissions('L3-05', 'WP:246-247', 'text', '/plan'),
  hooks('L3-06', 'WP:250-259', 'command', HOOKS_JSON),
  hooks('L3-07', 'WP:260-262', 'text', '.claude/hooks/tests-before-commit.sh'),
  hooks('L3-08', 'WP:260-262', 'text', 'chmod +x'),
  hooks('L3-09', 'WP:260-262', 'text', 'last 40 lines'),
  hooks('L3-10', 'WP:260-262', 'text', 'Any other non-zero exit code would NOT block'),
  hooks('L3-11', 'WP:263-264', 'command', 'git config core.hooksPath .githooks'),
  hooks('L3-12', 'WP:263-264', 'text', '.githooks/pre-commit'),
  hooks('L3-13', 'WP:265', 'text', 'change Money to round HALF_UP on purpose and commit'),
  hooks('L3-14', 'WP:266-268', 'text', 'escape hatch'),
  hooks('L3-15', 'WP:270-271', 'text', 'stop_hook_active'),
  hooks('L3-31', 'WP:270-271', 'text', 'ArchitectureTest'),
  hooks('L3-33', 'WP:250-259', 'text', 'PreToolUse'),
  entries('spec10', '`.claude/hooks/tests-before-commit.sh`', 'L3')('L3-16', 'S10:273-295', 'command', TESTS_BEFORE_COMMIT),
  skill('L3-17', 'WP:274-277', 'text', '.claude/skills/new-use-case/SKILL.md'),
  skill('L3-18', 'WP:274-277', 'text', 'reference.md'),
  skill('L3-19', 'WP:274-277', 'text', 'allowed-tools'),
  skill('L3-34', 'WP:274-277', 'text', 'Frontmatter: `name`'),
  skill('L3-30', 'WP:274-277', 'text', 'a precise `description`'),
  skill('L3-20', 'WP:278', 'text', 'Customers must be able to cancel an order that has not shipped yet.'),
  skill('L3-21', 'WP:279-282', 'text', '.claude/commands/ship.md'),
  skill('L3-22', 'WP:279-282', 'text', 'disable-model-invocation: true'),
  skill('L3-23', 'WP:279-282', 'text', '!`git status --short`'),
  skill('L3-24', 'WP:279-282', 'text', '$ARGUMENTS'),
  skill('L3-25', 'WP:279-282', 'text', '/ship'),
  skill('L3-26', 'WP:284-285', 'text', '.claude/agents/architecture-reviewer.md'),
  skill('L3-27', 'WP:284-285', 'text', 'tools: Read, Glob, Grep, Bash'),
  entries('spec10', 'Scenario: Red build blocks the commit', 'L3')('L3-28', 'S10:402-404', 'command', 'git commit -m "test"'),
  entries('spec10', 'Scenario: Escape hatch is closed', 'L3')('L3-29', 'S10:407-408', 'command', 'git commit --no-verify -m "x"'),

  // Level 4 (WP section 9, SDD part 2, the feature briefs)
  level4('L4-01', 'WP:303-307', 'text', 'start/level-4-openspec'),
  level4('L4-02', 'WP:303-307', 'text', 'start/level-4-speckit'),
  level4('L4-03', 'WP:303-307', 'text', 'start/level-4-agentos'),
  level4('L4-04', 'WP:311-312', 'text', 'feature brief'),
  level4('L4-05', 'WP:314-315', 'text', 'hold the same idea in three shapes'),
  common('L4-06', 'SDD:93-97', 'command', 'git stash -u'),
  common('L4-07', 'SDD:93-97', 'command', 'git switch start/level-4-<track>'),
  common('L4-08', 'SDD:93-97', 'text', '/model sonnet'),
  common('L4-65', 'SDD:93-97', 'command', 'claude # new session'),
  common('L4-09', 'SDD:99-102', 'text', 'your own words'),
  common('L4-10', 'SDD:99-102', 'text', 'your contribution as the human'),
  common('L4-63', 'SDD:99-102', 'text', 'built from `solution/level-3`'),
  trackA('L4-11', 'SDD:104-115', 'command', '/opsx:explore'),
  trackA('L4-12', 'SDD:104-115', 'text', 'reviews and ratings'),
  trackA('L4-13', 'SDD:104-115', 'command', '/opsx:propose add-book-reviews'),
  trackA('L4-14', 'SDD:104-115', 'command', 'openspec validate add-book-reviews --strict'),
  trackA('L4-15', 'SDD:104-115', 'text', 'silently ignored'),
  trackA('L4-16', 'SDD:104-115', 'text', 'openspec/changes/add-book-reviews/specs/.../spec.md'),
  trackA('L4-17', 'SDD:104-115', 'command', '/opsx:update'),
  trackA('L4-64', 'SDD:104-115', 'command', '/clear'),
  trackA('L4-18', 'SDD:104-115', 'command', '/opsx:apply'),
  trackA('L4-19', 'SDD:104-115', 'command', '/ship add book reviews'),
  trackA('L4-20', 'SDD:104-115', 'command', '/opsx:archive'),
  trackA('L4-21', 'SDD:104-115', 'text', 'openspec/specs/'),
  trackA('L4-22', 'SDD:104-115', 'text', 'runs in the terminal'),
  trackA('L4-23', 'SDD:104-115', 'text', 'restart Claude Code'),
  trackB('L4-24', 'SDD:117-127', 'command', '/speckit-specify'),
  trackB('L4-25', 'SDD:117-127', 'command', '/speckit-clarify'),
  trackB('L4-26', 'SDD:117-127', 'text', 'up to five questions'),
  trackB('L4-27', 'SDD:117-127', 'command', '/speckit-plan'),
  trackB('L4-28', 'SDD:117-127', 'text', 'Use the existing onion layers, see docs/architecture.md and the constitution.'),
  trackB('L4-29', 'SDD:117-127', 'command', '/speckit-tasks'),
  trackB('L4-30', 'SDD:117-127', 'command', '/speckit-implement'),
  trackB('L4-31', 'SDD:117-127', 'command', '/speckit-converge'),
  trackB('L4-32', 'SDD:117-127', 'command', '/ship add book reviews'),
  trackB('L4-33', 'SDD:117-127', 'text', '/speckit-analyze'),
  trackB('L4-34', 'SDD:117-127', 'text', '.specify/feature.json'),
  trackB('L4-35', 'SDD:117-127', 'text', 'dotted names are documentation notation'),
  trackB('L4-62', 'SDD:117-127', 'text', '/speckit-checklist'),
  trackC('L4-36', 'SDD:129-137', 'text', 'agent-os/standards/'),
  trackC('L4-37', 'SDD:129-137', 'command', '/plan'),
  trackC('L4-38', 'SDD:129-137', 'text', 'Shift+Tab'),
  trackC('L4-39', 'SDD:129-137', 'command', '/shape-spec'),
  trackC('L4-40', 'SDD:129-137', 'text', 'agent-os/specs/'),
  trackC('L4-41', 'SDD:129-137', 'command', '/inject-standards testing'),
  trackC('L4-42', 'SDD:129-137', 'command', '/ship add book reviews'),
  trackC('L4-43', 'SDD:129-137', 'text', 'refuses to run outside plan mode'),
  trackC('L4-44', 'SDD:129-137', 'text', 'WSL or Git Bash'),
  BRIEF_A('L4-45', FB_ROW, 'text', 'Reviews and ratings'),
  BRIEF_A('L4-46', FB_ROW, 'text', 'recommended for most people'),
  BRIEF_A(
    'L4-47',
    FB_ROW,
    'text',
    'Customers keep asking which books are actually good. Let them leave a rating from 1 to 5 stars and a short review for books they bought. Show the average rating in the catalogue search.',
  ),
  BRIEF_A(
    'L4-48',
    FB_ROW,
    'text',
    'Questions your spec should answer: who may review (only customers with a shipped order containing the book?), one review per customer per book or edits allowed, review length, how the average is rounded and shown when there are no reviews, whether discontinued books keep their reviews.',
  ),
  BRIEF_B('L4-49', FB_ROW, 'text', 'Wishlist with back-in-stock notice'),
  BRIEF_B('L4-50', FB_ROW, 'text', 'When a book is out of stock, customers should be able to put it on a wishlist, and we tell them when it is back.'),
  BRIEF_B(
    'L4-51',
    FB_ROW,
    'text',
    'Questions your spec should answer: identify customers by e-mail like orders do?, what "tell them" means here (no real e-mail: record a `NotificationRequested` entry that a query can list), what happens when stock comes back while a restock is pending, limits per customer. Touches the `process` layer: a new domain event when stock becomes available again.',
  ),
  BRIEF_C('L4-52', FB_ROW, 'text', 'Assistant recommends from the distributor'),
  BRIEF_C('L4-53', FB_ROW, 'text', '(advanced)'),
  BRIEF_C(
    'L4-54',
    FB_ROW,
    'text',
    "When we don't have what a customer asks for, the assistant should suggest titles from our distributor that we could order for them.",
  ),
  BRIEF_C(
    'L4-55',
    FB_ROW,
    'text',
    'Questions your spec should answer: how the assistant reaches the distributor, how suggestions are clearly marked as "not in stock, can be ordered", what the assistant must never promise (delivery dates?), how tests stay offline.',
  ),
  BRIEF_C('L4-56', FB_ROW, 'text', 'Two routes, and your spec must pick one explicitly:'),
  BRIEF_C('L4-57', FB_ROW, 'text', 'Within the architecture (recommended):'),
  BRIEF_C(
    'L4-58',
    FB_ROW,
    'text',
    'add a `searchTitles` method to the `DistributorAppService` port, implement it over REST in `acl`, expose it to the model through a new tool that calls a flow. Tests: scripted chat model plus a mocked port.',
  ),
  BRIEF_C('L4-59', FB_ROW, 'text', 'Via MCP:'),
  BRIEF_C(
    'L4-60',
    FB_ROW,
    'text',
    "use Spring AI's MCP client against our distributor MCP server. This breaks two rules on purpose (outbound integration only through `acl`; `CLAUDE.md` says the distributor is reached only through the port), so your spec must include an architecture change: update `docs/architecture.md`, `CLAUDE.md` and the ArchUnit rule with a documented exception. That is a legitimate outcome of a spec, as long as it is explicit.",
  ),
  entries('briefs', 'Facilitator notes', 'L4')(
    'L4-61',
    'FB:41',
    'text',
    'A fits every track in 35 minutes. B is fine for OpenSpec and Agent OS, tight for Spec Kit. C is for people who finished early or know Spring AI.',
  ),

  // Wrap-up (WP section 10, SDD installing)
  wrapUp('WU-01', 'WP:326-332', 'text', 'tree .claude'),
  wrapUp('WU-02', 'WP:326-332', 'text', 'context, behaviour, structure, verification'),
  wrapUp('WU-03', 'WP:326-332', 'text', 'the guide stays online'),
  wrapUp('WU-04', 'WP:326-332', 'text', 'revoked tonight'),
  wrapUp('WU-05', 'WP:329', 'text', 'Claude Code 101'),
  wrapUp('WU-06', 'WP:329', 'text', 'Claude Code in Action'),
  wrapUp('WU-07', 'WP:329', 'text', 'Introduction to agent skills'),
  wrapUp('WU-08', 'WP:329', 'text', 'Introduction to subagents'),
  wrapUp('WU-09', 'WP:329', 'text', 'Introduction to MCP', 'Introduction to Model Context Protocol'),
  wrapUp('WU-10', 'WP:330-331', 'text', 'OpenCode'),
  wrapUp('WU-11', 'WP:330-331', 'text', 'Claude Agent SDK'),
  install('WU-12', 'SDD:146-152', 'command', 'curl -fsSL https://openspec.dev/cli/install.sh | sh'),
  install('WU-13', 'SDD:146-152', 'command', 'irm https://openspec.dev/cli/install.ps1 | iex'),
  install('WU-14', 'SDD:146-152', 'command', 'npm install -g @fission-ai/openspec@latest'),
  install('WU-15', 'SDD:146-152', 'text', 'Node 22.12+'),
  install('WU-16', 'SDD:146-152', 'command', 'openspec init --tools claude'),
  install('WU-17', 'SDD:146-152', 'command', 'curl -LsSf https://astral.sh/uv/install.sh | sh'),
  install('WU-18', 'SDD:146-152', 'command', 'uv tool install specify-cli'),
  install('WU-19', 'SDD:146-152', 'command', 'specify init --here --force --integration claude --script sh'),
  install('WU-20', 'SDD:146-152', 'text', '--script ps'),
  install('WU-21', 'SDD:146-152', 'command', 'git clone https://github.com/buildermethods/agent-os.git ~/agent-os && rm -rf ~/agent-os/.git'),
  install('WU-22', 'SDD:146-152', 'command', '~/agent-os/scripts/project-install.sh'),

  // Index (WP section 2, the agenda)
  agenda('IX-01', 'WP:25-35', 'text', 'Welcome, setup check while people settle'),
  agenda('IX-02', 'WP:25-35', 'text', 'Agentic engineering and the harness'),
  agenda('IX-03', 'WP:25-35', 'text', '1:55 | Break'),
  agenda('IX-04', 'WP:25-35', 'text', 'Wrap-up: who cleared which level, show and tell, Q&A'),
  agenda('IX-05', 'WP:25-35', 'text', '240 minutes'),
  agenda('IX-06', 'WP:25-35', 'text', 'Break the agent'),
  agenda('IX-07', 'WP:25-35', 'text', 'Context engineering'),
  agenda('IX-08', 'WP:25-35', 'text', 'Give your agent eyes (LSP + MCP)'),
  agenda('IX-09', 'WP:25-35', 'text', 'Behavioral control'),
  agenda('IX-10', 'WP:25-35', 'text', 'Spec-driven development'),
  // The Format column: sync minutes went to solo time (spec 15), the level totals did not change.
  agenda('IX-11', 'WP:25-35', 'text', 'Solo 22, debrief 3'),
  agenda('IX-12', 'WP:25-35', 'text', 'Solo 32, debrief 3'),
  agenda('IX-13', 'WP:25-35', 'text', 'Solo 18 (LSP 10, MCP 8), debrief 2'),
  agenda('IX-14', 'WP:25-35', 'text', 'Solo 28 (permissions 4, hooks 14, skill and command 10), debrief 2'),
  agenda('IX-15', 'WP:25-35', 'text', 'Solo 42, debrief 3'),
  agenda('IX-16', 'WP:25-35', 'text', 'During the levels Maik and Tom walk around and help.'),

  // Troubleshooting (WP section 11 and the Level 2 part A, the S11:38 topics)
  risks('TS-01', 'S11:38', 'command', 'docker info'),
  risks('TS-02', 'S11:38', 'command', 'which jdtls'),
  risks('TS-03', 'S11:38', 'text', 'Git for Windows (Git Bash) or WSL2'),
  risks('TS-04', 'S11:38', 'text', 'Agent OS needs bash (WSL or Git Bash)'),
  risks('TS-05', 'S11:38', 'text', '/usage'),
  risks('TS-06', 'S11:38', 'text', '/model sonnet'),
  risks('TS-07', 'S11:38', 'command', './mvnw -pl distributor package'),
  risks(
    'TS-08',
    'S11:38',
    'command',
    'claude mcp add distributor -- java -jar <absolute path>/distributor/target/distributor.jar --spring.profiles.active=stdio',
  ),
  risks('TS-09', 'S11:38', 'text', 'BOOKSTORE_AI_API_KEY'),
  risks('TS-10', 'S11:38', 'text', '/status'),
  risks('TS-11', 'S11:38', 'command', 'git stash -u && git switch solution/level-N', 'git stash -u && git switch solution/level-1'),
  risks('TS-12', 'S11:38', 'text', 'switch to that key'),
  plan(PART_A_LSP, 'TS')('TS-13', 'S11:38', 'command', 'claude --debug'),
  plan(PART_A_LSP, 'TS')('TS-14', 'S11:38', 'text', 'Executable not found in $PATH: "jdtls"'),
  plan(PART_B_MCP, 'TS')('TS-15', 'S11:38', 'command', './mvnw -pl distributor spring-boot:run'),
  plan(PART_B_MCP, 'TS')('TS-16', 'S11:38', 'text', '/mcp'),

  // Cheat sheet (res-3 section 2.9: the commands and files of the day)
  cheat(SETUP)('CS-01', 'cheat sheet rows (section 2.9)', 'text', 'claude --version'),
  cheat(SETUP)('CS-02', 'cheat sheet rows (section 2.9)', 'text', '/status'),
  cheat(SETUP)('CS-03', 'cheat sheet rows (section 2.9)', 'text', '/model sonnet'),
  cheat(RISKS)('CS-04', 'cheat sheet rows (section 2.9)', 'text', '/usage'),
  cheat(LEVEL1)('CS-05', 'cheat sheet rows (section 2.9)', 'text', '/init'),
  cheat(LEVEL1)('CS-06', 'cheat sheet rows (section 2.9)', 'text', '/context'),
  cheat(LEVEL1)('CS-07', 'cheat sheet rows (section 2.9)', 'text', '/memory'),
  cheat(LEVEL1)('CS-08', 'cheat sheet rows (section 2.9)', 'text', '/clear'),
  cheat(LEVEL1)('CS-09', 'cheat sheet rows (section 2.9)', 'text', '/compact focus on <topic>'),
  cheat(LEVEL0)('CS-10', 'cheat sheet rows (section 2.9)', 'text', '/rewind'),
  cheat(LEVEL0)('CS-11', 'cheat sheet rows (section 2.9)', 'text', '`Esc` twice'),
  cheat(LEVEL2)('CS-12', 'cheat sheet rows (section 2.9)', 'text', '/plugin'),
  cheat(LEVEL2)('CS-13', 'cheat sheet rows (section 2.9)', 'text', '/plugin install jdtls-lsp@claude-plugins-official'),
  cheat(LEVEL2)('CS-14', 'cheat sheet rows (section 2.9)', 'text', '/plugin marketplace add anthropics/claude-plugins-official'),
  cheat(LEVEL2)('CS-15', 'cheat sheet rows (section 2.9)', 'text', '/mcp'),
  cheat(LEVEL2)('CS-16', 'cheat sheet rows (section 2.9)', 'text', 'claude mcp add --transport http --scope project distributor http://localhost:8090/mcp'),
  cheat(LEVEL2)('CS-17', 'cheat sheet rows (section 2.9)', 'text', 'claude --debug'),
  cheat(LEVEL3)('CS-18', 'cheat sheet rows (section 2.9)', 'text', 'Shift+Tab'),
  cheat(LEVEL3)('CS-19', 'cheat sheet rows (section 2.9)', 'text', '/plan'),
  cheat(LEVEL3)('CS-20', 'cheat sheet rows (section 2.9)', 'text', '/ship'),
  cheat(LEVEL3)('CS-21', 'cheat sheet rows (section 2.9)', 'text', '$ARGUMENTS'),
  cheat(LEVEL4)('CS-22', 'cheat sheet rows (section 2.9)', 'text', '/opsx:explore'),
  cheat(LEVEL4)('CS-23', 'cheat sheet rows (section 2.9)', 'text', '/opsx:propose'),
  cheat(LEVEL4)('CS-24', 'cheat sheet rows (section 2.9)', 'text', '/opsx:update'),
  cheat(LEVEL4)('CS-25', 'cheat sheet rows (section 2.9)', 'text', '/opsx:apply'),
  cheat(LEVEL4)('CS-26', 'cheat sheet rows (section 2.9)', 'text', '/opsx:archive'),
  cheat(LEVEL4)('CS-27', 'cheat sheet rows (section 2.9)', 'text', 'openspec validate --strict'),
  cheat(LEVEL4)('CS-28', 'cheat sheet rows (section 2.9)', 'text', '/speckit-specify'),
  cheat(LEVEL4)('CS-29', 'cheat sheet rows (section 2.9)', 'text', '/speckit-clarify'),
  cheat(LEVEL4)('CS-30', 'cheat sheet rows (section 2.9)', 'text', '/speckit-plan'),
  cheat(LEVEL4)('CS-31', 'cheat sheet rows (section 2.9)', 'text', '/speckit-tasks'),
  cheat(LEVEL4)('CS-32', 'cheat sheet rows (section 2.9)', 'text', '/speckit-implement'),
  cheat(LEVEL4)('CS-33', 'cheat sheet rows (section 2.9)', 'text', '/speckit-converge'),
  cheat(LEVEL4)('CS-34', 'cheat sheet rows (section 2.9)', 'text', '/shape-spec'),
  cheat(LEVEL4)('CS-35', 'cheat sheet rows (section 2.9)', 'text', '/inject-standards'),
  cheat(LEVEL1)('CS-36', 'cheat sheet rows (section 2.9)', 'text', 'CLAUDE.md'),
  cheat(LEVEL1)('CS-37', 'cheat sheet rows (section 2.9)', 'text', '~/.claude/CLAUDE.md'),
  cheat(LEVEL1)('CS-38', 'cheat sheet rows (section 2.9)', 'text', 'CLAUDE.local.md'),
  cheat(LEVEL1)('CS-39', 'cheat sheet rows (section 2.9)', 'text', '.claude/rules/'),
  cheat(LEVEL1)('CS-40', 'cheat sheet rows (section 2.9)', 'text', 'docs/architecture.md'),
  cheat(LEVEL3)('CS-41', 'cheat sheet rows (section 2.9)', 'text', '.claude/settings.json'),
  cheat(LEVEL3)('CS-42', 'cheat sheet rows (section 2.9)', 'text', '.claude/skills/'),
  cheat(LEVEL3)('CS-43', 'cheat sheet rows (section 2.9)', 'text', 'SKILL.md'),
  cheat(LEVEL3)('CS-44', 'cheat sheet rows (section 2.9)', 'text', '.claude/commands/ship.md'),
  cheat(LEVEL3)('CS-45', 'cheat sheet rows (section 2.9)', 'text', '.claude/agents/architecture-reviewer.md'),
  cheat(LEVEL3)('CS-46', 'cheat sheet rows (section 2.9)', 'text', '.claude/hooks/tests-before-commit.sh'),
  cheat(LEVEL3)('CS-47', 'cheat sheet rows (section 2.9)', 'text', '.githooks/pre-commit'),
  cheat(LEVEL3)('CS-48', 'cheat sheet rows (section 2.9)', 'text', 'git config core.hooksPath .githooks'),
  cheat(LEVEL3)('CS-49', 'cheat sheet rows (section 2.9)', 'text', 'stop_hook_active'),
  cheat(LEVEL3)('CS-50', 'cheat sheet rows (section 2.9)', 'text', '$CLAUDE_PROJECT_DIR'),
  cheat(LEVEL3)('CS-51', 'cheat sheet rows (section 2.9)', 'text', 'deny, then ask, then allow'),
  cheat(LEVEL2)('CS-52', 'cheat sheet rows (section 2.9)', 'text', '.mcp.json'),
];

// Rows of the research table that the script does not match, with the reason. The verifiers
// tick each of them by reading the page. page is a key of PAGE_FILES.
const LEVEL_CLEARED = 'enforced by the structure rule: the checklist equals the plan bullets, in order and count';
export const MANUAL_ROWS = [
  {row: 'WP:117-119', item: 'L0 Level cleared when', page: 'L0', reason: LEVEL_CLEARED},
  {row: 'WP:162-166', item: 'L1 Level cleared when (3 bullets)', page: 'L1', reason: LEVEL_CLEARED},
  {row: 'WP:218-221', item: 'L2 Level cleared when (3 bullets)', page: 'L2', reason: LEVEL_CLEARED},
  {row: 'WP:287-290', item: 'L3 Level cleared when (3 bullets)', page: 'L3', reason: LEVEL_CLEARED},
  {row: 'WP:317-321', item: 'L4 Level cleared when (4 bullets)', page: 'L4', reason: LEVEL_CLEARED},
  {row: 'WP:111-113', item: 'observation card shape', page: 'L0', reason: 'enforced by the content rule (h): fenced markdown block, header, three rows, "What went right:", hint, notes/level-0.md'},
  {row: 'S11:34', item: 'who cleared which level (links to each #level-cleared-when) and the recap map', page: 'WU', reason: 'prose of spec 11, which is no manifest source; the verifier checks the links and the recap against the plan'},
  {row: 'S11:38', item: 'the five troubleshooting topics and their prose', page: 'TS', reason: 'the eight anchors are pinned by the structure rule; the topic prose is a verifier read against the plan risks table, with no invented command'},
  {row: 'AS:19-30, 32-41, 45-55, 65-73, 158', item: 'onion text block, packages, nine rules with the process exception, naming suffixes, ArchitectureTest as the referee', page: 'AR', reason: 'the architecture standard is not a manifest source; tables and diagrams, the verifier traces each rule and suffix to the standard and checks that no enforced package list is claimed'},
  {row: 'WP:329', item: 'URLs of the five Anthropic Academy courses, OpenCode and the Agent SDK overview', page: 'WU', reason: 'the URLs are not in the pack (research res-3 section 2.8 fetched them); the verifier requests each with a GET that follows redirects'},
  {row: 'WP:56', item: '"green, under a minute" promise of ./mvnw verify', page: 'SU', reason: 'deliberately not required: the build may take longer after spec 12 (research C12), so the page must not promise a number the plan does not state'},
  {row: 'S10:273-295', item: 'optional Stop hook script quote (spec 10 lines 297-314)', page: 'L3', reason: 'optional on the page (the exercise does not need it); a quote is compared line by line by the verifier'},
  {row: 'S10:402-404', item: 'checkpoints without a pack-stated command: .env read denied, skill selected automatically', page: 'L3', reason: 'prose scenarios of spec 10 (lines 410-417); the verifier traces each Expected line to them'},
  {row: 'WP:140-147', item: 'the CLAUDE.md content list as steps', page: 'L1', reason: 'the key literals are entries; the step order and wording are a verifier read'},
  {row: 'WP:191-194', item: 'plugin declaration sentences (project scope "as on solution/level-2", L3 Part A "on solution/level-2 it declares the jdtls-lsp plugin")', page: 'L2', reason: 'deliberately not required: they depend on the spec 10 re-verification of the plugin declaration (follow-up F4); the verifier checks that the page states no declaration behaviour without a pack source'},
];
