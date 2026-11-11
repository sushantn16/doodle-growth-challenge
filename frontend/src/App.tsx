import type { Variant } from './api/experiment'
import type { Poll } from './api/polls'
import { PollList } from './components/PollList'
import { UpgradeButton } from './components/UpgradeButton'

interface Props {
  variantRequest: Promise<Variant>
  pollsRequest: Promise<Poll[]>
}

export default function App({ variantRequest, pollsRequest }: Props) {
  return (
    <div className="page">
      <header className="header">
        <h1 className="header-title">My polls</h1>
        <UpgradeButton variantRequest={variantRequest} />
      </header>
      <main>
        <PollList pollsRequest={pollsRequest} />
      </main>
    </div>
  )
}
