package com.wultra.app.presencecheck.iproov.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.OffsetDateTime;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Contains a description of the user with User ID, Name, Status of the user, suspension and activated date.
 */
@ApiModel(description = "Contains a description of the user with User ID, Name, Status of the user, suspension and activated date.")
@Validated


public class UserResponse   {
  @JsonProperty("user_id")
  private String userId = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("status")
  private String status = null;

  @JsonProperty("suspension_date")
  private OffsetDateTime suspensionDate = null;

  @JsonProperty("activation_date")
  private OffsetDateTime activationDate = null;

  public UserResponse userId(String userId) {
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

  public UserResponse name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
  **/
  @ApiModelProperty(example = "John Doe", required = true, value = "")
  @NotNull


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public UserResponse status(String status) {
    this.status = status;
    return this;
  }

  /**
   * A status of either 'inactive', 'active', 'suspended' or 'deleted'.
   * @return status
  **/
  @ApiModelProperty(example = "Active", required = true, value = "A status of either 'inactive', 'active', 'suspended' or 'deleted'.")
  @NotNull


  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public UserResponse suspensionDate(OffsetDateTime suspensionDate) {
    this.suspensionDate = suspensionDate;
    return this;
  }

  /**
   * Get suspensionDate
   * @return suspensionDate
  **/
  @ApiModelProperty(example = "2016-04-16T16:06:05Z", value = "")

  @Valid

  public OffsetDateTime getSuspensionDate() {
    return suspensionDate;
  }

  public void setSuspensionDate(OffsetDateTime suspensionDate) {
    this.suspensionDate = suspensionDate;
  }

  public UserResponse activationDate(OffsetDateTime activationDate) {
    this.activationDate = activationDate;
    return this;
  }

  /**
   * Get activationDate
   * @return activationDate
  **/
  @ApiModelProperty(example = "2016-05-16T16:06:05Z", value = "")

  @Valid

  public OffsetDateTime getActivationDate() {
    return activationDate;
  }

  public void setActivationDate(OffsetDateTime activationDate) {
    this.activationDate = activationDate;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserResponse userResponse = (UserResponse) o;
    return Objects.equals(this.userId, userResponse.userId) &&
        Objects.equals(this.name, userResponse.name) &&
        Objects.equals(this.status, userResponse.status) &&
        Objects.equals(this.suspensionDate, userResponse.suspensionDate) &&
        Objects.equals(this.activationDate, userResponse.activationDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, name, status, suspensionDate, activationDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserResponse {\n");
    
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    suspensionDate: ").append(toIndentedString(suspensionDate)).append("\n");
    sb.append("    activationDate: ").append(toIndentedString(activationDate)).append("\n");
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

