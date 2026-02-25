import React from 'react';

interface CollaboratorBadgeProps {
  username: string;
  color?: string;
}

const CollaboratorBadge: React.FC<CollaboratorBadgeProps> = ({ username, color }) => {
  const getInitials = (name: string): string => {
    return name
      .split(' ')
      .map((part) => part[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  const initials = getInitials(username);
  const bgStyle = color ? { backgroundColor: color } : undefined;

  return (
    <div className="flex items-center space-x-2">
      <div
        className="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-semibold"
        style={bgStyle}
        title={username}
      >
        {initials}
      </div>
      <span className="text-sm text-gray-700">{username}</span>
    </div>
  );
};

export default CollaboratorBadge;