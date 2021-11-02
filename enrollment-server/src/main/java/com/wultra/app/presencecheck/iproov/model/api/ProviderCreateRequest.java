package com.wultra.app.presencecheck.iproov.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.wultra.app.presencecheck.iproov.model.api.ServiceProviderFactors;
import com.wultra.app.presencecheck.iproov.model.api.ServiceProviderFlags;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ProviderCreateRequest
 */
@Validated


public class ProviderCreateRequest   {
  @JsonProperty("friendly_name")
  private String friendlyName = null;

  @JsonProperty("internal_name")
  private String internalName = null;

  /**
   * Gets or Sets mode
   */
  public enum ModeEnum {
    VERIFY("verify"),
    
    ENROL("enrol"),
    
    DEVICE("device");

    private String value;

    ModeEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static ModeEnum fromValue(String text) {
      for (ModeEnum b : ModeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("mode")
  @Valid
  private List<ModeEnum> mode = new ArrayList<>();

  @JsonProperty("flags")
  @Valid
  private List<ServiceProviderFlags> flags = null;

  @JsonProperty("factors")
  @Valid
  private List<ServiceProviderFactors> factors = null;

  public ProviderCreateRequest friendlyName(String friendlyName) {
    this.friendlyName = friendlyName;
    return this;
  }

  /**
   * The friendly name of the service provider
   * @return friendlyName
  **/
  @ApiModelProperty(required = true, value = "The friendly name of the service provider")
  @NotNull


  public String getFriendlyName() {
    return friendlyName;
  }

  public void setFriendlyName(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public ProviderCreateRequest internalName(String internalName) {
    this.internalName = internalName;
    return this;
  }

  /**
   * The internal name of the service provider
   * @return internalName
  **/
  @ApiModelProperty(value = "The internal name of the service provider")


  public String getInternalName() {
    return internalName;
  }

  public void setInternalName(String internalName) {
    this.internalName = internalName;
  }

  public ProviderCreateRequest mode(List<ModeEnum> mode) {
    this.mode = mode;
    return this;
  }

  public ProviderCreateRequest addModeItem(ModeEnum modeItem) {
    this.mode.add(modeItem);
    return this;
  }

  /**
   * The mode the service provider accepts
   * @return mode
  **/
  @ApiModelProperty(required = true, value = "The mode the service provider accepts")
  @NotNull


  public List<ModeEnum> getMode() {
    return mode;
  }

  public void setMode(List<ModeEnum> mode) {
    this.mode = mode;
  }

  public ProviderCreateRequest flags(List<ServiceProviderFlags> flags) {
    this.flags = flags;
    return this;
  }

  public ProviderCreateRequest addFlagsItem(ServiceProviderFlags flagsItem) {
    if (this.flags == null) {
      this.flags = new ArrayList<>();
    }
    this.flags.add(flagsItem);
    return this;
  }

  /**
   * The flags for the service provider
   * @return flags
  **/
  @ApiModelProperty(value = "The flags for the service provider")

  @Valid

  public List<ServiceProviderFlags> getFlags() {
    return flags;
  }

  public void setFlags(List<ServiceProviderFlags> flags) {
    this.flags = flags;
  }

  public ProviderCreateRequest factors(List<ServiceProviderFactors> factors) {
    this.factors = factors;
    return this;
  }

  public ProviderCreateRequest addFactorsItem(ServiceProviderFactors factorsItem) {
    if (this.factors == null) {
      this.factors = new ArrayList<>();
    }
    this.factors.add(factorsItem);
    return this;
  }

  /**
   * The factors for the service provider
   * @return factors
  **/
  @ApiModelProperty(value = "The factors for the service provider")

  @Valid

  public List<ServiceProviderFactors> getFactors() {
    return factors;
  }

  public void setFactors(List<ServiceProviderFactors> factors) {
    this.factors = factors;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProviderCreateRequest providerCreateRequest = (ProviderCreateRequest) o;
    return Objects.equals(this.friendlyName, providerCreateRequest.friendlyName) &&
        Objects.equals(this.internalName, providerCreateRequest.internalName) &&
        Objects.equals(this.mode, providerCreateRequest.mode) &&
        Objects.equals(this.flags, providerCreateRequest.flags) &&
        Objects.equals(this.factors, providerCreateRequest.factors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(friendlyName, internalName, mode, flags, factors);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProviderCreateRequest {\n");
    
    sb.append("    friendlyName: ").append(toIndentedString(friendlyName)).append("\n");
    sb.append("    internalName: ").append(toIndentedString(internalName)).append("\n");
    sb.append("    mode: ").append(toIndentedString(mode)).append("\n");
    sb.append("    flags: ").append(toIndentedString(flags)).append("\n");
    sb.append("    factors: ").append(toIndentedString(factors)).append("\n");
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

