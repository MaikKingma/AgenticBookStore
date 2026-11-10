import { useMemo, useSyncExternalStore } from 'react'
import type { ComponentProps, ReactNode } from 'react'

// A small hash router. The storefront lives at /shop/ and keeps its route in the hash (#/books/<id>), so a deep link
// needs no help from the server. The hash is the only state: useSyncExternalStore reads it and listens for hashchange.

export type KnownRoute =
  | { name: 'catalogue' }
  | { name: 'book'; id: string }
  | { name: 'basket' }
  | { name: 'checkout' }
  | { name: 'orders' }
  | { name: 'order'; id: string }
  | { name: 'assistant' }

export type Route = KnownRoute | { name: 'not-found' }

// A path parameter is percent-decoded. A malformed escape (for example "%E0%A4%A") makes the whole route unknown
// instead of throwing out of the render.
function decodeParameter(segment: string): string | undefined {
  if (segment === '') return undefined
  try {
    return decodeURIComponent(segment)
  } catch {
    return undefined
  }
}

// The hash as the browser reports it: "" or "#/..." . The match is strict: no trailing slash, no case folding, no
// query string. "#/orders" and "#/orders/<id>" are told apart by their segment count, so neither shadows the other.
function parseRoute(hash: string): Route {
  const notFound: Route = { name: 'not-found' }
  if (hash === '' || hash === '#/') return { name: 'catalogue' }
  if (!hash.startsWith('#/')) return notFound

  const [first, second, ...rest] = hash.slice(2).split('/')
  if (rest.length > 0) return notFound

  if (second === undefined) {
    switch (first) {
      case 'basket':
        return { name: 'basket' }
      case 'checkout':
        return { name: 'checkout' }
      case 'orders':
        return { name: 'orders' }
      case 'assistant':
        return { name: 'assistant' }
      default:
        return notFound
    }
  }

  const id = decodeParameter(second)
  if (id === undefined) return notFound
  if (first === 'books') return { name: 'book', id }
  if (first === 'orders') return { name: 'order', id }
  return notFound
}

function subscribe(onChange: () => void): () => void {
  window.addEventListener('hashchange', onChange)
  return () => window.removeEventListener('hashchange', onChange)
}

function currentHash(): string {
  return window.location.hash
}

// The snapshot is the hash string, which is stable between reads. The route object is derived from it.
export function useRoute(): Route {
  const hash = useSyncExternalStore(subscribe, currentHash)
  return useMemo(() => parseRoute(hash), [hash])
}

// Sets the hash. The path starts with a slash and is already percent-encoded where it needs to be.
export function navigate(path: string): void {
  window.location.hash = path
}

export function Link({ to, ...anchor }: { to: string } & Omit<ComponentProps<'a'>, 'href'>) {
  return <a {...anchor} href={`#${to}`} />
}

function NotFoundPage() {
  return (
    <section>
      <h1>Page not found</h1>
      <p>
        <Link to="/">Back to the catalogue</Link>
      </p>
    </section>
  )
}

// Renders the page of the current route through the render function. An unknown route shows the not found view and
// leaves the render function alone.
export function HashRouter({ children }: { children: (route: KnownRoute) => ReactNode }) {
  const route = useRoute()
  return route.name === 'not-found' ? <NotFoundPage /> : children(route)
}
