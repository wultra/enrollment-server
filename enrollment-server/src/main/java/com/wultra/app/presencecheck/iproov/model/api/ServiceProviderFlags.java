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
 * ServiceProviderFlags
 */
@Validated


public class ServiceProviderFlags   {
  @JsonProperty("enable_deepsplice")
  private Boolean enableDeepsplice = null;

  @JsonProperty("enable_deepmorph")
  private Boolean enableDeepmorph = null;

  @JsonProperty("enable_welcome_message")
  private Boolean enableWelcomeMessage = null;

  @JsonProperty("enable_unvalidated_users")
  private Boolean enableUnvalidatedUsers = null;

  @JsonProperty("enable_anomaly")
  private Boolean enableAnomaly = null;

  @JsonProperty("enable_risk_profile")
  private Boolean enableRiskProfile = null;

  @JsonProperty("enable_unique")
  private Boolean enableUnique = null;

  @JsonProperty("enable_image_enrol")
  private Boolean enableImageEnrol = null;

  @JsonProperty("enable_validate_frame")
  private Boolean enableValidateFrame = null;

  @JsonProperty("enable_redirect_domain")
  private Boolean enableRedirectDomain = null;

  @JsonProperty("enable_production_mode")
  private Boolean enableProductionMode = null;

  public ServiceProviderFlags enableDeepsplice(Boolean enableDeepsplice) {
    this.enableDeepsplice = enableDeepsplice;
    return this;
  }

  /**
   * Enable Deep Splice
   * @return enableDeepsplice
  **/
  @ApiModelProperty(example = "false", value = "Enable Deep Splice")


  public Boolean isEnableDeepsplice() {
    return enableDeepsplice;
  }

  public void setEnableDeepsplice(Boolean enableDeepsplice) {
    this.enableDeepsplice = enableDeepsplice;
  }

  public ServiceProviderFlags enableDeepmorph(Boolean enableDeepmorph) {
    this.enableDeepmorph = enableDeepmorph;
    return this;
  }

  /**
   * Enable Deep Morph
   * @return enableDeepmorph
  **/
  @ApiModelProperty(example = "false", value = "Enable Deep Morph")


  public Boolean isEnableDeepmorph() {
    return enableDeepmorph;
  }

  public void setEnableDeepmorph(Boolean enableDeepmorph) {
    this.enableDeepmorph = enableDeepmorph;
  }

  public ServiceProviderFlags enableWelcomeMessage(Boolean enableWelcomeMessage) {
    this.enableWelcomeMessage = enableWelcomeMessage;
    return this;
  }

  /**
   * Remove the welcome message from the API response to be shown to the user
   * @return enableWelcomeMessage
  **/
  @ApiModelProperty(example = "true", value = "Remove the welcome message from the API response to be shown to the user")


  public Boolean isEnableWelcomeMessage() {
    return enableWelcomeMessage;
  }

  public void setEnableWelcomeMessage(Boolean enableWelcomeMessage) {
    this.enableWelcomeMessage = enableWelcomeMessage;
  }

  public ServiceProviderFlags enableUnvalidatedUsers(Boolean enableUnvalidatedUsers) {
    this.enableUnvalidatedUsers = enableUnvalidatedUsers;
    return this;
  }

  /**
   * Allow unvalidated users to verify
   * @return enableUnvalidatedUsers
  **/
  @ApiModelProperty(example = "true", value = "Allow unvalidated users to verify")


  public Boolean isEnableUnvalidatedUsers() {
    return enableUnvalidatedUsers;
  }

  public void setEnableUnvalidatedUsers(Boolean enableUnvalidatedUsers) {
    this.enableUnvalidatedUsers = enableUnvalidatedUsers;
  }

  public ServiceProviderFlags enableAnomaly(Boolean enableAnomaly) {
    this.enableAnomaly = enableAnomaly;
    return this;
  }

  /**
   * Enable the anomaly module
   * @return enableAnomaly
  **/
  @ApiModelProperty(example = "false", value = "Enable the anomaly module")


  public Boolean isEnableAnomaly() {
    return enableAnomaly;
  }

  public void setEnableAnomaly(Boolean enableAnomaly) {
    this.enableAnomaly = enableAnomaly;
  }

  public ServiceProviderFlags enableRiskProfile(Boolean enableRiskProfile) {
    this.enableRiskProfile = enableRiskProfile;
    return this;
  }

  /**
   * Enable Risk Profiles (note risk profiles must be provisioned additionally)
   * @return enableRiskProfile
  **/
  @ApiModelProperty(example = "false", value = "Enable Risk Profiles (note risk profiles must be provisioned additionally)")


  public Boolean isEnableRiskProfile() {
    return enableRiskProfile;
  }

  public void setEnableRiskProfile(Boolean enableRiskProfile) {
    this.enableRiskProfile = enableRiskProfile;
  }

  public ServiceProviderFlags enableUnique(Boolean enableUnique) {
    this.enableUnique = enableUnique;
    return this;
  }

  /**
   * Enable Uniqueness Checking
   * @return enableUnique
  **/
  @ApiModelProperty(example = "false", value = "Enable Uniqueness Checking")


  public Boolean isEnableUnique() {
    return enableUnique;
  }

  public void setEnableUnique(Boolean enableUnique) {
    this.enableUnique = enableUnique;
  }

  public ServiceProviderFlags enableImageEnrol(Boolean enableImageEnrol) {
    this.enableImageEnrol = enableImageEnrol;
    return this;
  }

  /**
   * Enable Photo Enrol
   * @return enableImageEnrol
  **/
  @ApiModelProperty(example = "false", value = "Enable Photo Enrol")


  public Boolean isEnableImageEnrol() {
    return enableImageEnrol;
  }

  public void setEnableImageEnrol(Boolean enableImageEnrol) {
    this.enableImageEnrol = enableImageEnrol;
  }

  public ServiceProviderFlags enableValidateFrame(Boolean enableValidateFrame) {
    this.enableValidateFrame = enableValidateFrame;
    return this;
  }

  /**
   * Enable frame return on validation of a claim
   * @return enableValidateFrame
  **/
  @ApiModelProperty(example = "false", value = "Enable frame return on validation of a claim")


  public Boolean isEnableValidateFrame() {
    return enableValidateFrame;
  }

  public void setEnableValidateFrame(Boolean enableValidateFrame) {
    this.enableValidateFrame = enableValidateFrame;
  }

  public ServiceProviderFlags enableRedirectDomain(Boolean enableRedirectDomain) {
    this.enableRedirectDomain = enableRedirectDomain;
    return this;
  }

  /**
   * Enable supplying redirect URLs
   * @return enableRedirectDomain
  **/
  @ApiModelProperty(example = "false", value = "Enable supplying redirect URLs")


  public Boolean isEnableRedirectDomain() {
    return enableRedirectDomain;
  }

  public void setEnableRedirectDomain(Boolean enableRedirectDomain) {
    this.enableRedirectDomain = enableRedirectDomain;
  }

  public ServiceProviderFlags enableProductionMode(Boolean enableProductionMode) {
    this.enableProductionMode = enableProductionMode;
    return this;
  }

  /**
   * Enable production mode (disables setting client side requests etc.)
   * @return enableProductionMode
  **/
  @ApiModelProperty(example = "false", value = "Enable production mode (disables setting client side requests etc.)")


  public Boolean isEnableProductionMode() {
    return enableProductionMode;
  }

  public void setEnableProductionMode(Boolean enableProductionMode) {
    this.enableProductionMode = enableProductionMode;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServiceProviderFlags serviceProviderFlags = (ServiceProviderFlags) o;
    return Objects.equals(this.enableDeepsplice, serviceProviderFlags.enableDeepsplice) &&
        Objects.equals(this.enableDeepmorph, serviceProviderFlags.enableDeepmorph) &&
        Objects.equals(this.enableWelcomeMessage, serviceProviderFlags.enableWelcomeMessage) &&
        Objects.equals(this.enableUnvalidatedUsers, serviceProviderFlags.enableUnvalidatedUsers) &&
        Objects.equals(this.enableAnomaly, serviceProviderFlags.enableAnomaly) &&
        Objects.equals(this.enableRiskProfile, serviceProviderFlags.enableRiskProfile) &&
        Objects.equals(this.enableUnique, serviceProviderFlags.enableUnique) &&
        Objects.equals(this.enableImageEnrol, serviceProviderFlags.enableImageEnrol) &&
        Objects.equals(this.enableValidateFrame, serviceProviderFlags.enableValidateFrame) &&
        Objects.equals(this.enableRedirectDomain, serviceProviderFlags.enableRedirectDomain) &&
        Objects.equals(this.enableProductionMode, serviceProviderFlags.enableProductionMode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enableDeepsplice, enableDeepmorph, enableWelcomeMessage, enableUnvalidatedUsers, enableAnomaly, enableRiskProfile, enableUnique, enableImageEnrol, enableValidateFrame, enableRedirectDomain, enableProductionMode);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServiceProviderFlags {\n");
    
    sb.append("    enableDeepsplice: ").append(toIndentedString(enableDeepsplice)).append("\n");
    sb.append("    enableDeepmorph: ").append(toIndentedString(enableDeepmorph)).append("\n");
    sb.append("    enableWelcomeMessage: ").append(toIndentedString(enableWelcomeMessage)).append("\n");
    sb.append("    enableUnvalidatedUsers: ").append(toIndentedString(enableUnvalidatedUsers)).append("\n");
    sb.append("    enableAnomaly: ").append(toIndentedString(enableAnomaly)).append("\n");
    sb.append("    enableRiskProfile: ").append(toIndentedString(enableRiskProfile)).append("\n");
    sb.append("    enableUnique: ").append(toIndentedString(enableUnique)).append("\n");
    sb.append("    enableImageEnrol: ").append(toIndentedString(enableImageEnrol)).append("\n");
    sb.append("    enableValidateFrame: ").append(toIndentedString(enableValidateFrame)).append("\n");
    sb.append("    enableRedirectDomain: ").append(toIndentedString(enableRedirectDomain)).append("\n");
    sb.append("    enableProductionMode: ").append(toIndentedString(enableProductionMode)).append("\n");
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

