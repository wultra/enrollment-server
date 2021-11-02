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
 * EnrolResponse
 */
@Validated


public class EnrolResponse   {
  @JsonProperty("token")
  private String token = null;

  @JsonProperty("user_id")
  private String userId = null;

  @JsonProperty("success")
  private Boolean success = null;

  public EnrolResponse token(String token) {
    this.token = token;
    return this;
  }

  /**
   * Get token
   * @return token
  **/
  @ApiModelProperty(example = "31706131726336496d655177346e55503279616b69547344446e5258684c7542", required = true, value = "")
  @NotNull


  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public EnrolResponse userId(String userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Get userId
   * @return userId
  **/
  @ApiModelProperty(example = "enquiries@iproov.com", required = true, value = "")
  @NotNull


  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public EnrolResponse success(Boolean success) {
    this.success = success;
    return this;
  }

  /**
   * Get success
   * @return success
  **/
  @ApiModelProperty(example = "true", required = true, value = "")
  @NotNull


  public Boolean isSuccess() {
    return success;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnrolResponse enrolResponse = (EnrolResponse) o;
    return Objects.equals(this.token, enrolResponse.token) &&
        Objects.equals(this.userId, enrolResponse.userId) &&
        Objects.equals(this.success, enrolResponse.success);
  }

  @Override
  public int hashCode() {
    return Objects.hash(token, userId, success);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EnrolResponse {\n");
    
    sb.append("    token: ").append(toIndentedString(token)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    success: ").append(toIndentedString(success)).append("\n");
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

