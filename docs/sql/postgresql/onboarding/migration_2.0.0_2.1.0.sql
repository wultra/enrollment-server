-- Changeset enrollment-server-onboarding/2.1.x/20251219-process-target-activation-id.xml::1::Lubos Racansky
-- Create a new column target_activation_id to es_onboarding_process
ALTER TABLE es_onboarding_process ADD target_activation_id VARCHAR(36);
