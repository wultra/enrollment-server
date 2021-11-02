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
 * ClaimValidateRequest
 */
@Validated


public class ClaimValidateRequest   {
  @JsonProperty("api_key")
  private String apiKey = null;

  @JsonProperty("secret")
  private String secret = null;

  @JsonProperty("user_id")
  private String userId = null;

  @JsonProperty("token")
  private String token = null;

  @JsonProperty("ip")
  private String ip = null;

  @JsonProperty("client")
  private String client = null;

  @JsonProperty("risk_profile")
  private String riskProfile = null;

  public ClaimValidateRequest apiKey(String apiKey) {
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

  public ClaimValidateRequest secret(String secret) {
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

  public ClaimValidateRequest userId(String userId) {
    this.userId = userId;
    return this;
  }

  /**
   * The asserted identifier of the user
   * @return userId
  **/
  @ApiModelProperty(example = "enquiries@iproov.com", required = true, value = "The asserted identifier of the user")
  @NotNull


  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public ClaimValidateRequest token(String token) {
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

  public ClaimValidateRequest ip(String ip) {
    this.ip = ip;
    return this;
  }

  /**
   * IP address of the device making this request. Note: This field has been deprecated and will be omitted in the next release.
   * @return ip
  **/
  @ApiModelProperty(required = true, value = "IP address of the device making this request. Note: This field has been deprecated and will be omitted in the next release.")
  @NotNull


  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public ClaimValidateRequest client(String client) {
    this.client = client;
    return this;
  }

  /**
   * Fingerprint or client identifier of the device making the request (e.g. User Agent)
   * @return client
  **/
  @ApiModelProperty(required = true, value = "Fingerprint or client identifier of the device making the request (e.g. User Agent)")
  @NotNull


  public String getClient() {
    return client;
  }

  public void setClient(String client) {
    this.client = client;
  }

  public ClaimValidateRequest riskProfile(String riskProfile) {
    this.riskProfile = riskProfile;
    return this;
  }

  /**
   * The pre-defined risk profile to use for this claim
   * @return riskProfile
  **/
  @ApiModelProperty(value = "The pre-defined risk profile to use for this claim")


  public String getRiskProfile() {
    return riskProfile;
  }

  public void setRiskProfile(String riskProfile) {
    this.riskProfile = riskProfile;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClaimValidateRequest claimValidateRequest = (ClaimValidateRequest) o;
    return Objects.equals(this.apiKey, claimValidateRequest.apiKey) &&
        Objects.equals(this.secret, claimValidateRequest.secret) &&
        Objects.equals(this.userId, claimValidateRequest.userId) &&
        Objects.equals(this.token, claimValidateRequest.token) &&
        Objects.equals(this.ip, claimValidateRequest.ip) &&
        Objects.equals(this.client, claimValidateRequest.client) &&
        Objects.equals(this.riskProfile, claimValidateRequest.riskProfile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiKey, secret, userId, token, ip, client, riskProfile);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClaimValidateRequest {\n");
    
    sb.append("    apiKey: ").append(toIndentedString(apiKey)).append("\n");
    sb.append("    secret: ").append(toIndentedString(secret)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    token: ").append(toIndentedString(token)).append("\n");
    sb.append("    ip: ").append(toIndentedString(ip)).append("\n");
    sb.append("    client: ").append(toIndentedString(client)).append("\n");
    sb.append("    riskProfile: ").append(toIndentedString(riskProfile)).append("\n");
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

