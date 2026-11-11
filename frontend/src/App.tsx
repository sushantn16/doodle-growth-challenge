import type { Variant } from './api'
import { PollList } from './PollList'
import { UpgradeButton } from './UpgradeButton'

export default function App({ variant }: { variant: Promise<Variant> }) {
  return (
    <div className="page">
      <header className="header">
        <h1 className="header-title">My polls</h1>
        <UpgradeButton variant={variant} />
      </header>
      <main>
        <PollList />
      </main>
    </div>
  )
}
