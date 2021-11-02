package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.wultra.app.docverify.zenid.model.api.ZenidSharedLicenseCountables;
import com.wultra.app.docverify.zenid.model.api.ZenidWebControllersSelfCheckSelfCheckItem;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Response object for UploadSample
 */
@ApiModel(description = "Response object for UploadSample")
@Validated


public class ZenidWebDiagnosticsResponse   {
  @JsonProperty("IsAllOk")
  private Boolean isAllOk = null;

  @JsonProperty("SelfCheckItems")
  @Valid
  private List<ZenidWebControllersSelfCheckSelfCheckItem> selfCheckItems = null;

  @JsonProperty("LicenseExpiration")
  private LocalDate licenseExpiration = null;

  @JsonProperty("LicenseRemaining")
  private ZenidSharedLicenseCountables licenseRemaining = null;

  /**
   * Gets or Sets supportedDocuments
   */
  public enum SupportedDocumentsEnum {
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

    SupportedDocumentsEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static SupportedDocumentsEnum fromValue(String text) {
      for (SupportedDocumentsEnum b : SupportedDocumentsEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("SupportedDocuments")
  @Valid
  private List<SupportedDocumentsEnum> supportedDocuments = null;

  /**
   * If throght processing some error occurs, ErrorCode property is set.
   */
  public enum ErrorCodeEnum {
    UNKNOWNSAMPLEID("UnknownSampleID"),
    
    UNKNOWNUPLOADSESSIONID("UnknownUploadSessionID"),
    
    EMPTYBODY("EmptyBody"),
    
    INTERNALSERVERERROR("InternalServerError"),
    
    INVALIDTIMESTAMP("InvalidTimeStamp"),
    
    SAMPLEININVALIDSTATE("SampleInInvalidState"),
    
    INVALIDSAMPLECOMBINATION("InvalidSampleCombination"),
    
    ACCESSDENIED("AccessDenied"),
    
    UNKNOWNPERSON("UnknownPerson"),
    
    INVALIDINPUTDATA("InvalidInputData");

    private String value;

    ErrorCodeEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static ErrorCodeEnum fromValue(String text) {
      for (ErrorCodeEnum b : ErrorCodeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("ErrorCode")
  private ErrorCodeEnum errorCode = null;

  @JsonProperty("ErrorText")
  private String errorText = null;

  @JsonProperty("MessageType")
  private String messageType = null;

  public ZenidWebDiagnosticsResponse isAllOk(Boolean isAllOk) {
    this.isAllOk = isAllOk;
    return this;
  }

  /**
   * Get isAllOk
   * @return isAllOk
  **/
  @ApiModelProperty(value = "")


  public Boolean isIsAllOk() {
    return isAllOk;
  }

  public void setIsAllOk(Boolean isAllOk) {
    this.isAllOk = isAllOk;
  }

  public ZenidWebDiagnosticsResponse selfCheckItems(List<ZenidWebControllersSelfCheckSelfCheckItem> selfCheckItems) {
    this.selfCheckItems = selfCheckItems;
    return this;
  }

  public ZenidWebDiagnosticsResponse addSelfCheckItemsItem(ZenidWebControllersSelfCheckSelfCheckItem selfCheckItemsItem) {
    if (this.selfCheckItems == null) {
      this.selfCheckItems = new ArrayList<>();
    }
    this.selfCheckItems.add(selfCheckItemsItem);
    return this;
  }

  /**
   * Get selfCheckItems
   * @return selfCheckItems
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ZenidWebControllersSelfCheckSelfCheckItem> getSelfCheckItems() {
    return selfCheckItems;
  }

  public void setSelfCheckItems(List<ZenidWebControllersSelfCheckSelfCheckItem> selfCheckItems) {
    this.selfCheckItems = selfCheckItems;
  }

  public ZenidWebDiagnosticsResponse licenseExpiration(LocalDate licenseExpiration) {
    this.licenseExpiration = licenseExpiration;
    return this;
  }

  /**
   * Get licenseExpiration
   * @return licenseExpiration
  **/
  @ApiModelProperty(value = "")

  @Valid

  public LocalDate getLicenseExpiration() {
    return licenseExpiration;
  }

  public void setLicenseExpiration(LocalDate licenseExpiration) {
    this.licenseExpiration = licenseExpiration;
  }

  public ZenidWebDiagnosticsResponse licenseRemaining(ZenidSharedLicenseCountables licenseRemaining) {
    this.licenseRemaining = licenseRemaining;
    return this;
  }

  /**
   * Get licenseRemaining
   * @return licenseRemaining
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedLicenseCountables getLicenseRemaining() {
    return licenseRemaining;
  }

  public void setLicenseRemaining(ZenidSharedLicenseCountables licenseRemaining) {
    this.licenseRemaining = licenseRemaining;
  }

  public ZenidWebDiagnosticsResponse supportedDocuments(List<SupportedDocumentsEnum> supportedDocuments) {
    this.supportedDocuments = supportedDocuments;
    return this;
  }

  public ZenidWebDiagnosticsResponse addSupportedDocumentsItem(SupportedDocumentsEnum supportedDocumentsItem) {
    if (this.supportedDocuments == null) {
      this.supportedDocuments = new ArrayList<>();
    }
    this.supportedDocuments.add(supportedDocumentsItem);
    return this;
  }

  /**
   * Get supportedDocuments
   * @return supportedDocuments
  **/
  @ApiModelProperty(value = "")


  public List<SupportedDocumentsEnum> getSupportedDocuments() {
    return supportedDocuments;
  }

  public void setSupportedDocuments(List<SupportedDocumentsEnum> supportedDocuments) {
    this.supportedDocuments = supportedDocuments;
  }

  public ZenidWebDiagnosticsResponse errorCode(ErrorCodeEnum errorCode) {
    this.errorCode = errorCode;
    return this;
  }

  /**
   * If throght processing some error occurs, ErrorCode property is set.
   * @return errorCode
  **/
  @ApiModelProperty(value = "If throght processing some error occurs, ErrorCode property is set.")


  public ErrorCodeEnum getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(ErrorCodeEnum errorCode) {
    this.errorCode = errorCode;
  }

  public ZenidWebDiagnosticsResponse errorText(String errorText) {
    this.errorText = errorText;
    return this;
  }

  /**
   * Error text
   * @return errorText
  **/
  @ApiModelProperty(value = "Error text")


  public String getErrorText() {
    return errorText;
  }

  public void setErrorText(String errorText) {
    this.errorText = errorText;
  }

  public ZenidWebDiagnosticsResponse messageType(String messageType) {
    this.messageType = messageType;
    return this;
  }

  /**
   * Get messageType
   * @return messageType
  **/
  @ApiModelProperty(readOnly = true, value = "")


  public String getMessageType() {
    return messageType;
  }

  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ZenidWebDiagnosticsResponse zenidWebDiagnosticsResponse = (ZenidWebDiagnosticsResponse) o;
    return Objects.equals(this.isAllOk, zenidWebDiagnosticsResponse.isAllOk) &&
        Objects.equals(this.selfCheckItems, zenidWebDiagnosticsResponse.selfCheckItems) &&
        Objects.equals(this.licenseExpiration, zenidWebDiagnosticsResponse.licenseExpiration) &&
        Objects.equals(this.licenseRemaining, zenidWebDiagnosticsResponse.licenseRemaining) &&
        Objects.equals(this.supportedDocuments, zenidWebDiagnosticsResponse.supportedDocuments) &&
        Objects.equals(this.errorCode, zenidWebDiagnosticsResponse.errorCode) &&
        Objects.equals(this.errorText, zenidWebDiagnosticsResponse.errorText) &&
        Objects.equals(this.messageType, zenidWebDiagnosticsResponse.messageType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isAllOk, selfCheckItems, licenseExpiration, licenseRemaining, supportedDocuments, errorCode, errorText, messageType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidWebDiagnosticsResponse {\n");
    
    sb.append("    isAllOk: ").append(toIndentedString(isAllOk)).append("\n");
    sb.append("    selfCheckItems: ").append(toIndentedString(selfCheckItems)).append("\n");
    sb.append("    licenseExpiration: ").append(toIndentedString(licenseExpiration)).append("\n");
    sb.append("    licenseRemaining: ").append(toIndentedString(licenseRemaining)).append("\n");
    sb.append("    supportedDocuments: ").append(toIndentedString(supportedDocuments)).append("\n");
    sb.append("    errorCode: ").append(toIndentedString(errorCode)).append("\n");
    sb.append("    errorText: ").append(toIndentedString(errorText)).append("\n");
    sb.append("    messageType: ").append(toIndentedString(messageType)).append("\n");
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

