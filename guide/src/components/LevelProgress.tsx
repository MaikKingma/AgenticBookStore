import type {ReactNode} from 'react';
import Link from '@docusaurus/Link';
import styles from './LevelProgress.module.css';

// The route of each level page (the docs routes of this guide, see the file names under docs/).
// <Link> adds the base URL and the hash router of the offline build, and the build fails when a
// route does not exist, so a renamed page cannot leave a dead crumb.
const LEVEL_ROUTES = [
  '/level-0-break-the-agent',
  '/level-1-context-engineering',
  '/level-2-give-your-agent-eyes',
  '/level-3-behavioral-control',
  '/level-4-spec-driven-development',
];

type Props = {
  /** The level of the page: an integer from 0 to 4. */
  current: number;
};

type State = 'done' | 'current' | 'upcoming';

const stateOf = (level: number, current: number): State => (level < current ? 'done' : level === current ? 'current' : 'upcoming');

// A static level progress breadcrumb: five chevron-shaped crumbs, each a link to its level page.
// The chevron is two empty spans before the link (an outline layer and the fill layer: real elements
// with real background colours, so axe can read the colour under each label); the link itself is
// never clipped, so its focus ring shows in full. Levels before the current one are done: an empty
// aria-hidden check mark element (the stylesheet draws the check character as an escape in its
// content, so the markup holds no glyph) and a visually hidden "completed". The current level is the
// link with aria-current="step". The component holds no state and reads no storage. data-state and
// data-part are the stable hooks (tests and the built rule never read the hashed class names). A
// value outside 0 to 4 throws, so a wrong page fails the build (the static render runs this
// function for every page).
export default function LevelProgress({current}: Props): ReactNode {
  if (!Number.isInteger(current) || current < 0 || current > 4) {
    throw new RangeError(`LevelProgress: current must be an integer from 0 to 4, got ${String(current)}`);
  }
  return (
    <nav aria-label="Level progress" className={styles.progress}>
      <ol className={styles.steps}>
        {LEVEL_ROUTES.map((route, level) => {
          const state = stateOf(level, current);
          return (
            <li key={level} className={styles.step} data-state={state}>
              <span aria-hidden="true" data-part="edge" className={styles.edge} />
              <span aria-hidden="true" data-part="fill" className={styles.fill} />
              <Link to={route} className={styles.link} aria-current={state === 'current' ? 'step' : undefined}>
                {state === 'done' && <span aria-hidden="true" data-part="check" className={styles.check} />}
                <span>{`Level ${level}`}</span>
                {state === 'done' && <span className={styles.status}>{' (completed)'}</span>}
              </Link>
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
