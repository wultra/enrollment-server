package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.wultra.app.docverify.zenid.model.api.ZenidSharedHash;
import com.wultra.app.docverify.zenid.model.api.ZenidSharedMineAllResult;
import com.wultra.app.docverify.zenid.model.api.ZenidWebUploadSampleResponse;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Response object for UploadSample
 */
@ApiModel(description = "Response object for UploadSample")
@Validated


public class ZenidWebUploadSampleResponse   {
  @JsonProperty("SampleID")
  private String sampleID = null;

  @JsonProperty("CustomData")
  private String customData = null;

  @JsonProperty("UploadSessionID")
  private UUID uploadSessionID = null;

  /**
   * Real SampleType
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

  @JsonProperty("MinedData")
  private ZenidSharedMineAllResult minedData = null;

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

  @JsonProperty("ProjectedImage")
  private ZenidSharedHash projectedImage = null;

  @JsonProperty("ParentSampleID")
  private String parentSampleID = null;

  @JsonProperty("AnonymizedImage")
  private ZenidSharedHash anonymizedImage = null;

  @JsonProperty("ImageUrlFormat")
  private String imageUrlFormat = null;

  @JsonProperty("ImagePageCount")
  private Integer imagePageCount = null;

  @JsonProperty("Subsamples")
  @Valid
  private List<ZenidWebUploadSampleResponse> subsamples = null;

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

  public ZenidWebUploadSampleResponse sampleID(String sampleID) {
    this.sampleID = sampleID;
    return this;
  }

  /**
   * Unique ID of the sample in ZenID system.
   * @return sampleID
  **/
  @ApiModelProperty(value = "Unique ID of the sample in ZenID system.")


  public String getSampleID() {
    return sampleID;
  }

  public void setSampleID(String sampleID) {
    this.sampleID = sampleID;
  }

  public ZenidWebUploadSampleResponse customData(String customData) {
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

  public ZenidWebUploadSampleResponse uploadSessionID(UUID uploadSessionID) {
    this.uploadSessionID = uploadSessionID;
    return this;
  }

  /**
   * Copy of the input parameter UploadSessionID
   * @return uploadSessionID
  **/
  @ApiModelProperty(example = "00000000-0000-0000-0000-000000000000", value = "Copy of the input parameter UploadSessionID")

  @Valid

  public UUID getUploadSessionID() {
    return uploadSessionID;
  }

  public void setUploadSessionID(UUID uploadSessionID) {
    this.uploadSessionID = uploadSessionID;
  }

  public ZenidWebUploadSampleResponse sampleType(SampleTypeEnum sampleType) {
    this.sampleType = sampleType;
    return this;
  }

  /**
   * Real SampleType
   * @return sampleType
  **/
  @ApiModelProperty(value = "Real SampleType")


  public SampleTypeEnum getSampleType() {
    return sampleType;
  }

  public void setSampleType(SampleTypeEnum sampleType) {
    this.sampleType = sampleType;
  }

  public ZenidWebUploadSampleResponse minedData(ZenidSharedMineAllResult minedData) {
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

  public ZenidWebUploadSampleResponse state(StateEnum state) {
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

  public ZenidWebUploadSampleResponse projectedImage(ZenidSharedHash projectedImage) {
    this.projectedImage = projectedImage;
    return this;
  }

  /**
   * hash of the source projected image
   * @return projectedImage
  **/
  @ApiModelProperty(value = "hash of the source projected image")

  @Valid

  public ZenidSharedHash getProjectedImage() {
    return projectedImage;
  }

  public void setProjectedImage(ZenidSharedHash projectedImage) {
    this.projectedImage = projectedImage;
  }

  public ZenidWebUploadSampleResponse parentSampleID(String parentSampleID) {
    this.parentSampleID = parentSampleID;
    return this;
  }

  /**
   * hash of the parent sampleID if this is a subsample
   * @return parentSampleID
  **/
  @ApiModelProperty(value = "hash of the parent sampleID if this is a subsample")


  public String getParentSampleID() {
    return parentSampleID;
  }

  public void setParentSampleID(String parentSampleID) {
    this.parentSampleID = parentSampleID;
  }

  public ZenidWebUploadSampleResponse anonymizedImage(ZenidSharedHash anonymizedImage) {
    this.anonymizedImage = anonymizedImage;
    return this;
  }

  /**
   * Hash of the censored projected image
   * @return anonymizedImage
  **/
  @ApiModelProperty(value = "Hash of the censored projected image")

  @Valid

  public ZenidSharedHash getAnonymizedImage() {
    return anonymizedImage;
  }

  public void setAnonymizedImage(ZenidSharedHash anonymizedImage) {
    this.anonymizedImage = anonymizedImage;
  }

  public ZenidWebUploadSampleResponse imageUrlFormat(String imageUrlFormat) {
    this.imageUrlFormat = imageUrlFormat;
    return this;
  }

  /**
   * link to the source projected image
   * @return imageUrlFormat
  **/
  @ApiModelProperty(value = "link to the source projected image")


  public String getImageUrlFormat() {
    return imageUrlFormat;
  }

  public void setImageUrlFormat(String imageUrlFormat) {
    this.imageUrlFormat = imageUrlFormat;
  }

  public ZenidWebUploadSampleResponse imagePageCount(Integer imagePageCount) {
    this.imagePageCount = imagePageCount;
    return this;
  }

  /**
   * Number of pages this document has (in case of PDF or TIFF). This can be used in history URL /history/image/{hash}?page=1
   * @return imagePageCount
  **/
  @ApiModelProperty(value = "Number of pages this document has (in case of PDF or TIFF). This can be used in history URL /history/image/{hash}?page=1")


  public Integer getImagePageCount() {
    return imagePageCount;
  }

  public void setImagePageCount(Integer imagePageCount) {
    this.imagePageCount = imagePageCount;
  }

  public ZenidWebUploadSampleResponse subsamples(List<ZenidWebUploadSampleResponse> subsamples) {
    this.subsamples = subsamples;
    return this;
  }

  public ZenidWebUploadSampleResponse addSubsamplesItem(ZenidWebUploadSampleResponse subsamplesItem) {
    if (this.subsamples == null) {
      this.subsamples = new ArrayList<>();
    }
    this.subsamples.add(subsamplesItem);
    return this;
  }

  /**
   * If subsample processing is enable, this list contains further images extracted from the primary image, each with extra document image
   * @return subsamples
  **/
  @ApiModelProperty(value = "If subsample processing is enable, this list contains further images extracted from the primary image, each with extra document image")

  @Valid

  public List<ZenidWebUploadSampleResponse> getSubsamples() {
    return subsamples;
  }

  public void setSubsamples(List<ZenidWebUploadSampleResponse> subsamples) {
    this.subsamples = subsamples;
  }

  public ZenidWebUploadSampleResponse errorCode(ErrorCodeEnum errorCode) {
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

  public ZenidWebUploadSampleResponse errorText(String errorText) {
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

  public ZenidWebUploadSampleResponse messageType(String messageType) {
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
    ZenidWebUploadSampleResponse zenidWebUploadSampleResponse = (ZenidWebUploadSampleResponse) o;
    return Objects.equals(this.sampleID, zenidWebUploadSampleResponse.sampleID) &&
        Objects.equals(this.customData, zenidWebUploadSampleResponse.customData) &&
        Objects.equals(this.uploadSessionID, zenidWebUploadSampleResponse.uploadSessionID) &&
        Objects.equals(this.sampleType, zenidWebUploadSampleResponse.sampleType) &&
        Objects.equals(this.minedData, zenidWebUploadSampleResponse.minedData) &&
        Objects.equals(this.state, zenidWebUploadSampleResponse.state) &&
        Objects.equals(this.projectedImage, zenidWebUploadSampleResponse.projectedImage) &&
        Objects.equals(this.parentSampleID, zenidWebUploadSampleResponse.parentSampleID) &&
        Objects.equals(this.anonymizedImage, zenidWebUploadSampleResponse.anonymizedImage) &&
        Objects.equals(this.imageUrlFormat, zenidWebUploadSampleResponse.imageUrlFormat) &&
        Objects.equals(this.imagePageCount, zenidWebUploadSampleResponse.imagePageCount) &&
        Objects.equals(this.subsamples, zenidWebUploadSampleResponse.subsamples) &&
        Objects.equals(this.errorCode, zenidWebUploadSampleResponse.errorCode) &&
        Objects.equals(this.errorText, zenidWebUploadSampleResponse.errorText) &&
        Objects.equals(this.messageType, zenidWebUploadSampleResponse.messageType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sampleID, customData, uploadSessionID, sampleType, minedData, state, projectedImage, parentSampleID, anonymizedImage, imageUrlFormat, imagePageCount, subsamples, errorCode, errorText, messageType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidWebUploadSampleResponse {\n");
    
    sb.append("    sampleID: ").append(toIndentedString(sampleID)).append("\n");
    sb.append("    customData: ").append(toIndentedString(customData)).append("\n");
    sb.append("    uploadSessionID: ").append(toIndentedString(uploadSessionID)).append("\n");
    sb.append("    sampleType: ").append(toIndentedString(sampleType)).append("\n");
    sb.append("    minedData: ").append(toIndentedString(minedData)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    projectedImage: ").append(toIndentedString(projectedImage)).append("\n");
    sb.append("    parentSampleID: ").append(toIndentedString(parentSampleID)).append("\n");
    sb.append("    anonymizedImage: ").append(toIndentedString(anonymizedImage)).append("\n");
    sb.append("    imageUrlFormat: ").append(toIndentedString(imageUrlFormat)).append("\n");
    sb.append("    imagePageCount: ").append(toIndentedString(imagePageCount)).append("\n");
    sb.append("    subsamples: ").append(toIndentedString(subsamples)).append("\n");
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

