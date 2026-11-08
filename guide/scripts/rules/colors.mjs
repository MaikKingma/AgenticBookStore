// The colour-literal rule: brand colours live in src/css/custom.css and nowhere else.
// Outside that file no stylesheet may hold a colour literal in a property value, and
// no style prop (JSX object or HTML attribute) of a .tsx, .jsx, .mdx or .md file may.
// A colour literal is a hex value, a colour function or a CSS named colour. inherit,
// currentColor and transparent are not literals. Markdown anchors and heading ids
// such as [x](#cafe) are not style props, so they are never read.
import {blank, matchingBrace, position, withoutBlockComments} from '../lib/text.mjs';

const NAMED_COLORS = `aliceblue antiquewhite aqua aquamarine azure beige bisque black blanchedalmond blue blueviolet
brown burlywood cadetblue chartreuse chocolate coral cornflowerblue cornsilk crimson cyan darkblue darkcyan
darkgoldenrod darkgray darkgreen darkgrey darkkhaki darkmagenta darkolivegreen darkorange darkorchid darkred
darksalmon darkseagreen darkslateblue darkslategray darkslategrey darkturquoise darkviolet deeppink deepskyblue
dimgray dimgrey dodgerblue firebrick floralwhite forestgreen fuchsia gainsboro ghostwhite gold goldenrod gray green
greenyellow grey honeydew hotpink indianred indigo ivory khaki lavender lavenderblush lawngreen lemonchiffon
lightblue lightcoral lightcyan lightgoldenrodyellow lightgray lightgreen lightgrey lightpink lightsalmon
lightseagreen lightskyblue lightslategray lightslategrey lightsteelblue lightyellow lime limegreen linen magenta
maroon mediumaquamarine mediumblue mediumorchid mediumpurple mediumseagreen mediumslateblue mediumspringgreen
mediumturquoise mediumvioletred midnightblue mintcream mistyrose moccasin navajowhite navy oldlace olive olivedrab
orange orangered orchid palegoldenrod palegreen paleturquoise palevioletred papayawhip peachpuff peru pink plum
powderblue purple rebeccapurple red rosybrown royalblue saddlebrown salmon sandybrown seagreen seashell sienna
silver skyblue slateblue slategray slategrey snow springgreen steelblue tan teal thistle tomato turquoise violet
wheat white whitesmoke yellow yellowgreen`.split(/\s+/);

const COLOR_FUNCTIONS = ['rgb', 'rgba', 'hsl', 'hsla', 'hwb', 'lab', 'lch', 'oklab', 'oklch', 'color', 'color-mix'];

// Not part of a longer word or custom property name: a letter, digit, underscore or
// hyphen before or after rules the match out (so --my-red-tint is not "red").
const BOUNDARY_BEFORE = '(?<![\\w-])';
const HEX = /#(?:[0-9a-f]{8}|[0-9a-f]{6}|[0-9a-f]{3,4})(?![0-9a-z_-])/gi;
const FUNCTION_CALL = new RegExp(`${BOUNDARY_BEFORE}(${COLOR_FUNCTIONS.join('|')})\\s*\\(`, 'gi');
const NAMED = new RegExp(`${BOUNDARY_BEFORE}(${NAMED_COLORS.join('|')})(?![\\w-])`, 'gi');

const HOME = 'colours belong in src/css/custom.css only; use a var(--...) reference, inherit, currentColor or transparent';

// The colour literals of one value text with their offsets, in order of appearance.
function colorHits(text) {
  const value = blank(text, /url\([^)]*\)/gi);
  const hits = [];
  for (const [regex, pick] of [
    [HEX, (m) => m[0]],
    [FUNCTION_CALL, (m) => `${m[1]}(`],
    [NAMED, (m) => m[1]],
  ]) {
    for (const match of value.matchAll(regex)) hits.push({at: match.index, token: pick(match)});
  }
  return hits.sort((a, b) => a.at - b.at);
}

// The colour literals of one value text, in order of appearance.
export function colorTokens(value) {
  return colorHits(value).map((hit) => hit.token);
}

// Colour literals in the property values of a stylesheet.
export function cssColorErrors(file, text) {
  let css = withoutBlockComments(text);
  css = blank(css, /"(?:[^"\\\n]|\\.)*"|'(?:[^'\\\n]|\\.)*'/g);
  css = blank(css, /url\([^)]*\)/gi); // a data URI may hold ; and { that would split a declaration
  const errors = [];
  for (const declaration of css.matchAll(/([-\w]+)\s*:([^;{}]*)(?=[;}]|$)/g)) {
    const valueStart = declaration.index + declaration[0].length - declaration[2].length;
    for (const hit of colorHits(declaration[2])) {
      errors.push(`colors: ${file}:${position(text, valueStart + hit.at)} colour literal "${hit.token}"; ${HOME}`);
    }
  }
  return errors;
}

// Colour literals inside the string literals of a JSX style object. A literal that
// is a key (after { or , and before :) is not a value.
function jsxStringErrors(file, text, start, end) {
  const errors = [];
  const body = text.slice(start, end);
  for (const literal of body.matchAll(/"((?:[^"\\]|\\.)*)"|'((?:[^'\\]|\\.)*)'|`((?:[^`\\]|\\.)*)`/g)) {
    const before = body.slice(0, literal.index).trimEnd().slice(-1);
    const after = body.slice(literal.index + literal[0].length).trimStart()[0];
    if ((before === '{' || before === ',') && after === ':') continue;
    const content = literal[1] ?? literal[2] ?? literal[3];
    const contentStart = start + literal.index + 1;
    for (const hit of colorHits(content)) {
      errors.push(
        `colors: ${file}:${position(text, contentStart + hit.at)} colour literal "${hit.token}" in a style prop; ${HOME}`,
      );
    }
  }
  return errors;
}

// Colour literals in the style props of a .tsx, .jsx, .mdx or .md file.
export function styleColorErrors(file, text) {
  const errors = [];
  for (const prop of text.matchAll(/\bstyle\s*=\s*(["'{])/g)) {
    const open = prop.index + prop[0].length - 1;
    if (prop[1] === '{') {
      errors.push(...jsxStringErrors(file, text, open, matchingBrace(text, open) + 1));
    } else {
      const close = text.indexOf(prop[1], open + 1);
      const end = close === -1 ? text.length : close;
      const attribute = text.slice(open + 1, end);
      for (const declaration of `${attribute};`.matchAll(/([-\w]+)\s*:([^;]*)(?=;)/g)) {
        const valueStart = open + 1 + declaration.index + declaration[0].length - declaration[2].length;
        for (const hit of colorHits(declaration[2])) {
          errors.push(
            `colors: ${file}:${position(text, valueStart + hit.at)} colour literal "${hit.token}" in a style prop; ${HOME}`,
          );
        }
      }
    }
  }
  return errors;
}
