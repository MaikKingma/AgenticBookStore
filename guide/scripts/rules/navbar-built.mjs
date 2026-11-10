// The navbar rule on the rendered pages (spec 14, T1). It reads the server-rendered HTML of the
// Pages build (`npm run check:built`, default build dir only) and asserts what a visitor gets:
// - the OpenValue logo is its own link, in front of the title: href exactly https://openvalue.eu,
//   target _blank, rel with noopener and noreferrer, an accessible name; the slate logo file for
//   light mode and the white one for dark mode, 32 px high, under the base URL; the logo is not
//   inside the title link;
// - the title link goes to the guide home page (the base URL) and has no target;
// - the right side has the GitHub link and the "Follow on LinkedIn" link, each with target _blank,
//   rel with noopener and noreferrer, and an accessible name that says what it is.
// The mobile sidebar is not in the server-rendered HTML; the browser rig covers it.
import {elementTree, textContent} from '../lib/html.mjs';

export const LOGO_HREF = 'https://openvalue.eu';
export const TITLE = 'Crafting your Agentic Harness';
// The logo file shown per colour mode: the slate file on the light navbar, the white file on the slate one.
const LOGO_FILES = {light: 'openvalue-logo-dark.png', dark: 'openvalue-logo-light.png'};
const LOGO_HEIGHT = '32';
const EXTERNAL = [
  {name: 'GitHub', href: 'https://github.com/MaikKingma/AgenticBookStore', says: /GitHub/i},
  {name: 'LinkedIn', href: 'https://www.linkedin.com/in/maik-kingma/', says: /LinkedIn/i},
];

// The baseUrl of docusaurus.config.ts as written in its source, or undefined.
export function baseUrlOf(configText) {
  return /\bbaseUrl\s*:\s*(['"])([^'"]*)\1/.exec(configText)?.[2];
}

// The href of a link as written in the HTML, read by the HTML rule for unquoted values: the value
// runs to the next space or ">", so a trailing slash belongs to it (href=/AgenticBookStore/>).
// The shared scanner (lib/html.mjs) drops that slash, because it must read type=checkbox/> too,
// and so cannot tell the base URL /AgenticBookStore/ from /AgenticBookStore. The first href wins.
function hrefOf(html, node) {
  const tag = /^<a\b((?:[^>"']|"[^"]*"|'[^']*')*)>/.exec(html.slice(node.offset));
  const match = tag && /\shref\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'>]+))/.exec(tag[1]);
  return match ? (match[1] ?? match[2] ?? match[3]) : undefined;
}

const classesOf = (node) => (node.attrs.get('class') ?? '').split(/\s+/).filter(Boolean);
const hasClass = (node, name) => classesOf(node).includes(name);

// Every element below a node, in document order.
function descendants(node, out = []) {
  for (const child of node.children) {
    if ('text' in child) continue;
    out.push(child);
    descendants(child, out);
  }
  return out;
}

const hasAncestor = (node, test) => {
  for (let parent = node.parent; parent && parent.name !== '#root'; parent = parent.parent) {
    if (test(parent)) return true;
  }
  return false;
};

const insideRight = (node) => hasAncestor(node, (parent) => hasClass(parent, 'navbar__items--right'));

// aria-label wins; otherwise the text of the link and the alt text of its images.
function accessibleName(link) {
  const label = (link.attrs.get('aria-label') ?? '').trim();
  if (label !== '') return label;
  const alts = descendants(link)
    .filter((node) => node.name === 'img')
    .map((node) => (node.attrs.get('alt') ?? '').trim());
  return [textContent(link).trim(), ...alts].filter((part) => part !== '').join(' ');
}

// What every external link of the navbar must have: target _blank, rel holding the two tokens.
function externalLinkErrors(prefix, what, link) {
  const errors = [];
  const target = link.attrs.get('target');
  if (target !== '_blank') errors.push(`${prefix} ${what} target must be _blank (found ${target ?? 'none'})`);
  const rel = (link.attrs.get('rel') ?? '').toLowerCase().split(/\s+/).filter(Boolean);
  if (!rel.includes('noopener') || !rel.includes('noreferrer')) {
    errors.push(`${prefix} ${what} rel must hold noopener and noreferrer (found ${link.attrs.get('rel') ?? 'none'})`);
  }
  return errors;
}

// file: the page path for the message; html: its text; base: the base URL of the build (for
// example /AgenticBookStore/). Returns the error lines of one page.
export function navbarErrors({file, html, base}) {
  const prefix = `navbar: ${file}:`;
  const nav = descendants(elementTree(html)).find((node) => node.name === 'nav' && hasClass(node, 'navbar'));
  if (!nav) return [`${prefix} no navbar (a nav element with the class navbar) found`];

  const errors = [];
  const links = descendants(nav).filter((node) => node.name === 'a');
  const hasImage = (link) => descendants(link).some((node) => node.name === 'img');
  const title = links.find((link) => hasClass(link, 'navbar__brand'));

  // The title link.
  if (!title) {
    errors.push(`${prefix} no title link (a link with the class navbar__brand) in the navbar`);
  } else {
    const href = hrefOf(html, title);
    if (href !== base) errors.push(`${prefix} title link href must be the base URL ${base} (found ${href ?? 'none'})`);
    if (title.attrs.has('target')) {
      errors.push(`${prefix} title link must not have a target (found ${title.attrs.get('target')})`);
    }
    const text = textContent(title).trim();
    if (text !== TITLE) errors.push(`${prefix} title must be "${TITLE}" (found "${text}")`);
    if (hasImage(title) || descendants(title).some((node) => node.name === 'a')) {
      errors.push(`${prefix} the logo is inside the title link; it must be a link of its own`);
    }
  }

  // The logo link: a link around images that is not the title link.
  const logos = links.filter((link) => link !== title && hasImage(link));
  if (logos.length === 0) errors.push(`${prefix} no logo link (a link around the logo images) in the navbar`);
  if (logos.length > 1) errors.push(`${prefix} more than one logo link in the navbar (found ${logos.length})`);
  for (const logo of logos.slice(0, 1)) {
    const href = hrefOf(html, logo);
    if (href !== LOGO_HREF) errors.push(`${prefix} logo link href must be exactly ${LOGO_HREF} (found ${href ?? 'none'})`);
    errors.push(...externalLinkErrors(prefix, 'logo link', logo));
    if (accessibleName(logo) === '') {
      errors.push(`${prefix} logo link has no accessible name (aria-label, or the alt text of an image)`);
    }
    if (insideRight(logo)) errors.push(`${prefix} logo link must be in the left part of the navbar`);
    if (title && logo.offset > title.offset) errors.push(`${prefix} logo link must come before the title link`);
    const images = descendants(logo).filter((node) => node.name === 'img');
    for (const [mode, fileName] of Object.entries(LOGO_FILES)) {
      const image = images.find((node) => classesOf(node).some((name) => name.startsWith(`themedComponent--${mode}`)));
      if (!image) {
        errors.push(`${prefix} ${mode} mode logo image is missing (class themedComponent--${mode})`);
        continue;
      }
      const expected = `${base}img/${fileName}`;
      const src = image.attrs.get('src');
      if (src !== expected) errors.push(`${prefix} ${mode} mode logo image must be ${expected} (found ${src ?? 'none'})`);
      const height = image.attrs.get('height');
      if (height !== LOGO_HEIGHT) {
        errors.push(`${prefix} ${mode} mode logo image must have height ${LOGO_HEIGHT} (found ${height ?? 'none'})`);
      }
    }
  }

  // GitHub and LinkedIn on the right.
  for (const {name, href, says} of EXTERNAL) {
    const found = links.filter((link) => hrefOf(html, link) === href);
    if (found.length === 0) errors.push(`${prefix} no ${name} link (href ${href}) in the navbar`);
    for (const link of found) {
      errors.push(...externalLinkErrors(prefix, `${name} link`, link));
      if (!says.test(accessibleName(link))) {
        errors.push(`${prefix} ${name} link has no accessible name that says ${name} (aria-label or text)`);
      }
      if (!insideRight(link)) errors.push(`${prefix} ${name} link must be in the right part of the navbar`);
    }
  }
  return errors;
}
