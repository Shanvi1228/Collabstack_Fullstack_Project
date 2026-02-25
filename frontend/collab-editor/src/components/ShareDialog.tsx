import React, { useState, useEffect } from 'react';
import {
    addCollaborator,
    generateShareToken,
    getCollaborators,
} from '../api/documents.api';
import type { CollaboratorResponse } from '../types/document.types';

interface ShareDialogProps {
    documentId: string;
    isOpen: boolean;
    onClose: () => void;
}

const ShareDialog: React.FC<ShareDialogProps> = ({ documentId, isOpen, onClose }) => {
    const [email, setEmail] = useState('');
    const [role, setRole] = useState<'EDITOR' | 'VIEWER'>('EDITOR');
    const [shareLink, setShareLink] = useState('');
    const [collaborators, setCollaborators] = useState<CollaboratorResponse[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [copied, setCopied] = useState(false);

    useEffect(() => {
        if (isOpen) {
            loadCollaborators();
        }
    }, [isOpen, documentId]);

    const loadCollaborators = async () => {
        try {
            const result = await getCollaborators(documentId);
            setCollaborators(result);
        } catch {
            // silent
        }
    };

    const handleAddCollaborator = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!email.trim()) return;
        setIsLoading(true);
        setError('');
        setSuccess('');

        try {
            await addCollaborator(documentId, email, role);
            setSuccess(`Added ${email} as ${role.toLowerCase()}`);
            setEmail('');
            loadCollaborators();
        } catch (err: unknown) {
            const msg =
                err && typeof err === 'object' && 'response' in err
                    ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
                    : undefined;
            setError(msg || 'Failed to add collaborator');
        } finally {
            setIsLoading(false);
        }
    };

    const handleGenerateLink = async () => {
        setIsLoading(true);
        setError('');
        try {
            const token = await generateShareToken(documentId);
            const link = `${window.location.origin}/documents/join/${token}`;
            setShareLink(link);
        } catch {
            setError('Failed to generate share link');
        } finally {
            setIsLoading(false);
        }
    };

    const handleCopyLink = async () => {
        try {
            await navigator.clipboard.writeText(shareLink);
            setCopied(true);
            setTimeout(() => setCopied(false), 2000);
        } catch {
            // fallback
            const textarea = document.createElement('textarea');
            textarea.value = shareLink;
            document.body.appendChild(textarea);
            textarea.select();
            document.execCommand('copy');
            document.body.removeChild(textarea);
            setCopied(true);
            setTimeout(() => setCopied(false), 2000);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-md mx-4">
                {/* Header */}
                <div className="flex items-center justify-between px-6 py-4 border-b">
                    <h2 className="text-lg font-semibold text-gray-900">Share Document</h2>
                    <button
                        onClick={onClose}
                        className="text-gray-400 hover:text-gray-600 text-xl leading-none"
                    >
                        &times;
                    </button>
                </div>

                <div className="px-6 py-4 space-y-5">
                    {/* Add by email */}
                    <form onSubmit={handleAddCollaborator}>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Add people by email
                        </label>
                        <div className="flex space-x-2">
                            <input
                                type="email"
                                placeholder="user@example.com"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                className="flex-1 px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                            <select
                                value={role}
                                onChange={(e) => setRole(e.target.value as 'EDITOR' | 'VIEWER')}
                                className="px-2 py-2 border border-gray-300 rounded-md text-sm"
                            >
                                <option value="EDITOR">Editor</option>
                                <option value="VIEWER">Viewer</option>
                            </select>
                            <button
                                type="submit"
                                disabled={isLoading || !email.trim()}
                                className="px-4 py-2 bg-blue-600 text-white rounded-md text-sm hover:bg-blue-700 disabled:bg-gray-300"
                            >
                                Add
                            </button>
                        </div>
                    </form>

                    {/* Share link */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Share via link
                        </label>
                        {shareLink ? (
                            <div className="flex space-x-2">
                                <input
                                    type="text"
                                    readOnly
                                    value={shareLink}
                                    className="flex-1 px-3 py-2 border border-gray-300 rounded-md text-sm bg-gray-50"
                                />
                                <button
                                    onClick={handleCopyLink}
                                    className="px-4 py-2 bg-gray-100 text-gray-700 rounded-md text-sm hover:bg-gray-200 border border-gray-300"
                                >
                                    {copied ? 'Copied!' : 'Copy'}
                                </button>
                            </div>
                        ) : (
                            <button
                                onClick={handleGenerateLink}
                                disabled={isLoading}
                                className="w-full px-4 py-2 border border-gray-300 text-gray-700 rounded-md text-sm hover:bg-gray-50"
                            >
                                Generate shareable link
                            </button>
                        )}
                    </div>

                    {/* Messages */}
                    {error && <p className="text-sm text-red-600">{error}</p>}
                    {success && <p className="text-sm text-green-600">{success}</p>}

                    {/* Collaborators list */}
                    {collaborators.length > 0 && (
                        <div>
                            <h3 className="text-sm font-medium text-gray-700 mb-2">Current collaborators</h3>
                            <ul className="space-y-2 max-h-40 overflow-y-auto">
                                {collaborators.map((c) => (
                                    <li
                                        key={c.userId}
                                        className="flex items-center justify-between py-1 text-sm"
                                    >
                                        <span className="text-gray-900">
                                            {c.username} <span className="text-gray-400">({c.email})</span>
                                        </span>
                                        <span className="text-xs px-2 py-0.5 rounded bg-gray-100 text-gray-600">
                                            {c.role}
                                        </span>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="px-6 py-3 border-t flex justify-end">
                    <button
                        onClick={onClose}
                        className="px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-md"
                    >
                        Done
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ShareDialog;
