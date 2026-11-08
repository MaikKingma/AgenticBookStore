import type {Config} from '@docusaurus/types';
import config from './docusaurus.config';

// The offline build: one index.html with a hash router and relative assets, so
// the unzipped folder opens from file://. It skips static rendering and checks
// no links, so it only ever runs after the Pages build (npm run build) passed.
// It overrides baseUrl and the router and nothing else.
const offlineConfig: Config = {
  ...config,
  baseUrl: '/',
  future: {
    ...config.future,
    experimental_router: 'hash',
  },
};

export default offlineConfig;
