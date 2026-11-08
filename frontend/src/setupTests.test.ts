import { render, screen } from '@testing-library/react'
import { createElement } from 'react'
import { describe, expect, it, vi } from 'vitest'

// These two tests guard the shared hooks in setupTests.ts. The first one dirties every kind of state the hooks reset,
// the second one runs right after it (tests in a file run in order) and proves each one is clean again. If a reset
// statement is removed from setupTests.ts, the matching assertion in the second test fails.
describe('setupTests afterEach hooks', () => {
  it('dirties the state that every test must start without', () => {
    sessionStorage.setItem('dirty-key', 'dirty-value')
    window.location.hash = '#/dirty-route'
    vi.stubGlobal('fetch', () => 'stubbed fetch')
    vi.spyOn(Math, 'abs').mockReturnValue(42)
    render(createElement('p', null, 'dirty tree'))

    expect(sessionStorage.getItem('dirty-key')).toBe('dirty-value')
    expect(window.location.hash).toBe('#/dirty-route')
    expect(String(fetch)).toContain('stubbed fetch')
    expect(Math.abs(-1)).toBe(42)
    expect(screen.getByText('dirty tree')).toBeInTheDocument()
  })

  it('starts the next test with clean state', () => {
    expect(document.body).toBeEmptyDOMElement()
    expect(sessionStorage.length).toBe(0)
    expect(window.location.hash).toBe('')
    expect(String(fetch)).not.toContain('stubbed fetch')
    expect(vi.isMockFunction(Math.abs)).toBe(false)
    expect(Math.abs(-1)).toBe(1)
  })

  it('has the console trap installed before the test body runs', () => {
    expect(vi.isMockFunction(console.error)).toBe(true)
    expect(vi.isMockFunction(console.warn)).toBe(true)

    // The trap fails a test that logs, so this test records the calls it provokes and then clears them. The spies
    // call through to the real console by default, so silence them first to keep the test run output clean.
    vi.mocked(console.error).mockImplementation(() => {})
    vi.mocked(console.warn).mockImplementation(() => {})
    console.error('provoked error')
    console.warn('provoked warning')
    expect(console.error).toHaveBeenCalledWith('provoked error')
    expect(console.warn).toHaveBeenCalledWith('provoked warning')
    vi.mocked(console.error).mockClear()
    vi.mocked(console.warn).mockClear()
  })
})

// This guard proves that the console trap in setupTests.ts fails a test. Each of the two tests below logs on purpose
// with the spy silenced, so the trap in afterEach fails it, and fails turns that failure into the expected result. Each
// test records the error message of its failure. The last test checks that both failures came from the trap, so a
// removed trap makes the two tests above it pass the wrong way round and this test fails as well.
describe('setupTests console trap guard', () => {
  const trapMessages: string[] = []

  it.fails('fails a test that calls console.error', (context) => {
    context.onTestFailed(({ task }) => {
      trapMessages.push(...(task.result?.errors ?? []).map((error) => error.message))
    })
    vi.mocked(console.error).mockImplementation(() => {})
    console.error('provoked error')
  })

  it.fails('fails a test that calls console.warn', (context) => {
    context.onTestFailed(({ task }) => {
      trapMessages.push(...(task.result?.errors ?? []).map((error) => error.message))
    })
    vi.mocked(console.warn).mockImplementation(() => {})
    console.warn('provoked warning')
  })

  it('recorded one failure from the console trap for each console method', () => {
    expect(trapMessages).toHaveLength(2)
    for (const message of trapMessages) {
      expect(message).toContain('console.error or console.warn was called during the test')
    }
  })
})
