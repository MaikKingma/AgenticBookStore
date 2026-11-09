// The assistant keeps its memory per conversation id, so the page sends the same id for every message of a browser
// session. The id lives in the session storage: a reload keeps the conversation, a new tab starts another one.
const STORAGE_KEY = 'openbook.conversationId'

// The stored id, or a new one that is stored first. Nothing is cached here, the storage is the only place of the id.
export function getConversationId(): string {
  const stored = sessionStorage.getItem(STORAGE_KEY)
  if (stored !== null) {
    return stored
  }
  const created = crypto.randomUUID()
  sessionStorage.setItem(STORAGE_KEY, created)
  return created
}
