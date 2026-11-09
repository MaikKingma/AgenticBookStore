import { describe, expect, it, vi } from 'vitest'
import { getConversationId } from './conversation'

const STORAGE_KEY = 'openbook.conversationId'

// Three different ids: the one that is already stored, and two that the browser would create.
const STORED_ID = '3c8a1f5e-9d04-4b72-a6e1-0f5d7b2c4a98'
const FIRST_CREATED_ID = '5e1b7d92-4a06-4c38-8f3d-1a9c6e0b2d74'
const SECOND_CREATED_ID = '2d9f6a03-7b18-4e54-9c2a-8e1d5b3f0a67'

function stubRandomUuid() {
  return vi.spyOn(crypto, 'randomUUID').mockReturnValueOnce(FIRST_CREATED_ID).mockReturnValueOnce(SECOND_CREATED_ID)
}

describe('getConversationId', () => {
  it('returns the id that is stored for the session and creates none', () => {
    sessionStorage.setItem(STORAGE_KEY, STORED_ID)
    const randomUuid = stubRandomUuid()

    expect(getConversationId()).toBe(STORED_ID)
    expect(randomUuid).toHaveBeenCalledTimes(0)
    expect(sessionStorage.getItem(STORAGE_KEY)).toBe(STORED_ID)
  })

  it('creates an id once when none is stored, stores it and gives the same id on the next call', () => {
    const randomUuid = stubRandomUuid()

    expect(getConversationId()).toBe(FIRST_CREATED_ID)
    expect(sessionStorage.getItem(STORAGE_KEY)).toBe(FIRST_CREATED_ID)
    expect(randomUuid).toHaveBeenCalledTimes(1)

    expect(getConversationId()).toBe(FIRST_CREATED_ID)
    expect(randomUuid).toHaveBeenCalledTimes(1)
  })

  it('creates a new id after the session storage was cleared, so nothing is cached outside the storage', () => {
    const randomUuid = stubRandomUuid()
    expect(getConversationId()).toBe(FIRST_CREATED_ID)

    sessionStorage.clear()

    expect(getConversationId()).toBe(SECOND_CREATED_ID)
    expect(sessionStorage.getItem(STORAGE_KEY)).toBe(SECOND_CREATED_ID)
    expect(randomUuid).toHaveBeenCalledTimes(2)
  })
})
