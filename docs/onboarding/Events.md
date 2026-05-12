# Events

# Structure
This is the generic part of the event, which is common to all event types.

```json
{
    "id": "8f0d649e-a688-4ead-8581-fc2b67e88be4",
    "timestamp": "2026-04-23T14:30:00Z",
    "type": "documentVerification",
    "userId": "40405309-6406-4d6b-b4ef-642e52ac44f4",
    "processId": "8b2dfae4-d955-4d8f-a95b-2d9c5a4b0e26",
    "processType": "onboarding",
    "identityVerificationId": "d3827099-3b6c-4df9-887d-4eac402fc4f9",
    "eventData": {}
}
```

| Attribute                | Type   | Description                                                                                                                              |
|:-------------------------|:-------|:-----------------------------------------------------------------------------------------------------------------------------------------|
| `id`                     | String | Event ID                                                                                                                                 |
| `timestamp`              | String | Timestamp, when the action happened                                                                                                      |
| `type`                   | String | Event type. Currently supported types are `DOCUMENT_VERIFICATION_FINISHED` `FINAL_DOCUMENT_VERIFICATION_FINISHED` and `PRESENCE_CHECK_FINISHED` |
| `userId`                 | String | UUID of the user                                                                                                                         |
| `processId`              | String | UUID of the process                                                                                                                      |
| `processType`            | String | Name of the process, e.g. `onboarding`                                                                                                   |
| `identityVerificationId` | String | UUID of the identity verification stage. Can be `null` if the event is not related to the identity verification stage.                   |
| `eventData`              | Object | Object with structure different for each `type`                                                                                          |

## Event data for different event types
 
Different event `type` has different structure in `eventData`.

### Event data for DOCUMENT_VERIFICATION_FINISHED

This contains the results from the verification provider. Each document is sent separately.

```json
{
	"documentVerification": {
		"documentVerificationId": "String",
		"documentId": "String",
	    "status": "String",
	    "rejectReason": null,
	    "errorDetail": null,
    	"provider": "String",
		"score": Number,
		"documentCheckResult": {
		    "type": "String",
		    "country": "String",
		    "data": {
		        "surname": "String",
		        "givenNames": "String",
		        "dateOfBirth": "String",
		        "placeOfBirth": "String",
		        "sex": "String",
		        "nationality": "String",
		        "personalNumber": "String",
		        "documentNumber": "String",
		        "dateOfIssue": "String",
		        "dateOfExpiry": "String",
		        "authority": "String"
		    },
		    "images": [
		        {
		            "type": "String",
		            "data": "String"
		        }
		    ],
		    "rawData": Object
		}		
	}
}
```

| Attribute                | Type   | Description                                                                                                                                                                                                                                                        |
|:-------------------------|:-------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `documentVerificationId` | String | UUID of the document verification                                                                                                                                                                                                                                  |
| `documentId`             | String | UUID of the document                                                                                                                                                                                                                                               |
| `status`                 | String | Status of the verification. Supported values are `ACCEPTED`, `REJECTED` and `FAILED`.                                                                                                                                                                              |
| `rejectReason`           | String | Reject reason in case of `status` is `REJECTED`. Otherwise is `null`.                                                                                                                                                                                              |
| `errorDetail`            | String | Reject reason in case of `status` is `FAILED`. Otherwise is `null`.                                                                                                                                                                                                |
| `provider`               | String | Name of the configured external biometry provider. For example, `iProov` or `Microblink`.                                                                                                                                                                          |
| `score`                  | String | Outcome confidence of the verification check on scale 0-10.                                                                                                                                                                                                        |
| `documentCheckResult`    | Object | Contains some details about the document and extracted data. Object is present only in if the `status` is `ACCEPTED` or `REJECTED`. Otherwise is `null`. Complete response from verification provider can be found in `documentVerificationData.document.rawData`. |

### Event data for FINAL_DOCUMENT_VERIFICATION_FINISHED

This contains the overall document check result for all documents combined.

Bear in mind that even if the individual documents were verified by the verification provider, the overall document check result could still be negative due to the additional checks performed.

Additional checks:
- document crosscheck
- document type
- document country

```json
{
	"finalDocumentVerification": {
		"documentVerificationId": "String",
	    "status": "String",
	    "rejectReason": null,
	    "errorDetail": null,
    	"provider": "String",
		"documentIds": ["String","String"],
	}
}
```

### Event data for PRESENCE_CHECK_FINISHED

This contains the results of the verification provider.

```json
{
	"presenceCheck": {
	    "status": "ACCEPTED",
	    "rejectReason": null,
	    "errorDetail": null,
    	"provider": "IPROOV",
		"score": Number,
	    "presenceCheckResult": {
	      "frame": "String"
	    }
	}
}
```
