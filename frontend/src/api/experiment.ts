import { getJson } from './http'

export type Variant = 'A' | 'B'
export type EventType = 'VIEW' | 'CLICK'

/** The backend identifies the visitor by an HttpOnly cookie, creating one on the first visit. */
export async function fetchVariant(): Promise<Variant> {
  const { variant } = await getJson<{ variant: Variant }>('/api/experiment')
  return variant
}

/**
 * Records that the visitor saw or clicked the button. `variant` is the version shown; the backend
 * only counts the event if it matches the visitor's assignment.
 *
 * Fire and forget: a lost event must never break the page. `keepalive` lets the request finish
 * even if a click navigates away, as it would once the button opens a plans page.
 */
export function sendEvent(type: EventType, variant: Variant): void {
  fetch('/api/experiment/events', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ type, variant }),
    keepalive: true,
  }).catch(() => {
    // Tracking is best effort. The visitor doesn't need to know.
  })
}
