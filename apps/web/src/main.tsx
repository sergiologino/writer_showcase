import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode, useEffect } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App.tsx'
import './index.css'
import { applyTheme, getStoredTheme } from './lib/theme.ts'

const queryClient = new QueryClient()

function ThemeInit({ children }: { children: React.ReactNode }) {
  useEffect(() => {
    applyTheme(getStoredTheme())
  }, [])
  return children
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <ThemeInit>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </ThemeInit>
    </QueryClientProvider>
  </StrictMode>,
)
