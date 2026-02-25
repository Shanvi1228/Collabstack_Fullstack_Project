import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DocumentListPage from './pages/DocumentListPage';
import EditorPage from './pages/EditorPage';
import JoinPage from './pages/JoinPage';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';

const App: React.FC = () => {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route path="/documents" element={<DocumentListPage />} />
        <Route path="/documents/join/:shareToken" element={<JoinPage />} />
        <Route path="/documents/:id" element={<EditorPage />} />
        <Route path="/" element={<Navigate to="/documents" replace />} />
      </Route>
    </Routes>
  );
};

export default App;
