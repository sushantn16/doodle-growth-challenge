import { useEffect, useState } from 'react'
import { fetchPolls, type Poll } from './api'

const dateFormat = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' })

type State = { status: 'loading' } | { status: 'ready'; polls: Poll[] } | { status: 'failed' }

export function PollList() {
  const [state, setState] = useState<State>({ status: 'loading' })

  useEffect(() => {
    let cancelled = false
    fetchPolls()
      .then((polls) => !cancelled && setState({ status: 'ready', polls }))
      .catch(() => !cancelled && setState({ status: 'failed' }))
    return () => {
      cancelled = true
    }
  }, [])

  if (state.status === 'loading') {
    return <p className="message">Loading your polls…</p>
  }
  if (state.status === 'failed') {
    return (
      <p className="message" role="alert">
        Your polls couldn’t be loaded. Please reload the page.
      </p>
    )
  }
  if (state.polls.length === 0) {
    return <p className="message">You don’t have any polls yet.</p>
  }

  return (
    // The backend already sorts newest first.
    <ol className="poll-list">
      {state.polls.map((poll) => (
        <li key={poll.id} className="poll">
          {/* Titles come in any language, including right-to-left ones such as Arabic. */}
          <h2 className="poll-title" dir="auto">
            {poll.title}
          </h2>
          <p className="poll-date">
            Created <time dateTime={poll.createdAt}>{dateFormat.format(new Date(poll.createdAt))}</time>
          </p>
        </li>
      ))}
    </ol>
  )
}
