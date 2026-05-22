-- Changeset enrollment-server-onboarding/2.2.x/20260521-add-external-user-id.xml::1::Lubos Racansky
-- Add external_user_id column to es_onboarding_process table
ALTER TABLE es_onboarding_process ADD external_user_id VARCHAR2(256);
