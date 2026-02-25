import { useState, useEffect, useRef, useCallback } from 'react';
import type { OperationMessage } from '../types/document.types';
import { useAuthStore } from '../store/useAuthStore';
import { useDocumentStore } from '../store/useDocumentStore';

interface UseDocumentWebSocketReturn {
  isConnected: boolean;
  sendOperation: (op: OperationMessage) => void;
  sendCursor: (from: number, to: number) => void;
  wsRole: string | null;
}

export const useDocumentWebSocket = (
  documentId: string,
  onRemoteOperation: (op: OperationMessage) => void
): UseDocumentWebSocketReturn => {
  const [isConnected, setIsConnected] = useState(false);
  const [wsRole, setWsRole] = useState<string | null>(null);
  const socketRef = useRef<WebSocket | null>(null);
  const token = useAuthStore((s) => s.token);
  const { addConnectedUser, removeConnectedUser, setConnectedUsers, updateCursor, clearConnectedUsers } =
    useDocumentStore();

  useEffect(() => {
    if (!token || !documentId) return;

    const wsUrl = `${import.meta.env.VITE_WS_BASE_URL}/ws/documents/${documentId}?token=${token}`;
    const ws = new WebSocket(wsUrl);
    socketRef.current = ws;

    ws.onopen = () => {
      console.log('WebSocket connected');
      setIsConnected(true);
    };

    ws.onmessage = (event) => {
      try {
        const message: OperationMessage = JSON.parse(event.data);

        switch (message.type) {
          case 'PRESENCE':
            if (message.event === 'JOIN' && message.userId && message.username) {
              addConnectedUser(message.userId, message.username);
            } else if (message.event === 'LEAVE' && message.userId) {
              removeConnectedUser(message.userId);
            }
            break;

          case 'PRESENCE_LIST':
            if (message.users) {
              setConnectedUsers(message.users);
            }
            break;

          case 'CURSOR':
            if (message.userId != null && message.from != null && message.to != null) {
              updateCursor(message.userId, message.from, message.to);
            }
            break;

          case 'SYNC':
            // Sync message contains full content — treat as remote operation
            if (message.content) {
              onRemoteOperation(message);
            }
            // Store the user's role from the server
            if (message.role) {
              setWsRole(message.role);
            }
            break;

          case 'OPERATION':
            onRemoteOperation(message);
            break;
        }
      } catch (error) {
        console.error('Error parsing WebSocket message:', error);
      }
    };

    ws.onclose = () => {
      console.log('WebSocket disconnected');
      setIsConnected(false);
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    return () => {
      ws.close();
      clearConnectedUsers();
    };
  }, [documentId, token]);

  const sendOperation = useCallback((op: OperationMessage) => {
    if (socketRef.current?.readyState === WebSocket.OPEN) {
      socketRef.current.send(JSON.stringify(op));
    }
  }, []);

  const sendCursor = useCallback((from: number, to: number) => {
    if (socketRef.current?.readyState === WebSocket.OPEN) {
      socketRef.current.send(
        JSON.stringify({ type: 'CURSOR', from, to })
      );
    }
  }, []);

  return { isConnected, sendOperation, sendCursor, wsRole };
};