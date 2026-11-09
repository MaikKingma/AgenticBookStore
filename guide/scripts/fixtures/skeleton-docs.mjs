// The skeleton snapshot for the node:test self-tests (see skeleton-docs/README.md). Not a test file itself.
import {copyFileSync, cpSync, mkdirSync, mkdtempSync, realpathSync, rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {basename, dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';
import {SOURCE_FILES} from '../coverage.mjs';

const HERE = dirname(fileURLToPath(import.meta.url));
const GUIDE_ROOT = join(HERE, '..', '..');
const PACK = join(GUIDE_ROOT, '..', 'docs', 'spec-pack');

// guide/docs at 05e58a2: the eleven skeleton pages and reference/_category_.json, byte for byte.
export const SKELETON_DOCS = join(HERE, 'skeleton-docs', 'docs');

// The top-level folders of guide/ a copy leaves out: the live docs, and what is installed or generated.
const LEFT_OUT = new Set(['docs', 'node_modules', 'build', 'build-offline', '.docusaurus']);

// A scratch copy of the guide whose docs/ is the snapshot: every other file of guide/ (the
// checker scripts included), and the five pack files at ../docs/spec-pack, where the checker
// finds the pack relative to itself. Runs fn(guideDir) and removes the copy again. The path is
// the real one (macOS tmpdir is a symlink): check.mjs runs main() only when its argv path equals
// its own module URL, so `node <link path>/check.mjs` would print nothing and exit 0.
export function withSkeletonGuideCopy(fn) {
  const base = realpathSync(mkdtempSync(join(tmpdir(), 'guide-skeleton-copy-')));
  try {
    const guide = join(base, 'guide');
    cpSync(GUIDE_ROOT, guide, {recursive: true, filter: (src) => dirname(src) !== GUIDE_ROOT || !LEFT_OUT.has(basename(src))});
    cpSync(SKELETON_DOCS, join(guide, 'docs'), {recursive: true});
    for (const rel of Object.values(SOURCE_FILES)) {
      const target = join(base, 'docs', 'spec-pack', rel);
      mkdirSync(dirname(target), {recursive: true});
      copyFileSync(join(PACK, rel), target);
    }
    return fn(guide);
  } finally {
    rmSync(base, {recursive: true, force: true});
  }
}
