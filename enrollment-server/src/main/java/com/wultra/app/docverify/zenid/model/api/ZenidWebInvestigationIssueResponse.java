package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ZenidWebInvestigationIssueResponse
 */
@Validated


public class ZenidWebInvestigationIssueResponse   {
  @JsonProperty("IssueUrl")
  private String issueUrl = null;

  @JsonProperty("IssueDescription")
  private String issueDescription = null;

  /**
   * Document code of sample, where issue is present
   */
  public enum DocumentCodeEnum {
    IDC2("IDC2"),
    
    IDC1("IDC1"),
    
    DRV("DRV"),
    
    PAS("PAS"),
    
    SK_IDC_2008PLUS("SK_IDC_2008plus"),
    
    SK_DRV_2004_08_09("SK_DRV_2004_08_09"),
    
    SK_DRV_2013("SK_DRV_2013"),
    
    SK_DRV_2015("SK_DRV_2015"),
    
    SK_PAS_2008_14("SK_PAS_2008_14"),
    
    SK_IDC_1993("SK_IDC_1993"),
    
    SK_DRV_1993("SK_DRV_1993"),
    
    PL_IDC_2015("PL_IDC_2015"),
    
    DE_IDC_2010("DE_IDC_2010"),
    
    DE_IDC_2001("DE_IDC_2001"),
    
    HR_IDC_2013_15("HR_IDC_2013_15"),
    
    AT_IDE_2000("AT_IDE_2000"),
    
    HU_IDC_2000_01_12("HU_IDC_2000_01_12"),
    
    HU_IDC_2016("HU_IDC_2016"),
    
    AT_IDC_2002_05_10("AT_IDC_2002_05_10"),
    
    HU_ADD_2012("HU_ADD_2012"),
    
    AT_PAS_2006_14("AT_PAS_2006_14"),
    
    AT_DRV_2006("AT_DRV_2006"),
    
    AT_DRV_2013("AT_DRV_2013"),
    
    CZ_RES_2011_14("CZ_RES_2011_14"),
    
    CZ_RES_2006_T("CZ_RES_2006_T"),
    
    CZ_RES_2006_07("CZ_RES_2006_07"),
    
    CZ_GUN_2014("CZ_GUN_2014"),
    
    HU_PAS_2006_12("HU_PAS_2006_12"),
    
    HU_DRV_2012_13("HU_DRV_2012_13"),
    
    HU_DRV_2012_B("HU_DRV_2012_B"),
    
    EU_EHIC_2004_A("EU_EHIC_2004_A"),
    
    UNKNOWN("Unknown"),
    
    CZ_GUN_2017("CZ_GUN_2017"),
    
    CZ_RES_2020("CZ_RES_2020"),
    
    PL_IDC_2019("PL_IDC_2019"),
    
    IT_PAS_2006_10("IT_PAS_2006_10"),
    
    INT_ISIC_2008("INT_ISIC_2008"),
    
    DE_PAS("DE_PAS"),
    
    DK_PAS("DK_PAS"),
    
    ES_PAS("ES_PAS"),
    
    FI_PAS("FI_PAS"),
    
    FR_PAS("FR_PAS"),
    
    GB_PAS("GB_PAS"),
    
    IS_PAS("IS_PAS"),
    
    NL_PAS("NL_PAS"),
    
    RO_PAS("RO_PAS"),
    
    SE_PAS("SE_PAS"),
    
    PL_PAS("PL_PAS"),
    
    PL_DRV_2013("PL_DRV_2013"),
    
    CZ_BIRTH("CZ_BIRTH"),
    
    CZ_VEHICLE_I("CZ_VEHICLE_I"),
    
    INT_ISIC_2019("INT_ISIC_2019"),
    
    SI_PAS("SI_PAS"),
    
    SI_IDC("SI_IDC"),
    
    SI_DRV("SI_DRV"),
    
    EU_EHIC_2004_B("EU_EHIC_2004_B"),
    
    PL_IDC_2001_02_13("PL_IDC_2001_02_13"),
    
    IT_IDC_2016("IT_IDC_2016"),
    
    HR_PAS_2009_15("HR_PAS_2009_15"),
    
    HR_DRV_2013("HR_DRV_2013"),
    
    HR_IDC_2003("HR_IDC_2003"),
    
    SI_DRV_2009("SI_DRV_2009"),
    
    BG_PAS_2010("BG_PAS_2010"),
    
    BG_IDC_2010("BG_IDC_2010"),
    
    BG_DRV_2010_13("BG_DRV_2010_13"),
    
    HR_IDC_2021("HR_IDC_2021");

    private String value;

    DocumentCodeEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static DocumentCodeEnum fromValue(String text) {
      for (DocumentCodeEnum b : DocumentCodeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("DocumentCode")
  private DocumentCodeEnum documentCode = null;

  /**
   * FieldID wher issue is present
   */
  public enum FieldIDEnum {
    LASTNAME("LastName"),
    
    FIRSTNAME("FirstName"),
    
    BIRTHDATE("BirthDate"),
    
    A1("A1"),
    
    A2("A2"),
    
    A3("A3"),
    
    PHOTO("Photo"),
    
    BIRTHNUMBER("BirthNumber"),
    
    AUTHORITY("Authority"),
    
    MRZ1("Mrz1"),
    
    MRZ2("Mrz2"),
    
    MRZ3("Mrz3"),
    
    IDCARDNUMBER("IdcardNumber"),
    
    SEX("Sex"),
    
    MARITALSTATUS("MaritalStatus"),
    
    BIRTHADDRESS("BirthAddress"),
    
    BA1("BA1"),
    
    BA2("BA2"),
    
    ISSUEDATE("IssueDate"),
    
    EXPIRYDATE("ExpiryDate"),
    
    PASSPORTNUMBER("PassportNumber"),
    
    DRIVINGLICENSENUMBER("DrivinglicenseNumber"),
    
    BARCODE("Barcode"),
    
    BIRTHLASTNAME("BirthLastName"),
    
    SPECIALREMARKS("SpecialRemarks"),
    
    HEIGHT("Height"),
    
    EYESCOLOR("EyesColor"),
    
    TITLES("Titles"),
    
    AUTHORITY1("Authority1"),
    
    AUTHORITY2("Authority2"),
    
    LASTNAME1("LastName1"),
    
    LASTNAME2TITLESAFTER("LastName2TitlesAfter"),
    
    DRVCODES("DrvCodes"),
    
    SIGNATURE("Signature"),
    
    OTHERINFO("OtherInfo"),
    
    MINIHOLOGRAM("MiniHologram"),
    
    MINIPHOTO("MiniPhoto"),
    
    CARNUMBER("CarNumber"),
    
    LICENSETYPES("LicenseTypes"),
    
    FIRSTNAMEOFPARENTS("FirstNameOfParents"),
    
    BIRTHDATENUMBER("BirthDateNumber"),
    
    DRIVINGLICENSENUMBER2("DrivinglicenseNumber2"),
    
    RDIFCHIPACCESS("RDIFChipAccess"),
    
    PSEUDONYM("Pseudonym"),
    
    RESIDENCYPERMITDESCRIPTION("ResidencyPermitDescription"),
    
    RESIDENCYPERMITCODE("ResidencyPermitCode"),
    
    RESIDENCYNUMBER("ResidencyNumber"),
    
    AUTHORITYANDISSUEDATE("AuthorityAndIssueDate"),
    
    NATIONALITY("Nationality"),
    
    GUNLICENSENUMBER("GunlicenseNumber"),
    
    STAMP("Stamp"),
    
    STAMP2("Stamp2"),
    
    SURNAMEANDNAME1("SurnameAndName1"),
    
    SURNAMEANDNAME2("SurnameAndName2"),
    
    SURNAMEANDNAME3("SurnameAndName3"),
    
    MOTHERSSURNAMEANDNAME("MothersSurnameAndName"),
    
    TEMPORARYADDRESS1("TemporaryAddress1"),
    
    TEMPORARYADDRESS2("TemporaryAddress2"),
    
    ADDRESSSTARTINGDATE("AddressStartingDate"),
    
    TEMPORARYADDRESSSTARTINGDATE("TemporaryAddressStartingDate"),
    
    TEMPORARYADDRESSENDINGDATE("TemporaryAddressEndingDate"),
    
    NAMEINNATIONALLANGUAGE("NameInNationalLanguage"),
    
    BIRTHDATEANDADDRESS("BirthDateAndAddress"),
    
    SPECIALREMARKS2("SpecialRemarks2"),
    
    SPECIALREMARKS3("SpecialRemarks3"),
    
    UNKNOWN("Unknown"),
    
    HEALTHINSURANCECARDNUMBER("HealthInsuranceCardNumber"),
    
    INSURANCECOMPANYCODE("InsuranceCompanyCode"),
    
    ISSUINGCOUNTRY("IssuingCountry"),
    
    RESIDENCYNUMBERPHOTO("ResidencyNumberPhoto"),
    
    ISSUEDATEANDAUTHORITY("IssueDateAndAuthority");

    private String value;

    FieldIDEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static FieldIDEnum fromValue(String text) {
      for (FieldIDEnum b : FieldIDEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("FieldID")
  private FieldIDEnum fieldID = null;

  @JsonProperty("SampleID")
  private String sampleID = null;

  /**
   * Identification of the page type for issue
   */
  public enum PageCodeEnum {
    F("F"),
    
    B("B");

    private String value;

    PageCodeEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static PageCodeEnum fromValue(String text) {
      for (PageCodeEnum b : PageCodeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("PageCode")
  private PageCodeEnum pageCode = null;

  /**
   * Type of sample
   */
  public enum SampleTypeEnum {
    DOCUMENTPICTURE("DocumentPicture"),
    
    SELFIE("Selfie"),
    
    SELFIEVIDEO("SelfieVideo"),
    
    DOCUMENTVIDEO("DocumentVideo"),
    
    ARCHIVED("Archived"),
    
    UNKNOWN("Unknown");

    private String value;

    SampleTypeEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static SampleTypeEnum fromValue(String text) {
      for (SampleTypeEnum b : SampleTypeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("SampleType")
  private SampleTypeEnum sampleType = null;

  public ZenidWebInvestigationIssueResponse issueUrl(String issueUrl) {
    this.issueUrl = issueUrl;
    return this;
  }

  /**
   * Url with detailed visualization of the issue.
   * @return issueUrl
  **/
  @ApiModelProperty(value = "Url with detailed visualization of the issue.")


  public String getIssueUrl() {
    return issueUrl;
  }

  public void setIssueUrl(String issueUrl) {
    this.issueUrl = issueUrl;
  }

  public ZenidWebInvestigationIssueResponse issueDescription(String issueDescription) {
    this.issueDescription = issueDescription;
    return this;
  }

  /**
   * Description of issue
   * @return issueDescription
  **/
  @ApiModelProperty(value = "Description of issue")


  public String getIssueDescription() {
    return issueDescription;
  }

  public void setIssueDescription(String issueDescription) {
    this.issueDescription = issueDescription;
  }

  public ZenidWebInvestigationIssueResponse documentCode(DocumentCodeEnum documentCode) {
    this.documentCode = documentCode;
    return this;
  }

  /**
   * Document code of sample, where issue is present
   * @return documentCode
  **/
  @ApiModelProperty(value = "Document code of sample, where issue is present")


  public DocumentCodeEnum getDocumentCode() {
    return documentCode;
  }

  public void setDocumentCode(DocumentCodeEnum documentCode) {
    this.documentCode = documentCode;
  }

  public ZenidWebInvestigationIssueResponse fieldID(FieldIDEnum fieldID) {
    this.fieldID = fieldID;
    return this;
  }

  /**
   * FieldID wher issue is present
   * @return fieldID
  **/
  @ApiModelProperty(value = "FieldID wher issue is present")


  public FieldIDEnum getFieldID() {
    return fieldID;
  }

  public void setFieldID(FieldIDEnum fieldID) {
    this.fieldID = fieldID;
  }

  public ZenidWebInvestigationIssueResponse sampleID(String sampleID) {
    this.sampleID = sampleID;
    return this;
  }

  /**
   * ID of the identification issue
   * @return sampleID
  **/
  @ApiModelProperty(value = "ID of the identification issue")


  public String getSampleID() {
    return sampleID;
  }

  public void setSampleID(String sampleID) {
    this.sampleID = sampleID;
  }

  public ZenidWebInvestigationIssueResponse pageCode(PageCodeEnum pageCode) {
    this.pageCode = pageCode;
    return this;
  }

  /**
   * Identification of the page type for issue
   * @return pageCode
  **/
  @ApiModelProperty(value = "Identification of the page type for issue")


  public PageCodeEnum getPageCode() {
    return pageCode;
  }

  public void setPageCode(PageCodeEnum pageCode) {
    this.pageCode = pageCode;
  }

  public ZenidWebInvestigationIssueResponse sampleType(SampleTypeEnum sampleType) {
    this.sampleType = sampleType;
    return this;
  }

  /**
   * Type of sample
   * @return sampleType
  **/
  @ApiModelProperty(value = "Type of sample")


  public SampleTypeEnum getSampleType() {
    return sampleType;
  }

  public void setSampleType(SampleTypeEnum sampleType) {
    this.sampleType = sampleType;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ZenidWebInvestigationIssueResponse zenidWebInvestigationIssueResponse = (ZenidWebInvestigationIssueResponse) o;
    return Objects.equals(this.issueUrl, zenidWebInvestigationIssueResponse.issueUrl) &&
        Objects.equals(this.issueDescription, zenidWebInvestigationIssueResponse.issueDescription) &&
        Objects.equals(this.documentCode, zenidWebInvestigationIssueResponse.documentCode) &&
        Objects.equals(this.fieldID, zenidWebInvestigationIssueResponse.fieldID) &&
        Objects.equals(this.sampleID, zenidWebInvestigationIssueResponse.sampleID) &&
        Objects.equals(this.pageCode, zenidWebInvestigationIssueResponse.pageCode) &&
        Objects.equals(this.sampleType, zenidWebInvestigationIssueResponse.sampleType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(issueUrl, issueDescription, documentCode, fieldID, sampleID, pageCode, sampleType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidWebInvestigationIssueResponse {\n");
    
    sb.append("    issueUrl: ").append(toIndentedString(issueUrl)).append("\n");
    sb.append("    issueDescription: ").append(toIndentedString(issueDescription)).append("\n");
    sb.append("    documentCode: ").append(toIndentedString(documentCode)).append("\n");
    sb.append("    fieldID: ").append(toIndentedString(fieldID)).append("\n");
    sb.append("    sampleID: ").append(toIndentedString(sampleID)).append("\n");
    sb.append("    pageCode: ").append(toIndentedString(pageCode)).append("\n");
    sb.append("    sampleType: ").append(toIndentedString(sampleType)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

