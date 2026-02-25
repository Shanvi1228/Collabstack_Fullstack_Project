import { create } from 'zustand';
import type { ConnectedUser } from '../types/document.types';

const COLLABORATOR_COLORS = [
  '#ef4444', // red
  '#3b82f6', // blue
  '#22c55e', // green
  '#eab308', // yellow
  '#a855f7', // purple
  '#ec4899', // pink
  '#6366f1', // indigo
  '#14b8a6', // teal
];

function getColorForUser(userId: string): string {
  let hash = 0;
  for (let i = 0; i < userId.length; i++) {
    hash = userId.charCodeAt(i) + ((hash << 5) - hash);
  }
  return COLLABORATOR_COLORS[Math.abs(hash) % COLLABORATOR_COLORS.length];
}

interface DocumentState {
  activeDocumentId: string | null;
  connectedUsers: ConnectedUser[];
  setActiveDocument: (id: string | null) => void;
  addConnectedUser: (userId: string, username: string) => void;
  removeConnectedUser: (userId: string) => void;
  setConnectedUsers: (users: Array<{ userId: string; username: string }>) => void;
  updateCursor: (userId: string, from: number, to: number) => void;
  clearConnectedUsers: () => void;
}

export const useDocumentStore = create<DocumentState>((set) => ({
  activeDocumentId: null,
  connectedUsers: [],
  setActiveDocument: (id) => set({ activeDocumentId: id, connectedUsers: [] }),
  addConnectedUser: (userId, username) =>
    set((state) => {
      if (state.connectedUsers.some((u) => u.userId === userId)) {
        return state;
      }
      return {
        connectedUsers: [
          ...state.connectedUsers,
          { userId, username, color: getColorForUser(userId) },
        ],
      };
    }),
  removeConnectedUser: (userId) =>
    set((state) => ({
      connectedUsers: state.connectedUsers.filter((u) => u.userId !== userId),
    })),
  setConnectedUsers: (users) =>
    set({
      connectedUsers: users.map((u) => ({
        userId: u.userId,
        username: u.username,
        color: getColorForUser(u.userId),
      })),
    }),
  updateCursor: (userId, from, to) =>
    set((state) => ({
      connectedUsers: state.connectedUsers.map((u) =>
        u.userId === userId
          ? { ...u, cursorPos: from, selectionFrom: from, selectionTo: to }
          : u
      ),
    })),
  clearConnectedUsers: () => set({ connectedUsers: [] }),
}));