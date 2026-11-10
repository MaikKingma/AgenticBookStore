import type {PrismTheme} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';
import rehypeTaskListLabels from './src/plugins/rehype-task-list-labels.mjs';

// An empty Prism theme on purpose: a theme would put colour values and inline styles
// into code blocks. The code colours are variables in src/css/custom.css, the only
// file that holds colour values.
const brandPrismTheme: PrismTheme = {plain: {}, styles: []};

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

// This is the GitHub Pages build and the gate: it renders every page and checks
// every link and anchor. The offline build (docusaurus.offline.config.ts) spreads
// this config and changes only baseUrl and the router.
const config: Config = {
  title: 'Crafting your Agentic Harness',

  // Keep this flag on. Turning it off makes the Docusaurus 3 title syntax of
  // admonitions fail silently.
  future: {
    v4: true,
  },

  url: 'https://maikkingma.github.io',
  baseUrl: '/AgenticBookStore/',
  trailingSlash: false,

  organizationName: 'MaikKingma',
  projectName: 'AgenticBookStore',

  onBrokenLinks: 'throw',
  onBrokenAnchors: 'throw',

  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'throw',
      onBrokenMarkdownImages: 'throw',
    },
  },

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          routeBasePath: '/',
          sidebarPath: './sidebars.ts',
          // Gives every "- [ ]" checkbox an accessible name (aria-label = item text).
          // The offline config spreads this config, so it inherits the plugin.
          rehypePlugins: [rehypeTaskListLabels],
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: 'Crafting your Agentic Harness',
      items: [],
    },
    prism: {
      theme: brandPrismTheme,
      darkTheme: brandPrismTheme,
      additionalLanguages: ['bash', 'powershell', 'java', 'json', 'yaml'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
