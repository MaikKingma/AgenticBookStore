// The image provenance rule (spec 14, decision D-S6), a mirror of the font provenance rule:
// every file under static/img/, in any folder and with any extension, has a README.md line that
// holds both its name (its path below static/img/) and its sha256. One line, so a hash that
// sits next to another file's name does not count. A tree without images has nothing to prove.
import {createHash} from 'node:crypto';

const sha256 = (buffer) => createHash('sha256').update(buffer).digest('hex');

// True when the line holds the name as a whole token: not as the tail of a longer name
// such as "other-x.png" for "x.png". A path separator before the name is fine.
function mentions(line, name) {
  for (let at = line.indexOf(name); at !== -1; at = line.indexOf(name, at + 1)) {
    const before = line[at - 1];
    const after = line[at + name.length];
    if (!/[\w.-]/.test(before ?? ' ') && !/[\w.-]/.test(after ?? ' ')) return true;
  }
  return false;
}

// images: [{name, bytes}] with name relative to static/img/ and forward slashes; readme: the
// README.md text or undefined.
export function logoErrors({images, readme}) {
  if (images.length === 0) return [];
  if (readme === undefined) {
    return ['logo: README.md is missing, so the provenance of the images in static/img/ cannot be checked'];
  }
  const lines = readme.split('\n');
  const errors = [];
  for (const {name, bytes} of images) {
    const hash = sha256(bytes);
    if (!lines.some((line) => line.includes(hash) && mentions(line, name))) {
      errors.push(`logo: README.md has no line with both the name and the sha256 ${hash} of static/img/${name}`);
    }
  }
  return errors;
}
