# Offline Authentication QR Code

Offline authentication allows users to authorize operations even when their mobile device has no internet connection.
Rather than communicating with the server directly, the mobile application derives an authentication code from offline
authentication payload delivered through a QR code presented by an intermediate web application.

The mobile application processes the structured payload locally and computes an authentication code without requiring
network connectivity. Then the user manually types the derived authentication code into the intermediate web
application, which forwards it to the PowerAuth Server for verification.

The PowerAuth Server provides an API for generating QR code data and verifying the resulting authentication code.
This chapter describes how to request the QR code data and how to verify the authentication code. For further details
see the PowerAuth Server [offline authentication codes documentation](https://developers.wultra.com/components/powerauth-server/develop/documentation/Offline-Authentication-Codes).

## Flow Overview

1. Web application [requests QR code data](#request-qr-code-data) from the PowerAuth Server
2. Web application [displays the QR code](#display-the-qr-code)
3. User scans the QR code
4. Mobile token application [validates the payload and computes an authentication code](#mobile-app-computes-the-authentication-code)
5. User enters the authentication code into the web application
6. Web application [validates the authentication code](#verify-the-authentication-code) at the PowerAuth Server

## Request QR Code Data

The first step in the offline authentication process is generating a QR code for the user. To do this, create a request
payload containing the necessary operation attributes in predefined format and send it to the PowerAuth Server.
The server’s response can then be displayed directly as a QR code image.

To request the QR code data, use the PowerAuth Server REST method to [create personalized offline authentication payload](https://developers.wultra.com/components/powerauth-server/develop/documentation/WebServices-Methods-V4#method-createpersonalizedofflineauthpayload).
The method requires the identifier of the activation associated with the operation to be authorized and operation
attributes encoded in the following new-line separated format:

```
{OPERATION_ID}\n
{TITLE}\n
{MESSAGE}\n
{OPERATION_DATA}\n
{FLAGS}
```

Where
1. `{OPERATION_ID}` - operation identifier (UUID-like string).
2. `{TITLE}` - operation title in UTF8 format.
    - ASCII control characters (code < 32) are forbidden.
    - `\n` can be used for newline character.
    - `\\` can be used for backslash.
3. `{MESSAGE}` - message associated with operation in UTF8 format.
    - ASCII control characters (code < 32) are forbidden.
    - `\n` can be used for newline character.
    - `\\` can be used for backslash.
4. `{OPERATION_DATA}` - operation data as defined in [Operation Data Structure](./Operation-Data.md). Unsupported data
   fields are treated as `T{TEXT}`. Unsupported templates are treated as [`0` Generic](./Operation-Data.md#0-generic).
5. `{FLAGS}` - a string of characters representing an unordered set of flags that affect the operation processing. An
   empty value indicates that only the knowledge factor is allowed for 2FA. See defined flags below.

| Flag | Meaning                                          |
|------|:-------------------------------------------------|
| `B`  | Operation can be signed with biometric factor    |
| `X`  | The approval button will be flipped              |
| `F`  | The fraud warning will be shown                  |
| `C`  | Operation cannot be approved during a phone call |

For example:

```
5ff1b1ed-a3cc-45a3-8ab0-ed60950312b6
Payment
Please approve this payment
A1*A100CZK*ICZ2730300000001165254011*D20180425
BF
```

The PowerAuth Server REST method response contains an offline authentication payload that can be encoded directly into
a QR code. The `nonce` value is necessary for authentication code verification later. It may be stored, for example, in
a hidden HTML form field, a JavaScript variable, or in the user session associated with the operation.

The offline authentication payload consists of the submitted operation attributes data followed by server signature:

```
{SUBMITTED_DATA}\n{NONCE_B64}\n{KEY_TYPE}{DATA_SIGNATURE_B64}
```

For further details see the PowerAuth Server
[offline authentication codes documentation](https://developers.wultra.com/components/powerauth-server/develop/documentation/Offline-Authentication-Codes).

## Display the QR Code

To display the QR code in the web browser, use directly the offline authentication payload received from the server,
as in the following example in Java:

```java
BitMatrix matrix = new MultiFormatWriter().encode(
        new String(offlineData.getBytes("UTF-8"), "ISO-8859-1"), 
        BarcodeFormat.QR_CODE, 
        size, 
        size
);
BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
ByteArrayOutputStream baos = new ByteArrayOutputStream();
ImageIO.write(image, "png", baos);
byte[] bytes = baos.toByteArray();
return "data:image/png;base64," + BaseEncoding.base64().encode(bytes);
```

The user can now scan the QR code via the mobile token app.

## Mobile App Computes the Authentication Code

After the user scans the QR code using the mobile token application, the server signature is validated to verify
the authenticity of the received data. This is done by computing the signature of the received payload without the
signature itself, i.e. `{DATA}\n{NONCE_B64}\n{KEY_TYPE}`, using the signature key indicated by `{KEY_TYPE}`. Both
signatures must match. For further details about the server signature see the PowerAuth Server
[offline authentication codes documentation](https://developers.wultra.com/components/powerauth-server/develop/documentation/Offline-Authentication-Codes).

The mobile device prompts the user for PIN or biometric authentication and computes a 4×4-digit authentication code to
be written into the intermediate web application by the user.

## Verify the Authentication Code

Use the REST method [verifyOfflineAuthentication](https://developers.wultra.com/components/powerauth-server/develop/documentation/WebServices-Methods-V4#method-verifyofflineauthentication)
to determine if the authentication code was correctly validated or if it was invalid.

The user enters 4x4 digits in the intermediate web application. For the REST method the authentication code must be
converted into a standard PowerAuth format of two groups of 8 digits. For example, the code `1234-5678-9012-3456` must
be converted to `12345678-90123456`.

The `data` parameter of the REST method expects normalized data for authentication constructed exactly as the mobile app
does when computing the authentication code. To construct the normalized data for authentication:

- define `operationIdAndData` as concatenation of a subset of the submitted operation attributes as `{OPERATION_ID}&{OPERATION_DATA}`,
- use the `nonce` obtained in the server response before, and
- prepare constant strings `POST` and `/operation/authorize/offline`.

The following Java method from the
[PowerAuthHttpBody](https://github.com/wultra/powerauth-crypto/blob/develop/powerauth-java-http/src/main/java/com/wultra/security/powerauth/http/PowerAuthHttpBody.java)
class can be used to generate the `data` parameter for the REST method.

```java
String data = PowerAuthHttpBody.getAuthenticationBaseString(
        "POST",
        "/operation/authorize/offline",
        BaseEncoding.base64().decode(nonce),
        operationIdAndData.getBytes()
);
```

For further details about the authentication code verification, see the PowerAuth Server
[offline authentication codes documentation](https://developers.wultra.com/components/powerauth-server/develop/documentation/Offline-Authentication-Codes).
