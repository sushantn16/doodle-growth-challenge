import { vi } from 'vitest'

type Route = { status?: number; body?: unknown }

/**
 * Replaces fetch with a fake that answers by URL. Returns the mock, so tests can check which
 * requests were sent.
 */
export function mockFetch(routes: Record<string, Route>) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, _init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input.toString()
    const route = routes[url]
    if (!route) {
      return new Response(null, { status: 404 })
    }
    return new Response(route.body === undefined ? null : JSON.stringify(route.body), {
      status: route.status ?? 200,
      headers: { 'Content-Type': 'application/json' },
    })
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

/** The events sent to the backend so far, in order, e.g. "VIEW B". */
export function sentEvents(fetchMock: ReturnType<typeof mockFetch>): string[] {
  return fetchMock.mock.calls
    .filter(([url]) => url === '/api/experiment/events')
    .map(([, init]) => JSON.parse(String(init?.body)))
    .map(({ type, variant }) => `${type} ${variant}`)
}

/** How many requests went to `url`. */
export function requestsTo(fetchMock: ReturnType<typeof mockFetch>, url: string): number {
  return fetchMock.mock.calls.filter(([requested]) => requested === url).length
}
