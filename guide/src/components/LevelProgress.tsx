import type {ReactNode} from 'react';
import clsx from 'clsx';
import styles from './LevelProgress.module.css';

const LEVELS = [0, 1, 2, 3, 4];

type Props = {
  /** The level of the page: an integer from 0 to 4. */
  current: number;
};

// A static level progress indicator: five steps, the current one marked. It holds no
// state and reads no storage. A value outside 0 to 4 throws, so a wrong page fails the
// build (the static render runs this function for every page).
export default function LevelProgress({current}: Props): ReactNode {
  if (!Number.isInteger(current) || current < 0 || current > 4) {
    throw new RangeError(`LevelProgress: current must be an integer from 0 to 4, got ${String(current)}`);
  }
  return (
    <nav aria-label="Level progress" className={styles.progress}>
      <ol className={styles.steps}>
        {LEVELS.map((level) => (
          <li
            key={level}
            className={clsx(styles.step, level === current && styles.current)}
            aria-current={level === current ? 'step' : undefined}>
            {`Level ${level}`}
          </li>
        ))}
      </ol>
    </nav>
  );
}
