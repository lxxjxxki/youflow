import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ControllerPage from './pages/ControllerPage';
import PlayerPage from './pages/PlayerPage';
import useAuthStore from './store/authStore';

/**
 * 인증된 유저만 접근할 수 있는 라우트 래퍼.
 *
 * localStorage 직접 접근 대신 authStore.isAuthenticated를 사용한다.
 * 이유: authStore가 토큰 파싱/검증을 담당하므로 App은 상태만 구독하면 된다.
 *
 * 인증되지 않은 경우 /login으로 리다이렉트한다.
 */
function PrivateRoute({ children }) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  return isAuthenticated ? children : <Navigate to="/login" replace />;
}

/**
 * 앱 최상위 라우팅 설정.
 *
 * 라우트 구조:
 *   /login       — 로그인 페이지 (공개)
 *   /register    — 회원가입 페이지 (공개)
 *   /controller  — 플레이리스트 조작 화면 (인증 필요)
 *   /player      — YouTube 풀스크린 플레이어 (인증 필요, 서브모니터에 열기)
 *   /            — /controller로 리다이렉트
 */
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/controller"
          element={
            <PrivateRoute>
              <ControllerPage />
            </PrivateRoute>
          }
        />
        <Route
          path="/player"
          element={
            <PrivateRoute>
              <PlayerPage />
            </PrivateRoute>
          }
        />
        <Route path="/" element={<Navigate to="/controller" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
