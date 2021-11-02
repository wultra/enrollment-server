package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.wultra.app.docverify.zenid.model.api.ZenidSharedMineAllResult;
import com.wultra.app.docverify.zenid.model.api.ZenidWebInvestigationValidatorResponse;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Response object for the investigation nodes.
 */
@ApiModel(description = "Response object for the investigation nodes.")
@Validated


public class ZenidWebInvestigateResponse   {
  @JsonProperty("InvestigationID")
  private Integer investigationID = null;

  @JsonProperty("CustomData")
  private String customData = null;

  @JsonProperty("MinedData")
  private ZenidSharedMineAllResult minedData = null;

  @JsonProperty("DocumentsData")
  @Valid
  private List<ZenidSharedMineAllResult> documentsData = null;

  @JsonProperty("InvestigationUrl")
  private String investigationUrl = null;

  @JsonProperty("ValidatorResults")
  @Valid
  private List<ZenidWebInvestigationValidatorResponse> validatorResults = null;

  /**
   * State of the request - NotDone/Done/Error
   */
  public enum StateEnum {
    NOTDONE("NotDone"),
    
    DONE("Done"),
    
    ERROR("Error"),
    
    OPERATOR("Operator"),
    
    REJECTED("Rejected");

    private String value;

    StateEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StateEnum fromValue(String text) {
      for (StateEnum b : StateEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("State")
  private StateEnum state = null;

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

  public ZenidWebInvestigateResponse investigationID(Integer investigationID) {
    this.investigationID = investigationID;
    return this;
  }

  /**
   * Unique identification of the investigation (set of samples)
   * @return investigationID
  **/
  @ApiModelProperty(value = "Unique identification of the investigation (set of samples)")


  public Integer getInvestigationID() {
    return investigationID;
  }

  public void setInvestigationID(Integer investigationID) {
    this.investigationID = investigationID;
  }

  public ZenidWebInvestigateResponse customData(String customData) {
    this.customData = customData;
    return this;
  }

  /**
   * Copy of the input parameter CustomData
   * @return customData
  **/
  @ApiModelProperty(value = "Copy of the input parameter CustomData")


  public String getCustomData() {
    return customData;
  }

  public void setCustomData(String customData) {
    this.customData = customData;
  }

  public ZenidWebInvestigateResponse minedData(ZenidSharedMineAllResult minedData) {
    this.minedData = minedData;
    return this;
  }

  /**
   * Structure of data, mined from sample - {ZenidShared.MineAllResult}.
   * @return minedData
  **/
  @ApiModelProperty(value = "Structure of data, mined from sample - {ZenidShared.MineAllResult}.")

  @Valid

  public ZenidSharedMineAllResult getMinedData() {
    return minedData;
  }

  public void setMinedData(ZenidSharedMineAllResult minedData) {
    this.minedData = minedData;
  }

  public ZenidWebInvestigateResponse documentsData(List<ZenidSharedMineAllResult> documentsData) {
    this.documentsData = documentsData;
    return this;
  }

  public ZenidWebInvestigateResponse addDocumentsDataItem(ZenidSharedMineAllResult documentsDataItem) {
    if (this.documentsData == null) {
      this.documentsData = new ArrayList<>();
    }
    this.documentsData.add(documentsDataItem);
    return this;
  }

  /**
   * If investigation covers multiple documents, each will have their own entry here
   * @return documentsData
  **/
  @ApiModelProperty(value = "If investigation covers multiple documents, each will have their own entry here")

  @Valid

  public List<ZenidSharedMineAllResult> getDocumentsData() {
    return documentsData;
  }

  public void setDocumentsData(List<ZenidSharedMineAllResult> documentsData) {
    this.documentsData = documentsData;
  }

  public ZenidWebInvestigateResponse investigationUrl(String investigationUrl) {
    this.investigationUrl = investigationUrl;
    return this;
  }

  /**
   * URL of the investigation detail
   * @return investigationUrl
  **/
  @ApiModelProperty(value = "URL of the investigation detail")


  public String getInvestigationUrl() {
    return investigationUrl;
  }

  public void setInvestigationUrl(String investigationUrl) {
    this.investigationUrl = investigationUrl;
  }

  public ZenidWebInvestigateResponse validatorResults(List<ZenidWebInvestigationValidatorResponse> validatorResults) {
    this.validatorResults = validatorResults;
    return this;
  }

  public ZenidWebInvestigateResponse addValidatorResultsItem(ZenidWebInvestigationValidatorResponse validatorResultsItem) {
    if (this.validatorResults == null) {
      this.validatorResults = new ArrayList<>();
    }
    this.validatorResults.add(validatorResultsItem);
    return this;
  }

  /**
   * Result of the all validators - List of {ZenidWeb.InvestigationValidatorResponse}
   * @return validatorResults
  **/
  @ApiModelProperty(value = "Result of the all validators - List of {ZenidWeb.InvestigationValidatorResponse}")

  @Valid

  public List<ZenidWebInvestigationValidatorResponse> getValidatorResults() {
    return validatorResults;
  }

  public void setValidatorResults(List<ZenidWebInvestigationValidatorResponse> validatorResults) {
    this.validatorResults = validatorResults;
  }

  public ZenidWebInvestigateResponse state(StateEnum state) {
    this.state = state;
    return this;
  }

  /**
   * State of the request - NotDone/Done/Error
   * @return state
  **/
  @ApiModelProperty(value = "State of the request - NotDone/Done/Error")


  public StateEnum getState() {
    return state;
  }

  public void setState(StateEnum state) {
    this.state = state;
  }

  public ZenidWebInvestigateResponse errorCode(ErrorCodeEnum errorCode) {
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

  public ZenidWebInvestigateResponse errorText(String errorText) {
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

  public ZenidWebInvestigateResponse messageType(String messageType) {
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
    ZenidWebInvestigateResponse zenidWebInvestigateResponse = (ZenidWebInvestigateResponse) o;
    return Objects.equals(this.investigationID, zenidWebInvestigateResponse.investigationID) &&
        Objects.equals(this.customData, zenidWebInvestigateResponse.customData) &&
        Objects.equals(this.minedData, zenidWebInvestigateResponse.minedData) &&
        Objects.equals(this.documentsData, zenidWebInvestigateResponse.documentsData) &&
        Objects.equals(this.investigationUrl, zenidWebInvestigateResponse.investigationUrl) &&
        Objects.equals(this.validatorResults, zenidWebInvestigateResponse.validatorResults) &&
        Objects.equals(this.state, zenidWebInvestigateResponse.state) &&
        Objects.equals(this.errorCode, zenidWebInvestigateResponse.errorCode) &&
        Objects.equals(this.errorText, zenidWebInvestigateResponse.errorText) &&
        Objects.equals(this.messageType, zenidWebInvestigateResponse.messageType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(investigationID, customData, minedData, documentsData, investigationUrl, validatorResults, state, errorCode, errorText, messageType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidWebInvestigateResponse {\n");
    
    sb.append("    investigationID: ").append(toIndentedString(investigationID)).append("\n");
    sb.append("    customData: ").append(toIndentedString(customData)).append("\n");
    sb.append("    minedData: ").append(toIndentedString(minedData)).append("\n");
    sb.append("    documentsData: ").append(toIndentedString(documentsData)).append("\n");
    sb.append("    investigationUrl: ").append(toIndentedString(investigationUrl)).append("\n");
    sb.append("    validatorResults: ").append(toIndentedString(validatorResults)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
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

