package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Return value of api/profiles
 */
@ApiModel(description = "Return value of api/profiles")
@Validated


public class ZenidWebListProfilesResponse   {
  @JsonProperty("Results")
  @Valid
  private List<String> results = null;

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

  public ZenidWebListProfilesResponse results(List<String> results) {
    this.results = results;
    return this;
  }

  public ZenidWebListProfilesResponse addResultsItem(String resultsItem) {
    if (this.results == null) {
      this.results = new ArrayList<>();
    }
    this.results.add(resultsItem);
    return this;
  }

  /**
   * List of names of profiles
   * @return results
  **/
  @ApiModelProperty(value = "List of names of profiles")


  public List<String> getResults() {
    return results;
  }

  public void setResults(List<String> results) {
    this.results = results;
  }

  public ZenidWebListProfilesResponse errorCode(ErrorCodeEnum errorCode) {
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

  public ZenidWebListProfilesResponse errorText(String errorText) {
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

  public ZenidWebListProfilesResponse messageType(String messageType) {
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
    ZenidWebListProfilesResponse zenidWebListProfilesResponse = (ZenidWebListProfilesResponse) o;
    return Objects.equals(this.results, zenidWebListProfilesResponse.results) &&
        Objects.equals(this.errorCode, zenidWebListProfilesResponse.errorCode) &&
        Objects.equals(this.errorText, zenidWebListProfilesResponse.errorText) &&
        Objects.equals(this.messageType, zenidWebListProfilesResponse.messageType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(results, errorCode, errorText, messageType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidWebListProfilesResponse {\n");
    
    sb.append("    results: ").append(toIndentedString(results)).append("\n");
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

