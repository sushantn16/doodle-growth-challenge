import { useEffect, useState } from 'react'

export type AsyncState<T> = { status: 'loading' } | { status: 'ready'; value: T } | { status: 'failed' }

/**
 * Follows a promise and re-renders when it settles. Pass a promise that is created once, outside
 * render (see main.tsx), not a new one on every render.
 */
export function useAsync<T>(promise: Promise<T>): AsyncState<T> {
  // The promise is stored with its result, so a result always belongs to the current promise.
  const [settled, setSettled] = useState<{ promise: Promise<T>; state: AsyncState<T> } | null>(null)

  useEffect(() => {
    let current = true
    promise.then(
      (value) => current && setSettled({ promise, state: { status: 'ready', value } }),
      () => current && setSettled({ promise, state: { status: 'failed' } }),
    )
    return () => {
      current = false
    }
  }, [promise])

  return settled?.promise === promise ? settled.state : { status: 'loading' }
}
