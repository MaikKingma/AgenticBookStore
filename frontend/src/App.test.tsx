import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App.tsx'

describe('App', () => {
  it('renders the storefront placeholder inside the main landmark', () => {
    render(<App />)

    const main = screen.getByRole('main')
    expect(within(main).getByRole('heading', { name: 'Storefront' })).toBeInTheDocument()
  })
})
