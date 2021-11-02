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
 * ZenidWebListSamplesResponseSampleItem
 */
@Validated


public class ZenidWebListSamplesResponseSampleItem   {
  @JsonProperty("SampleID")
  private String sampleID = null;

  @JsonProperty("ParentSampleID")
  private String parentSampleID = null;

  @JsonProperty("CustomData")
  private String customData = null;

  @JsonProperty("UploadSessionID")
  private UUID uploadSessionID = null;

  /**
   * State of the investigation
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

  public ZenidWebListSamplesResponseSampleItem sampleID(String sampleID) {
    this.sampleID = sampleID;
    return this;
  }

  /**
   * DB ID of given sample.
   * @return sampleID
  **/
  @ApiModelProperty(value = "DB ID of given sample.")


  public String getSampleID() {
    return sampleID;
  }

  public void setSampleID(String sampleID) {
    this.sampleID = sampleID;
  }

  public ZenidWebListSamplesResponseSampleItem parentSampleID(String parentSampleID) {
    this.parentSampleID = parentSampleID;
    return this;
  }

  /**
   * If the sample is subsample image created from primary one, this is the ID of primary image
   * @return parentSampleID
  **/
  @ApiModelProperty(value = "If the sample is subsample image created from primary one, this is the ID of primary image")


  public String getParentSampleID() {
    return parentSampleID;
  }

  public void setParentSampleID(String parentSampleID) {
    this.parentSampleID = parentSampleID;
  }

  public ZenidWebListSamplesResponseSampleItem customData(String customData) {
    this.customData = customData;
    return this;
  }

  /**
   * CustomData attribute (copied from Request)
   * @return customData
  **/
  @ApiModelProperty(value = "CustomData attribute (copied from Request)")


  public String getCustomData() {
    return customData;
  }

  public void setCustomData(String customData) {
    this.customData = customData;
  }

  public ZenidWebListSamplesResponseSampleItem uploadSessionID(UUID uploadSessionID) {
    this.uploadSessionID = uploadSessionID;
    return this;
  }

  /**
   * GUID of upload session set.
   * @return uploadSessionID
  **/
  @ApiModelProperty(example = "00000000-0000-0000-0000-000000000000", value = "GUID of upload session set.")

  @Valid

  public UUID getUploadSessionID() {
    return uploadSessionID;
  }

  public void setUploadSessionID(UUID uploadSessionID) {
    this.uploadSessionID = uploadSessionID;
  }

  public ZenidWebListSamplesResponseSampleItem state(StateEnum state) {
    this.state = state;
    return this;
  }

  /**
   * State of the investigation
   * @return state
  **/
  @ApiModelProperty(value = "State of the investigation")


  public StateEnum getState() {
    return state;
  }

  public void setState(StateEnum state) {
    this.state = state;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ZenidWebListSamplesResponseSampleItem zenidWebListSamplesResponseSampleItem = (ZenidWebListSamplesResponseSampleItem) o;
    return Objects.equals(this.sampleID, zenidWebListSamplesResponseSampleItem.sampleID) &&
        Objects.equals(this.parentSampleID, zenidWebListSamplesResponseSampleItem.parentSampleID) &&
        Objects.equals(this.customData, zenidWebListSamplesResponseSampleItem.customData) &&
        Objects.equals(this.uploadSessionID, zenidWebListSamplesResponseSampleItem.uploadSessionID) &&
        Objects.equals(this.state, zenidWebListSamplesResponseSampleItem.state);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sampleID, parentSampleID, customData, uploadSessionID, state);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidWebListSamplesResponseSampleItem {\n");
    
    sb.append("    sampleID: ").append(toIndentedString(sampleID)).append("\n");
    sb.append("    parentSampleID: ").append(toIndentedString(parentSampleID)).append("\n");
    sb.append("    customData: ").append(toIndentedString(customData)).append("\n");
    sb.append("    uploadSessionID: ").append(toIndentedString(uploadSessionID)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
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

