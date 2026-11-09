import { useId, useState } from 'react'
import type { FormEvent } from 'react'
import { chat } from '../api/assistant'
import type { AssistantReplyView, Problem } from '../api/types'
import { ChatMessageList } from '../components/ChatMessageList'
import type { ChatMessage } from '../components/ChatMessageList'
import { ProblemDetailAlert } from '../components/ProblemDetailAlert'
import { MAX_CHAT_MESSAGE_LENGTH } from '../limits'
import { getConversationId } from '../state/conversation'
import { problemOf } from './problem'

const OFFLINE_TITLE = 'The assistant is offline'
const TOO_MANY_REQUESTS_TITLE = 'Too many requests, try again in a minute'

// The two failures of the assistant that have a text of their own. A 429 is recognised by its status alone, because its
// body is the one the framework writes (plain JSON, no code) or a problem of any code. The offline text is recognised by
// the code `assistant-offline`, never by the status. Every other failure is shown as the bookstore described it.
function shownProblem(problem: Problem): Problem {
  if (problem.status === 429) {
    return { status: 429, title: TOO_MANY_REQUESTS_TITLE, details: [] }
  }
  if (problem.code === 'assistant-offline') {
    return { status: problem.status, title: OFFLINE_TITLE, details: [] }
  }
  return problem
}

// Chat with the assistant. The message is sent as typed (no trimming) and shown as typed; only an empty message, a
// message of whitespace only and a message beyond the limit are held back in the browser, the bookstore stays the
// authority for everything else. The message joins the conversation together with its answer, so a failed send leaves
// the text in the field to be sent again and puts nothing into the conversation.
export function AssistantPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [draft, setDraft] = useState('')
  const [pending, setPending] = useState(false)
  const [problem, setProblem] = useState<Problem | undefined>(undefined)
  const inputId = useId()
  const counterId = useId()

  const tooLong = draft.length > MAX_CHAT_MESSAGE_LENGTH
  const sendable = draft.trim() !== '' && !tooLong

  async function send(message: string) {
    let answer: AssistantReplyView
    try {
      answer = await chat({ conversationId: getConversationId(), message })
    } catch (error: unknown) {
      setProblem(problemOf(error))
      setPending(false)
      return
    }
    setMessages((earlier) => [
      ...earlier,
      { author: 'user', text: message },
      { author: 'assistant', text: answer.reply },
    ])
    setDraft('')
    setPending(false)
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (pending || !sendable) return
    setPending(true)
    setProblem(undefined)
    void send(draft)
  }

  return (
    <section>
      <h1>Assistant</h1>
      <p>Ask about books, authors and what is in stock.</p>
      <ChatMessageList messages={messages} />
      {pending && <p role="status">Waiting for the assistant</p>}
      {problem !== undefined && <ProblemDetailAlert problem={shownProblem(problem)} />}
      <form className="card chat-form" aria-label="Message to the assistant" onSubmit={submit}>
        <label htmlFor={inputId}>Your message</label>
        <textarea
          id={inputId}
          rows={3}
          value={draft}
          readOnly={pending}
          aria-describedby={counterId}
          onChange={(event) => setDraft(event.target.value)}
        />
        <p id={counterId} className={tooLong ? 'chat-counter chat-counter-over' : 'chat-counter'}>
          {draft.length} / {MAX_CHAT_MESSAGE_LENGTH} characters{tooLong ? ', too long' : ''}
        </p>
        <div className="form-actions">
          <button type="submit" className="button button-primary" disabled={pending || !sendable}>
            Send
          </button>
        </div>
      </form>
    </section>
  )
}
