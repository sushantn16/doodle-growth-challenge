import { render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { PollList } from './PollList'
import { mockFetch } from './testing'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('PollList', () => {
  it('shows each poll’s title and creation date, in the order the backend sends them', async () => {
    mockFetch({
      '/api/polls': {
        body: [
          { id: 'newer', title: 'Team lunch', createdAt: '2017-01-27T12:52:49.056Z' },
          { id: 'older', title: 'Board games night', createdAt: '2017-01-09T12:48:19.216Z' },
        ],
      },
    })

    render(<PollList />)

    const polls = await screen.findAllByRole('listitem')
    expect(polls).toHaveLength(2)
    expect(within(polls[0]).getByRole('heading')).toHaveTextContent('Team lunch')
    expect(within(polls[0]).getByText(/2017/).closest('time')).toHaveAttribute('dateTime', '2017-01-27T12:52:49.056Z')
    expect(within(polls[1]).getByRole('heading')).toHaveTextContent('Board games night')
  })

  it('says so when the polls can’t be loaded', async () => {
    mockFetch({ '/api/polls': { status: 500 } })

    render(<PollList />)

    expect(await screen.findByRole('alert')).toHaveTextContent('couldn’t be loaded')
  })
})
