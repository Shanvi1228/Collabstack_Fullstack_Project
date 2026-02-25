export interface DocumentResponse {
  id: string;
  title: string;
  contentSnapshot: string;
  currentRevision: number;
  ownerUsername: string;
  collaboratorCount: number;
  userRole: 'OWNER' | 'EDITOR' | 'VIEWER';
  createdAt: string;
  updatedAt: string;
}

export interface DocumentCreateRequest {
  title: string;
  initialContent?: string;
}

export interface CollaboratorResponse {
  userId: string;
  username: string;
  email: string;
  role: string;
}

export interface OperationMessage {
  type: 'OPERATION' | 'PRESENCE' | 'SYNC' | 'CURSOR' | 'PRESENCE_LIST';
  opType?: 'INSERT' | 'DELETE' | 'REPLACE';
  position?: number;
  content?: string;
  length?: number;
  clientRevision?: number;
  userId?: string;
  username?: string;
  // CURSOR fields
  from?: number;
  to?: number;
  // PRESENCE fields
  event?: 'JOIN' | 'LEAVE';
  // PRESENCE_LIST fields
  users?: Array<{ userId: string; username: string }>;
  // SYNC fields
  revision?: number;
  role?: string;  // user's role sent with SYNC
}

export interface ConnectedUser {
  userId: string;
  username: string;
  color: string;
  cursorPos?: number;
  selectionFrom?: number;
  selectionTo?: number;
}

export interface ChatRequest {
  question: string;
}

export interface ChatResponse {
  answer: string;
  sourceSnippets: string[];
}