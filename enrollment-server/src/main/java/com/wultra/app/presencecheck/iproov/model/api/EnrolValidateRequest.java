package com.wultra.app.presencecheck.iproov.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.wultra.app.presencecheck.iproov.model.api.ClaimValidateRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * EnrolValidateRequest
 */
@Validated


public class EnrolValidateRequest extends ClaimValidateRequest  {
  @JsonProperty("activate")
  private Boolean activate = null;

  public EnrolValidateRequest activate(Boolean activate) {
    this.activate = activate;
    return this;
  }

  /**
   * Activate the user's account (default: true).  User will be SUSPENDED if false.
   * @return activate
  **/
  @ApiModelProperty(value = "Activate the user's account (default: true).  User will be SUSPENDED if false.")


  public Boolean isActivate() {
    return activate;
  }

  public void setActivate(Boolean activate) {
    this.activate = activate;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnrolValidateRequest enrolValidateRequest = (EnrolValidateRequest) o;
    return Objects.equals(this.activate, enrolValidateRequest.activate) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(activate, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EnrolValidateRequest {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    activate: ").append(toIndentedString(activate)).append("\n");
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

