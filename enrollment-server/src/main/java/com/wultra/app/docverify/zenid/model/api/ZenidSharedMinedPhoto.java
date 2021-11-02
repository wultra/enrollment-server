package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.wultra.app.docverify.zenid.model.api.ZenidSharedLazyMatImage;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * MinedPhoto - shows image data, and also two face-related values - estimated age and sex.
 */
@ApiModel(description = "MinedPhoto - shows image data, and also two face-related values - estimated age and sex.")
@Validated


public class ZenidSharedMinedPhoto   {
  @JsonProperty("ImageData")
  private ZenidSharedLazyMatImage imageData = null;

  @JsonProperty("EstimatedAge")
  private Double estimatedAge = null;

  /**
   * Gets or Sets estimatedSex
   */
  public enum EstimatedSexEnum {
    F("F"),
    
    M("M");

    private String value;

    EstimatedSexEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static EstimatedSexEnum fromValue(String text) {
      for (EstimatedSexEnum b : EstimatedSexEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("EstimatedSex")
  private EstimatedSexEnum estimatedSex = null;

  @JsonProperty("HasOccludedMouth")
  private Boolean hasOccludedMouth = null;

  @JsonProperty("HasSunGlasses")
  private Boolean hasSunGlasses = null;

  @JsonProperty("HasHeadWear")
  private Boolean hasHeadWear = null;

  @JsonProperty("Text")
  private String text = null;

  @JsonProperty("Confidence")
  private Integer confidence = null;

  public ZenidSharedMinedPhoto imageData(ZenidSharedLazyMatImage imageData) {
    this.imageData = imageData;
    return this;
  }

  /**
   * Get imageData
   * @return imageData
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedLazyMatImage getImageData() {
    return imageData;
  }

  public void setImageData(ZenidSharedLazyMatImage imageData) {
    this.imageData = imageData;
  }

  public ZenidSharedMinedPhoto estimatedAge(Double estimatedAge) {
    this.estimatedAge = estimatedAge;
    return this;
  }

  /**
   * Get estimatedAge
   * @return estimatedAge
  **/
  @ApiModelProperty(value = "")


  public Double getEstimatedAge() {
    return estimatedAge;
  }

  public void setEstimatedAge(Double estimatedAge) {
    this.estimatedAge = estimatedAge;
  }

  public ZenidSharedMinedPhoto estimatedSex(EstimatedSexEnum estimatedSex) {
    this.estimatedSex = estimatedSex;
    return this;
  }

  /**
   * Get estimatedSex
   * @return estimatedSex
  **/
  @ApiModelProperty(value = "")


  public EstimatedSexEnum getEstimatedSex() {
    return estimatedSex;
  }

  public void setEstimatedSex(EstimatedSexEnum estimatedSex) {
    this.estimatedSex = estimatedSex;
  }

  public ZenidSharedMinedPhoto hasOccludedMouth(Boolean hasOccludedMouth) {
    this.hasOccludedMouth = hasOccludedMouth;
    return this;
  }

  /**
   * Get hasOccludedMouth
   * @return hasOccludedMouth
  **/
  @ApiModelProperty(value = "")


  public Boolean isHasOccludedMouth() {
    return hasOccludedMouth;
  }

  public void setHasOccludedMouth(Boolean hasOccludedMouth) {
    this.hasOccludedMouth = hasOccludedMouth;
  }

  public ZenidSharedMinedPhoto hasSunGlasses(Boolean hasSunGlasses) {
    this.hasSunGlasses = hasSunGlasses;
    return this;
  }

  /**
   * Get hasSunGlasses
   * @return hasSunGlasses
  **/
  @ApiModelProperty(value = "")


  public Boolean isHasSunGlasses() {
    return hasSunGlasses;
  }

  public void setHasSunGlasses(Boolean hasSunGlasses) {
    this.hasSunGlasses = hasSunGlasses;
  }

  public ZenidSharedMinedPhoto hasHeadWear(Boolean hasHeadWear) {
    this.hasHeadWear = hasHeadWear;
    return this;
  }

  /**
   * Get hasHeadWear
   * @return hasHeadWear
  **/
  @ApiModelProperty(value = "")


  public Boolean isHasHeadWear() {
    return hasHeadWear;
  }

  public void setHasHeadWear(Boolean hasHeadWear) {
    this.hasHeadWear = hasHeadWear;
  }

  public ZenidSharedMinedPhoto text(String text) {
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

  public ZenidSharedMinedPhoto confidence(Integer confidence) {
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
    ZenidSharedMinedPhoto zenidSharedMinedPhoto = (ZenidSharedMinedPhoto) o;
    return Objects.equals(this.imageData, zenidSharedMinedPhoto.imageData) &&
        Objects.equals(this.estimatedAge, zenidSharedMinedPhoto.estimatedAge) &&
        Objects.equals(this.estimatedSex, zenidSharedMinedPhoto.estimatedSex) &&
        Objects.equals(this.hasOccludedMouth, zenidSharedMinedPhoto.hasOccludedMouth) &&
        Objects.equals(this.hasSunGlasses, zenidSharedMinedPhoto.hasSunGlasses) &&
        Objects.equals(this.hasHeadWear, zenidSharedMinedPhoto.hasHeadWear) &&
        Objects.equals(this.text, zenidSharedMinedPhoto.text) &&
        Objects.equals(this.confidence, zenidSharedMinedPhoto.confidence);
  }

  @Override
  public int hashCode() {
    return Objects.hash(imageData, estimatedAge, estimatedSex, hasOccludedMouth, hasSunGlasses, hasHeadWear, text, confidence);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidSharedMinedPhoto {\n");
    
    sb.append("    imageData: ").append(toIndentedString(imageData)).append("\n");
    sb.append("    estimatedAge: ").append(toIndentedString(estimatedAge)).append("\n");
    sb.append("    estimatedSex: ").append(toIndentedString(estimatedSex)).append("\n");
    sb.append("    hasOccludedMouth: ").append(toIndentedString(hasOccludedMouth)).append("\n");
    sb.append("    hasSunGlasses: ").append(toIndentedString(hasSunGlasses)).append("\n");
    sb.append("    hasHeadWear: ").append(toIndentedString(hasHeadWear)).append("\n");
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

