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
 * ZenidWebDeletePersonResponse
 */
@Validated


public class ZenidWebDeletePersonResponse   {
  @JsonProperty("DeletedSampleIDs")
  @Valid
  private List<String> deletedSampleIDs = null;

  @JsonProperty("DeletedFacesFromSampleIDs")
  @Valid
  private List<String> deletedFacesFromSampleIDs = null;

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

  public ZenidWebDeletePersonResponse deletedSampleIDs(List<String> deletedSampleIDs) {
    this.deletedSampleIDs = deletedSampleIDs;
    return this;
  }

  public ZenidWebDeletePersonResponse addDeletedSampleIDsItem(String deletedSampleIDsItem) {
    if (this.deletedSampleIDs == null) {
      this.deletedSampleIDs = new ArrayList<>();
    }
    this.deletedSampleIDs.add(deletedSampleIDsItem);
    return this;
  }

  /**
   * Get deletedSampleIDs
   * @return deletedSampleIDs
  **/
  @ApiModelProperty(value = "")


  public List<String> getDeletedSampleIDs() {
    return deletedSampleIDs;
  }

  public void setDeletedSampleIDs(List<String> deletedSampleIDs) {
    this.deletedSampleIDs = deletedSampleIDs;
  }

  public ZenidWebDeletePersonResponse deletedFacesFromSampleIDs(List<String> deletedFacesFromSampleIDs) {
    this.deletedFacesFromSampleIDs = deletedFacesFromSampleIDs;
    return this;
  }

  public ZenidWebDeletePersonResponse addDeletedFacesFromSampleIDsItem(String deletedFacesFromSampleIDsItem) {
    if (this.deletedFacesFromSampleIDs == null) {
      this.deletedFacesFromSampleIDs = new ArrayList<>();
    }
    this.deletedFacesFromSampleIDs.add(deletedFacesFromSampleIDsItem);
    return this;
  }

  /**
   * Get deletedFacesFromSampleIDs
   * @return deletedFacesFromSampleIDs
  **/
  @ApiModelProperty(value = "")


  public List<String> getDeletedFacesFromSampleIDs() {
    return deletedFacesFromSampleIDs;
  }

  public void setDeletedFacesFromSampleIDs(List<String> deletedFacesFromSampleIDs) {
    this.deletedFacesFromSampleIDs = deletedFacesFromSampleIDs;
  }

  public ZenidWebDeletePersonResponse errorCode(ErrorCodeEnum errorCode) {
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

  public ZenidWebDeletePersonResponse errorText(String errorText) {
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

  public ZenidWebDeletePersonResponse messageType(String messageType) {
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
    ZenidWebDeletePersonResponse zenidWebDeletePersonResponse = (ZenidWebDeletePersonResponse) o;
    return Objects.equals(this.deletedSampleIDs, zenidWebDeletePersonResponse.deletedSampleIDs) &&
        Objects.equals(this.deletedFacesFromSampleIDs, zenidWebDeletePersonResponse.deletedFacesFromSampleIDs) &&
        Objects.equals(this.errorCode, zenidWebDeletePersonResponse.errorCode) &&
        Objects.equals(this.errorText, zenidWebDeletePersonResponse.errorText) &&
        Objects.equals(this.messageType, zenidWebDeletePersonResponse.messageType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(deletedSampleIDs, deletedFacesFromSampleIDs, errorCode, errorText, messageType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidWebDeletePersonResponse {\n");
    
    sb.append("    deletedSampleIDs: ").append(toIndentedString(deletedSampleIDs)).append("\n");
    sb.append("    deletedFacesFromSampleIDs: ").append(toIndentedString(deletedFacesFromSampleIDs)).append("\n");
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

