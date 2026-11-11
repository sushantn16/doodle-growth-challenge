import { renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { useAsync } from './useAsync'

describe('useAsync', () => {
  it('is loading until the promise resolves, then ready with its value', async () => {
    const promise = Promise.resolve(42)
    const { result } = renderHook(() => useAsync(promise))

    expect(result.current).toEqual({ status: 'loading' })
    await waitFor(() => expect(result.current).toEqual({ status: 'ready', value: 42 }))
  })

  it('is failed when the promise rejects', async () => {
    const promise = Promise.reject(new Error('down'))
    const { result } = renderHook(() => useAsync(promise))

    await waitFor(() => expect(result.current).toEqual({ status: 'failed' }))
  })

  it('follows a new promise and ignores the old one, even if it settles later', async () => {
    let resolveOld!: (value: string) => void
    const old = new Promise<string>((resolve) => (resolveOld = resolve))
    const { result, rerender } = renderHook(({ promise }) => useAsync(promise), { initialProps: { promise: old } })

    rerender({ promise: Promise.resolve('new') })
    await waitFor(() => expect(result.current).toEqual({ status: 'ready', value: 'new' }))

    resolveOld('old')
    await new Promise((resolve) => setTimeout(resolve, 0))
    expect(result.current).toEqual({ status: 'ready', value: 'new' })
  })
})
