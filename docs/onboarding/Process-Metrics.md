# Process Metrics

The system stores various details about the process and its phases. This data can be extracted to provide an overview of how the process is performing.

## Document Verification

You can calculate following metrics based on the data below:

**Accepted Documents Rate**

```
(Accepted Documents / (All Documents − Failed Documents)) × 100
```

**Rejected Documents Rate**

```
(Rejected Documents / (All Documents − Failed Documents)) × 100
```

### All Documents

All document verification attempts.

```
select count(*) from es_document_verification
where timestamp_uploaded between now() - INTERVAL '90 day' and now() 
and side = 'FRONT'; -- the document is always evaluated as whole with the same result for FRONT and BACK
```

### Accepted Documents

Documents accepted by the provider that have passed the document type check and person cross-check (if the bank is using multiple documents).

```
select count(*) from es_document_verification
where timestamp_uploaded between now() - INTERVAL '90 day' and now() 
and side = 'FRONT' -- the document is always evaluated as whole with the same result for FRONT and BACK
and reject_reason is null;
```

### Rejected Documents

Documents rejected by the provider due to invalid checks, or by the onboarding server due to an invalid document type or unsuccessful person cross-check (if the bank is using multiple documents).

```
select count(*) from es_document_verification
where timestamp_uploaded between now() - INTERVAL '90 day' and now() 
and side = 'FRONT'
and reject_reason = 'documentVerificationRejected';
```

### Failed Documents

It failed due to a technical reason, such as a timeout or a network issue.

```
select count(*) from es_document_verification
where timestamp_uploaded between now() - INTERVAL '90 day' and now() 
and side = 'FRONT'
and reject_reason = 'documentVerificationFailed';
```

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

```
select  count(*) from es_sca_result
where timestamp_created between now() - INTERVAL '90 day' and now()
and presence_check_result in ('SUCCESS','FAILED');
```

### Successful Liveness Checks

Successful liveness checks.

```
select  count(*) from es_sca_result
where timestamp_created between now() - INTERVAL '90 day' and now()
and presence_check_result ='SUCCESS';
```

### Failed Liveness Checks

Failed liveness checks. We do not distinguish between rejected attempts and those that have failed due to technical reasons.

```
select  count(*) from es_sca_result
where timestamp_created between now() - INTERVAL '90 day' and now()
and presence_check_result ='FAILED';
```