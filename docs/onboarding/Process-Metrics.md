# Process Metrics

The system stores various details about the process and its phases. This data can be extracted to provide an overview of how the process is performing.

## Document Verification

You can calculate the following metrics based on the data below:

**Accepted Documents Rate**

```
(Accepted Documents / (All Documents − Failed Documents)) × 100
```

**Rejected Documents Rate**

```
(Rejected Documents / (All Documents − Failed Documents)) × 100
```

**Multiple Attempts Rate**

```
(Multiple Attempts / (All Documents − Failed Documents)) × 100
```

### All Documents

All document verification attempts.

```sql
SELECT COUNT(*) FROM es_document_verification
WHERE timestamp_uploaded BETWEEN now() - INTERVAL '90 day' AND now()
AND side = 'FRONT'; -- the document is always evaluated as whole with the same result for FRONT and BACK
```

### Accepted Documents

Documents accepted by the provider that have passed the document type check and person cross-check (if the bank is using multiple documents).

```sql
SELECT COUNT(*) FROM es_document_verification
WHERE timestamp_uploaded BETWEEN now() - INTERVAL '90 day' AND now()
AND side = 'FRONT'
AND reject_reason IS NULL;
```

### Rejected Documents

Documents rejected by the provider due to invalid checks, or by the onboarding server due to an invalid document type or unsuccessful person cross-check (if the bank is using multiple documents).

```sql
SELECT COUNT(*) FROM es_document_verification
WHERE COALESCE(timestamp_uploaded, timestamp_created) BETWEEN now() - INTERVAL '90 day' AND now()
AND side = 'FRONT'
AND status IN ('REJECTED', 'DISPOSED')
AND reject_origin = 'DOCUMENT_VERIFICATION';
```

### Failed Documents

A document verification attempt failed due to a technical reason, such as a timeout or a network issue.

```sql
SELECT COUNT(*) FROM es_document_verification
WHERE timestamp_uploaded BETWEEN now() - INTERVAL '90 day' AND now()
AND side = 'FRONT'
AND reject_reason = 'documentVerificationFailed';
```

### Multiple Attempts

Documents accepted after multiple attempts (more than one). This means that there is at least one rejected attempt and one accepted attempt (the final one).

```sql
SELECT COUNT(*) FROM (
	SELECT identity_verification_id, type
	FROM es_document_verification
	WHERE COALESCE(timestamp_uploaded, timestamp_created) between now() - INTERVAL '90 day' and now()
		AND side = 'FRONT'
	GROUP BY identity_verification_id, type
	HAVING
		COUNT(*) > 1
    	AND SUM(CASE WHEN status = 'ACCEPTED' AND reject_reason IS NULL THEN 1 ELSE 0 END) > 0
	AND SUM(CASE WHEN status IN ('REJECTED','DISPOSED') AND reject_reason IS NOT NULL THEN 1 ELSE 0 END) > 0) t;
```

**SQL decomposition**

1. Count all returned groups by wrapping everything in one select.
2. Select records from the `es_document_verification` table, limiting them to the `90 day` interval and the `FRONT` side only. Front side only because the table contains one or two sides for each document, but the front side is common to all document types.
3. The results are grouped by `identity_verification_id` and `type` because we want to ensure that attempts within a group originate from the same identity verification ID. There can be multiple documents under each identity verification ID (e.g. ID, passport, driving licence), so grouping by type is also used.
4. We are checking if there is more than one record in a group. This means there has been more than one attempt.
5. We check whether the group contains at least one accepted and one rejected attempt. Rejected attempts always have some value in `reject_reason` but we also use `status` for better code readability. The `DISPOSED` status depends on how the document was sent from the mobile, and it can overwrite the `REJECTED` status. For example, if the first document was rejected and replaced by a second document that was accepted, the first document is set to `DISPOSED`. This is why we need to use both statuses in the filter.

## Liveness Check

You can calculate following metrics based on the data below:

**Successful Liveness Check Rate**

```
(Successful Liveness Checks / All Liveness Checks) × 100
```

**Failed Liveness Check Rate**

```
(Unsuccessful Liveness Checks / All Liveness Checks) × 100
```

### All Liveness Checks

All liveness check verification attempts.

```sql
SELECT COUNT(*) FROM es_sca_result
WHERE timestamp_created BETWEEN now() - INTERVAL '90 day' AND now()
AND presence_check_result IN ('SUCCESS','FAILED');
```

### Successful Liveness Checks

Successful liveness checks.

```sql
SELECT COUNT(*) FROM es_sca_result
WHERE timestamp_created BETWEEN now() - INTERVAL '90 day' AND now()
AND presence_check_result = 'SUCCESS';
```

### Failed Liveness Checks

Failed liveness checks. We do not distinguish between rejected attempts and those that have failed due to technical reasons.

```sql
SELECT COUNT(*) FROM es_sca_result
WHERE timestamp_created BETWEEN now() - INTERVAL '90 day' AND now()
AND presence_check_result = 'FAILED';
```