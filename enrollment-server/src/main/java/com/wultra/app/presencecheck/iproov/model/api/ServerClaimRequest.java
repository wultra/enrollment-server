package com.wultra.app.presencecheck.iproov.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ServerClaimRequest
 */
@Validated


public class ServerClaimRequest   {
  @JsonProperty("api_key")
  private String apiKey = null;

  @JsonProperty("secret")
  private String secret = null;

  @JsonProperty("resource")
  private String resource = null;

  /**
   * The assurance type of the claim
   */
  public enum AssuranceTypeEnum {
    GENUINE_PRESENCE("genuine_presence"),
    
    LIVENESS("liveness");

    private String value;

    AssuranceTypeEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static AssuranceTypeEnum fromValue(String text) {
      for (AssuranceTypeEnum b : AssuranceTypeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("assurance_type")
  private AssuranceTypeEnum assuranceType = AssuranceTypeEnum.GENUINE_PRESENCE;

  @JsonProperty("success_url")
  private String successUrl = null;

  @JsonProperty("failure_url")
  private String failureUrl = null;

  @JsonProperty("abort_url")
  private String abortUrl = null;

  @JsonProperty("user_id")
  private String userId = null;

  @JsonProperty("risk_profile")
  private String riskProfile = null;

  public ServerClaimRequest apiKey(String apiKey) {
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

  public ServerClaimRequest secret(String secret) {
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

  public ServerClaimRequest resource(String resource) {
    this.resource = resource;
    return this;
  }

  /**
   * The resource being accessed (e.g. URL)
   * @return resource
  **/
  @ApiModelProperty(required = true, value = "The resource being accessed (e.g. URL)")
  @NotNull


  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public ServerClaimRequest assuranceType(AssuranceTypeEnum assuranceType) {
    this.assuranceType = assuranceType;
    return this;
  }

  /**
   * The assurance type of the claim
   * @return assuranceType
  **/
  @ApiModelProperty(value = "The assurance type of the claim")


  public AssuranceTypeEnum getAssuranceType() {
    return assuranceType;
  }

  public void setAssuranceType(AssuranceTypeEnum assuranceType) {
    this.assuranceType = assuranceType;
  }

  public ServerClaimRequest successUrl(String successUrl) {
    this.successUrl = successUrl;
    return this;
  }

  /**
   * The URL to redirect to on success. Note: This field has been deprecated and will be omitted in the next release.
   * @return successUrl
  **/
  @ApiModelProperty(value = "The URL to redirect to on success. Note: This field has been deprecated and will be omitted in the next release.")


  public String getSuccessUrl() {
    return successUrl;
  }

  public void setSuccessUrl(String successUrl) {
    this.successUrl = successUrl;
  }

  public ServerClaimRequest failureUrl(String failureUrl) {
    this.failureUrl = failureUrl;
    return this;
  }

  /**
   * The URL to redirect to on failure. Note: This field has been deprecated and will be omitted in the next release.
   * @return failureUrl
  **/
  @ApiModelProperty(value = "The URL to redirect to on failure. Note: This field has been deprecated and will be omitted in the next release.")


  public String getFailureUrl() {
    return failureUrl;
  }

  public void setFailureUrl(String failureUrl) {
    this.failureUrl = failureUrl;
  }

  public ServerClaimRequest abortUrl(String abortUrl) {
    this.abortUrl = abortUrl;
    return this;
  }

  /**
   * The URL to redirect to on user abort. Note: This field has been deprecated and will be omitted in the next release.
   * @return abortUrl
  **/
  @ApiModelProperty(value = "The URL to redirect to on user abort. Note: This field has been deprecated and will be omitted in the next release.")


  public String getAbortUrl() {
    return abortUrl;
  }

  public void setAbortUrl(String abortUrl) {
    this.abortUrl = abortUrl;
  }

  public ServerClaimRequest userId(String userId) {
    this.userId = userId;
    return this;
  }

  /**
   * The asserted identifier of the user.
   * @return userId
  **/
  @ApiModelProperty(example = "enquiries@iproov.com", value = "The asserted identifier of the user.")


  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public ServerClaimRequest riskProfile(String riskProfile) {
    this.riskProfile = riskProfile;
    return this;
  }

  /**
   * The pre-defined risk profile to use for this claim.
   * @return riskProfile
  **/
  @ApiModelProperty(value = "The pre-defined risk profile to use for this claim.")


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
    ServerClaimRequest serverClaimRequest = (ServerClaimRequest) o;
    return Objects.equals(this.apiKey, serverClaimRequest.apiKey) &&
        Objects.equals(this.secret, serverClaimRequest.secret) &&
        Objects.equals(this.resource, serverClaimRequest.resource) &&
        Objects.equals(this.assuranceType, serverClaimRequest.assuranceType) &&
        Objects.equals(this.successUrl, serverClaimRequest.successUrl) &&
        Objects.equals(this.failureUrl, serverClaimRequest.failureUrl) &&
        Objects.equals(this.abortUrl, serverClaimRequest.abortUrl) &&
        Objects.equals(this.userId, serverClaimRequest.userId) &&
        Objects.equals(this.riskProfile, serverClaimRequest.riskProfile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiKey, secret, resource, assuranceType, successUrl, failureUrl, abortUrl, userId, riskProfile);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServerClaimRequest {\n");
    
    sb.append("    apiKey: ").append(toIndentedString(apiKey)).append("\n");
    sb.append("    secret: ").append(toIndentedString(secret)).append("\n");
    sb.append("    resource: ").append(toIndentedString(resource)).append("\n");
    sb.append("    assuranceType: ").append(toIndentedString(assuranceType)).append("\n");
    sb.append("    successUrl: ").append(toIndentedString(successUrl)).append("\n");
    sb.append("    failureUrl: ").append(toIndentedString(failureUrl)).append("\n");
    sb.append("    abortUrl: ").append(toIndentedString(abortUrl)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
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

