// The file reads behind the coverage and content rules. The rules themselves are pure
// functions in scripts/rules/coverage.mjs and content.mjs; this module reads every page
// under docs/ and the five pack files of the manifest, and hands them over.
import {existsSync, readdirSync, readFileSync} from 'node:fs';
import {join, relative, resolve, sep} from 'node:path';
import {ENTRIES, MANUAL_ROWS, PAGE_FILES, SOURCE_FILES} from '../coverage.mjs';
import {contentErrors} from '../rules/content.mjs';
import {coverageErrors} from '../rules/coverage.mjs';

// The .md and .mdx pages under docs/ as {path relative to guide/: text}.
function readPages(root) {
  const pages = {};
  const docs = join(root, 'docs');
  if (!existsSync(docs)) return pages;
  for (const entry of readdirSync(docs, {recursive: true, withFileTypes: true})) {
    if (!entry.isFile() || !/\.mdx?$/.test(entry.name)) continue;
    const abs = join(entry.parentPath, entry.name);
    pages[relative(root, abs).split(sep).join('/')] = readFileSync(abs, 'utf8');
  }
  return pages;
}

// The five pack files of SOURCE_FILES as {source: text, or null when the file is missing}.
// workshopDir is the pack's workshop folder; the pack root is the folder above it.
function readPack(workshopDir, sourceFiles) {
  const packRoot = resolve(workshopDir, '..');
  return Object.fromEntries(
    Object.entries(sourceFiles).map(([source, rel]) => {
      const path = join(packRoot, rel);
      return [source, existsSync(path) ? readFileSync(path, 'utf8') : null];
    }),
  );
}

// Runs the coverage and the content rules. root: the guide folder. workshopDir: the pack's
// workshop folder. manifest: {entries, manualRows, sourceFiles, pageFiles}, the real one unless a
// self-test passes its own. Returns {coverage, content}: each {errors, summary}, and for the
// coverage also the lines that list the pending entries per skeleton page.
export function contentRules(root, workshopDir, manifest = {}) {
  const {entries = ENTRIES, manualRows = MANUAL_ROWS, sourceFiles = SOURCE_FILES, pageFiles = PAGE_FILES} = manifest;
  const pages = readPages(root);
  const pack = readPack(workshopDir, sourceFiles);

  const result = coverageErrors({entries, manualRows, sourceFiles, pageFiles, pack, pages});
  const pendingPages = Object.keys(result.pending).sort();
  const pendingCount = pendingPages.reduce((sum, file) => sum + result.pending[file].length, 0);
  const lines = [`pending coverage entries: ${pendingCount} on ${pendingPages.length} skeleton pages`];
  for (const file of pendingPages) {
    const ids = [...result.pending[file]].sort();
    lines.push(`  pending entries ${file}: ${ids.length} (${ids.join(', ')})`);
  }

  const content = contentErrors({pages, packTexts: Object.values(pack)});
  return {
    coverage: {
      errors: result.errors,
      lines,
      summary: `anchored ${result.anchored}/${result.total}, ${manualRows.length} manual rows, ${pendingCount} entries pending`,
    },
    content,
  };
}
