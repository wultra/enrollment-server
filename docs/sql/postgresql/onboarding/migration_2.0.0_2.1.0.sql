-- Changeset enrollment-server-onboarding/2.0.x/20251215-add-tag-2.0.0.xml::1::Lubos Racansky
-- Changeset enrollment-server-onboarding/2.1.x/20260330-audit-subject-id.xml::1::Pavel Sindelar
-- Add subject_id column to audit_log table
ALTER TABLE audit_log ADD subject_id VARCHAR(256);

-- Changeset enrollment-server-onboarding/2.1.x/20260330-audit-subject-id.xml::2::Pavel Sindelar
-- Create a new index on audit_log(subject_id)
CREATE INDEX audit_log_subject_id_idx ON audit_log(subject_id);
