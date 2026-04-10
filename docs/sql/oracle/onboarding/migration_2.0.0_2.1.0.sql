-- Changeset enrollment-server-onboarding/2.0.x/20251215-add-tag-2.0.0.xml::1::Lubos Racansky
-- Changeset enrollment-server-onboarding/2.1.x/20260330-audit-subject-id.xml::1::Pavel Sindelar
-- Add subject_id column to audit_log table
ALTER TABLE audit_log ADD subject_id VARCHAR2(256);

-- Changeset enrollment-server-onboarding/2.1.x/20260330-audit-subject-id.xml::2::Pavel Sindelar
-- Create a new index on audit_log(subject_id)
CREATE INDEX audit_log_subject_id_idx ON audit_log(subject_id);

-- Changeset docs/db/changelog/changesets/enrollment-server-onboarding/2.1.x/20260408-personal-data-cleaned-timestamp.xml::1::Michal Rozehnal
-- Create a new column timestamp_personal_data_cleaned in es_onboarding_process table to store the timestamp when the identity data was cleaned
ALTER TABLE es_onboarding_process ADD timestamp_personal_data_cleaned TIMESTAMP;

-- Changeset docs/db/changelog/changesets/enrollment-server-onboarding/2.1.x/20260408-personal-data-cleaned-timestamp.xml::2::Michal Rozehnal
-- Create a new index es_onboarding_process_timestamp_personal_data_cleaned_idx
CREATE INDEX es_onboarding_process_timestamp_personal_data_cleaned_idx ON es_onboarding_process(timestamp_personal_data_cleaned);

-- Changeset docs/db/changelog/changesets/enrollment-server-onboarding/2.1.x/20260408-personal-data-cleaned-timestamp.xml::3::Michal Rozehnal
-- Create a new index es_identity_verification_process_id_idx
CREATE INDEX es_identity_verification_process_id_idx ON es_identity_verification(process_id);

-- Changeset docs/db/changelog/changesets/enrollment-server-onboarding/2.1.x/20260408-personal-data-cleaned-timestamp.xml::4::Michal Rozehnal
-- Create a new column document_verification_id in es_document_data table
ALTER TABLE es_document_data ADD document_verification_id VARCHAR2(36);

ALTER TABLE es_document_data ADD CONSTRAINT es_document_verification_id_fk FOREIGN KEY (document_verification_id) REFERENCES es_document_verification (id);

-- Changeset docs/db/changelog/changesets/enrollment-server-onboarding/2.1.x/20260408-personal-data-cleaned-timestamp.xml::5::Michal Rozehnal
-- Create a new index es_document_data_document_verification_id_idx
CREATE INDEX es_document_data_document_verification_id_idx ON es_document_data(document_verification_id);
