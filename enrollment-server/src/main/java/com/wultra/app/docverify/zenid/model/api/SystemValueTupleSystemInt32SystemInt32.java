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
 * SystemValueTupleSystemInt32SystemInt32
 */
@Validated


public class SystemValueTupleSystemInt32SystemInt32   {
  @JsonProperty("Item1")
  private Integer item1 = null;

  @JsonProperty("Item2")
  private Integer item2 = null;

  public SystemValueTupleSystemInt32SystemInt32 item1(Integer item1) {
    this.item1 = item1;
    return this;
  }

  /**
   * Get item1
   * @return item1
  **/
  @ApiModelProperty(value = "")


  public Integer getItem1() {
    return item1;
  }

  public void setItem1(Integer item1) {
    this.item1 = item1;
  }

  public SystemValueTupleSystemInt32SystemInt32 item2(Integer item2) {
    this.item2 = item2;
    return this;
  }

  /**
   * Get item2
   * @return item2
  **/
  @ApiModelProperty(value = "")


  public Integer getItem2() {
    return item2;
  }

  public void setItem2(Integer item2) {
    this.item2 = item2;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SystemValueTupleSystemInt32SystemInt32 systemValueTupleSystemInt32SystemInt32 = (SystemValueTupleSystemInt32SystemInt32) o;
    return Objects.equals(this.item1, systemValueTupleSystemInt32SystemInt32.item1) &&
        Objects.equals(this.item2, systemValueTupleSystemInt32SystemInt32.item2);
  }

  @Override
  public int hashCode() {
    return Objects.hash(item1, item2);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SystemValueTupleSystemInt32SystemInt32 {\n");
    
    sb.append("    item1: ").append(toIndentedString(item1)).append("\n");
    sb.append("    item2: ").append(toIndentedString(item2)).append("\n");
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

