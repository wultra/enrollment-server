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
 * ZenidSharedLicenseCountables
 */
@Validated


public class ZenidSharedLicenseCountables   {
  @JsonProperty("PageCount")
  private Integer pageCount = null;

  @JsonProperty("SelfieCount")
  private Integer selfieCount = null;

  @JsonProperty("FraudCount")
  private Integer fraudCount = null;

  public ZenidSharedLicenseCountables pageCount(Integer pageCount) {
    this.pageCount = pageCount;
    return this;
  }

  /**
   * Note this is actually \"document count\"
   * @return pageCount
  **/
  @ApiModelProperty(value = "Note this is actually \"document count\"")


  public Integer getPageCount() {
    return pageCount;
  }

  public void setPageCount(Integer pageCount) {
    this.pageCount = pageCount;
  }

  public ZenidSharedLicenseCountables selfieCount(Integer selfieCount) {
    this.selfieCount = selfieCount;
    return this;
  }

  /**
   * Get selfieCount
   * @return selfieCount
  **/
  @ApiModelProperty(value = "")


  public Integer getSelfieCount() {
    return selfieCount;
  }

  public void setSelfieCount(Integer selfieCount) {
    this.selfieCount = selfieCount;
  }

  public ZenidSharedLicenseCountables fraudCount(Integer fraudCount) {
    this.fraudCount = fraudCount;
    return this;
  }

  /**
   * Get fraudCount
   * @return fraudCount
  **/
  @ApiModelProperty(value = "")


  public Integer getFraudCount() {
    return fraudCount;
  }

  public void setFraudCount(Integer fraudCount) {
    this.fraudCount = fraudCount;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ZenidSharedLicenseCountables zenidSharedLicenseCountables = (ZenidSharedLicenseCountables) o;
    return Objects.equals(this.pageCount, zenidSharedLicenseCountables.pageCount) &&
        Objects.equals(this.selfieCount, zenidSharedLicenseCountables.selfieCount) &&
        Objects.equals(this.fraudCount, zenidSharedLicenseCountables.fraudCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pageCount, selfieCount, fraudCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidSharedLicenseCountables {\n");
    
    sb.append("    pageCount: ").append(toIndentedString(pageCount)).append("\n");
    sb.append("    selfieCount: ").append(toIndentedString(selfieCount)).append("\n");
    sb.append("    fraudCount: ").append(toIndentedString(fraudCount)).append("\n");
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

