package com.wultra.app.presencecheck.iproov.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.wultra.app.presencecheck.iproov.model.api.AssociateUserRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * EnrolAssociateUserRequest
 */
@Validated


public class EnrolAssociateUserRequest extends AssociateUserRequest  {
  @JsonProperty("name")
  private String name = null;

  public EnrolAssociateUserRequest name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The display name of the user. If not provided it is parsed from the user id.
   * @return name
  **/
  @ApiModelProperty(value = "The display name of the user. If not provided it is parsed from the user id.")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnrolAssociateUserRequest enrolAssociateUserRequest = (EnrolAssociateUserRequest) o;
    return Objects.equals(this.name, enrolAssociateUserRequest.name) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EnrolAssociateUserRequest {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

