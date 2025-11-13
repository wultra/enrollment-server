-- Changeset enrollment-server-onboarding/2.0.x/20251111-process-configuration.xml::1::Lubos Racansky
-- Create a new sequence es_onboarding_process_configuration_seq
CREATE SEQUENCE es_onboarding_process_configuration_seq START WITH 1 INCREMENT BY 50 CACHE 20;

-- Changeset enrollment-server-onboarding/2.0.x/20251111-process-configuration.xml::2::Lubos Racansky
-- Create a new table es_onboarding_process_configuration
CREATE TABLE es_onboarding_process_configuration (id NUMBER(38, 0) DEFAULT es_onboarding_process_configuration_seq.nextval NOT NULL, process_type VARCHAR2(255) NOT NULL, config CLOB NOT NULL, CONSTRAINT PK_ES_ONBOARDING_PROCESS_CONFIGURATION PRIMARY KEY (id), CONSTRAINT es_onboarding_process_configuration_process_type_uk UNIQUE (process_type));

-- Changeset enrollment-server-onboarding/2.0.x/20251111-process-configuration.xml::3::Lubos Racansky
-- Create a new column process_config_id to es_onboarding_process
ALTER TABLE es_onboarding_process ADD process_config_id NUMBER(38, 0);

ALTER TABLE es_onboarding_process ADD CONSTRAINT es_onboarding_process_process_config_id_fk FOREIGN KEY (process_config_id) REFERENCES es_onboarding_process_configuration (id);
