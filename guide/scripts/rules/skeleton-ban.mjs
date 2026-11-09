// The skeleton marker ban (spec 11, T11). While the guide was built, a page that still carried the
// marker was exempt from body coverage; a finished guide has no marker anywhere, so any page that
// still carries it is an error here, never a warning.
import {SKELETON_MARKER} from './structure.mjs';

// Pure rule. files: {file, text} as collectTextFiles returns them (posix paths relative to guide/).
// Reads only docs/** and asks the same question as the skeleton exemption (text.includes of
// SKELETON_MARKER): the checker's own sources and fixtures mention the marker by design.
export function skeletonBanErrors(files) {
  return files
    .filter(({file, text}) => file.startsWith('docs/') && text.includes(SKELETON_MARKER))
    .map(({file}) => file)
    .sort()
    .map((file) => `skeleton: ${file}: the skeleton marker ${SKELETON_MARKER} must be removed, a finished page carries none`);
}
