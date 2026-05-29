-- Add unique constraint to ticket code to prevent race conditions
ALTER TABLE tickets ADD CONSTRAINT tickets_code_unique UNIQUE (code);

-- Create sequence for ticket code generation
CREATE SEQUENCE IF NOT EXISTS ticket_code_seq START WITH 1001;
