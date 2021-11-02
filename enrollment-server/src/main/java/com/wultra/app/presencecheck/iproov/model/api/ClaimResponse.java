package com.wultra.app.presencecheck.iproov.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.wultra.app.presencecheck.iproov.model.api.FallbackDefinition;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ClaimResponse
 */
@Validated


public class ClaimResponse   {
  @JsonProperty("fallback")
  @Valid
  private List<FallbackDefinition> fallback = new ArrayList<>();

  @JsonProperty("token")
  private String token = null;

  @JsonProperty("primary")
  private String primary = null;

  @JsonProperty("user_id")
  private String userId = null;

  @JsonProperty("pod")
  private String pod = null;

  @JsonProperty("redirect_domain")
  private String redirectDomain = null;

  @JsonProperty("risk_profile")
  private String riskProfile = null;

  public ClaimResponse fallback(List<FallbackDefinition> fallback) {
    this.fallback = fallback;
    return this;
  }

  public ClaimResponse addFallbackItem(FallbackDefinition fallbackItem) {
    this.fallback.add(fallbackItem);
    return this;
  }

  /**
   * The fallback gives relevant fallback information. It contains a 'type' key and a 'message' key that provides more information about the fallback to be optionally displayed to the user
   * @return fallback
  **/
  @ApiModelProperty(required = true, value = "The fallback gives relevant fallback information. It contains a 'type' key and a 'message' key that provides more information about the fallback to be optionally displayed to the user")
  @NotNull

  @Valid

  public List<FallbackDefinition> getFallback() {
    return fallback;
  }

  public void setFallback(List<FallbackDefinition> fallback) {
    this.fallback = fallback;
  }

  public ClaimResponse token(String token) {
    this.token = token;
    return this;
  }

  /**
   * The token should be referenced if there are issues with an individuals claim and is used as a transaction id
   * @return token
  **/
  @ApiModelProperty(example = "31706131726336496d655177346e55503279616b69547344446e5258684c7542", required = true, value = "The token should be referenced if there are issues with an individuals claim and is used as a transaction id")
  @NotNull


  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public ClaimResponse primary(String primary) {
    this.primary = primary;
    return this;
  }

  /**
   * Get primary
   * @return primary
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getPrimary() {
    return primary;
  }

  public void setPrimary(String primary) {
    this.primary = primary;
  }

  public ClaimResponse userId(String userId) {
    this.userId = userId;
    return this;
  }

  /**
   * The user id of the user associated with the token. Null if no user is associated with the token.
   * @return userId
  **/
  @ApiModelProperty(example = "enquiries@iproov.com", required = true, value = "The user id of the user associated with the token. Null if no user is associated with the token.")
  @NotNull


  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public ClaimResponse pod(String pod) {
    this.pod = pod;
    return this;
  }

  /**
   * The pod that will be used for the claim.
   * @return pod
  **/
  @ApiModelProperty(example = "edge02.eu4", required = true, value = "The pod that will be used for the claim.")
  @NotNull


  public String getPod() {
    return pod;
  }

  public void setPod(String pod) {
    this.pod = pod;
  }

  public ClaimResponse redirectDomain(String redirectDomain) {
    this.redirectDomain = redirectDomain;
    return this;
  }

  /**
   * If the service provider has a dedicated landing page hosted at iProov this will contain the URL to redirect to.
   * @return redirectDomain
  **/
  @ApiModelProperty(value = "If the service provider has a dedicated landing page hosted at iProov this will contain the URL to redirect to.")


  public String getRedirectDomain() {
    return redirectDomain;
  }

  public void setRedirectDomain(String redirectDomain) {
    this.redirectDomain = redirectDomain;
  }

  public ClaimResponse riskProfile(String riskProfile) {
    this.riskProfile = riskProfile;
    return this;
  }

  /**
   * If the service provider has specified a risk profile then it will be used.
   * @return riskProfile
  **/
  @ApiModelProperty(value = "If the service provider has specified a risk profile then it will be used.")


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
    ClaimResponse claimResponse = (ClaimResponse) o;
    return Objects.equals(this.fallback, claimResponse.fallback) &&
        Objects.equals(this.token, claimResponse.token) &&
        Objects.equals(this.primary, claimResponse.primary) &&
        Objects.equals(this.userId, claimResponse.userId) &&
        Objects.equals(this.pod, claimResponse.pod) &&
        Objects.equals(this.redirectDomain, claimResponse.redirectDomain) &&
        Objects.equals(this.riskProfile, claimResponse.riskProfile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fallback, token, primary, userId, pod, redirectDomain, riskProfile);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClaimResponse {\n");
    
    sb.append("    fallback: ").append(toIndentedString(fallback)).append("\n");
    sb.append("    token: ").append(toIndentedString(token)).append("\n");
    sb.append("    primary: ").append(toIndentedString(primary)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    pod: ").append(toIndentedString(pod)).append("\n");
    sb.append("    redirectDomain: ").append(toIndentedString(redirectDomain)).append("\n");
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

