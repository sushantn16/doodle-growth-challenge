import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { Variant } from '../api/experiment'
import { mockFetch, sentEvents } from '../test/mockFetch'
import { UpgradeButton } from './UpgradeButton'

let fetchMock: ReturnType<typeof mockFetch>

beforeEach(() => {
  fetchMock = mockFetch({ '/api/experiment/events': { status: 204 } })
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function renderButton(variant: Promise<Variant>) {
  return render(<UpgradeButton variantRequest={variant} />)
}

describe('UpgradeButton', () => {
  it('shows "Upgrade" to a visitor in version A', async () => {
    renderButton(Promise.resolve('A'))

    expect(await screen.findByRole('button', { name: 'Upgrade' })).toBeVisible()
  })

  it('shows "Remove ads" to a visitor in version B', async () => {
    renderButton(Promise.resolve('B'))

    expect(await screen.findByRole('button', { name: 'Remove ads' })).toBeVisible()
    expect(screen.queryByRole('button', { name: 'Upgrade' })).not.toBeInTheDocument()
  })

  it('records a view, with the version shown, once the button is on screen', async () => {
    renderButton(Promise.resolve('A'))
    await screen.findByRole('button', { name: 'Upgrade' })

    await waitFor(() => expect(sentEvents(fetchMock)).toEqual(['VIEW A']))
  })

  it('records a click and tells the visitor what happens next', async () => {
    renderButton(Promise.resolve('B'))

    await userEvent.click(await screen.findByRole('button', { name: 'Remove ads' }))

    expect(sentEvents(fetchMock)).toEqual(['VIEW B', 'CLICK B'])
    expect(screen.getByRole('status')).toHaveTextContent('Plans are coming soon.')
  })

  it('records a click from the keyboard', async () => {
    renderButton(Promise.resolve('A'))
    const button = await screen.findByRole('button', { name: 'Upgrade' })

    await userEvent.tab()
    expect(button).toHaveFocus()
    await userEvent.keyboard('{Enter}')
    await userEvent.keyboard(' ')

    expect(sentEvents(fetchMock)).toEqual(['VIEW A', 'CLICK A', 'CLICK A'])
  })

  it('keeps the page working when recording an event fails', async () => {
    fetchMock = mockFetch({ '/api/experiment/events': { status: 409 } })
    renderButton(Promise.resolve('A'))

    await userEvent.click(await screen.findByRole('button', { name: 'Upgrade' }))

    expect(screen.getByRole('status')).toHaveTextContent('Plans are coming soon.')
  })

  it('shows no button and records nothing when the version can’t be loaded', async () => {
    renderButton(Promise.reject(new Error('backend down')))

    // The placeholder stays, so the header keeps its height.
    await waitFor(() => expect(document.querySelector('.upgrade-placeholder')).toBeInTheDocument())
    await new Promise((resolve) => setTimeout(resolve, 0))
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
    expect(sentEvents(fetchMock)).toEqual([])
  })
})
