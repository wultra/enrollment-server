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
 * AssociateUserRequest
 */
@Validated


public class AssociateUserRequest   {
  @JsonProperty("api_key")
  private String apiKey = null;

  @JsonProperty("secret")
  private String secret = null;

  @JsonProperty("user_id")
  private String userId = null;

  @JsonProperty("token")
  private String token = null;

  public AssociateUserRequest apiKey(String apiKey) {
    this.apiKey = apiKey;
    return this;
  }

  /**
   * The API key of the service provider
   * @return apiKey
  **/
  @ApiModelProperty(required = true, value = "The API key of the service provider")
  @NotNull


  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public AssociateUserRequest secret(String secret) {
    this.secret = secret;
    return this;
  }

  /**
   * The API secret for the service provider
   * @return secret
  **/
  @ApiModelProperty(required = true, value = "The API secret for the service provider")
  @NotNull


  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public AssociateUserRequest userId(String userId) {
    this.userId = userId;
    return this;
  }

  /**
   * The asserted identifier of the user.
   * @return userId
  **/
  @ApiModelProperty(example = "enquiries@iproov.com", required = true, value = "The asserted identifier of the user.")
  @NotNull


  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public AssociateUserRequest token(String token) {
    this.token = token;
    return this;
  }

  /**
   * The token for the claim
   * @return token
  **/
  @ApiModelProperty(example = "31706131726336496d655177346e55503279616b69547344446e5258684c7542", required = true, value = "The token for the claim")
  @NotNull


  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AssociateUserRequest associateUserRequest = (AssociateUserRequest) o;
    return Objects.equals(this.apiKey, associateUserRequest.apiKey) &&
        Objects.equals(this.secret, associateUserRequest.secret) &&
        Objects.equals(this.userId, associateUserRequest.userId) &&
        Objects.equals(this.token, associateUserRequest.token);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiKey, secret, userId, token);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AssociateUserRequest {\n");
    
    sb.append("    apiKey: ").append(toIndentedString(apiKey)).append("\n");
    sb.append("    secret: ").append(toIndentedString(secret)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    token: ").append(toIndentedString(token)).append("\n");
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

