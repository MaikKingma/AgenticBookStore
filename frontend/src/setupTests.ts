import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, beforeEach, expect, vi } from 'vitest'

// Console trap: React key and act warnings and jsdom "Not implemented" errors reach console.error or console.warn.
// They fail the test instead of passing as noise on stderr. A test that provokes a log on purpose must clear the
// recorded calls with vi.mocked(console.error).mockClear() (or the warn equivalent) before it ends.
beforeEach(() => {
  vi.spyOn(console, 'error')
  vi.spyOn(console, 'warn')
})

// Every test starts without a rendered tree, stubbed globals, spies, stored session data or a route in the hash.
afterEach(() => {
  cleanup()

  // Read the recorded calls before restoreAllMocks removes the spies. Unmounting above can log too.
  const logged = [console.error, console.warn].flatMap((method) => (vi.isMockFunction(method) ? method.mock.calls : []))

  vi.unstubAllGlobals()
  vi.restoreAllMocks()
  sessionStorage.clear()
  window.location.hash = ''

  expect(logged, 'console.error or console.warn was called during the test').toEqual([])
})
