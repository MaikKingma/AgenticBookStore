// Entry point of `npm run check:built`. It scans the Pages build (build/ by default)
// and needs `npm run build` to have run first. Usage: node scripts/check-built.mjs [build-dir]
import {fileURLToPath, pathToFileURL} from 'node:url';
import {scanBuilt} from './rules/built.mjs';

function main() {
  const dir = process.argv[2] ?? fileURLToPath(new URL('../build', import.meta.url));
  const {errors, count, checkboxes, windowsLines} = scanBuilt(dir);
  if (errors.length > 0) {
    for (const error of errors) console.error(error);
    console.error(`check:built failed with ${errors.length} error(s)`);
    process.exit(1);
  }
  console.log(`ok   built: ${count} html files, no literal ":::", ${checkboxes} checkboxes, all named`);
  console.log(`ok   built: ${windowsLines} Windows PowerShell lines in tab panels and PowerShell code blocks, all pack command lines`);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main();
}
