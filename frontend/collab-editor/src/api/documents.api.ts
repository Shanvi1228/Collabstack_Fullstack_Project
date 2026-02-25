import client from './client';
import type { ApiResponse } from '../types/common.types';
import type {
  DocumentResponse,
  DocumentCreateRequest,
  CollaboratorResponse,
  ChatRequest,
  ChatResponse,
} from '../types/document.types';

export const getDocuments = (): Promise<DocumentResponse[]> =>
  client.get<ApiResponse<DocumentResponse[]>>('/documents').then(r => r.data.data);

export const createDocument = (req: DocumentCreateRequest): Promise<DocumentResponse> =>
  client.post<ApiResponse<DocumentResponse>>('/documents', req).then(r => r.data.data);

export const getDocument = (id: string): Promise<DocumentResponse> =>
  client.get<ApiResponse<DocumentResponse>>(`/documents/${id}`).then(r => r.data.data);

export const updateTitle = (id: string, title: string): Promise<DocumentResponse> =>
  client.put<ApiResponse<DocumentResponse>>(`/documents/${id}/title`, { title }).then(r => r.data.data);

export const deleteDocument = (id: string): Promise<void> =>
  client.delete(`/documents/${id}`);

export const addCollaborator = (id: string, email: string, role: string): Promise<CollaboratorResponse> =>
  client.post<ApiResponse<CollaboratorResponse>>(`/documents/${id}/collaborators`, { email, role }).then(r => r.data.data);

export const getCollaborators = (id: string): Promise<CollaboratorResponse[]> =>
  client.get<ApiResponse<CollaboratorResponse[]>>(`/documents/${id}/collaborators`).then(r => r.data.data);

export const generateShareToken = (id: string): Promise<string> =>
  client.post<ApiResponse<string>>(`/documents/${id}/share`).then(r => r.data.data);

export const joinViaShareToken = (shareToken: string): Promise<DocumentResponse> =>
  client.post<ApiResponse<DocumentResponse>>(`/documents/join/${shareToken}`).then(r => r.data.data);

export const chatWithDocument = (id: string, req: ChatRequest): Promise<ChatResponse> =>
  client.post<ApiResponse<ChatResponse>>(`/documents/${id}/chat`, req).then(r => r.data.data);