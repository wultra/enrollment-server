package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.wultra.app.docverify.zenid.model.api.ZenidWebListSamplesResponseSampleItem;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Return value of api/samples
 */
@ApiModel(description = "Return value of api/samples")
@Validated


public class ZenidWebListSamplesResponse   {
  @JsonProperty("Results")
  @Valid
  private List<ZenidWebListSamplesResponseSampleItem> results = null;

  @JsonProperty("TimeStamp")
  private Long timeStamp = null;

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

  public ZenidWebListSamplesResponse results(List<ZenidWebListSamplesResponseSampleItem> results) {
    this.results = results;
    return this;
  }

  public ZenidWebListSamplesResponse addResultsItem(ZenidWebListSamplesResponseSampleItem resultsItem) {
    if (this.results == null) {
      this.results = new ArrayList<>();
    }
    this.results.add(resultsItem);
    return this;
  }

  /**
   * List of declarations of samples - ID, CustomData, UploadSessionID, State
   * @return results
  **/
  @ApiModelProperty(value = "List of declarations of samples - ID, CustomData, UploadSessionID, State")

  @Valid

  public List<ZenidWebListSamplesResponseSampleItem> getResults() {
    return results;
  }

  public void setResults(List<ZenidWebListSamplesResponseSampleItem> results) {
    this.results = results;
  }

  public ZenidWebListSamplesResponse timeStamp(Long timeStamp) {
    this.timeStamp = timeStamp;
    return this;
  }

  /**
   * Timestamp limit (if defined as input)
   * @return timeStamp
  **/
  @ApiModelProperty(value = "Timestamp limit (if defined as input)")


  public Long getTimeStamp() {
    return timeStamp;
  }

  public void setTimeStamp(Long timeStamp) {
    this.timeStamp = timeStamp;
  }

  public ZenidWebListSamplesResponse errorCode(ErrorCodeEnum errorCode) {
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

  public ZenidWebListSamplesResponse errorText(String errorText) {
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

  public ZenidWebListSamplesResponse messageType(String messageType) {
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
    ZenidWebListSamplesResponse zenidWebListSamplesResponse = (ZenidWebListSamplesResponse) o;
    return Objects.equals(this.results, zenidWebListSamplesResponse.results) &&
        Objects.equals(this.timeStamp, zenidWebListSamplesResponse.timeStamp) &&
        Objects.equals(this.errorCode, zenidWebListSamplesResponse.errorCode) &&
        Objects.equals(this.errorText, zenidWebListSamplesResponse.errorText) &&
        Objects.equals(this.messageType, zenidWebListSamplesResponse.messageType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(results, timeStamp, errorCode, errorText, messageType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidWebListSamplesResponse {\n");
    
    sb.append("    results: ").append(toIndentedString(results)).append("\n");
    sb.append("    timeStamp: ").append(toIndentedString(timeStamp)).append("\n");
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

