import React from 'react';
import CollaboratorBadge from './CollaboratorBadge';
import type { ConnectedUser } from '../types/document.types';

interface PresenceBarProps {
  connectedUsers: ConnectedUser[];
}

const PresenceBar: React.FC<PresenceBarProps> = ({ connectedUsers }) => {
  if (connectedUsers.length === 0) {
    return null;
  }

  return (
    <div className="bg-blue-50 border-b border-blue-200 px-4 py-2">
      <div className="flex items-center space-x-4">
        <span className="text-sm text-gray-700 font-medium">
          {connectedUsers.length} {connectedUsers.length === 1 ? 'person' : 'people'} editing
        </span>
        <div className="flex items-center space-x-3">
          {connectedUsers.map((user) => (
            <CollaboratorBadge key={user.userId} username={user.username} color={user.color} />
          ))}
        </div>
      </div>
    </div>
  );
};

export default PresenceBar;