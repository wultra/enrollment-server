-- testSubmitDocumentsV2_newDocumentsAreSubmitted_responseOk
insert into es_onboarding_process (id, identification_data, activation_id, status, custom_data, timestamp_created, error_score)
values ( 'b9d4cf32-3e3c-4bb1-8f66-5a3c91fe2f8b', '{}', 'b7717831-4ed3-4597-88c1-b4646b91a76f', 'VERIFICATION_IN_PROGRESS', '{}', now(), 0);

insert into es_identity_verification (id, activation_id, user_id, process_id, status, phase, timestamp_created)
values ('e0e8d44b-f51e-4cdf-9bc6-3cc2c0850b01', 'b7717831-4ed3-4597-88c1-b4646b91a76f', 'test-user', 'b9d4cf32-3e3c-4bb1-8f66-5a3c91fe2f8b', 'IN_PROGRESS', 'DOCUMENT_UPLOAD', now());

-- testSubmitDocumentsV2_documentsAreResubmitted_responseOk
insert into es_onboarding_process (id, identification_data, activation_id, status, custom_data, timestamp_created, error_score)
values ( 'c3c2c4c1-2a84-4c6e-a8bd-4e3b824d5c91', '{}', '8e4a0b0a-84f3-4c71-9cc0-c9ac7b3f6593', 'VERIFICATION_IN_PROGRESS', '{}', now(), 0);

insert into es_identity_verification (id, activation_id, user_id, process_id, status, phase, timestamp_created)
values ('1f9f3d7c-5c41-4f62-8c18-97af2df6bd62', '8e4a0b0a-84f3-4c71-9cc0-c9ac7b3f6593', 'test-user-2', 'c3c2c4c1-2a84-4c6e-a8bd-4e3b824d5c91', 'IN_PROGRESS', 'DOCUMENT_UPLOAD', now());

insert into es_document_verification(id, activation_id, identity_verification_id, type, side, status, filename, upload_id, timestamp_created, used_for_verification)
values ('bc8b0f4a-8a6f-4dc7-ae2b-05d6ca059e33', '8e4a0b0a-84f3-4c71-9cc0-c9ac7b3f6593', '1f9f3d7c-5c41-4f62-8c18-97af2df6bd62', 'ID_CARD', 'FRONT', 'UPLOAD_IN_PROGRESS', 'id_card_front.jpg', 'fa709f49-ac67-4974-9d45-6f946ffdfb63', now(), true),
       ('8f3a3887-6f7a-4d84-b907-3bdf257cbb46', '8e4a0b0a-84f3-4c71-9cc0-c9ac7b3f6593', '1f9f3d7c-5c41-4f62-8c18-97af2df6bd62', 'ID_CARD', 'BACK', 'UPLOAD_IN_PROGRESS', 'id_card_back.jpg', '79ae47a3-40c3-42a2-b722-1282ff2d7679', now(), true);

-- testCleanup_fullDocumentVerificationAndPresenceCheckCleanup_allDataAreCleaned
INSERT INTO es_onboarding_process_configuration (id, process_type, config)
VALUES (101, 'onboardingTest', '{}');

INSERT INTO es_onboarding_process (id, identification_data, user_id, activation_id, status, activation_removed, error_score, custom_data, timestamp_created, timestamp_last_updated, timestamp_finished, process_config_id)
VALUES ('0c47c3cf-6f77-4f52-93f2-934efc6322dd', e'{
  "birthDate" : "1970-03-21",
  "clientNumber" : "264962080414477774"
}', 'mockuser_264962080414477774', '5d1fbb02-94b9-4a49-a0fd-7cda061ca655', 'VERIFICATION_IN_PROGRESS', false,  0, '{"locale":"cs","ipAddress":"127.0.0.1","userAgent":"ReactorNetty/1.2.12"}', '2026-03-02 12:08:19.090000', '2026-03-02 12:08:48.009000', '2026-03-02 12:08:48.009000', 101);

INSERT INTO es_identity_verification (id, activation_id, user_id, process_id, status, phase, session_info, timestamp_created, timestamp_last_updated, timestamp_finished)
VALUES ('08923a0a-5f4c-41dc-acda-75bb921c75a4', '5d1fbb02-94b9-4a49-a0fd-7cda061ca655', 'mockuser_264962080414477774', '0c47c3cf-6f77-4f52-93f2-934efc6322dd', 'ACCEPTED', 'COMPLETED', '{"sessionAttributes":{"imageUploaded":true,"mockVerificationToken":"d988b368-ff76-4ec1-9e2d-3fbbe61cb854","timestampLastUsed":1772449728009}}', '2026-03-02 12:08:19.405000', '2026-03-02 12:08:48.009000', '2026-03-02 12:08:48.009000');

INSERT INTO es_document_verification (id, activation_id, identity_verification_id, type, side, other_side_id, provider_name, status, filename, upload_id, verification_id, photo_id, verification_score, used_for_verification, timestamp_created, timestamp_uploaded, timestamp_verified, timestamp_last_updated)
VALUES ('7a56e1fb-5cc7-4850-ba7e-2b9c4d754d16', '5d1fbb02-94b9-4a49-a0fd-7cda061ca655', '08923a0a-5f4c-41dc-acda-75bb921c75a4', 'ID_CARD', 'BACK', '897f4d47-7962-43af-8c5d-b4b142e02085', 'microblink', 'VERIFICATION_IN_PROGRESS', 'images/id_card_mock_back.png', '03a2785a-a563-4feb-bde4-3ce0367e4e9d', '4d36b379-c721-4078-b1c2-6dcb974a9999', '6a1ded3b-ab40-42bf-958a-968a380acc5d', 10, true, '2026-03-02 12:08:19.539000', '2026-03-02 12:08:19.539000', '2026-03-02 12:08:24.014000',  '2026-03-02 12:08:39.008000'),
       ('40e7f9a6-6cbe-478e-b1ee-98b8ad797b9e', '5d1fbb02-94b9-4a49-a0fd-7cda061ca655', '08923a0a-5f4c-41dc-acda-75bb921c75a4', 'DRIVING_LICENSE', 'FRONT', null, 'microblink', 'VERIFICATION_IN_PROGRESS', 'images/driving_license_mock_front.png', '62338ec9-ff2f-4751-9762-e09d34c62796', '4d36b379-c721-4078-b1c2-6dcb974a9999', '77fcfade-cc91-42d6-bba7-e91d0af68a1d', 10, true, '2026-03-02 12:08:28.922000', '2026-03-02 12:08:28.922000', '2026-03-02 12:08:33.012000',  '2026-03-02 12:08:39.008000'),
       ('897f4d47-7962-43af-8c5d-b4b142e02085', '5d1fbb02-94b9-4a49-a0fd-7cda061ca655', '08923a0a-5f4c-41dc-acda-75bb921c75a4', 'ID_CARD', 'FRONT', '7a56e1fb-5cc7-4850-ba7e-2b9c4d754d16', 'microblink', 'VERIFICATION_IN_PROGRESS', 'images/id_card_mock_front.png', 'c8bf612b-6718-4614-b150-ce17bc39221c', '4d36b379-c721-4078-b1c2-6dcb974a9999', '6a1ded3b-ab40-42bf-958a-968a380acc5d', 10, true, '2026-03-02 12:08:19.539000', '2026-03-02 12:08:19.539000', '2026-03-02 12:08:24.014000',  '2026-03-02 12:08:39.008000');

INSERT INTO es_processed_document_data (id, data_type, data, timestamp_created)
VALUES ('6a1ded3b-ab40-42bf-958a-968a380acc5d', 'FACE_IMAGE', E'\\x'::bytea, '2026-03-02 12:08:19.639000'),
       ('77fcfade-cc91-42d6-bba7-e91d0af68a1d', 'FACE_IMAGE', E'\\x'::bytea, '2026-03-02 12:08:28.965000');

INSERT INTO es_document_data (id, data, timestamp_created)
VALUES ('c8bf612b-6718-4614-b150-ce17bc39221c', E'\\x'::bytea, '2026-03-02 12:08:19.545000'),
       ('03a2785a-a563-4feb-bde4-3ce0367e4e9d', E'\\x'::bytea, '2026-03-02 12:08:19.545000'),
       ('62338ec9-ff2f-4751-9762-e09d34c62796', E'\\x'::bytea, '2026-03-02 12:08:28.926000');

INSERT INTO es_document_result (id, document_verification_id, phase, verification_result, extracted_data, timestamp_created)
VALUES (3592, '897f4d47-7962-43af-8c5d-b4b142e02085', 'UPLOAD', '{}', '{}', '2026-03-02 12:08:19.539000'),
       (3593, '7a56e1fb-5cc7-4850-ba7e-2b9c4d754d16', 'UPLOAD', '{}', '{}', '2026-03-02 12:08:19.539000'),
       (3594, '40e7f9a6-6cbe-478e-b1ee-98b8ad797b9e', 'UPLOAD', '{}', '{}', '2026-03-02 12:08:28.922000');




