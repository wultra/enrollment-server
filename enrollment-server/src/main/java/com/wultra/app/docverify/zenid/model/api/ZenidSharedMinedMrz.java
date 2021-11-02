package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.wultra.app.docverify.zenid.model.api.ZenidSharedMrz;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Declare mined Text, Confidence, and also structure Mrz.
 */
@ApiModel(description = "Declare mined Text, Confidence, and also structure Mrz.")
@Validated


public class ZenidSharedMinedMrz   {
  @JsonProperty("Mrz")
  private ZenidSharedMrz mrz = null;

  @JsonProperty("Text")
  private String text = null;

  @JsonProperty("Confidence")
  private Integer confidence = null;

  public ZenidSharedMinedMrz mrz(ZenidSharedMrz mrz) {
    this.mrz = mrz;
    return this;
  }

  /**
   * Get mrz
   * @return mrz
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMrz getMrz() {
    return mrz;
  }

  public void setMrz(ZenidSharedMrz mrz) {
    this.mrz = mrz;
  }

  public ZenidSharedMinedMrz text(String text) {
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

  public ZenidSharedMinedMrz confidence(Integer confidence) {
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
    ZenidSharedMinedMrz zenidSharedMinedMrz = (ZenidSharedMinedMrz) o;
    return Objects.equals(this.mrz, zenidSharedMinedMrz.mrz) &&
        Objects.equals(this.text, zenidSharedMinedMrz.text) &&
        Objects.equals(this.confidence, zenidSharedMinedMrz.confidence);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mrz, text, confidence);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidSharedMinedMrz {\n");
    
    sb.append("    mrz: ").append(toIndentedString(mrz)).append("\n");
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

