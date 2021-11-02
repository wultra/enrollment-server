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
 * Server Error
 */
@ApiModel(description = "Server Error")
@Validated


public class ServerErrorResponse   {
  /**
   * Gets or Sets error
   */
  public enum ErrorEnum {
    SERVER_ERROR("server_error");

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

  public ServerErrorResponse error(ErrorEnum error) {
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

  public ServerErrorResponse errorDescription(String errorDescription) {
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
    ServerErrorResponse serverErrorResponse = (ServerErrorResponse) o;
    return Objects.equals(this.error, serverErrorResponse.error) &&
        Objects.equals(this.errorDescription, serverErrorResponse.errorDescription);
  }

  @Override
  public int hashCode() {
    return Objects.hash(error, errorDescription);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServerErrorResponse {\n");
    
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

