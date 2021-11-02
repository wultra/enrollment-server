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
 * ServiceProviderFactors
 */
@Validated


public class ServiceProviderFactors   {
  @JsonProperty("welcome_message")
  private String welcomeMessage = null;

  @JsonProperty("logo")
  private String logo = null;

  @JsonProperty("push_message")
  private String pushMessage = null;

  @JsonProperty("default_risk_profile")
  private String defaultRiskProfile = null;

  @JsonProperty("client_timeout")
  private Integer clientTimeout = null;

  public ServiceProviderFactors welcomeMessage(String welcomeMessage) {
    this.welcomeMessage = welcomeMessage;
    return this;
  }

  /**
   * The custom welcome message to use
   * @return welcomeMessage
  **/
  @ApiModelProperty(value = "The custom welcome message to use")


  public String getWelcomeMessage() {
    return welcomeMessage;
  }

  public void setWelcomeMessage(String welcomeMessage) {
    this.welcomeMessage = welcomeMessage;
  }

  public ServiceProviderFactors logo(String logo) {
    this.logo = logo;
    return this;
  }

  /**
   * The path to the logo to use (must be an iProov hosted URL via https://secure.iproov.me)
   * @return logo
  **/
  @ApiModelProperty(value = "The path to the logo to use (must be an iProov hosted URL via https://secure.iproov.me)")


  public String getLogo() {
    return logo;
  }

  public void setLogo(String logo) {
    this.logo = logo;
  }

  public ServiceProviderFactors pushMessage(String pushMessage) {
    this.pushMessage = pushMessage;
    return this;
  }

  /**
   * The custom push message to use
   * @return pushMessage
  **/
  @ApiModelProperty(value = "The custom push message to use")


  public String getPushMessage() {
    return pushMessage;
  }

  public void setPushMessage(String pushMessage) {
    this.pushMessage = pushMessage;
  }

  public ServiceProviderFactors defaultRiskProfile(String defaultRiskProfile) {
    this.defaultRiskProfile = defaultRiskProfile;
    return this;
  }

  /**
   * The default risk profile a service provider uses if one isn't specified when setting up a claim
   * @return defaultRiskProfile
  **/
  @ApiModelProperty(value = "The default risk profile a service provider uses if one isn't specified when setting up a claim")


  public String getDefaultRiskProfile() {
    return defaultRiskProfile;
  }

  public void setDefaultRiskProfile(String defaultRiskProfile) {
    this.defaultRiskProfile = defaultRiskProfile;
  }

  public ServiceProviderFactors clientTimeout(Integer clientTimeout) {
    this.clientTimeout = clientTimeout;
    return this;
  }

  /**
   * The service provider specific timeout to use
   * @return clientTimeout
  **/
  @ApiModelProperty(value = "The service provider specific timeout to use")


  public Integer getClientTimeout() {
    return clientTimeout;
  }

  public void setClientTimeout(Integer clientTimeout) {
    this.clientTimeout = clientTimeout;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServiceProviderFactors serviceProviderFactors = (ServiceProviderFactors) o;
    return Objects.equals(this.welcomeMessage, serviceProviderFactors.welcomeMessage) &&
        Objects.equals(this.logo, serviceProviderFactors.logo) &&
        Objects.equals(this.pushMessage, serviceProviderFactors.pushMessage) &&
        Objects.equals(this.defaultRiskProfile, serviceProviderFactors.defaultRiskProfile) &&
        Objects.equals(this.clientTimeout, serviceProviderFactors.clientTimeout);
  }

  @Override
  public int hashCode() {
    return Objects.hash(welcomeMessage, logo, pushMessage, defaultRiskProfile, clientTimeout);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServiceProviderFactors {\n");
    
    sb.append("    welcomeMessage: ").append(toIndentedString(welcomeMessage)).append("\n");
    sb.append("    logo: ").append(toIndentedString(logo)).append("\n");
    sb.append("    pushMessage: ").append(toIndentedString(pushMessage)).append("\n");
    sb.append("    defaultRiskProfile: ").append(toIndentedString(defaultRiskProfile)).append("\n");
    sb.append("    clientTimeout: ").append(toIndentedString(clientTimeout)).append("\n");
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

