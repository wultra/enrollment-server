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
 * AvailabilityResponse
 */
@Validated


public class AvailabilityResponse   {
  @JsonProperty("up")
  private Boolean up = null;

  public AvailabilityResponse up(Boolean up) {
    this.up = up;
    return this;
  }

  /**
   * Whether iProov service is available or not.
   * @return up
  **/
  @ApiModelProperty(example = "true", required = true, value = "Whether iProov service is available or not.")
  @NotNull


  public Boolean isUp() {
    return up;
  }

  public void setUp(Boolean up) {
    this.up = up;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AvailabilityResponse availabilityResponse = (AvailabilityResponse) o;
    return Objects.equals(this.up, availabilityResponse.up);
  }

  @Override
  public int hashCode() {
    return Objects.hash(up);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AvailabilityResponse {\n");
    
    sb.append("    up: ").append(toIndentedString(up)).append("\n");
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

