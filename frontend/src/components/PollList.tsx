import type { Poll } from '../api/polls'
import { useAsync } from '../hooks/useAsync'
import styles from './PollList.module.css'

const dateFormat = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' })

/** `pollsRequest` is the pending request for the polls, started once in main.tsx. */
export function PollList({ pollsRequest }: { pollsRequest: Promise<Poll[]> }) {
  const request = useAsync(pollsRequest)

  if (request.status === 'loading') {
    return <p className={styles.message}>Loading your polls…</p>
  }
  if (request.status === 'failed') {
    return (
      <p className={styles.message} role="alert">
        Your polls couldn’t be loaded. Please reload the page.
      </p>
    )
  }
  if (request.value.length === 0) {
    return <p className={styles.message}>You don’t have any polls yet.</p>
  }

  return (
    // The backend already sorts newest first.
    <ol className={styles.list}>
      {request.value.map((poll) => (
        <li key={poll.id} className={styles.poll}>
          {/* Titles come in any language, including right-to-left ones such as Arabic. */}
          <h2 className={styles.title} dir="auto">
            {poll.title}
          </h2>
          <p className={styles.date}>
            Created <time dateTime={poll.createdAt}>{dateFormat.format(new Date(poll.createdAt))}</time>
          </p>
        </li>
      ))}
    </ol>
  )
}
