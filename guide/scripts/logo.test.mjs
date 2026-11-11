// Spec 14, T1: the logo assets and their provenance. Two groups of tests: the pure rule
// (rules/logo.mjs: every file under static/img/ has a README line with its name and sha256),
// wired through runChecks, and the facts of the two committed PNGs (decoded with node:zlib).
import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {readFileSync} from 'node:fs';
import {dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';
import {inflateSync} from 'node:zlib';
import {runChecks} from './check.mjs';
import {cssVariables, parseHex, resolveColor} from './rules/contrast.mjs';
import {logoErrors} from './rules/logo.mjs';
import {replaceOnce, withSiteTree} from './lib/test-fixtures.mjs';

const GUIDE_ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const IMG_DIR = join(GUIDE_ROOT, 'static', 'img');
const LIGHT = 'openvalue-logo-light.png';
const DARK = 'openvalue-logo-dark.png';

const sha256 = (buffer) => createHash('sha256').update(buffer).digest('hex');
const realImage = (name) => readFileSync(join(IMG_DIR, name));
const startingWith = (list, prefix) => list.filter((entry) => entry.startsWith(prefix));

// The images of the real site as the rule takes them: [{name, bytes}], name relative to static/img/.
const goodImages = () => [LIGHT, DARK].map((name) => ({name, bytes: realImage(name)}));
const rowFor = (name, bytes) => `| ${name} | Shift Left, Look Right.pdf | ${sha256(bytes)} |`;
const goodReadme = () => ['# Guide', '', ...goodImages().map(({name, bytes}) => rowFor(name, bytes)), ''].join('\n');

// --- the pure rule -------------------------------------------------------------------------

test('logo rule: every image has a README line with its name and its sha256', () => {
  assert.deepEqual(logoErrors({images: goodImages(), readme: goodReadme()}), []);
});

test('logo rule: no image at all is no error (a tree without static/img has nothing to prove)', () => {
  assert.deepEqual(logoErrors({images: [], readme: goodReadme()}), []);
});

test('logo rule: a removed README row is an error that names the file and its hash', () => {
  const images = goodImages();
  const readme = goodReadme()
    .split('\n')
    .filter((line) => !line.includes(DARK))
    .join('\n');
  assert.deepEqual(logoErrors({images, readme}), [
    `logo: README.md has no line with both the name and the sha256 ${sha256(images[1].bytes)} of static/img/${DARK}`,
  ]);
});

test('logo rule: one changed hex digit in a README hash is an error', () => {
  const images = goodImages();
  const hash = sha256(images[0].bytes);
  const flipped = (hash[0] === 'a' ? 'b' : 'a') + hash.slice(1);
  const readme = replaceOnce(goodReadme(), hash, flipped);
  assert.deepEqual(logoErrors({images, readme}), [
    `logo: README.md has no line with both the name and the sha256 ${hash} of static/img/${LIGHT}`,
  ]);
});

test('logo rule: a hash on the last digit changed is an error too', () => {
  const images = goodImages();
  const hash = sha256(images[1].bytes);
  const flipped = hash.slice(0, -1) + (hash.endsWith('0') ? '1' : '0');
  const readme = replaceOnce(goodReadme(), hash, flipped);
  assert.equal(logoErrors({images, readme}).length, 1);
});

test('logo rule: the hashes of the two images swapped between their rows are two errors', () => {
  const [light, dark] = goodImages();
  const readme = ['# Guide', rowFor(LIGHT, dark.bytes), rowFor(DARK, light.bytes), ''].join('\n');
  assert.equal(logoErrors({images: [light, dark], readme}).length, 2);
});

test('logo rule: the name on one line and the hash on another is an error', () => {
  const [light, dark] = goodImages();
  const readme = ['# Guide', `| ${LIGHT} |`, `| ${sha256(light.bytes)} |`, rowFor(DARK, dark.bytes), ''].join('\n');
  assert.deepEqual(logoErrors({images: [light, dark], readme}), [
    `logo: README.md has no line with both the name and the sha256 ${sha256(light.bytes)} of static/img/${LIGHT}`,
  ]);
});

test('logo rule: a name that only ends the name of another file does not count', () => {
  const bytes = Buffer.from('not a real png');
  const readme = `| other-${'x.png'} | ${sha256(bytes)} |\n`;
  assert.equal(logoErrors({images: [{name: 'x.png', bytes}], readme}).length, 1);
});

for (const name of ['sub/x.svg', 'X.PNG', 'deep/er/logo.jpeg', 'noextension']) {
  test(`logo rule: a third image ${name} without a README row is an error`, () => {
    const bytes = Buffer.from(`<svg>${name}</svg>`);
    assert.deepEqual(logoErrors({images: [...goodImages(), {name, bytes}], readme: goodReadme()}), [
      `logo: README.md has no line with both the name and the sha256 ${sha256(bytes)} of static/img/${name}`,
    ]);
  });
}

test('logo rule: a missing README is an error, with images present', () => {
  assert.deepEqual(logoErrors({images: goodImages(), readme: undefined}), [
    'logo: README.md is missing, so the provenance of the images in static/img/ cannot be checked',
  ]);
});

// --- wired through runChecks ---------------------------------------------------------------

const goodTreeFiles = () => ({[`static/img/${LIGHT}`]: realImage(LIGHT), [`static/img/${DARK}`]: realImage(DARK)});

test('runChecks: the two real logos pass the logo rule and the ok line states the exact count', () => {
  withSiteTree(goodTreeFiles(), (root) => {
    const {lines, errors} = runChecks(root);
    assert.deepEqual(startingWith(errors, 'logo:'), []);
    assert.equal(startingWith(lines, 'ok   logo (').length, 1, lines.join('\n'));
    assert.match(startingWith(lines, 'ok   logo (')[0], /^ok {3}logo \(2 images[,)]/);
  });
});

test('runChecks: an image in a nested folder with any extension without a README row fails the logo rule', () => {
  for (const name of ['sub/x.svg', 'X.PNG']) {
    withSiteTree({...goodTreeFiles(), [`static/img/${name}`]: Buffer.from(name)}, (root) => {
      const {lines, errors} = runChecks(root);
      assert.deepEqual(startingWith(errors, 'logo:'), [
        `logo: README.md has no line with both the name and the sha256 ${sha256(Buffer.from(name))} of static/img/${name}`,
      ]);
      assert.equal(startingWith(lines, 'FAIL logo (1 errors)').length, 1, lines.join('\n'));
    });
  }
});

test('runChecks: with images present, a missing README fails the logo rule', () => {
  withSiteTree({...goodTreeFiles(), 'README.md': null}, (root) => {
    const {errors} = runChecks(root);
    assert.equal(startingWith(errors, 'logo:').length, 1, errors.join('\n'));
  });
});

test('runChecks: a changed logo file (new bytes, old README hash) fails the logo rule', () => {
  const changed = Buffer.concat([realImage(LIGHT), Buffer.from([0])]);
  withSiteTree({...goodTreeFiles(), [`static/img/${LIGHT}`]: changed}, (root) => {
    assert.deepEqual(startingWith(runChecks(root).errors, 'logo:'), [
      `logo: README.md has no line with both the name and the sha256 ${sha256(changed)} of static/img/${LIGHT}`,
    ]);
  });
});

// --- the committed PNGs --------------------------------------------------------------------

// A minimal PNG decoder for 8-bit RGBA, non-interlaced files (enough for these two files).
function decodePng(buffer) {
  assert.deepEqual([...buffer.subarray(0, 8)], [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a], 'PNG signature');
  let offset = 8;
  let header;
  const idat = [];
  while (offset < buffer.length) {
    const length = buffer.readUInt32BE(offset);
    const type = buffer.toString('latin1', offset + 4, offset + 8);
    const body = buffer.subarray(offset + 8, offset + 8 + length);
    if (type === 'IHDR') {
      header = {
        width: body.readUInt32BE(0),
        height: body.readUInt32BE(4),
        depth: body[8],
        colourType: body[9],
        interlace: body[12],
      };
    }
    if (type === 'IDAT') idat.push(body);
    offset += 12 + length;
  }
  assert.ok(header, 'IHDR');
  assert.equal(header.depth, 8);
  assert.equal(header.colourType, 6, 'RGBA');
  assert.equal(header.interlace, 0);
  const raw = inflateSync(Buffer.concat(idat));
  const stride = header.width * 4;
  const pixels = Buffer.alloc(stride * header.height);
  for (let y = 0; y < header.height; y++) {
    const filter = raw[y * (stride + 1)];
    for (let x = 0; x < stride; x++) {
      const value = raw[y * (stride + 1) + 1 + x];
      const left = x >= 4 ? pixels[y * stride + x - 4] : 0;
      const up = y > 0 ? pixels[(y - 1) * stride + x] : 0;
      const upLeft = x >= 4 && y > 0 ? pixels[(y - 1) * stride + x - 4] : 0;
      const paeth = () => {
        const p = left + up - upLeft;
        const [pa, pb, pc] = [Math.abs(p - left), Math.abs(p - up), Math.abs(p - upLeft)];
        return pa <= pb && pa <= pc ? left : pb <= pc ? up : upLeft;
      };
      const predictor = [0, left, up, (left + up) >> 1, paeth()][filter];
      assert.notEqual(predictor, undefined, `PNG filter ${filter}`);
      pixels[y * stride + x] = (value + predictor) & 255;
    }
  }
  return {...header, pixels};
}

const channels = ({width, height, pixels}) => {
  const out = {rgb: new Set(), alpha: Buffer.alloc(width * height)};
  for (let i = 0; i < width * height; i++) {
    out.rgb.add(`${pixels[i * 4]},${pixels[i * 4 + 1]},${pixels[i * 4 + 2]}`);
    out.alpha[i] = pixels[i * 4 + 3];
  }
  return out;
};

test('the logo PNGs: 422x77 RGBA, alpha 0 in all four corners, identical alpha channels, one colour each', () => {
  const light = decodePng(realImage(LIGHT));
  const dark = decodePng(realImage(DARK));
  for (const image of [light, dark]) {
    assert.equal(image.width, 422);
    assert.equal(image.height, 77);
    for (const [x, y] of [[0, 0], [421, 0], [0, 76], [421, 76]]) {
      assert.equal(image.pixels[(y * 422 + x) * 4 + 3], 0, `corner ${x},${y}`);
    }
  }
  const [lightChannels, darkChannels] = [channels(light), channels(dark)];
  assert.ok(lightChannels.alpha.equals(darkChannels.alpha), 'the alpha channels are byte-identical');
  assert.ok(lightChannels.alpha.some((a) => a > 0 && a < 255), 'anti-aliased edges are kept');
  assert.ok(lightChannels.alpha.some((a) => a === 255), 'the glyphs are opaque');
  assert.deepEqual([...lightChannels.rgb], ['255,255,255']);
  assert.deepEqual([...darkChannels.rgb], ['29,36,44']);
});

// The logo shown in light mode is the slate file, in dark mode the white file; the pair variable
// --guide-navbar-logo names that colour per mode, so the contrast of the navbar logo is the
// contrast of the real asset.
test('the logo PNG shown in each mode has the colour of --guide-navbar-logo in that mode', () => {
  const {light, dark} = cssVariables(readFileSync(join(GUIDE_ROOT, 'src', 'css', 'custom.css'), 'utf8'));
  const modes = {light, dark: new Map([...light, ...dark])};
  const shown = {light: DARK, dark: LIGHT};
  for (const [mode, file] of Object.entries(shown)) {
    const [rgb] = channels(decodePng(realImage(file))).rgb;
    const [r, g, b] = rgb.split(',').map(Number);
    const resolved = resolveColor('--guide-navbar-logo', modes[mode]);
    assert.deepEqual(parseHex(resolved.hex), {r, g, b}, `${mode} mode shows ${file}`);
  }
});

test('the README records the provenance of each logo: deck, page 1, image object, soft mask, recolour, no resampling', () => {
  const readme = readFileSync(join(GUIDE_ROOT, 'README.md'), 'utf8');
  for (const name of [LIGHT, DARK]) {
    const lines = readme.split('\n').filter((line) => line.includes(name) && line.includes(sha256(realImage(name))));
    assert.equal(lines.length, 1, `${name}: one README line with its name and sha256`);
    for (const token of ['Shift Left, Look Right.pdf', 'page 1', '/Im4', 'soft mask', 'recoloured', 'no resampling']) {
      assert.ok(lines[0].includes(token), `${name}: the README line says "${token}"`);
    }
    assert.ok(!/\/(Users|home)\//.test(lines[0]) && !lines[0].includes('\\'), `${name}: the line holds no local path`);
  }
});
