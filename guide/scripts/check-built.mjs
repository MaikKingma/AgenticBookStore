// Entry point of `npm run check:built`. It scans the Pages build (build/ by default)
// and needs `npm run build` to have run first. Usage: node scripts/check-built.mjs [build-dir]
// The navbar rule (rules/navbar-built.mjs) describes the real site, so it runs only on the
// default build dir; a build dir given on the command line (the fixture builds of the
// self-tests) is scanned without it.
import {existsSync, readFileSync} from 'node:fs';
import {fileURLToPath, pathToFileURL} from 'node:url';
import {scanBuilt} from './rules/built.mjs';
import {baseUrlOf} from './rules/navbar-built.mjs';

const CONFIG = fileURLToPath(new URL('../docusaurus.config.ts', import.meta.url));

function main() {
  const explicit = process.argv[2];
  const dir = explicit ?? fileURLToPath(new URL('../build', import.meta.url));
  const configErrors = [];
  let options = {};
  if (explicit === undefined) {
    const base = existsSync(CONFIG) ? baseUrlOf(readFileSync(CONFIG, 'utf8')) : undefined;
    if (base === undefined) {
      configErrors.push('navbar: docusaurus.config.ts has no readable baseUrl, so the title link of the navbar cannot be checked');
    } else {
      options = {navbar: {base}};
    }
  }
  const {errors: scanErrors, count, checkboxes, windowsLines, navbarPages} = scanBuilt(dir, options);
  const errors = [...configErrors, ...scanErrors];
  if (errors.length > 0) {
    for (const error of errors) console.error(error);
    console.error(`check:built failed with ${errors.length} error(s)`);
    process.exit(1);
  }
  console.log(`ok   built: ${count} html files, no literal ":::", ${checkboxes} checkboxes, all named`);
  console.log(`ok   built: ${windowsLines} Windows PowerShell lines in tab panels and PowerShell code blocks, all pack command lines`);
  if (options.navbar) {
    console.log(`ok   navbar: ${navbarPages} pages, each with the logo link, the title link, the GitHub link and the LinkedIn link`);
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main();
}
