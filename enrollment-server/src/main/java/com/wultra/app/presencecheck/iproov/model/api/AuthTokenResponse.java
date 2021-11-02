package com.wultra.app.presencecheck.iproov.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A OAuth2 standard compliant response giving an OAuth access token. Usable for up to 50 requests
 */
@ApiModel(description = "A OAuth2 standard compliant response giving an OAuth access token. Usable for up to 50 requests")
@Validated


public class AuthTokenResponse   {
  @JsonProperty("access_token")
  private String accessToken = null;

  @JsonProperty("token_type")
  private String tokenType = null;

  @JsonProperty("expires_in")
  private Integer expiresIn = null;

  @JsonProperty("scope")
  @Valid
  private List<String> scope = null;

  public AuthTokenResponse accessToken(String accessToken) {
    this.accessToken = accessToken;
    return this;
  }

  /**
   * Get accessToken
   * @return accessToken
  **/
  @ApiModelProperty(example = "zAVFcpZ1Lxa5DeFnIbOotU4QjX7GEYuonoEj2WY2VygoJzCYhjEqiQqnNrVq", required = true, value = "")
  @NotNull


  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public AuthTokenResponse tokenType(String tokenType) {
    this.tokenType = tokenType;
    return this;
  }

  /**
   * Get tokenType
   * @return tokenType
  **/
  @ApiModelProperty(example = "Bearer", required = true, value = "")
  @NotNull


  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  public AuthTokenResponse expiresIn(Integer expiresIn) {
    this.expiresIn = expiresIn;
    return this;
  }

  /**
   * Get expiresIn
   * @return expiresIn
  **/
  @ApiModelProperty(example = "3600", required = true, value = "")
  @NotNull


  public Integer getExpiresIn() {
    return expiresIn;
  }

  public void setExpiresIn(Integer expiresIn) {
    this.expiresIn = expiresIn;
  }

  public AuthTokenResponse scope(List<String> scope) {
    this.scope = scope;
    return this;
  }

  public AuthTokenResponse addScopeItem(String scopeItem) {
    if (this.scope == null) {
      this.scope = new ArrayList<>();
    }
    this.scope.add(scopeItem);
    return this;
  }

  /**
   * Get scope
   * @return scope
  **/
  @ApiModelProperty(example = "[\"user-read\"]", value = "")


  public List<String> getScope() {
    return scope;
  }

  public void setScope(List<String> scope) {
    this.scope = scope;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuthTokenResponse authTokenResponse = (AuthTokenResponse) o;
    return Objects.equals(this.accessToken, authTokenResponse.accessToken) &&
        Objects.equals(this.tokenType, authTokenResponse.tokenType) &&
        Objects.equals(this.expiresIn, authTokenResponse.expiresIn) &&
        Objects.equals(this.scope, authTokenResponse.scope);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessToken, tokenType, expiresIn, scope);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AuthTokenResponse {\n");
    
    sb.append("    accessToken: ").append(toIndentedString(accessToken)).append("\n");
    sb.append("    tokenType: ").append(toIndentedString(tokenType)).append("\n");
    sb.append("    expiresIn: ").append(toIndentedString(expiresIn)).append("\n");
    sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
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

