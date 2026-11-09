import { BasketBadge } from './components/BasketBadge'
import { BasketPage } from './pages/BasketPage'
import { BookDetailPage } from './pages/BookDetailPage'
import { CataloguePage } from './pages/CataloguePage'
import { CheckoutPage } from './pages/CheckoutPage'
import { OrderPage } from './pages/OrderPage'
import { OrdersPage } from './pages/OrdersPage'
import { HashRouter, Link, useRoute } from './router'
import type { KnownRoute } from './router'
import { BasketProvider } from './state/basket'

// The navigation items. A page that belongs to an item (a book to the catalogue, the checkout to the basket, one order
// to the orders) keeps that item marked as the current page.
const NAVIGATION = [
  { to: '/', label: 'Catalogue', pages: ['catalogue', 'book'] },
  { to: '/basket', label: 'Basket', pages: ['basket', 'checkout'] },
  { to: '/orders', label: 'Orders', pages: ['orders', 'order'] },
  { to: '/assistant', label: 'Assistant', pages: ['assistant'] },
] as const

function SiteHeader() {
  const route = useRoute()
  return (
    <header className="site-header">
      <div className="site-header-inner">
        <Link to="/" className="site-home">
          <span className="wordmark">Open</span>
          <span className="wordmark-book">Book</span>
        </Link>
        <nav aria-label="Main">
          <ul className="site-nav">
            {NAVIGATION.map((item) => (
              <li key={item.to}>
                <Link to={item.to} aria-current={item.pages.some((page) => page === route.name) ? 'page' : undefined}>
                  {item.label}
                </Link>
                {item.to === '/basket' && <BasketBadge />}
              </li>
            ))}
          </ul>
        </nav>
      </div>
    </header>
  )
}

function SiteFooter() {
  return <footer className="site-footer">OpenBook, the J-Fall 2026 workshop shop by OpenValue</footer>
}

// Stands in for a page that a later task adds, so that every route of the router already leads somewhere.
function PagePending({ title }: { title: string }) {
  return (
    <section>
      <h1>{title}</h1>
      <p>This page is not available yet.</p>
    </section>
  )
}

function Page({ route }: { route: KnownRoute }) {
  switch (route.name) {
    case 'catalogue':
      return <CataloguePage />
    case 'book':
      return <BookDetailPage id={route.id} />
    case 'basket':
      return <BasketPage />
    case 'checkout':
      return <CheckoutPage />
    case 'orders':
      return <OrdersPage />
    case 'order':
      return <OrderPage id={route.id} />
    case 'assistant':
      return <PagePending title="Assistant" />
  }
}

function App() {
  return (
    <BasketProvider>
      <SiteHeader />
      <main className="page">
        <HashRouter>{(route) => <Page route={route} />}</HashRouter>
      </main>
      <SiteFooter />
    </BasketProvider>
  )
}

export default App
