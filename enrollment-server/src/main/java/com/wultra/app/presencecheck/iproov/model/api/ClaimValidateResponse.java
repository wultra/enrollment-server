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
 * ClaimValidateResponse
 */
@Validated


public class ClaimValidateResponse   {
  @JsonProperty("passed")
  private Boolean passed = null;

  @JsonProperty("token")
  private String token = null;

  @JsonProperty("type")
  private String type = null;

  @JsonProperty("frame_available")
  private String frameAvailable = null;

  @JsonProperty("frame")
  private String frame = null;

  @JsonProperty("frame_jpeg")
  private String frameJpeg = null;

  @JsonProperty("iso_19794_5")
  private String iso197945 = null;

  @JsonProperty("reason")
  private String reason = null;

  @JsonProperty("risk_profile")
  private String riskProfile = null;

  /**
   * Which assurance type was utilized by the transaction
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

  public ClaimValidateResponse passed(Boolean passed) {
    this.passed = passed;
    return this;
  }

  /**
   * Get passed
   * @return passed
  **/
  @ApiModelProperty(example = "true", required = true, value = "")
  @NotNull


  public Boolean isPassed() {
    return passed;
  }

  public void setPassed(Boolean passed) {
    this.passed = passed;
  }

  public ClaimValidateResponse token(String token) {
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

  public ClaimValidateResponse type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
  **/
  @ApiModelProperty(example = "verify", required = true, value = "")
  @NotNull


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public ClaimValidateResponse frameAvailable(String frameAvailable) {
    this.frameAvailable = frameAvailable;
    return this;
  }

  /**
   * Present and True if there is frame available for returning to the integrator.  Enabled on a per service provider basis. Contact support@iproov.com to request this functionality.
   * @return frameAvailable
  **/
  @ApiModelProperty(example = "false", value = "Present and True if there is frame available for returning to the integrator.  Enabled on a per service provider basis. Contact support@iproov.com to request this functionality.")


  public String getFrameAvailable() {
    return frameAvailable;
  }

  public void setFrameAvailable(String frameAvailable) {
    this.frameAvailable = frameAvailable;
  }

  public ClaimValidateResponse frame(String frame) {
    this.frame = frame;
    return this;
  }

  /**
   * If `frame_available` is present and True, a base64 encoded representation of the frame.
   * @return frame
  **/
  @ApiModelProperty(value = "If `frame_available` is present and True, a base64 encoded representation of the frame.")


  public String getFrame() {
    return frame;
  }

  public void setFrame(String frame) {
    this.frame = frame;
  }

  public ClaimValidateResponse frameJpeg(String frameJpeg) {
    this.frameJpeg = frameJpeg;
    return this;
  }

  /**
   * a base64 encoded representation of the frame in JPEG format.
   * @return frameJpeg
  **/
  @ApiModelProperty(value = "a base64 encoded representation of the frame in JPEG format.")


  public String getFrameJpeg() {
    return frameJpeg;
  }

  public void setFrameJpeg(String frameJpeg) {
    this.frameJpeg = frameJpeg;
  }

  public ClaimValidateResponse iso197945(String iso197945) {
    this.iso197945 = iso197945;
    return this;
  }

  /**
   * If `frame_available` is present and True, a base64 encoded string that contains an ISO 19794_5 compliant image.
   * @return iso197945
  **/
  @ApiModelProperty(value = "If `frame_available` is present and True, a base64 encoded string that contains an ISO 19794_5 compliant image.")


  public String getIso197945() {
    return iso197945;
  }

  public void setIso197945(String iso197945) {
    this.iso197945 = iso197945;
  }

  public ClaimValidateResponse reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * The failure reason (enabled on a per service provider basis)
   * @return reason
  **/
  @ApiModelProperty(example = "Please Keep Still", value = "The failure reason (enabled on a per service provider basis)")


  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public ClaimValidateResponse riskProfile(String riskProfile) {
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

  public ClaimValidateResponse assuranceType(AssuranceTypeEnum assuranceType) {
    this.assuranceType = assuranceType;
    return this;
  }

  /**
   * Which assurance type was utilized by the transaction
   * @return assuranceType
  **/
  @ApiModelProperty(value = "Which assurance type was utilized by the transaction")


  public AssuranceTypeEnum getAssuranceType() {
    return assuranceType;
  }

  public void setAssuranceType(AssuranceTypeEnum assuranceType) {
    this.assuranceType = assuranceType;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClaimValidateResponse claimValidateResponse = (ClaimValidateResponse) o;
    return Objects.equals(this.passed, claimValidateResponse.passed) &&
        Objects.equals(this.token, claimValidateResponse.token) &&
        Objects.equals(this.type, claimValidateResponse.type) &&
        Objects.equals(this.frameAvailable, claimValidateResponse.frameAvailable) &&
        Objects.equals(this.frame, claimValidateResponse.frame) &&
        Objects.equals(this.frameJpeg, claimValidateResponse.frameJpeg) &&
        Objects.equals(this.iso197945, claimValidateResponse.iso197945) &&
        Objects.equals(this.reason, claimValidateResponse.reason) &&
        Objects.equals(this.riskProfile, claimValidateResponse.riskProfile) &&
        Objects.equals(this.assuranceType, claimValidateResponse.assuranceType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(passed, token, type, frameAvailable, frame, frameJpeg, iso197945, reason, riskProfile, assuranceType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClaimValidateResponse {\n");
    
    sb.append("    passed: ").append(toIndentedString(passed)).append("\n");
    sb.append("    token: ").append(toIndentedString(token)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    frameAvailable: ").append(toIndentedString(frameAvailable)).append("\n");
    sb.append("    frame: ").append(toIndentedString(frame)).append("\n");
    sb.append("    frameJpeg: ").append(toIndentedString(frameJpeg)).append("\n");
    sb.append("    iso197945: ").append(toIndentedString(iso197945)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    riskProfile: ").append(toIndentedString(riskProfile)).append("\n");
    sb.append("    assuranceType: ").append(toIndentedString(assuranceType)).append("\n");
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

