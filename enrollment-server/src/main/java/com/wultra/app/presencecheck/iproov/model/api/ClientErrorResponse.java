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
 * Missing data in the request, or the state of the requested entity object is such that the operation could not succeed.
 */
@ApiModel(description = "Missing data in the request, or the state of the requested entity object is such that the operation could not succeed.")
@Validated


public class ClientErrorResponse   {
  /**
   * Gets or Sets error
   */
  public enum ErrorEnum {
    INVALID_AGENT("invalid_agent"),
    
    INVALID_USER_ID("invalid_user_id"),
    
    INVALID_TOKEN("invalid_token"),
    
    INVALID_VALIDATION("invalid_validation"),
    
    INVALID_REASON("invalid_reason"),
    
    INVALID_RISK_APPETITE("invalid_risk_appetite"),
    
    INVALID_IP("invalid_ip"),
    
    INVALID_GRANT("invalid_grant"),
    
    INVALID_REQUEST("invalid_request"),
    
    INVALID_CLIENT("invalid_client"),
    
    INVALID_SCOPE("invalid_scope"),
    
    UNSUPPORTED_GRANT_TYPE("unsupported_grant_type"),
    
    MISSING_DATA("missing_data"),
    
    NO_USER("no_user"),
    
    INACTIVE_USER("inactive_user");

    private String value;

    ErrorEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static ErrorEnum fromValue(String text) {
      for (ErrorEnum b : ErrorEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("error")
  private ErrorEnum error = null;

  @JsonProperty("error_description")
  private String errorDescription = null;

  public ClientErrorResponse error(ErrorEnum error) {
    this.error = error;
    return this;
  }

  /**
   * Get error
   * @return error
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public ErrorEnum getError() {
    return error;
  }

  public void setError(ErrorEnum error) {
    this.error = error;
  }

  public ClientErrorResponse errorDescription(String errorDescription) {
    this.errorDescription = errorDescription;
    return this;
  }

  /**
   * Get errorDescription
   * @return errorDescription
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getErrorDescription() {
    return errorDescription;
  }

  public void setErrorDescription(String errorDescription) {
    this.errorDescription = errorDescription;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClientErrorResponse clientErrorResponse = (ClientErrorResponse) o;
    return Objects.equals(this.error, clientErrorResponse.error) &&
        Objects.equals(this.errorDescription, clientErrorResponse.errorDescription);
  }

  @Override
  public int hashCode() {
    return Objects.hash(error, errorDescription);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClientErrorResponse {\n");
    
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    errorDescription: ").append(toIndentedString(errorDescription)).append("\n");
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

