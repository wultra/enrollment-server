package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Simple MD5 hash wrapper with easy compare/text conversions
 */
@ApiModel(description = "Simple MD5 hash wrapper with easy compare/text conversions")
@Validated


public class ZenidSharedHash   {
  @JsonProperty("AsText")
  private String asText = null;

  @JsonProperty("IsNull")
  private Boolean isNull = null;

  public ZenidSharedHash asText(String asText) {
    this.asText = asText;
    return this;
  }

  /**
   * Get asText
   * @return asText
  **/
  @ApiModelProperty(value = "")


  public String getAsText() {
    return asText;
  }

  public void setAsText(String asText) {
    this.asText = asText;
  }

  public ZenidSharedHash isNull(Boolean isNull) {
    this.isNull = isNull;
    return this;
  }

  /**
   * Get isNull
   * @return isNull
  **/
  @ApiModelProperty(readOnly = true, value = "")


  public Boolean isIsNull() {
    return isNull;
  }

  public void setIsNull(Boolean isNull) {
    this.isNull = isNull;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ZenidSharedHash zenidSharedHash = (ZenidSharedHash) o;
    return Objects.equals(this.asText, zenidSharedHash.asText) &&
        Objects.equals(this.isNull, zenidSharedHash.isNull);
  }

  @Override
  public int hashCode() {
    return Objects.hash(asText, isNull);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidSharedHash {\n");
    
    sb.append("    asText: ").append(toIndentedString(asText)).append("\n");
    sb.append("    isNull: ").append(toIndentedString(isNull)).append("\n");
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

