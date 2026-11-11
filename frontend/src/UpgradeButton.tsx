import { useEffect, useState } from 'react'
import { sendEvent, type Variant } from './api'

/** Version B changes only the text. Everything else about the button stays the same. */
const LABELS: Record<Variant, string> = {
  A: 'Upgrade',
  B: 'Remove ads',
}

type State = { status: 'loading' } | { status: 'ready'; variant: Variant } | { status: 'failed' }

/** `variant` is the pending request for the visitor's variant. It is started once, in main.tsx. */
export function UpgradeButton({ variant: variantRequest }: { variant: Promise<Variant> }) {
  const [state, setState] = useState<State>({ status: 'loading' })
  const [clicked, setClicked] = useState(false)

  useEffect(() => {
    let cancelled = false
    variantRequest
      .then((variant) => !cancelled && setState({ status: 'ready', variant }))
      // Without a variant, show no button. Falling back to A would put extra visitors in A.
      .catch(() => !cancelled && setState({ status: 'failed' }))
    return () => {
      cancelled = true
    }
  }, [variantRequest])

  const variant = state.status === 'ready' ? state.variant : null

  // "Saw" means the button is on the screen. The backend counts each visitor once,
  // so a reload, or StrictMode running this effect twice in development, is harmless.
  useEffect(() => {
    if (variant) {
      sendEvent('VIEW', variant)
    }
  }, [variant])

  function handleClick() {
    if (variant) {
      sendEvent('CLICK', variant)
    }
    setClicked(true)
  }

  return (
    <div className="upgrade">
      {/* Reserves the button's height while loading, so nothing on the page jumps. */}
      {variant ? (
        <button type="button" className="button-primary" onClick={handleClick}>
          {LABELS[variant]}
        </button>
      ) : (
        <span className="upgrade-placeholder" aria-hidden="true" />
      )}
      <p className="upgrade-status" role="status">
        {clicked ? 'Plans are coming soon.' : ''}
      </p>
    </div>
  )
}
