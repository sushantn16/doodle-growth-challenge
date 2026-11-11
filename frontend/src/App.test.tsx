import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { StrictMode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { fetchVariant } from './api'
import { mockFetch, requestsTo, sentEvents } from './testing'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('App', () => {
  // StrictMode runs effects twice, as `npm run dev` does. Two requests for a version, both without
  // a cookie, would create two visitor ids, and events could then be counted for the other one.
  it('asks for the visitor’s version once per page load, even in StrictMode', async () => {
    const fetchMock = mockFetch({
      '/api/experiment': { body: { variant: 'B' } },
      '/api/experiment/events': { status: 204 },
      '/api/polls': { body: [{ id: 'p1', title: 'Team lunch', createdAt: '2017-01-27T12:52:49.056Z' }] },
    })

    // Started the same way main.tsx starts it.
    render(
      <StrictMode>
        <App variant={fetchVariant()} />
      </StrictMode>,
    )
    await userEvent.click(await screen.findByRole('button', { name: 'Remove ads' }))

    expect(requestsTo(fetchMock, '/api/experiment')).toBe(1)
    expect(await screen.findByRole('heading', { name: 'Team lunch' })).toBeVisible()
    await waitFor(() => expect(sentEvents(fetchMock)).toContain('CLICK B'))
    expect(sentEvents(fetchMock).every((event) => event.endsWith(' B'))).toBe(true)
  })

  it('puts the button in the page header, before the poll list', async () => {
    mockFetch({
      '/api/experiment': { body: { variant: 'A' } },
      '/api/experiment/events': { status: 204 },
      '/api/polls': { body: [] },
    })

    render(<App variant={fetchVariant()} />)

    const button = await screen.findByRole('button', { name: 'Upgrade' })
    expect(screen.getByRole('banner')).toContainElement(button)
    const main = screen.getByRole('main')
    expect(button.compareDocumentPosition(main) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })
})
