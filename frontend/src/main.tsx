import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import Arm from './arm/Arm.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Arm />
  </StrictMode>,
)