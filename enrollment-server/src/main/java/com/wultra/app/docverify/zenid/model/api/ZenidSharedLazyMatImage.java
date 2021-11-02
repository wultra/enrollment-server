package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.wultra.app.docverify.zenid.model.api.ZenidSharedHash;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ZenidSharedLazyMatImage
 */
@Validated


public class ZenidSharedLazyMatImage   {
  @JsonProperty("ImageHash")
  private ZenidSharedHash imageHash = null;

  public ZenidSharedLazyMatImage imageHash(ZenidSharedHash imageHash) {
    this.imageHash = imageHash;
    return this;
  }

  /**
   * Get imageHash
   * @return imageHash
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedHash getImageHash() {
    return imageHash;
  }

  public void setImageHash(ZenidSharedHash imageHash) {
    this.imageHash = imageHash;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ZenidSharedLazyMatImage zenidSharedLazyMatImage = (ZenidSharedLazyMatImage) o;
    return Objects.equals(this.imageHash, zenidSharedLazyMatImage.imageHash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(imageHash);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidSharedLazyMatImage {\n");
    
    sb.append("    imageHash: ").append(toIndentedString(imageHash)).append("\n");
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

