import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { joinViaShareToken } from '../api/documents.api';

const JoinPage: React.FC = () => {
    const { shareToken } = useParams<{ shareToken: string }>();
    const navigate = useNavigate();
    const [error, setError] = useState('');

    useEffect(() => {
        if (!shareToken) return;

        joinViaShareToken(shareToken)
            .then((doc) => {
                navigate(`/documents/${doc.id}`, { replace: true });
            })
            .catch(() => {
                setError('Invalid or expired share link. Please ask the document owner for a new link.');
            });
    }, [shareToken, navigate]);

    if (error) {
        return (
            <div className="flex flex-col items-center justify-center h-screen">
                <div className="text-red-500 mb-4">{error}</div>
                <button
                    onClick={() => navigate('/documents')}
                    className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                >
                    Go to My Documents
                </button>
            </div>
        );
    }

    return (
        <div className="flex items-center justify-center h-screen">
            <div className="text-gray-500">Joining document...</div>
        </div>
    );
};

export default JoinPage;
