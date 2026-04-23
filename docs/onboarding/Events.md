# Events

# Structure
Generic part of the event.

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

| Attribute                | Type   | Description                                                                                                            |
|:-------------------------|:-------|:-----------------------------------------------------------------------------------------------------------------------|
| `id`                     | String | Event ID                                                                                                               |
| `timestamp`              | String | Timestamp, when the action happened                                                                                    |
| `type`                   | String | Event type. Currently supported types are `documentVerification` `documentVerificationFinal` and `presenceCheck`       |
| `userId`                 | String | UUID of the user                                                                                                       |
| `processId`              | String | UUID of the process                                                                                                    |
| `processType`            | String | Name of the process, e.g. `onboarding`                                                                                 |
| `identityVerificationId` | String | UUID of the identity verification stage. Can be `null` if the event is not related to the identity verification stage. |
| `eventData`              | Object | Object with structure different for each `type`                                                                        |

## Event data for different event types

### Event data for documentVerification
Contains result from verification provider. Each document is sent separately.

```json
{
	"documentVerification": {
		"documentVerificationId": "String",
		"documentId": "String",
	    "status": "ACCEPTED",
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

| Attribute                  | Type   | Description                                                                                                                                                                                                                                                        |
|:---------------------------|:-------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `status`                   | String | Status of the verification. Supported values are `ACCEPTED`, `REJECTED` and `FAILED`.                                                                                                                                                                              |
| `rejectReason`             | String | Reject reason in case of `status` is `REJECTED`. Otherwise is `null`.                                                                                                                                                                                              |
| `errorDetail`              | String | Reject reason in case of `status` is `FAILED`. Otherwise is `null`.                                                                                                                                                                                                |
| `documentVerificationData` | Object | Contains some details about the document and extracted data. Object is present only in if the `status` is `ACCEPTED` or `REJECTED`. Otherwise is `null`. Complete response from verification provider can be found in `documentVerificationData.document.rawData`. |

### Event data for documentVerificationFinal
Contains overall document check result from all documents combined.

Keep in mind that even if the separate documents were verified by the verification provider, overall document check result can be negative because we are performing additional checks.
- document crosscheck
- document type
- document country

```json
{
	"documentVerificationFinal": {
		"documentVerificationId": "String",
		"documentIds": ["String","String"],
	    "status": "ACCEPTED",
	    "rejectReason": null,
	    "errorDetail": null,
    	"provider": "String"
	}
}
```

### Event data for presenceCheck

Contains result from verification provider.

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
