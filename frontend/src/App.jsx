import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ControllerPage from './pages/ControllerPage'
import PlayerPage from './pages/PlayerPage'

function PrivateRoute({ children }) {
  return localStorage.getItem('token') ? children : <Navigate to="/login" />
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/controller" element={<PrivateRoute><ControllerPage /></PrivateRoute>} />
        <Route path="/player" element={<PrivateRoute><PlayerPage /></PrivateRoute>} />
        <Route path="/" element={<Navigate to="/controller" />} />
      </Routes>
    </BrowserRouter>
  )
}
