import type { ReactElement } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './components/AppShell'
import { FeedPage } from './pages/FeedPage'
import { AuthCallbackPage } from './pages/AuthCallbackPage'
import { LandingPage } from './pages/LandingPage'
import { LoginPage } from './pages/LoginPage'
import { MediaLibraryPage } from './pages/MediaLibraryPage'
import { PostEditorPage } from './pages/PostEditorPage'
import { ProfilePage } from './pages/ProfilePage'
import { PublishingChannelsPage } from './pages/PublishingChannelsPage'
import { PublicBlogPage } from './pages/PublicBlogPage'
import { PublicPostPage } from './pages/PublicPostPage'
import { RegisterPage } from './pages/RegisterPage'

function RequireAuth({ children }: { children: ReactElement }) {
  const token = localStorage.getItem('accessToken')
  if (!token) {
    return <Navigate to="/login" replace />
  }
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/auth/callback" element={<AuthCallbackPage />} />
      <Route path="/blog/:workspaceSlug" element={<PublicBlogPage />} />
      <Route path="/blog/:workspaceSlug/p/:postSlug" element={<PublicPostPage />} />
      <Route
        path="/app"
        element={
          <RequireAuth>
            <AppShell />
          </RequireAuth>
        }
      >
        <Route index element={<Navigate to="feed" replace />} />
        <Route path="feed" element={<FeedPage />} />
        <Route path="media" element={<MediaLibraryPage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="channels" element={<PublishingChannelsPage />} />
        <Route path="posts/new" element={<PostEditorPage />} />
        <Route path="posts/:id" element={<PostEditorPage />} />
      </Route>
      <Route path="/" element={<LandingPage />} />
    </Routes>
  )
}
