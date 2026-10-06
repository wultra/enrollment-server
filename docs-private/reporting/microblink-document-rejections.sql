/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

-- Report of messages and failed checks of documents rejected by Microblink in the last 30 days.
-- See https://github.com/wultra/enrollment-server/issues/1936
--
-- Requires PostgreSQL 12+ (jsonpath).
-- Column `count` is the number of documents (each upload attempt, including DISPOSED, counts once).
-- Only checks on the 2nd level of the tree are reported, deeper failures are ignored.
--
-- Usage: psql --csv -f microblink-document-rejections.sql


-- Messages
WITH documents AS (
    SELECT DISTINCT ON (verification.id)
        verification.id,
        result.verification_result::jsonb AS verification_result
    FROM es_document_verification verification
    INNER JOIN es_document_result result
        ON verification.id = result.document_verification_id
    WHERE COALESCE(verification.timestamp_uploaded, verification.timestamp_created) BETWEEN now() - INTERVAL '30 day' AND now()
      AND verification.side = 'FRONT'
      AND verification.status IN ('REJECTED', 'DISPOSED')
      AND verification.reject_origin = 'DOCUMENT_VERIFICATION'
      AND verification.provider_name = 'microblink'
      AND (verification.reject_reason LIKE 'Rejected by provider%' OR verification.reject_reason = 'Other')
      AND result.verification_result LIKE '{%'
    ORDER BY verification.id, result.timestamp_created DESC, result.id DESC
)
SELECT
    message ->> 'code' AS code,
    COUNT(DISTINCT documents.id) AS count
FROM documents,
    jsonb_array_elements(documents.verification_result -> 'messages') AS message
GROUP BY code
ORDER BY count DESC, code;


-- Failed checks
WITH documents AS (
    SELECT DISTINCT ON (verification.id)
        verification.id,
        result.verification_result::jsonb AS verification_result
    FROM es_document_verification verification
    INNER JOIN es_document_result result
        ON verification.id = result.document_verification_id
    WHERE COALESCE(verification.timestamp_uploaded, verification.timestamp_created) BETWEEN now() - INTERVAL '30 day' AND now()
      AND verification.side = 'FRONT'
      AND verification.status IN ('REJECTED', 'DISPOSED')
      AND verification.reject_origin = 'DOCUMENT_VERIFICATION'
      AND verification.provider_name = 'microblink'
      AND (verification.reject_reason LIKE 'Rejected by provider%' OR verification.reject_reason = 'Other')
      AND result.verification_result LIKE '{%'
    ORDER BY verification.id, result.timestamp_created DESC, result.id DESC
)
SELECT
    check_result ->> 'name' AS name,
    check_result ->> 'type' AS type,
    COUNT(DISTINCT documents.id) AS count
FROM documents,
    jsonb_path_query(documents.verification_result, '$.checks[*].checks[*] ? (@.result == "Fail")') AS check_result
GROUP BY name, type
ORDER BY count DESC, name, type;
