// The contrast pairs of the guide, by variable name only: this file holds no colour
// value. Every variable named here must be declared in src/css/custom.css with a hex
// value or a single var() reference. A pair is checked in light mode and, when dark
// mode is on, in dark mode. kind 'text' needs 4.5:1, kind 'nontext' needs 3:1.
//
// Admonitions: Docusaurus maps note to the Infima type secondary, tip to success, and
// caution to warning (an alias with its own label), so the caution pairs name the
// warning variables on purpose. The note border is decorative (title and icon carry
// the type), so it has no pair.

const text = (name, fg, bg) => ({name, fg, bg, kind: 'text'});
const nontext = (name, fg, bg) => ({name, fg, bg, kind: 'nontext'});

// [label, Infima colour name]
const ADMONITIONS = [
  ['note', 'secondary'],
  ['tip', 'success'],
  ['info', 'info'],
  ['warning', 'warning'],
  ['danger', 'danger'],
  ['caution', 'warning'],
];

// The label (as used by the code tint variable) of each Infima colour name.
const TINT = {secondary: 'note', success: 'tip', info: 'info', warning: 'warning', danger: 'danger'};

// Admonition types whose border carries a pair (see the header: note has none).
const BORDERED = new Set(['tip', 'info', 'warning', 'danger', 'caution']);

const admonitionPairs = ADMONITIONS.flatMap(([label, colour]) => {
  const bg = `--ifm-color-${colour}-contrast-background`;
  const fg = `--ifm-color-${colour}-contrast-foreground`;
  const pairs = [
    text(`admonition ${label} text`, fg, bg),
    text(`admonition ${label} inline code`, fg, `--guide-alert-${TINT[colour]}-code`),
  ];
  if (BORDERED.has(label)) pairs.push(nontext(`admonition ${label} border`, `--ifm-color-${colour}-dark`, bg));
  return pairs;
});

export const CONTRAST_PAIRS = [
  text('body text', '--ifm-font-color-base', '--ifm-background-color'),
  text('secondary text', '--ifm-font-color-secondary', '--ifm-background-color'),
  text('headings', '--ifm-heading-color', '--ifm-background-color'),
  text('links', '--ifm-link-color', '--ifm-background-color'),
  text('link hover', '--ifm-link-hover-color', '--ifm-background-color'),
  text('primary', '--ifm-color-primary', '--ifm-background-color'),
  text('inline code', '--ifm-font-color-base', '--ifm-code-background'),
  text('inline code in a heading', '--ifm-heading-color', '--ifm-code-background'),
  text('inline code in a link', '--ifm-link-color', '--ifm-code-background'),
  text('code block text', '--prism-color', '--prism-background-color'),
  text('code block syntax colour', '--guide-code-accent', '--prism-background-color'),
  text('code block highlighted line', '--prism-color', '--docusaurus-highlighted-code-line-bg'),
  text('code block syntax colour on a highlighted line', '--guide-code-accent', '--docusaurus-highlighted-code-line-bg'),
  text('navbar text', '--ifm-navbar-link-color', '--ifm-navbar-background-color'),
  text('navbar hover', '--ifm-navbar-link-hover-color', '--ifm-navbar-background-color'),
  text('navbar active', '--ifm-navbar-link-active-color', '--ifm-navbar-background-color'),
  text('mobile menu text', '--guide-navbar-menu-color', '--ifm-navbar-background-color'),
  text('mobile menu hover', '--guide-navbar-menu-color', '--guide-navbar-menu-bg'),
  text('mobile menu active', '--guide-navbar-menu-active', '--guide-navbar-menu-bg'),
  text('sidebar menu', '--ifm-menu-color', '--ifm-background-color'),
  text('sidebar menu hover', '--ifm-menu-color', '--ifm-menu-color-background-hover'),
  text('sidebar menu active', '--ifm-menu-color-active', '--ifm-menu-color-background-active'),
  text('breadcrumb active', '--ifm-breadcrumb-color-active', '--ifm-breadcrumb-item-background-active'),
  text('tabs', '--ifm-tabs-color', '--ifm-background-color'),
  text('tabs hover', '--ifm-tabs-color', '--ifm-hover-overlay'),
  text('tabs active', '--ifm-tabs-color-active', '--ifm-background-color'),
  text('table of contents', '--ifm-toc-link-color', '--ifm-background-color'),
  text('table stripe', '--ifm-font-color-base', '--ifm-table-stripe-background'),
  text('block quote', '--ifm-blockquote-color', '--ifm-background-color'),
  text('keyboard key', '--ifm-color-emphasis-800', '--ifm-color-emphasis-0'),
  ...admonitionPairs,
  text('level progress current label', '--guide-level-current-fg', '--guide-level-current-bg'),
  text('level progress inactive label', '--guide-level-inactive-fg', '--guide-level-inactive-bg'),
  nontext('navbar accent line', '--guide-navbar-accent', '--ifm-navbar-background-color'),
  nontext('navbar toggle on hover', '--ifm-navbar-link-color', '--guide-navbar-hover-bg'),
  nontext('navbar logo', '--guide-navbar-logo', '--ifm-navbar-background-color'),
  nontext('level progress current fill', '--guide-level-current-bg', '--ifm-background-color'),
  nontext('level progress current outline', '--guide-level-current-border', '--ifm-background-color'),
];
