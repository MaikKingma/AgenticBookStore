// The file reads behind the structure rules. The rules themselves are pure functions in
// scripts/rules/structure.mjs and plan.mjs; this module reads every file under docs/
// and the two pack files (the workshop plan and the SDD framework guide).
import {existsSync, readdirSync, readFileSync} from 'node:fs';
import {join, relative, sep} from 'node:path';
import {structureErrors} from '../rules/structure.mjs';

const PLAN = 'workshop-plan.md';
const SDD = 'sdd-framework-guide.md';

function readPack(workshopDir, name) {
  const path = join(workshopDir, name);
  return {path, text: existsSync(path) ? readFileSync(path, 'utf8') : null};
}

// Runs the structure rules. root: the guide folder. workshopDir: the pack's workshop
// folder, found relative to the checker file by check.mjs. Returns {errors, summary}.
export function structureRules(root, workshopDir) {
  const files = {};
  const nulFiles = [];
  const docs = join(root, 'docs');
  if (existsSync(docs)) {
    for (const entry of readdirSync(docs, {recursive: true, withFileTypes: true})) {
      if (!entry.isFile()) continue;
      const abs = join(entry.parentPath, entry.name);
      const rel = relative(root, abs).split(sep).join('/');
      const bytes = readFileSync(abs);
      if (bytes.includes(0)) nulFiles.push(rel);
      files[rel] = bytes.toString('utf8');
    }
  }
  const plan = readPack(workshopDir, PLAN);
  const sdd = readPack(workshopDir, SDD);
  const result = structureErrors({
    files,
    nulFiles: nulFiles.sort(),
    planText: plan.text,
    planPath: plan.path,
    sddText: sdd.text,
    sddPath: sdd.path,
  });
  if (!existsSync(docs)) result.errors.unshift('structure: docs/ is missing');
  return result;
}
