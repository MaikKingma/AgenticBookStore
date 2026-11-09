// One line of the conversation: what the visitor sent or what the assistant answered, as plain text.
export interface ChatMessage {
  author: 'user' | 'assistant'
  text: string
}

const AUTHOR_LABEL = { user: 'You', assistant: 'Assistant' } as const

// The conversation as a list that screen readers announce when an answer arrives. The list is there from the first
// render, because a live region has to exist before it changes. The text is a React text node, so markup in an answer
// is shown as it is and never interpreted.
export function ChatMessageList({ messages }: { messages: ChatMessage[] }) {
  return (
    <ol className="chat-messages" aria-live="polite" aria-label="Conversation">
      {messages.map((message, index) => (
        <li key={index} className={`chat-message chat-message-${message.author}`}>
          <strong>{AUTHOR_LABEL[message.author]}:</strong> {message.text}
        </li>
      ))}
    </ol>
  )
}
