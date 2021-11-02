package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Return object for /api/face
 */
@ApiModel(description = "Return object for /api/face")
@Validated


public class ZenidWebUploadFaceResponse   {
  /**
   * Possibly result of the upload face photo
   */
  public enum UploadFaceResultEnum {
    OK("Ok"),
    
    FACENOTDETECTED("FaceNotDetected"),
    
    IMAGEEXISTSWITHDIFFERENTCUSTOMERDATA("ImageExistsWithDifferentCustomerData");

    private String value;

    UploadFaceResultEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static UploadFaceResultEnum fromValue(String text) {
      for (UploadFaceResultEnum b : UploadFaceResultEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("UploadFaceResult")
  private UploadFaceResultEnum uploadFaceResult = null;

  @JsonProperty("OriginalImageHash")
  private String originalImageHash = null;

  @JsonProperty("PersistedFace")
  private UUID persistedFace = null;

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

  public ZenidWebUploadFaceResponse uploadFaceResult(UploadFaceResultEnum uploadFaceResult) {
    this.uploadFaceResult = uploadFaceResult;
    return this;
  }

  /**
   * Possibly result of the upload face photo
   * @return uploadFaceResult
  **/
  @ApiModelProperty(value = "Possibly result of the upload face photo")


  public UploadFaceResultEnum getUploadFaceResult() {
    return uploadFaceResult;
  }

  public void setUploadFaceResult(UploadFaceResultEnum uploadFaceResult) {
    this.uploadFaceResult = uploadFaceResult;
  }

  public ZenidWebUploadFaceResponse originalImageHash(String originalImageHash) {
    this.originalImageHash = originalImageHash;
    return this;
  }

  /**
   * hash of the original image
   * @return originalImageHash
  **/
  @ApiModelProperty(value = "hash of the original image")


  public String getOriginalImageHash() {
    return originalImageHash;
  }

  public void setOriginalImageHash(String originalImageHash) {
    this.originalImageHash = originalImageHash;
  }

  public ZenidWebUploadFaceResponse persistedFace(UUID persistedFace) {
    this.persistedFace = persistedFace;
    return this;
  }

  /**
   * GUID - link of the face image in the Oxford API repository
   * @return persistedFace
  **/
  @ApiModelProperty(example = "00000000-0000-0000-0000-000000000000", value = "GUID - link of the face image in the Oxford API repository")

  @Valid

  public UUID getPersistedFace() {
    return persistedFace;
  }

  public void setPersistedFace(UUID persistedFace) {
    this.persistedFace = persistedFace;
  }

  public ZenidWebUploadFaceResponse errorCode(ErrorCodeEnum errorCode) {
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

  public ZenidWebUploadFaceResponse errorText(String errorText) {
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

  public ZenidWebUploadFaceResponse messageType(String messageType) {
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
    ZenidWebUploadFaceResponse zenidWebUploadFaceResponse = (ZenidWebUploadFaceResponse) o;
    return Objects.equals(this.uploadFaceResult, zenidWebUploadFaceResponse.uploadFaceResult) &&
        Objects.equals(this.originalImageHash, zenidWebUploadFaceResponse.originalImageHash) &&
        Objects.equals(this.persistedFace, zenidWebUploadFaceResponse.persistedFace) &&
        Objects.equals(this.errorCode, zenidWebUploadFaceResponse.errorCode) &&
        Objects.equals(this.errorText, zenidWebUploadFaceResponse.errorText) &&
        Objects.equals(this.messageType, zenidWebUploadFaceResponse.messageType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uploadFaceResult, originalImageHash, persistedFace, errorCode, errorText, messageType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidWebUploadFaceResponse {\n");
    
    sb.append("    uploadFaceResult: ").append(toIndentedString(uploadFaceResult)).append("\n");
    sb.append("    originalImageHash: ").append(toIndentedString(originalImageHash)).append("\n");
    sb.append("    persistedFace: ").append(toIndentedString(persistedFace)).append("\n");
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

