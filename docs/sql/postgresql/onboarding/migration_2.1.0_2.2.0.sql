-- Changeset enrollment-server-onboarding/2.2.x/20260521-add-external-user-id.xml::1::Lubos Racansky
-- Add external_user_id column to es_onboarding_process table
ALTER TABLE es_onboarding_process ADD external_user_id VARCHAR(256);

-- Changeset enrollment-server-onboarding/2.2.x/20261119-add-identity-verification-process-id-index.xml::1::Lubos Racansky
CREATE INDEX es_identity_verification_process_id_idx ON es_identity_verification(process_id);
