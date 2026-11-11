import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { fetchVariant } from './api/experiment'

// Ask for the variant once per page load, before React renders. Inside a component, StrictMode
// would run the request twice in development, and two requests without a cookie would create two
// visitor ids, of which the browser keeps only one.
const variant = fetchVariant()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App variant={variant} />
  </StrictMode>,
)
