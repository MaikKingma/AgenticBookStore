// Fixtures for the structure self-tests. Not a test file itself. Everything here is
// written out by hand on purpose: the expected titles, header values, anchors and file
// names are a second copy of the contract, so a wrong constant in the rule module
// cannot grade itself.
import {mkdtempSync, mkdirSync, rmSync, writeFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {dirname, join} from 'node:path';
import {withSiteTree} from './test-fixtures.mjs';

const MARKER = '{/* guide-skeleton */}';

// The five levels as a small workshop plan would hold them.
export const FIXTURE_LEVELS = [
  {
    name: 'Break the agent',
    title: 'Level 0: Break the agent',
    file: 'docs/01-level-0-break-the-agent.mdx',
    minutes: 25,
    goal: 'experience the failure modes on a codebase where the agent knows nothing about our conventions.',
    items: [
      'Your observation card lists at least three failure modes, each with evidence (the failing test, the ArchUnit output or the wrong file).',
    ],
    start: '`main` (tag `level-0-start`)',
    catchUp: 'git stash -u && git switch -C level-0 level-0-start',
    compare: null,
    anchors: [],
  },
  {
    name: 'Context engineering',
    title: 'Level 1: Context engineering',
    file: 'docs/02-level-1-context-engineering.mdx',
    minutes: 35,
    goal: 'give the agent the smallest set of high-signal context that makes the right thing the obvious thing.',
    items: [
      '`CLAUDE.md` has at most 60 lines.',
      '`/context` shows the path-scoped rules loading only for matching files.',
      'In a fresh session, the Level 0 task ends with `./mvnw verify` green, shown in the terminal, and no ArchUnit violation.',
    ],
    start: '`level-0-start`',
    catchUp: 'git stash -u && git switch -C level-1 level-0-start',
    compare: 'solution/level-1',
    anchors: [],
  },
  {
    name: 'Give your agent eyes (LSP + MCP)',
    title: 'Level 2: Give your agent eyes (LSP + MCP)',
    file: 'docs/03-level-2-give-your-agent-eyes.mdx',
    minutes: 20,
    goal: 'let the agent see what you see: the code through a language server, and real external data through our own MCP server, instead of guessing.',
    items: [
      'After a deliberate type error, Claude Code reports new diagnostics and fixes them.',
      '`/mcp` shows the distributor connected.',
      'The ACL contract test built from real MCP data is green.',
    ],
    start: 'your Level 1 result, or `solution/level-1`',
    catchUp: 'git stash -u && git switch solution/level-1',
    compare: 'solution/level-2',
    anchors: [
      ['Part A: LSP', 'part-a-lsp'],
      ['Part B: MCP', 'part-b-mcp'],
      ['Stretch', 'stretch'],
    ],
  },
  {
    name: 'Behavioral control (permissions, hooks, skills, commands, subagents)',
    title: 'Level 3: Behavioral control',
    file: 'docs/04-level-3-behavioral-control.mdx',
    minutes: 30,
    goal: 'turn the things that must happen every time into guarantees, and package repeatable work.',
    items: [
      'A deliberately red commit is blocked by the hook (exit 2) and then fixed by the agent.',
      '`git commit --no-verify` is denied.',
      'The cancel-order use case built with the skill is committed through `/ship`.',
    ],
    start: 'your Level 2 result, or `solution/level-2`',
    catchUp: 'git stash -u && git switch solution/level-2',
    compare: 'solution/level-3',
    anchors: [
      ['Part A: Permissions', 'part-a-permissions'],
      ['Part B: Hooks', 'part-b-hooks'],
      ['Optional: Stop hook', 'optional-stop-hook'],
      ['Part C: Skill and command', 'part-c-skill-and-command'],
      ['Stretch', 'stretch'],
    ],
  },
  {
    name: 'Spec-driven development',
    title: 'Level 4: Spec-driven development',
    file: 'docs/05-level-4-spec-driven-development.mdx',
    minutes: 45,
    goal: 'turn a vague request into a spec, a plan and tasks, then let the agent implement and verify it inside the harness you built.',
    items: [
      'Spec artefacts committed next to the code.',
      '`./mvnw verify` green, including `ArchitectureTest`, shown in the terminal (not claimed).',
      'Commit made through your `/ship` command and passed the test-gate hook.',
      'Every acceptance scenario in your spec maps to a test (ask the agent to list the mapping).',
    ],
    start: '`start/level-4-<track>`',
    catchUp: 'git stash -u && git switch start/level-4-<track>',
    compare: null,
    anchors: [
      ['Tracks', 'tracks'],
      ['Feature briefs', 'feature-briefs'],
    ],
  },
];

export const TROUBLESHOOTING_ANCHORS = [
  ['Windows hooks', 'windows-hooks'],
  ['jdtls not starting', 'jdtls-not-starting'],
  ['Usage limits', 'usage-limits'],
  ['MCP not connecting', 'mcp-not-connecting'],
  ['The `ANTHROPIC_API_KEY` pitfall', 'anthropic-api-key'],
  ['Offline copy shows a blank page', 'offline-copy-blank-page'],
  ['Docker is not running', 'docker-not-running'],
  ['I am far behind', 'far-behind'],
];

// Breaks text at spaces so that no line is longer than width (a word is never split).
function wrap(text, width, indent) {
  const lines = [];
  let line = '';
  for (const word of text.split(' ')) {
    if (line !== '' && line.length + 1 + word.length > width) {
      lines.push(line);
      line = indent + word;
    } else {
      line = line === '' ? word : `${line} ${word}`;
    }
  }
  lines.push(line);
  return lines.join('\n');
}

// A workshop plan with the five level sections. The goals and the bullets wrap over
// continuation lines like the real plan, and one table row quotes "Level cleared when".
export function fixturePlan(levels = FIXTURE_LEVELS) {
  const sections = levels.map(
    (l, n) => `## ${n + 4}. Level ${n}: ${l.name} (${l.minutes} min)

${wrap(`**Goal:** ${l.goal}`, 60, '')}

**Start:** somewhere. Catch-up / compare: \`solution/level-${n}\`.

**Steps**
1. Do the thing. Level cleared when you are done is not a heading here.
2. Look at \`x\`.

**Level cleared when**
${l.items.map((item) => `- ${wrap(item, 70, '  ')}`).join('\n')}

**Debrief (3 min):** talk.
`,
  );
  return `# Workshop Plan

| Decision | Choice | Why |
|---|---|---|
| Structure | Levels 0 to 4 | Each level ends with a check: "Level cleared when". |

## 3. Setup check

Some words.

${sections.join('\n')}
## 10. Wrap-up (15 min)
- Show of hands.
`;
}

// The SDD framework guide with its "Level cleared when (all tracks)" list.
export function fixtureSdd(items = FIXTURE_LEVELS[4].items) {
  return `# SDD framework guide

### Level cleared when (all tracks)
${items.map((item) => `- ${wrap(item, 70, '  ')}`).join('\n')}

---

## Installing a framework yourself
- Not a level list.
`;
}

const quote = (title) => `"${title}"`;

// A level page that follows the page grammar. options.skeleton keeps the marker.
export function levelPage(n, {skeleton = true} = {}) {
  const l = FIXTURE_LEVELS[n];
  const lines = [
    '---',
    `title: ${quote(l.title)}`,
    `sidebar_position: ${n + 3}`,
    '---',
    '',
    "import LevelProgress from '@site/src/components/LevelProgress';",
    '',
    `<LevelProgress current={${n}} />`,
    '',
    ':::note[At a glance]',
    '',
    `**Goal:** ${l.goal.charAt(0).toUpperCase()}${l.goal.slice(1)}`,
    '',
    `**Start branch:** ${l.start}`,
    '',
    '**Catch-up:**',
    '',
    '```bash',
    l.catchUp,
    '```',
    '',
    `**Time:** ${l.minutes} minutes`,
  ];
  if (l.compare) lines.push('', `**Compare:** \`${l.compare}\``);
  lines.push('', ':::', '');
  if (skeleton) lines.push(MARKER, '');
  for (const [text, id] of l.anchors) lines.push(`## ${text} {/* #${id} */}`, '');
  lines.push('## Level cleared when', '', ...l.items.map((item) => `- [ ] ${item}`), '');
  return lines.join('\n');
}

const OTHER_PAGES = [
  ['docs/index.md', 'Welcome', 1, '/'],
  ['docs/00-setup-check.md', 'Setup check', 2],
  ['docs/06-wrap-up.md', 'Wrap-up', 8],
  ['docs/reference/cheat-sheet.md', 'Cheat sheet', 1],
  ['docs/reference/architecture.md', 'Architecture', 2],
  ['docs/reference/troubleshooting.md', 'Troubleshooting', 3],
];

// A non-level page. The marker is always present: these pages are skeletons.
export function otherPage(file) {
  const [, title, position, slug] = OTHER_PAGES.find(([f]) => f === file);
  const lines = ['---', `title: ${title}`, ...(slug ? [`slug: ${slug}`] : []), `sidebar_position: ${position}`, '---', '', MARKER, ''];
  if (file.endsWith('troubleshooting.md')) {
    for (const [text, id] of TROUBLESHOOTING_ANCHORS) lines.push(`## ${text} {/* #${id} */}`, '');
  }
  return lines.join('\n');
}

// All the files of a valid guide docs folder, as {path relative to guide/: text}.
export function goodDocs() {
  const files = {};
  FIXTURE_LEVELS.forEach((l, n) => {
    files[l.file] = levelPage(n);
  });
  for (const [file] of OTHER_PAGES) files[file] = otherPage(file);
  files['docs/reference/_category_.json'] = '{\n  "label": "Reference",\n  "position": 9\n}\n';
  return files;
}

// Runs fn(root, workshopDir) on a temp guide that holds the good docs plus `files` (a
// null content removes a file) and a temp workshop folder holding the fixture plan and
// SDD guide, overridable with pack = {plan, sdd}.
export function withStructureTree(files, fn, pack = {}) {
  const workshop = mkdtempSync(join(tmpdir(), 'guide-workshop-test-'));
  try {
    const packFiles = {
      'workshop-plan.md': 'plan' in pack ? pack.plan : fixturePlan(),
      'sdd-framework-guide.md': 'sdd' in pack ? pack.sdd : fixtureSdd(),
    };
    for (const [name, content] of Object.entries(packFiles)) {
      if (content === null) continue;
      const abs = join(workshop, name);
      mkdirSync(dirname(abs), {recursive: true});
      writeFileSync(abs, content);
    }
    return withSiteTree({...goodDocs(), ...files}, (root) => fn(root, workshop));
  } finally {
    rmSync(workshop, {recursive: true, force: true});
  }
}
