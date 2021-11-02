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
 * MinedSex - test of field, its confidence and property Sex (parsed text)
 */
@ApiModel(description = "MinedSex - test of field, its confidence and property Sex (parsed text)")
@Validated


public class ZenidSharedMinedSex   {
  /**
   * Gets or Sets sex
   */
  public enum SexEnum {
    F("F"),
    
    M("M");

    private String value;

    SexEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static SexEnum fromValue(String text) {
      for (SexEnum b : SexEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("Sex")
  private SexEnum sex = null;

  @JsonProperty("Text")
  private String text = null;

  @JsonProperty("Confidence")
  private Integer confidence = null;

  public ZenidSharedMinedSex sex(SexEnum sex) {
    this.sex = sex;
    return this;
  }

  /**
   * Get sex
   * @return sex
  **/
  @ApiModelProperty(value = "")


  public SexEnum getSex() {
    return sex;
  }

  public void setSex(SexEnum sex) {
    this.sex = sex;
  }

  public ZenidSharedMinedSex text(String text) {
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

  public ZenidSharedMinedSex confidence(Integer confidence) {
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
    ZenidSharedMinedSex zenidSharedMinedSex = (ZenidSharedMinedSex) o;
    return Objects.equals(this.sex, zenidSharedMinedSex.sex) &&
        Objects.equals(this.text, zenidSharedMinedSex.text) &&
        Objects.equals(this.confidence, zenidSharedMinedSex.confidence);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sex, text, confidence);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidSharedMinedSex {\n");
    
    sb.append("    sex: ").append(toIndentedString(sex)).append("\n");
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

