import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { mockFetch, sentEvents } from './testing'
import { UpgradeButton } from './UpgradeButton'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('UpgradeButton', () => {
  it('shows "Upgrade" to a visitor in version A', async () => {
    mockFetch({ '/api/experiment': { body: { variant: 'A' } }, '/api/experiment/events': { status: 204 } })

    render(<UpgradeButton />)

    expect(await screen.findByRole('button', { name: 'Upgrade' })).toBeVisible()
  })

  it('shows "Remove ads" to a visitor in version B', async () => {
    mockFetch({ '/api/experiment': { body: { variant: 'B' } }, '/api/experiment/events': { status: 204 } })

    render(<UpgradeButton />)

    expect(await screen.findByRole('button', { name: 'Remove ads' })).toBeVisible()
    expect(screen.queryByRole('button', { name: 'Upgrade' })).not.toBeInTheDocument()
  })

  it('records a view once the button is shown', async () => {
    const fetchMock = mockFetch({
      '/api/experiment': { body: { variant: 'A' } },
      '/api/experiment/events': { status: 204 },
    })

    render(<UpgradeButton />)
    await screen.findByRole('button', { name: 'Upgrade' })

    await waitFor(() => expect(sentEvents(fetchMock)).toEqual(['VIEW']))
  })

  it('records a click and tells the visitor what happens next', async () => {
    const fetchMock = mockFetch({
      '/api/experiment': { body: { variant: 'B' } },
      '/api/experiment/events': { status: 204 },
    })
    render(<UpgradeButton />)

    await userEvent.click(await screen.findByRole('button', { name: 'Remove ads' }))

    expect(sentEvents(fetchMock)).toEqual(['VIEW', 'CLICK'])
    expect(screen.getByRole('status')).toHaveTextContent('Plans are coming soon.')
  })

  it('can be reached and pressed with the keyboard', async () => {
    const fetchMock = mockFetch({
      '/api/experiment': { body: { variant: 'A' } },
      '/api/experiment/events': { status: 204 },
    })
    render(<UpgradeButton />)
    const button = await screen.findByRole('button', { name: 'Upgrade' })

    await userEvent.tab()
    expect(button).toHaveFocus()
    await userEvent.keyboard('{Enter}')

    expect(sentEvents(fetchMock)).toEqual(['VIEW', 'CLICK'])
  })

  it('shows no button and records nothing when the variant can’t be loaded', async () => {
    const fetchMock = mockFetch({ '/api/experiment': { status: 500 } })

    render(<UpgradeButton />)

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith('/api/experiment'))
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
    expect(sentEvents(fetchMock)).toEqual([])
  })
})
