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
 * ZenidWebDeleteSampleResponse
 */
@Validated


public class ZenidWebDeleteSampleResponse   {
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

  public ZenidWebDeleteSampleResponse errorCode(ErrorCodeEnum errorCode) {
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

  public ZenidWebDeleteSampleResponse errorText(String errorText) {
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

  public ZenidWebDeleteSampleResponse messageType(String messageType) {
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
    ZenidWebDeleteSampleResponse zenidWebDeleteSampleResponse = (ZenidWebDeleteSampleResponse) o;
    return Objects.equals(this.errorCode, zenidWebDeleteSampleResponse.errorCode) &&
        Objects.equals(this.errorText, zenidWebDeleteSampleResponse.errorText) &&
        Objects.equals(this.messageType, zenidWebDeleteSampleResponse.messageType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(errorCode, errorText, messageType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidWebDeleteSampleResponse {\n");
    
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

