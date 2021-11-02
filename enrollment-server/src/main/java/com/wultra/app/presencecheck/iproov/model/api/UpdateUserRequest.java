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
 * UpdateUserRequest
 */
@Validated


public class UpdateUserRequest   {
  @JsonProperty("user_id")
  private String userId = null;

  @JsonProperty("name")
  private String name = null;

  public UpdateUserRequest userId(String userId) {
    this.userId = userId;
    return this;
  }

  /**
   * The new identifier for the user
   * @return userId
  **/
  @ApiModelProperty(value = "The new identifier for the user")


  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public UpdateUserRequest name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name to display to the user when authenticating
   * @return name
  **/
  @ApiModelProperty(value = "The name to display to the user when authenticating")


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
    UpdateUserRequest updateUserRequest = (UpdateUserRequest) o;
    return Objects.equals(this.userId, updateUserRequest.userId) &&
        Objects.equals(this.name, updateUserRequest.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateUserRequest {\n");
    
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
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

