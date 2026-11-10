import {readdirSync, readFileSync} from 'node:fs';
import {join} from 'node:path';

// Folders at the top of guide/ that are installed or generated, never authored.
const SKIPPED_TOP_LEVEL = new Set(['node_modules', 'build', 'build-offline', '.docusaurus']);

// Font and image binaries, skipped by extension. A file with a NUL byte is binary too.
const BINARY_EXTENSIONS = new Set([
  '.woff',
  '.woff2',
  '.ttf',
  '.otf',
  '.eot',
  '.png',
  '.jpg',
  '.jpeg',
  '.gif',
  '.webp',
  '.ico',
  '.zip',
]);

function isBinaryName(name) {
  const dot = name.lastIndexOf('.');
  return dot !== -1 && BINARY_EXTENSIONS.has(name.slice(dot).toLowerCase());
}

function walk(root, relDir, out) {
  const entries = readdirSync(join(root, relDir), {withFileTypes: true});
  for (const entry of entries) {
    const rel = relDir === '' ? entry.name : `${relDir}/${entry.name}`;
    if (entry.isDirectory()) {
      if (relDir === '' && SKIPPED_TOP_LEVEL.has(entry.name)) continue;
      walk(root, rel, out);
    } else if (entry.isFile() && !isBinaryName(entry.name)) {
      out.push(rel);
    }
  }
}

// Every text file under root, as {file, text}, sorted by posix relative path.
// Invalid UTF-8 decodes to the replacement character, which the ASCII rule flags.
export function collectTextFiles(root) {
  const rels = [];
  walk(root, '', rels);
  rels.sort();
  const files = [];
  for (const file of rels) {
    const bytes = readFileSync(join(root, file));
    if (bytes.includes(0)) continue;
    files.push({file, text: bytes.toString('utf8')});
  }
  return files;
}
