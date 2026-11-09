import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, onTestFinished, vi } from 'vitest'
import { jsonResponse, mockFetch, problemResponse } from '../test/fetchMock'
import type { FetchMock, MockRoute } from '../test/fetchMock'
import { AssistantPage } from './AssistantPage'

const STORAGE_KEY = 'openbook.conversationId'
const CHAT = '/assistant/chat'

// The id of the conversation that is stored before the page opens, and the one the browser creates when none is stored.
const SEEDED_ID = '4b6e0a93-2d17-4f58-b3c1-7a9d5e2f0c84'
const CREATED_ID = '1f7c3a58-6e09-4d24-a8b5-0c2e9d6f3b71'

// Three messages of the visitor and the three answers. In the conversation they alternate, and neither the texts nor
// their lengths go up or down from one line to the next.
const FIRST = 'Do you have anything by Austen?'
const FIRST_REPLY = 'Yes: Emma and Persuasion are in stock.'
const SECOND = 'What about Dickens?'
const SECOND_REPLY = 'Great Expectations is available.'
const THIRD = 'Thanks, I will order Emma.'
const THIRD_REPLY = 'Done. Anything else I can find for you?'

function reply(text: string): () => Response {
  return () => jsonResponse({ conversationId: SEEDED_ID, reply: text })
}

// The route answers the first request with the first answer, the second with the second, and so on.
function inOrder(...answers: Array<() => Response>): MockRoute {
  let next = 0
  return {
    method: 'POST',
    path: CHAT,
    respond: () => {
      const answer = answers[next]
      next += 1
      return answer()
    },
  }
}

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((done) => {
    resolve = done
  })
  return { promise, resolve }
}

function seedConversation() {
  sessionStorage.setItem(STORAGE_KEY, SEEDED_ID)
}

function openAssistant() {
  return render(<AssistantPage />)
}

function input(): HTMLTextAreaElement {
  return screen.getByLabelText('Your message')
}

function sendButton() {
  return screen.getByRole('button', { name: 'Send' })
}

function messageForm() {
  return screen.getByRole('form', { name: 'Message to the assistant' })
}

function conversation() {
  return screen.getByRole('list', { name: 'Conversation' })
}

function conversationTexts(): Array<string | null> {
  return within(conversation()).queryAllByRole('listitem').map((item) => item.textContent)
}

function sentBodies(mock: FetchMock): unknown[] {
  return mock.calls.map((call) => JSON.parse(call.body ?? 'null') as unknown)
}

function typeAndSend(user: ReturnType<typeof userEvent.setup>, text: string) {
  return user.type(input(), text).then(() => user.click(sendButton()))
}

function change(text: string) {
  fireEvent.change(input(), { target: { value: text } })
}

// Records, for every submit event that reaches the document, whether the form handler had prevented its default.
function watchSubmits(): boolean[] {
  const seen: boolean[] = []
  const listener = (event: Event) => {
    seen.push(event.defaultPrevented)
  }
  document.addEventListener('submit', listener)
  onTestFinished(() => document.removeEventListener('submit', listener))
  return seen
}

// After a failed send the visitor can go on: the text is still there, the field accepts input and Send is enabled.
function expectUsableWith(text: string) {
  expect(input()).toHaveValue(text)
  expect(input()).not.toHaveAttribute('readonly')
  expect(input()).toBeEnabled()
  expect(sendButton()).toBeEnabled()
}

describe('AssistantPage', () => {
  it('has the heading Assistant, an empty conversation list that is announced politely, an empty field and Send disabled', () => {
    openAssistant()

    expect(screen.getByRole('heading', { level: 1, name: 'Assistant' })).toBeInTheDocument()
    expect(conversation()).toHaveAttribute('aria-live', 'polite')
    expect(conversationTexts()).toEqual([])
    expect(input()).toHaveValue('')
    expect(input()).toHaveAccessibleDescription('0 / 1000 characters')
    expect(sendButton()).toBeDisabled()
  })

  it('posts the message with the stored conversation id, shows the message and the reply and empties the field', async () => {
    seedConversation()
    const mock = mockFetch([inOrder(reply(FIRST_REPLY))])
    const user = userEvent.setup()
    openAssistant()

    await typeAndSend(user, FIRST)

    expect(await screen.findByText(FIRST_REPLY, { exact: false })).toBeInTheDocument()
    expect(sentBodies(mock)).toEqual([{ conversationId: SEEDED_ID, message: FIRST }])
    expect(mock.calls[0].method).toBe('POST')
    expect(mock.calls[0].path).toBe(CHAT)
    expect(conversationTexts()).toEqual([`You: ${FIRST}`, `Assistant: ${FIRST_REPLY}`])
    expect(input()).toHaveValue('')
    expect(sendButton()).toBeDisabled()
    expect(input()).toHaveAccessibleDescription('0 / 1000 characters')
  })

  it('shows three exchanges in the order they happened and posts the same stored id every time', async () => {
    seedConversation()
    const mock = mockFetch([inOrder(reply(FIRST_REPLY), reply(SECOND_REPLY), reply(THIRD_REPLY))])
    const user = userEvent.setup()
    openAssistant()

    await typeAndSend(user, FIRST)
    expect(await screen.findByText(FIRST_REPLY, { exact: false })).toBeInTheDocument()
    await typeAndSend(user, SECOND)
    expect(await screen.findByText(SECOND_REPLY, { exact: false })).toBeInTheDocument()
    await typeAndSend(user, THIRD)
    expect(await screen.findByText(THIRD_REPLY, { exact: false })).toBeInTheDocument()

    expect(conversationTexts()).toEqual([
      `You: ${FIRST}`,
      `Assistant: ${FIRST_REPLY}`,
      `You: ${SECOND}`,
      `Assistant: ${SECOND_REPLY}`,
      `You: ${THIRD}`,
      `Assistant: ${THIRD_REPLY}`,
    ])
    expect(sentBodies(mock)).toEqual([
      { conversationId: SEEDED_ID, message: FIRST },
      { conversationId: SEEDED_ID, message: SECOND },
      { conversationId: SEEDED_ID, message: THIRD },
    ])
    expect(sessionStorage.getItem(STORAGE_KEY)).toBe(SEEDED_ID)
  })

  it('creates the conversation id once when none is stored, stores it and sends it with every message', async () => {
    const randomUuid = vi.spyOn(crypto, 'randomUUID').mockReturnValue(CREATED_ID)
    const mock = mockFetch([inOrder(reply(FIRST_REPLY), reply(SECOND_REPLY), reply(THIRD_REPLY))])
    const user = userEvent.setup()
    openAssistant()
    expect(randomUuid).toHaveBeenCalledTimes(0)

    await typeAndSend(user, FIRST)
    expect(await screen.findByText(FIRST_REPLY, { exact: false })).toBeInTheDocument()
    await typeAndSend(user, SECOND)
    expect(await screen.findByText(SECOND_REPLY, { exact: false })).toBeInTheDocument()
    await typeAndSend(user, THIRD)
    expect(await screen.findByText(THIRD_REPLY, { exact: false })).toBeInTheDocument()

    expect(randomUuid).toHaveBeenCalledTimes(1)
    expect(sessionStorage.getItem(STORAGE_KEY)).toBe(CREATED_ID)
    expect(sentBodies(mock)).toEqual([
      { conversationId: CREATED_ID, message: FIRST },
      { conversationId: CREATED_ID, message: SECOND },
      { conversationId: CREATED_ID, message: THIRD },
    ])
  })

  it('sends the message exactly as typed, with its edge whitespace, and shows it as typed', async () => {
    seedConversation()
    const typed = '  Is Dune in stock?  '
    const mock = mockFetch([inOrder(reply(FIRST_REPLY))])
    const user = userEvent.setup()
    openAssistant()
    change(typed)

    await user.click(sendButton())

    expect(await screen.findByText(FIRST_REPLY, { exact: false })).toBeInTheDocument()
    expect(sentBodies(mock)).toEqual([{ conversationId: SEEDED_ID, message: typed }])
    expect(conversationTexts()).toEqual([`You: ${typed}`, `Assistant: ${FIRST_REPLY}`])
  })

  it('shows markup and edge whitespace in a reply, and markup in a message, as text and creates no element from them', async () => {
    seedConversation()
    const markup = '  <b>Emma</b> is in stock, see <i>Persuasion</i> too.\n'
    mockFetch([inOrder(reply(markup))])
    const user = userEvent.setup()
    openAssistant()

    await typeAndSend(user, '<u>Is Emma in stock?</u>')

    expect(await screen.findByText('Assistant:')).toBeInTheDocument()
    expect(conversationTexts()).toEqual([`You: <u>Is Emma in stock?</u>`, `Assistant: ${markup}`])
    expect(conversation().querySelector('b, i, u')).toBeNull()
  })

  it('prevents the default action of the form submit', async () => {
    seedConversation()
    mockFetch([inOrder(reply(FIRST_REPLY))])
    const submits = watchSubmits()
    const user = userEvent.setup()
    openAssistant()

    await typeAndSend(user, FIRST)

    expect(await screen.findByText(FIRST_REPLY, { exact: false })).toBeInTheDocument()
    expect(submits).toEqual([true])
  })

  it('prevents the default action of a form submit that is held back because the field is empty', () => {
    const mock = mockFetch([inOrder(reply(FIRST_REPLY))])
    const submits = watchSubmits()
    openAssistant()

    fireEvent.submit(messageForm())

    expect(submits).toEqual([true])
    expect(mock.calls).toEqual([])
  })

  it('shows the same message and the same reply twice as four lines, one after the other', async () => {
    seedConversation()
    mockFetch([inOrder(reply('Hi'), reply('Hi'))])
    const user = userEvent.setup()
    openAssistant()

    await typeAndSend(user, 'Hello')
    expect(await screen.findByText('Hi', { exact: false })).toBeInTheDocument()
    await typeAndSend(user, 'Hello')

    await vi.waitFor(() => expect(conversationTexts()).toHaveLength(4))
    expect(conversationTexts()).toEqual(['You: Hello', 'Assistant: Hi', 'You: Hello', 'Assistant: Hi'])
  })
})

describe('AssistantPage message field', () => {
  it('keeps Send disabled for an empty field and for a field of whitespace only, and enables it for any other text', () => {
    openAssistant()
    expect(sendButton()).toBeDisabled()

    change('   ')
    expect(sendButton()).toBeDisabled()

    change(' \n\t ')
    expect(sendButton()).toBeDisabled()

    change(' a ')
    expect(sendButton()).toBeEnabled()

    change('')
    expect(sendButton()).toBeDisabled()
  })

  it('counts the characters against the limit and keeps Send enabled at 1000 characters', () => {
    openAssistant()
    expect(input()).not.toHaveAttribute('maxlength')

    change(SECOND)
    expect(input()).toHaveAccessibleDescription('19 / 1000 characters')
    expect(sendButton()).toBeEnabled()

    change('a'.repeat(1000))
    expect(input()).toHaveValue('a'.repeat(1000))
    expect(input()).toHaveAccessibleDescription('1000 / 1000 characters')
    expect(sendButton()).toBeEnabled()
  })

  it('counts the whitespace at the edges of the message too, because the limit applies to the message as typed', () => {
    openAssistant()

    change('  Is Dune in stock?  ')
    expect(input()).toHaveAccessibleDescription('21 / 1000 characters')
    expect(sendButton()).toBeEnabled()

    change('a'.repeat(999) + ' ')
    expect(input()).toHaveAccessibleDescription('1000 / 1000 characters')
    expect(sendButton()).toBeEnabled()

    change(' ' + 'a'.repeat(999))
    expect(input()).toHaveAccessibleDescription('1000 / 1000 characters')
    expect(sendButton()).toBeEnabled()
  })

  it('disables Send at 1001 characters, shows the counter with the limit, and enables it again at 1000', () => {
    openAssistant()

    change('a'.repeat(1001))

    expect(input()).toHaveValue('a'.repeat(1001))
    expect(input()).toHaveAccessibleDescription('1001 / 1000 characters, too long')
    expect(sendButton()).toBeDisabled()

    change('a'.repeat(1000))

    expect(input()).toHaveAccessibleDescription('1000 / 1000 characters')
    expect(sendButton()).toBeEnabled()
  })

  it('disables Send when edge whitespace brings the message to 1001 characters, although the text without it is shorter', () => {
    openAssistant()

    change('a'.repeat(1000) + ' ')
    expect(input()).toHaveAccessibleDescription('1001 / 1000 characters, too long')
    expect(sendButton()).toBeDisabled()

    change(' ' + 'a'.repeat(1000))
    expect(input()).toHaveAccessibleDescription('1001 / 1000 characters, too long')
    expect(sendButton()).toBeDisabled()

    change('a'.repeat(999) + ' ')
    expect(input()).toHaveAccessibleDescription('1000 / 1000 characters')
    expect(sendButton()).toBeEnabled()
  })

  it('counts a character outside the Basic Multilingual Plane as two, like the bookstore does, and disables Send at 1001', () => {
    const book = String.fromCodePoint(0x1f4d6)
    expect(book).toHaveLength(2)
    openAssistant()

    change('a'.repeat(999) + book)
    expect(input()).toHaveAccessibleDescription('1001 / 1000 characters, too long')
    expect(sendButton()).toBeDisabled()

    change('a'.repeat(998) + book)
    expect(input()).toHaveAccessibleDescription('1000 / 1000 characters')
    expect(sendButton()).toBeEnabled()
  })

  it('sends a message of exactly 1000 characters', async () => {
    seedConversation()
    const longest = 'b'.repeat(1000)
    const mock = mockFetch([inOrder(reply(FIRST_REPLY))])
    const user = userEvent.setup()
    openAssistant()
    change(longest)

    await user.click(sendButton())

    expect(await screen.findByText(FIRST_REPLY, { exact: false })).toBeInTheDocument()
    expect(sentBodies(mock)).toEqual([{ conversationId: SEEDED_ID, message: longest }])
  })

  it('sends nothing when the form is submitted past the disabled button with a message that is empty, blank or too long', async () => {
    seedConversation()
    const mock = mockFetch([inOrder(reply(FIRST_REPLY))])
    const user = userEvent.setup()
    openAssistant()

    fireEvent.submit(messageForm())
    change('  \n ')
    fireEvent.submit(messageForm())
    change('c'.repeat(1001))
    fireEvent.submit(messageForm())
    change(FIRST)
    await user.click(sendButton())

    expect(await screen.findByText(FIRST_REPLY, { exact: false })).toBeInTheDocument()
    expect(sentBodies(mock)).toEqual([{ conversationId: SEEDED_ID, message: FIRST }])
    expect(conversationTexts()).toEqual([`You: ${FIRST}`, `Assistant: ${FIRST_REPLY}`])
  })
})

describe('AssistantPage while the assistant thinks', () => {
  it('disables Send, locks the field and shows that it waits, and two clicks give one request', async () => {
    seedConversation()
    const held = deferred<Response>()
    const mock = mockFetch([{ method: 'POST', path: CHAT, respond: () => held.promise }])
    const user = userEvent.setup()
    openAssistant()
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
    change(FIRST)

    await user.click(sendButton())
    await user.click(sendButton())

    expect(await screen.findByRole('status')).toHaveTextContent('Waiting for the assistant')
    expect(sendButton()).toBeDisabled()
    expect(input()).toHaveAttribute('readonly')
    expect(input()).toHaveValue(FIRST)
    expect(sentBodies(mock)).toEqual([{ conversationId: SEEDED_ID, message: FIRST }])

    held.resolve(reply(FIRST_REPLY)())

    expect(await screen.findByText(FIRST_REPLY, { exact: false })).toBeInTheDocument()
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
    expect(input()).not.toHaveAttribute('readonly')
    expect(sentBodies(mock)).toHaveLength(1)
  })

  it('sends one request when the form is submitted again past the disabled button', async () => {
    seedConversation()
    const held = deferred<Response>()
    const mock = mockFetch([{ method: 'POST', path: CHAT, respond: () => held.promise }])
    const user = userEvent.setup()
    openAssistant()
    change(FIRST)

    await user.click(sendButton())
    expect(await screen.findByRole('status')).toHaveTextContent('Waiting for the assistant')
    fireEvent.submit(messageForm())
    fireEvent.submit(messageForm())

    expect(sentBodies(mock)).toEqual([{ conversationId: SEEDED_ID, message: FIRST }])

    held.resolve(reply(FIRST_REPLY)())

    expect(await screen.findByText(FIRST_REPLY, { exact: false })).toBeInTheDocument()
    expect(conversationTexts()).toEqual([`You: ${FIRST}`, `Assistant: ${FIRST_REPLY}`])
    expect(sentBodies(mock)).toHaveLength(1)
  })
})

describe('AssistantPage errors', () => {
  const OFFLINE = 'The assistant is offline'
  const TOO_MANY = 'Too many requests, try again in a minute'

  function alertText(): string | null {
    return screen.getByRole('alert').textContent
  }

  it('shows "The assistant is offline" for the code assistant-offline, keeps the text and the field usable, and a following send succeeds', async () => {
    seedConversation()
    const offline = () =>
      problemResponse(503, {
        title: 'Service Unavailable',
        status: 503,
        code: 'assistant-offline',
        detail: 'The language model cannot be reached.',
      })
    const mock = mockFetch([inOrder(offline, reply(FIRST_REPLY))])
    const user = userEvent.setup()
    openAssistant()

    await typeAndSend(user, FIRST)

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(alertText()).toBe(OFFLINE)
    expectUsableWith(FIRST)
    expect(conversationTexts()).toEqual([])
    expect(screen.queryByRole('status')).not.toBeInTheDocument()

    await user.click(sendButton())

    expect(await screen.findByText(FIRST_REPLY, { exact: false })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(sentBodies(mock)).toEqual([
      { conversationId: SEEDED_ID, message: FIRST },
      { conversationId: SEEDED_ID, message: FIRST },
    ])
    expect(conversationTexts()).toEqual([`You: ${FIRST}`, `Assistant: ${FIRST_REPLY}`])
    expect(input()).toHaveValue('')
  })

  it('removes the alert as soon as the next send starts and shows it again only for the new failure', async () => {
    seedConversation()
    const held = deferred<Response>()
    let calls = 0
    mockFetch([
      {
        method: 'POST',
        path: CHAT,
        respond: () => {
          calls += 1
          return calls === 1
            ? problemResponse(503, { title: 'Service Unavailable', status: 503, code: 'assistant-offline' })
            : held.promise
        },
      },
    ])
    const user = userEvent.setup()
    openAssistant()
    await typeAndSend(user, FIRST)
    expect(await screen.findByRole('alert')).toBeInTheDocument()

    await user.click(sendButton())

    expect(await screen.findByRole('status')).toHaveTextContent('Waiting for the assistant')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()

    held.resolve(reply(FIRST_REPLY)())

    expect(await screen.findByText(FIRST_REPLY, { exact: false })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('shows the offline text for the code assistant-offline on any status', async () => {
    seedConversation()
    mockFetch([inOrder(() => problemResponse(500, { title: 'Internal Server Error', status: 500, code: 'assistant-offline' }))])
    const user = userEvent.setup()
    openAssistant()

    await typeAndSend(user, FIRST)

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(alertText()).toBe(OFFLINE)
    expectUsableWith(FIRST)
  })

  it('shows the too many requests text for a 429 with a Boot error JSON body, keeps the text and a following send succeeds', async () => {
    seedConversation()
    const boot = () =>
      jsonResponse({ timestamp: '2026-06-15T10:00:00.123+00:00', status: 429, error: 'Too Many Requests', path: CHAT }, 429)
    const mock = mockFetch([inOrder(boot, reply(SECOND_REPLY))])
    const user = userEvent.setup()
    openAssistant()

    await typeAndSend(user, SECOND)

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(alertText()).toBe(TOO_MANY)
    expectUsableWith(SECOND)

    await user.click(sendButton())

    expect(await screen.findByText(SECOND_REPLY, { exact: false })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(sentBodies(mock)).toHaveLength(2)
  })

  it('shows the same text for a 429 with a ProblemDetail body of any code, and none of its title or detail', async () => {
    seedConversation()
    mockFetch([
      inOrder(() =>
        problemResponse(429, { title: 'Rate limit exceeded', status: 429, code: 'rate-limited', detail: 'Twenty requests a minute.' }),
      ),
    ])
    const user = userEvent.setup()
    openAssistant()

    await typeAndSend(user, SECOND)

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(alertText()).toBe(TOO_MANY)
    expectUsableWith(SECOND)
  })

  it('shows the generic alert with the title and the detail for a 503 with another code', async () => {
    seedConversation()
    mockFetch([
      inOrder(() =>
        problemResponse(503, {
          title: 'Upstream Timeout',
          status: 503,
          code: 'upstream-timeout',
          detail: 'The model did not answer in time.',
        }),
      ),
    ])
    const user = userEvent.setup()
    openAssistant()

    await typeAndSend(user, THIRD)

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(alertText()).toBe('Upstream TimeoutThe model did not answer in time.')
    expect(screen.queryByText(OFFLINE)).not.toBeInTheDocument()
    expectUsableWith(THIRD)
  })

  it('shows the generic alert with the error text for a 503 whose body is a Boot error JSON without a code', async () => {
    seedConversation()
    mockFetch([inOrder(() => jsonResponse({ status: 503, error: 'Service Unavailable', path: CHAT }, 503))])
    const user = userEvent.setup()
    openAssistant()

    await typeAndSend(user, THIRD)

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(alertText()).toBe('Service Unavailable')
    expectUsableWith(THIRD)
  })

  it('shows the validation message of the server for a 400 on the field message and keeps the field usable', async () => {
    seedConversation()
    mockFetch([
      inOrder(() =>
        problemResponse(400, {
          title: 'Validation failed',
          status: 400,
          code: 'validation-failed',
          details: [{ field: 'message', message: 'must not be longer than 1000 characters' }],
        }),
      ),
    ])
    const user = userEvent.setup()
    openAssistant()

    await typeAndSend(user, FIRST)

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(alertText()).toBe('Validation failedmessage: must not be longer than 1000 characters')
    expectUsableWith(FIRST)
  })

  it('shows the network alert when the bookstore cannot be reached, and keeps the text and the field usable', async () => {
    seedConversation()
    const mock = mockFetch([
      {
        method: 'POST',
        path: CHAT,
        respond: () => {
          throw new TypeError('Failed to fetch')
        },
      },
    ])
    const user = userEvent.setup()
    openAssistant()

    await typeAndSend(user, FIRST)

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(alertText()).toBe('Cannot reach the bookstore')
    expect(mock.calls.map((call) => call.path)).toEqual([CHAT])
    expectUsableWith(FIRST)
  })

  it('shows a generic alert and sends nothing when the browser cannot create a conversation id', async () => {
    vi.spyOn(crypto, 'randomUUID').mockImplementation(() => {
      throw new TypeError('randomUUID is not available')
    })
    const mock = mockFetch([])
    const user = userEvent.setup()
    openAssistant()

    await typeAndSend(user, FIRST)

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(alertText()).toBe('Something went wrong')
    expect(mock.calls).toEqual([])
    expectUsableWith(FIRST)
  })
})
