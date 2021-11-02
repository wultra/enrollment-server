package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ZenidWebControllersSelfCheckSelfCheckItem
 */
@Validated


public class ZenidWebControllersSelfCheckSelfCheckItem   {
  @JsonProperty("Name")
  private String name = null;

  @JsonProperty("Status")
  private Boolean status = null;

  @JsonProperty("Comment")
  private String comment = null;

  public ZenidWebControllersSelfCheckSelfCheckItem name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
  **/
  @ApiModelProperty(value = "")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ZenidWebControllersSelfCheckSelfCheckItem status(Boolean status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
  **/
  @ApiModelProperty(value = "")


  public Boolean isStatus() {
    return status;
  }

  public void setStatus(Boolean status) {
    this.status = status;
  }

  public ZenidWebControllersSelfCheckSelfCheckItem comment(String comment) {
    this.comment = comment;
    return this;
  }

  /**
   * Get comment
   * @return comment
  **/
  @ApiModelProperty(value = "")


  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ZenidWebControllersSelfCheckSelfCheckItem zenidWebControllersSelfCheckSelfCheckItem = (ZenidWebControllersSelfCheckSelfCheckItem) o;
    return Objects.equals(this.name, zenidWebControllersSelfCheckSelfCheckItem.name) &&
        Objects.equals(this.status, zenidWebControllersSelfCheckSelfCheckItem.status) &&
        Objects.equals(this.comment, zenidWebControllersSelfCheckSelfCheckItem.comment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, status, comment);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidWebControllersSelfCheckSelfCheckItem {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    comment: ").append(toIndentedString(comment)).append("\n");
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

