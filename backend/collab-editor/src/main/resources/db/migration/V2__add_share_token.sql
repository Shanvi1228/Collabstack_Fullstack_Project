-- Add share_token column to documents table for link-based sharing
ALTER TABLE documents ADD COLUMN share_token VARCHAR(64) UNIQUE;
