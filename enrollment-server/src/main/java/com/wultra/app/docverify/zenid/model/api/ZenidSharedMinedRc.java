package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Object containing mined information about birth-number - checksum, date, sex...
 */
@ApiModel(description = "Object containing mined information about birth-number - checksum, date, sex...")
@Validated


public class ZenidSharedMinedRc   {
  @JsonProperty("BirthDate")
  private LocalDate birthDate = null;

  @JsonProperty("Checksum")
  private Integer checksum = null;

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

  public ZenidSharedMinedRc birthDate(LocalDate birthDate) {
    this.birthDate = birthDate;
    return this;
  }

  /**
   * Date of the birth - can be parsed from RC identifier
   * @return birthDate
  **/
  @ApiModelProperty(value = "Date of the birth - can be parsed from RC identifier")

  @Valid

  public LocalDate getBirthDate() {
    return birthDate;
  }

  public void setBirthDate(LocalDate birthDate) {
    this.birthDate = birthDate;
  }

  public ZenidSharedMinedRc checksum(Integer checksum) {
    this.checksum = checksum;
    return this;
  }

  /**
   * Get checksum
   * @return checksum
  **/
  @ApiModelProperty(readOnly = true, value = "")


  public Integer getChecksum() {
    return checksum;
  }

  public void setChecksum(Integer checksum) {
    this.checksum = checksum;
  }

  public ZenidSharedMinedRc sex(SexEnum sex) {
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

  public ZenidSharedMinedRc text(String text) {
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

  public ZenidSharedMinedRc confidence(Integer confidence) {
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
    ZenidSharedMinedRc zenidSharedMinedRc = (ZenidSharedMinedRc) o;
    return Objects.equals(this.birthDate, zenidSharedMinedRc.birthDate) &&
        Objects.equals(this.checksum, zenidSharedMinedRc.checksum) &&
        Objects.equals(this.sex, zenidSharedMinedRc.sex) &&
        Objects.equals(this.text, zenidSharedMinedRc.text) &&
        Objects.equals(this.confidence, zenidSharedMinedRc.confidence);
  }

  @Override
  public int hashCode() {
    return Objects.hash(birthDate, checksum, sex, text, confidence);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidSharedMinedRc {\n");
    
    sb.append("    birthDate: ").append(toIndentedString(birthDate)).append("\n");
    sb.append("    checksum: ").append(toIndentedString(checksum)).append("\n");
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

