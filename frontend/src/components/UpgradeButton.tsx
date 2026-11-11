import { useEffect, useState } from 'react'
import { sendEvent, type Variant } from '../api/experiment'
import { useAsync } from '../hooks/useAsync'

/** Version B changes only the text. Everything else about the button stays the same. */
const LABELS: Record<Variant, string> = {
  A: 'Upgrade',
  B: 'Remove ads',
}

/** `variantRequest` is the pending request for the visitor's version, started once in main.tsx. */
export function UpgradeButton({ variantRequest }: { variantRequest: Promise<Variant> }) {
  const request = useAsync(variantRequest)
  // Without a version, show no button. Falling back to A would put extra visitors in A.
  const variant = request.status === 'ready' ? request.value : null
  const [clicked, setClicked] = useState(false)

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
