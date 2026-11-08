// Shared fixtures for the node:test self-tests. Not a test file itself.
import {readFileSync, readdirSync} from 'node:fs';
import {join, dirname} from 'node:path';
import {fileURLToPath} from 'node:url';

const GUIDE_ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const FONT_DIR = 'src/css/fonts';

// The woff2 files of the real site, as {file name: Buffer}.
export function goodFontFiles() {
  return Object.fromEntries(
    readdirSync(join(GUIDE_ROOT, FONT_DIR))
      .filter((name) => name.endsWith('.woff2'))
      .sort()
      .map((name) => [name, readFileSync(join(GUIDE_ROOT, FONT_DIR, name))]),
  );
}

// The brand files of the real site, as {path relative to guide/: text or Buffer}: the
// stylesheet, the config, the README with the provenance, the licence and the fonts.
// A fixture tree that holds them passes the brand, contrast and font rules.
export function goodSiteFiles() {
  const text = (rel) => readFileSync(join(GUIDE_ROOT, rel), 'utf8');
  const files = {
    'src/css/custom.css': text('src/css/custom.css'),
    'docusaurus.config.ts': text('docusaurus.config.ts'),
    'README.md': text('README.md'),
    'static/fonts/OFL.txt': text('static/fonts/OFL.txt'),
  };
  for (const [name, buffer] of Object.entries(goodFontFiles())) files[`${FONT_DIR}/${name}`] = buffer;
  return files;
}

// A package.json equal to the allowlist, with the check script, as a fresh object.
export function goodPackage() {
  return {
    scripts: {check: 'node --test "scripts/**/*.test.mjs" && node scripts/check.mjs'},
    dependencies: {
      '@docusaurus/core': '3.10.2',
      '@docusaurus/faster': '3.10.2',
      '@docusaurus/preset-classic': '3.10.2',
      '@mdx-js/react': '^3.0.0',
      clsx: '^2.0.0',
      'prism-react-renderer': '^2.3.0',
      react: '^19.0.0',
      'react-dom': '^19.0.0',
    },
    devDependencies: {
      '@docusaurus/module-type-aliases': '3.10.2',
      '@docusaurus/tsconfig': '3.10.2',
      '@docusaurus/types': '3.10.2',
      '@types/react': '^19.0.0',
      typescript: '~6.0.2',
    },
  };
}
