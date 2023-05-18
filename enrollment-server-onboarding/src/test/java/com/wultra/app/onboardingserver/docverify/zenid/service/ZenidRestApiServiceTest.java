/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2023 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.docverify.zenid.service;

import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.docverify.zenid.model.api.ZenidWebInvestigateResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for {@link ZenidRestApiService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@SpringBootTest(
        classes = EnrollmentServerTestApplication.class,
        properties = {
                "enrollment-server-onboarding.document-verification.provider=zenid",
                "enrollment-server-onboarding.document-verification.zenid.serviceBaseUrl=http://localhost:" + ZenidRestApiServiceTest.PORT
        })
@ActiveProfiles("test")
@Slf4j
class ZenidRestApiServiceTest {

    // TODO (racansky, 2023-05-18) find the way how to set the same random port for mock server and property
    static final int PORT = 52936;

    @Autowired
    private ZenidRestApiService tested;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setup() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start(PORT);
    }

    @AfterEach
    void cleanup() throws Exception {
        mockWebServer.shutdown();
    }

    /**
     * Test mainly jackson serialization configuration, response must be parsed without any exception.
     */
    @Test
    void testInvestigateSamples() throws Exception {
        assertNotNull(tested);

        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                // Anonymized real data taken from ZenID
                .setBody("""
                        {
                            "InvestigationID": 3123,
                            "CustomData": "",
                            "MinedData": {
                                "FirstName": {
                                    "Text": "John",
                                    "Confidence": 100
                                },
                                "LastName": {
                                    "Text": "Doe",
                                    "Confidence": 100
                                },
                                "Address": {
                                    "ID": "123",
                                    "A1": "PRAHA, PRAHA č.p. 99",
                                    "A2": "okr. PRAHA",
                                    "A3": "",
                                    "A4": null,
                                    "AdministrativeAreaLevel1": null,
                                    "AdministrativeAreaLevel2": "Praha",
                                    "Locality": "Praha",
                                    "Sublocality": "Praha",
                                    "Suburb": null,
                                    "Street": null,
                                    "HouseNumber": "99",
                                    "StreetNumber": null,
                                    "PostalCode": "11000",
                                    "GoogleSearchable": "Praha,  99, Praha",
                                    "Text": "PRAHA, PRAHA č.p. 99, okr. PRAHA",
                                    "Confidence": 100
                                },
                                "BirthAddress": {
                                    "Text": "PRAHA\\nPRAHA 2",
                                    "Confidence": 99
                                },
                                "BirthLastName": null,
                                "BirthNumber": {
                                    "BirthDate": "1983-10-05",
                                    "Checksum": 2,
                                    "Sex": "M",
                                    "Text": "831005/0031",
                                    "Confidence": 100
                                },
                                "BirthDate": {
                                    "Date": "1983-10-05",
                                    "Text": "05.10.1983",
                                    "Confidence": 100
                                },
                                "ExpiryDate": {
                                    "Date": "2025-05-11",
                                    "Text": "11.05.2025",
                                    "Confidence": 100
                                },
                                "IssueDate": {
                                    "Date": "2015-05-11",
                                    "Text": "11.05.2015",
                                    "Confidence": 90
                                },
                                "IdcardNumber": {
                                    "Text": "123456",
                                    "Confidence": 100
                                },
                                "DrivinglicenseNumber": null,
                                "PassportNumber": null,
                                "Sex": {
                                    "Sex": "M",
                                    "Text": "M",
                                    "Confidence": 100
                                },
                                "Nationality": {
                                    "Text": "CZE",
                                    "Confidence": 100
                                },
                                "Authority": {
                                    "Text": "MěÚ PRAHA",
                                    "Confidence": 98
                                },
                                "MaritalStatus": {
                                    "MaritalStatus": "Married",
                                    "ImpliedSex": "M",
                                    "Text": "ženatý",
                                    "Confidence": 100
                                },
                                "Photo": {
                                    "ImageData": {
                                        "ImageHash": {
                                            "AsText": "8a2219de7fc50e7c802b5e64b2c4c469",
                                            "IsNull": false
                                        }
                                    },
                                    "EstimatedAge": 33.0,
                                    "EstimatedSex": "M",
                                    "HasOccludedMouth": false,
                                    "HasSunGlasses": false,
                                    "HasHeadWear": false,
                                    "Text": "",
                                    "Confidence": 100
                                },
                                "Mrz": {
                                    "Mrz": {
                                        "Type": "ID_v2012",
                                        "Subtype": "OP",
                                        "BirthDate": "831005",
                                        "BirthDateVerified": true,
                                        "DocumentNumber": "123456",
                                        "DocumentNumberVerified": true,
                                        "ExpiryDate": "250511",
                                        "ExpiryDateVerified": true,
                                        "GivenName": "JOHN",
                                        "ChecksumVerified": true,
                                        "ChecksumDigit": 0,
                                        "LastName": "DOE",
                                        "Nationality": "CZE",
                                        "Sex": "M",
                                        "BirthNumber": "",
                                        "BirthNumberChecksum": null,
                                        "BirthNumberVerified": null,
                                        "BirthdateChecksum": 1,
                                        "DocumentNumChecksum": 8,
                                        "ExpiryChecksum": 8,
                                        "IssueDate": null,
                                        "IssueDateParsed": null,
                                        "AdditionalData": null,
                                        "Issuer": "CZE",
                                        "BirthDateParsed": "1983-10-05",
                                        "ExpiryDateParsed": "2025-05-11",
                                        "MrzLength": {
                                            "Item1": 30,
                                            "Item2": 30
                                        },
                                        "MrzDefType": "TD1_IDC"
                                    },
                                    "Text": "IDCZE1234568<<<<<<<<<<<<<<<\\n8310051M2505118CZE<<<<<<<<<<<0\\nDOE<<JOHN<<<<<<<<<<<<<<<",
                                    "Confidence": 100
                                },
                                "DocumentCode": "IDC2",
                                "DocumentCountry": "Cz",
                                "DocumentRole": "Idc",
                                "PageCode": null,
                                "Height": null,
                                "EyesColor": null,
                                "CarNumber": null,
                                "VisaNumber": null,
                                "FirstNameOfParents": null,
                                "ResidencyNumber": null,
                                "ResidencyNumberPhoto": null,
                                "FathersName": null,
                                "ResidencyPermitDescription": null,
                                "ResidencyPermitCode": null,
                                "GunlicenseNumber": null,
                                "Titles": {
                                    "Text": "Bc.",
                                    "Confidence": 100
                                },
                                "TitlesAfter": {
                                    "Text": "",
                                    "Confidence": 100
                                },
                                "SpecialRemarks": null,
                                "MothersName": null,
                                "HealthInsuranceCardNumber": null,
                                "HealthInsuranceNumber": null,
                                "InsuranceCompanyCode": null,
                                "IssuingCountry": {
                                    "Text": "CZ",
                                    "Confidence": 100
                                },
                                "FathersBirthDate": null,
                                "FathersSurname": null,
                                "FathersBirthNumber": null,
                                "FathersBirthSurname": null,
                                "MothersBirthDate": null,
                                "MothersSurname": null,
                                "MothersBirthNumber": null,
                                "MothersBirthSurname": null,
                                "BirthCertificateNumber": null
                            },
                            "DocumentsData": null,
                            "InvestigationUrl": "/investigation/detail/3123",
                            "Rank": {
                                "Overall": "A",
                                "Samples": [
                                    {
                                        "SampleType": "DocumentPicture",
                                        "Page": "B",
                                        "Rank": "A",
                                        "DocumentCode": "IDC2",
                                        "SampleID": "ab4f380bc372053b43d6f2c8523be8b7"
                                    },
                                    {
                                        "SampleType": "DocumentPicture",
                                        "Page": "F",
                                        "Rank": "A",
                                        "DocumentCode": "IDC2",
                                        "SampleID": "b00813d8f361f83448dffa0d2aa707ed"
                                    }
                                ],
                                "Conditions": []
                            },
                            "ValidatorResults": [
                                {
                                    "Name": "OCR confidence",
                                    "Code": 1,
                                    "Score": 90,
                                    "AcceptScore": 75,
                                    "Issues": [
                                        {
                                            "IssueUrl": "/csi/detail/6722",
                                            "IssueDescription": "Idc issue date:  Score:90 Low field confidence",
                                            "DocumentCode": "IDC2",
                                            "FieldID": "IssueDate",
                                            "SampleID": "b00813d8f361f83448dffa0d2aa707ed",
                                            "PageCode": "F",
                                            "SampleType": "DocumentPicture"
                                        }
                                    ],
                                    "Ok": true
                                },
                                {
                                    "Name": "Moiré Patterns",
                                    "Code": 21,
                                    "Score": 46,
                                    "AcceptScore": 10,
                                    "Issues": [
                                        {
                                            "IssueUrl": "/csi/detail/6723",
                                            "IssueDescription": "Idc back:  Score:46 Moiré pattern detected.",
                                            "DocumentCode": "IDC2",
                                            "FieldID": null,
                                            "SampleID": "ab4f380bc372053b43d6f2c8523be8b7",
                                            "PageCode": "B",
                                            "SampleType": "DocumentPicture"
                                        }
                                    ],
                                    "Ok": true
                                },
                                {
                                    "Name": "Focus",
                                    "Code": 41,
                                    "Score": 66,
                                    "AcceptScore": 15,
                                    "Issues": [
                                        {
                                            "IssueUrl": "/csi/detail/6724",
                                            "IssueDescription": "Idc back:  Score:66 Low quality of picture caused by focus, camera or card movement",
                                            "DocumentCode": "IDC2",
                                            "FieldID": null,
                                            "SampleID": "ab4f380bc372053b43d6f2c8523be8b7",
                                            "PageCode": "B",
                                            "SampleType": "DocumentPicture"
                                        }
                                    ],
                                    "Ok": true
                                },
                                {
                                    "Name": "MRZ Birth Date",
                                    "Code": 2,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "Issue and expiration date",
                                    "Code": 3,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "Personal number",
                                    "Code": 4,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "Letter Spacing",
                                    "Code": 7,
                                    "Score": 100,
                                    "AcceptScore": 70,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "Vertical Displacement",
                                    "Code": 8,
                                    "Score": 100,
                                    "AcceptScore": 70,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "Font shape",
                                    "Code": 9,
                                    "Score": 100,
                                    "AcceptScore": 70,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "DPI",
                                    "Code": 15,
                                    "Score": 100,
                                    "AcceptScore": 30,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "EXIF",
                                    "Code": 18,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "Card recalled",
                                    "Code": 19,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "Barcode",
                                    "Code": 22,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "Document complete",
                                    "Code": 23,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "MRZ First Name",
                                    "Code": 24,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "MRZ Last Name",
                                    "Code": 25,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "MRZ Date of Expiry",
                                    "Code": 26,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "MRZ Sex",
                                    "Code": 27,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "MRZ ID Card Number",
                                    "Code": 28,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "MRZ Birth Number Date",
                                    "Code": 30,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "MRZ Birth Number Sex",
                                    "Code": 31,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "MRZ checksum",
                                    "Code": 33,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "Required fields",
                                    "Code": 38,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "Document picture exists",
                                    "Code": 45,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "Valid ID Card Number Range",
                                    "Code": 46,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "Insolvency check",
                                    "Code": 47,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "Color of the face photo",
                                    "Code": 49,
                                    "Score": 100,
                                    "AcceptScore": 100,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "Specular reflection on image",
                                    "Code": 53,
                                    "Score": 100,
                                    "AcceptScore": 20,
                                    "Issues": [],
                                    "Ok": true
                                },
                                {
                                    "Name": "Field manipulation",
                                    "Code": 62,
                                    "Score": 100,
                                    "AcceptScore": 15,
                                    "Issues": [],
                                    "Ok": true
                                }
                            ],
                            "State": "Done",
                            "ErrorCode": null,
                            "ErrorText": null,
                            "ProcessingTimeMs": 18,
                            "MessageType": "InvestigateResponse"
                        }
                        """));

        final ResponseEntity<ZenidWebInvestigateResponse> result = tested.investigateSamples(List.of("3123"));

        final RecordedRequest recordedRequest = mockWebServer.takeRequest(1L, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("GET /api/investigateSamples?sampleIDs=3123&async=false HTTP/1.1", recordedRequest.getRequestLine());

        logger.debug("Parsed result {}", result);
    }
}
