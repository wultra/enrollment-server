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
 * InvalidateClaimResponse
 */
@Validated


public class InvalidateClaimResponse   {
  @JsonProperty("claim_aborted")
  private Boolean claimAborted = null;

  @JsonProperty("user_informed")
  private Boolean userInformed = null;

  public InvalidateClaimResponse claimAborted(Boolean claimAborted) {
    this.claimAborted = claimAborted;
    return this;
  }

  /**
   * True if claim was invalidated.
   * @return claimAborted
  **/
  @ApiModelProperty(example = "true", required = true, value = "True if claim was invalidated.")
  @NotNull


  public Boolean isClaimAborted() {
    return claimAborted;
  }

  public void setClaimAborted(Boolean claimAborted) {
    this.claimAborted = claimAborted;
  }

  public InvalidateClaimResponse userInformed(Boolean userInformed) {
    this.userInformed = userInformed;
    return this;
  }

  /**
   * True if the user was successfully informed that the claim has been invalidated.
   * @return userInformed
  **/
  @ApiModelProperty(example = "true", required = true, value = "True if the user was successfully informed that the claim has been invalidated.")
  @NotNull


  public Boolean isUserInformed() {
    return userInformed;
  }

  public void setUserInformed(Boolean userInformed) {
    this.userInformed = userInformed;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InvalidateClaimResponse invalidateClaimResponse = (InvalidateClaimResponse) o;
    return Objects.equals(this.claimAborted, invalidateClaimResponse.claimAborted) &&
        Objects.equals(this.userInformed, invalidateClaimResponse.userInformed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(claimAborted, userInformed);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InvalidateClaimResponse {\n");
    
    sb.append("    claimAborted: ").append(toIndentedString(claimAborted)).append("\n");
    sb.append("    userInformed: ").append(toIndentedString(userInformed)).append("\n");
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

