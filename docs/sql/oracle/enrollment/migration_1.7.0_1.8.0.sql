-- Changeset enrollment-server/1.8.x/20240620-add-resultTexts.xml::1::Lubos Racansky
-- Add result_texts column
ALTER TABLE es_operation_template ADD result_texts CLOB;
