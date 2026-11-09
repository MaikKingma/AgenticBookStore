import { postJson } from './client'
import type { AssistantReplyView, ChatPayload } from './types'

export function chat(payload: ChatPayload): Promise<AssistantReplyView> {
  return postJson<AssistantReplyView>('/assistant/chat', payload)
}
