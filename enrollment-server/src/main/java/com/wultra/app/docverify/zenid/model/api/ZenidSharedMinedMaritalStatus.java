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
 * MinedMaritalStatus - test of field, its confidence and property MaritalStatus (parsed text)
 */
@ApiModel(description = "MinedMaritalStatus - test of field, its confidence and property MaritalStatus (parsed text)")
@Validated


public class ZenidSharedMinedMaritalStatus   {
  /**
   * Gets or Sets maritalStatus
   */
  public enum MaritalStatusEnum {
    SINGLE("Single"),
    
    MARRIED("Married"),
    
    DIVORCED("Divorced"),
    
    WIDOWED("Widowed"),
    
    PARTNERSHIP("Partnership");

    private String value;

    MaritalStatusEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static MaritalStatusEnum fromValue(String text) {
      for (MaritalStatusEnum b : MaritalStatusEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("MaritalStatus")
  private MaritalStatusEnum maritalStatus = null;

  /**
   * Gets or Sets impliedSex
   */
  public enum ImpliedSexEnum {
    F("F"),
    
    M("M");

    private String value;

    ImpliedSexEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static ImpliedSexEnum fromValue(String text) {
      for (ImpliedSexEnum b : ImpliedSexEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("ImpliedSex")
  private ImpliedSexEnum impliedSex = null;

  @JsonProperty("Text")
  private String text = null;

  @JsonProperty("Confidence")
  private Integer confidence = null;

  public ZenidSharedMinedMaritalStatus maritalStatus(MaritalStatusEnum maritalStatus) {
    this.maritalStatus = maritalStatus;
    return this;
  }

  /**
   * Get maritalStatus
   * @return maritalStatus
  **/
  @ApiModelProperty(value = "")


  public MaritalStatusEnum getMaritalStatus() {
    return maritalStatus;
  }

  public void setMaritalStatus(MaritalStatusEnum maritalStatus) {
    this.maritalStatus = maritalStatus;
  }

  public ZenidSharedMinedMaritalStatus impliedSex(ImpliedSexEnum impliedSex) {
    this.impliedSex = impliedSex;
    return this;
  }

  /**
   * Get impliedSex
   * @return impliedSex
  **/
  @ApiModelProperty(value = "")


  public ImpliedSexEnum getImpliedSex() {
    return impliedSex;
  }

  public void setImpliedSex(ImpliedSexEnum impliedSex) {
    this.impliedSex = impliedSex;
  }

  public ZenidSharedMinedMaritalStatus text(String text) {
    this.text = text;
    return this;
  }

  /**
   * Get text
   * @return text
  **/
  @ApiModelProperty(value = "")


  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public ZenidSharedMinedMaritalStatus confidence(Integer confidence) {
    this.confidence = confidence;
    return this;
  }

  /**
   * Get confidence
   * @return confidence
  **/
  @ApiModelProperty(value = "")


  public Integer getConfidence() {
    return confidence;
  }

  public void setConfidence(Integer confidence) {
    this.confidence = confidence;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ZenidSharedMinedMaritalStatus zenidSharedMinedMaritalStatus = (ZenidSharedMinedMaritalStatus) o;
    return Objects.equals(this.maritalStatus, zenidSharedMinedMaritalStatus.maritalStatus) &&
        Objects.equals(this.impliedSex, zenidSharedMinedMaritalStatus.impliedSex) &&
        Objects.equals(this.text, zenidSharedMinedMaritalStatus.text) &&
        Objects.equals(this.confidence, zenidSharedMinedMaritalStatus.confidence);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maritalStatus, impliedSex, text, confidence);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidSharedMinedMaritalStatus {\n");
    
    sb.append("    maritalStatus: ").append(toIndentedString(maritalStatus)).append("\n");
    sb.append("    impliedSex: ").append(toIndentedString(impliedSex)).append("\n");
    sb.append("    text: ").append(toIndentedString(text)).append("\n");
    sb.append("    confidence: ").append(toIndentedString(confidence)).append("\n");
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

