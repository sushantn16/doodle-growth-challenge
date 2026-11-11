import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { fetchVariant } from './api/experiment'
import { fetchPolls } from './api/polls'

// Start both requests once per page load, before React renders. Inside a component, StrictMode
// would send them twice in development, and two version requests without a cookie would create
// two visitor ids, of which the browser keeps only one.
const variantRequest = fetchVariant()
const pollsRequest = fetchPolls()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App variantRequest={variantRequest} pollsRequest={pollsRequest} />
  </StrictMode>,
)
