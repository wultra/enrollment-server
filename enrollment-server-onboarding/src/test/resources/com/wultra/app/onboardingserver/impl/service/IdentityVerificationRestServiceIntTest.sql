-- test testSubmitDocumentsV2_newDocumentsAreSubmitted_responseOk
insert into es_onboarding_process (id, identification_data, activation_id, status, custom_data, timestamp_created, error_score)
values ( 'b9d4cf32-3e3c-4bb1-8f66-5a3c91fe2f8b', '{}', 'b7717831-4ed3-4597-88c1-b4646b91a76f', 'VERIFICATION_IN_PROGRESS', '{}', now(), 0);

insert into es_identity_verification (id, activation_id, user_id, process_id, status, phase, timestamp_created)
values ('e0e8d44b-f51e-4cdf-9bc6-3cc2c0850b01', 'b7717831-4ed3-4597-88c1-b4646b91a76f', 'test-user', 'b9d4cf32-3e3c-4bb1-8f66-5a3c91fe2f8b', 'IN_PROGRESS', 'DOCUMENT_UPLOAD', now());

-- test testSubmitDocumentsV2_documentsAreResubmitted_responseOk
insert into es_onboarding_process (id, identification_data, activation_id, status, custom_data, timestamp_created, error_score)
values ( 'c3c2c4c1-2a84-4c6e-a8bd-4e3b824d5c91', '{}', '8e4a0b0a-84f3-4c71-9cc0-c9ac7b3f6593', 'VERIFICATION_IN_PROGRESS', '{}', now(), 0);

insert into es_identity_verification (id, activation_id, user_id, process_id, status, phase, timestamp_created)
values ('1f9f3d7c-5c41-4f62-8c18-97af2df6bd62', '8e4a0b0a-84f3-4c71-9cc0-c9ac7b3f6593', 'test-user-2', 'c3c2c4c1-2a84-4c6e-a8bd-4e3b824d5c91', 'IN_PROGRESS', 'DOCUMENT_UPLOAD', now());

insert into es_document_verification(id, activation_id, identity_verification_id, type, side, status, filename, upload_id, timestamp_created, used_for_verification)
values
    ('bc8b0f4a-8a6f-4dc7-ae2b-05d6ca059e33', '8e4a0b0a-84f3-4c71-9cc0-c9ac7b3f6593', '1f9f3d7c-5c41-4f62-8c18-97af2df6bd62', 'ID_CARD', 'FRONT', 'UPLOAD_IN_PROGRESS', 'id_card_front.jpg', 'fa709f49-ac67-4974-9d45-6f946ffdfb63', now(), true),
    ('8f3a3887-6f7a-4d84-b907-3bdf257cbb46', '8e4a0b0a-84f3-4c71-9cc0-c9ac7b3f6593', '1f9f3d7c-5c41-4f62-8c18-97af2df6bd62', 'ID_CARD', 'BACK', 'UPLOAD_IN_PROGRESS', 'id_card_back.jpg', '79ae47a3-40c3-42a2-b722-1282ff2d7679', now(), true);