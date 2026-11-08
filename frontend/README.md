# Frontend: the OpenBook storefront

A React 19 and TypeScript app built with Vite and tested with Vitest and Testing Library. The bookstore serves the
built app under `/shop/`. The app has no business rules: the bookstore stays the authority for every price, stock
figure and limit.

## No global Node.js needed

The Maven build installs Node.js (version in `pom.xml`) into `frontend/node/`. All commands below use that Node.js.

## Build and test with Maven

From the repository root:

```
./mvnw -f frontend/pom.xml clean verify
```

This installs Node.js, runs `npm ci`, runs the tests (`npm test`) and builds the app into `frontend/dist/`
(`npm run build`). With `-DskipTests` the tests are skipped and the app is still built. The full build
(`./mvnw clean verify`) runs this module first and packages `frontend/dist/` into the bookstore.

## Run the dev server

Start the bookstore first. It must listen on `127.0.0.1:8080` (the default), because the dev server proxies the API
calls `/books`, `/inventory`, `/orders` and `/assistant` to that address.

The tools in `node_modules/.bin` start with `#!/usr/bin/env node`, so the Maven-installed Node.js must be on the PATH.
Run this in the `frontend/` directory, after at least one Maven build has installed Node.js and the dependencies.

macOS and Linux:

```
PATH="$PWD/node:$PATH" node/npm run dev
```

Windows (cmd):

```
set PATH=%CD%\node;%PATH%
node\npm run dev
```

Windows (PowerShell):

```
$env:PATH = "$PWD\node;$env:PATH"
node\npm run dev
```

Then open http://127.0.0.1:5173/shop/ in a browser.

## Other commands

Run these in `frontend/` with the same PATH prefix as above:

| Command | What it does |
|---|---|
| `node/npm test` | Runs all Vitest tests once |
| `node/npm run build` | Type-checks (`tsc -b`) and builds into `dist/` |
| `node/npm run preview` | Serves the built app from `dist/` |

## Dependencies

All versions in `package.json` are exact and `package-lock.json` is committed. `.npmrc` makes `npm install` save exact
versions. Do not add `^` or `~` ranges.
