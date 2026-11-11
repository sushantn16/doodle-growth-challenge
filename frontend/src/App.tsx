import { PollList } from './PollList'
import { UpgradeButton } from './UpgradeButton'

export default function App() {
  return (
    <div className="page">
      <header className="header">
        <h1 className="header-title">My polls</h1>
        <UpgradeButton />
      </header>
      <main>
        <PollList />
      </main>
    </div>
  )
}
