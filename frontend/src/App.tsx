import { PollList } from './PollList'

export default function App() {
  return (
    <div className="page">
      <header className="header">
        <h1 className="header-title">My polls</h1>
      </header>
      <main>
        <PollList />
      </main>
    </div>
  )
}
