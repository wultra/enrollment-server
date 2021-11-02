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
 * One of: invalid OAuth token, expired OAuth token, invalid credentials, API Key and/or Secret is  * missing, malformed, or incorrect.
 */
@ApiModel(description = "One of: invalid OAuth token, expired OAuth token, invalid credentials, API Key and/or Secret is  * missing, malformed, or incorrect.")
@Validated


public class InvalidCredentialErrorResponse   {
  /**
   * Gets or Sets error
   */
  public enum ErrorEnum {
    INVALID_KEY("invalid_key"),
    
    INVALID_OAUTH("invalid_oauth"),
    
    TOKEN_QUOTA_EXCEEDED("token_quota_exceeded");

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

  public InvalidCredentialErrorResponse error(ErrorEnum error) {
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

  public InvalidCredentialErrorResponse errorDescription(String errorDescription) {
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
    InvalidCredentialErrorResponse invalidCredentialErrorResponse = (InvalidCredentialErrorResponse) o;
    return Objects.equals(this.error, invalidCredentialErrorResponse.error) &&
        Objects.equals(this.errorDescription, invalidCredentialErrorResponse.errorDescription);
  }

  @Override
  public int hashCode() {
    return Objects.hash(error, errorDescription);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InvalidCredentialErrorResponse {\n");
    
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

