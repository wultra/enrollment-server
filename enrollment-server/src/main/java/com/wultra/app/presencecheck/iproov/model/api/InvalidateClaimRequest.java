package com.wultra.app.presencecheck.iproov.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * InvalidateClaimRequest
 */
@Validated


public class InvalidateClaimRequest   {
  @JsonProperty("reason")
  private String reason = null;

  public InvalidateClaimRequest reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * The reason to be displayed to the user for the invalidation of the claim
   * @return reason
  **/
  @ApiModelProperty(required = true, value = "The reason to be displayed to the user for the invalidation of the claim")
  @NotNull


  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InvalidateClaimRequest invalidateClaimRequest = (InvalidateClaimRequest) o;
    return Objects.equals(this.reason, invalidateClaimRequest.reason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InvalidateClaimRequest {\n");
    
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
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

