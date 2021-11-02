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
 * FallbackDefinition
 */
@Validated


public class FallbackDefinition   {
  @JsonProperty("type")
  private String type = null;

  @JsonProperty("message")
  private String message = null;

  public FallbackDefinition type(String type) {
    this.type = type;
    return this;
  }

  /**
   * The type of the fallback to be used
   * @return type
  **/
  @ApiModelProperty(example = "Info", required = true, value = "The type of the fallback to be used")
  @NotNull


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public FallbackDefinition message(String message) {
    this.message = message;
    return this;
  }

  /**
   * The message to show the user about the fallback
   * @return message
  **/
  @ApiModelProperty(example = "Sorry, only iProov is available", required = true, value = "The message to show the user about the fallback")
  @NotNull


  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FallbackDefinition fallbackDefinition = (FallbackDefinition) o;
    return Objects.equals(this.type, fallbackDefinition.type) &&
        Objects.equals(this.message, fallbackDefinition.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, message);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FallbackDefinition {\n");
    
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
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

