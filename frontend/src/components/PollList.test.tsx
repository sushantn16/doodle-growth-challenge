import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { PollList } from './PollList'

describe('PollList', () => {
  it('shows each poll’s title and creation date, in the order the backend sends them', async () => {
    const pollsRequest = Promise.resolve([
      { id: 'newer', title: 'Team lunch', createdAt: '2017-01-27T12:52:49.056Z' },
      { id: 'older', title: 'Board games night', createdAt: '2017-01-09T12:48:19.216Z' },
    ])

    render(<PollList pollsRequest={pollsRequest} />)

    const polls = await screen.findAllByRole('listitem')
    expect(polls).toHaveLength(2)
    expect(within(polls[0]).getByRole('heading')).toHaveTextContent('Team lunch')
    expect(within(polls[0]).getByText(/2017/).closest('time')).toHaveAttribute('dateTime', '2017-01-27T12:52:49.056Z')
    expect(within(polls[1]).getByRole('heading')).toHaveTextContent('Board games night')
  })

  it('says so when there are no polls yet', async () => {
    render(<PollList pollsRequest={Promise.resolve([])} />)

    expect(await screen.findByText('You don’t have any polls yet.')).toBeVisible()
    expect(screen.queryByRole('list')).not.toBeInTheDocument()
  })

  it('says so when the polls can’t be loaded', async () => {
    render(<PollList pollsRequest={Promise.reject(new Error('backend down'))} />)

    expect(await screen.findByRole('alert')).toHaveTextContent('couldn’t be loaded')
  })
})
