import '@testing-library/jest-dom/vitest'
import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'

// @testing-library/react only auto-registers its afterEach(cleanup) when it detects a global
// test-framework `afterEach` (i.e. when Vitest's `test.globals` is enabled). This project runs
// Vitest without globals, so cleanup was never happening between tests: every render() call was
// leaving its DOM tree mounted, and later tests could silently match leftover elements from
// earlier ones. Registering it explicitly here fixes that for every test file, not just one.
afterEach(cleanup)

// Vitest's jsdom environment is shared across every test within a file (isolation happens per
// file, not per test), so a real localStorage object persists between tests. Components that
// read/write localStorage directly (e.g. TodayPage's wisdom/memory draft persistence) would
// otherwise leak state from one test into the next whenever they reuse the same IDs — clearing it
// here keeps every test starting from a clean slate, the same guarantee `cleanup()` gives the DOM.
afterEach(() => {
  globalThis.localStorage?.clear()
})
