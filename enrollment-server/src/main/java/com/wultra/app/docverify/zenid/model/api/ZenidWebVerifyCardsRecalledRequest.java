package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.wultra.app.docverify.zenid.model.api.ZenidWebVerifyCardsRecalledRequestCardInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ZenidWebVerifyCardsRecalledRequest
 */
@Validated


public class ZenidWebVerifyCardsRecalledRequest   {
  @JsonProperty("CardsToVerify")
  @Valid
  private List<ZenidWebVerifyCardsRecalledRequestCardInfo> cardsToVerify = null;

  public ZenidWebVerifyCardsRecalledRequest cardsToVerify(List<ZenidWebVerifyCardsRecalledRequestCardInfo> cardsToVerify) {
    this.cardsToVerify = cardsToVerify;
    return this;
  }

  public ZenidWebVerifyCardsRecalledRequest addCardsToVerifyItem(ZenidWebVerifyCardsRecalledRequestCardInfo cardsToVerifyItem) {
    if (this.cardsToVerify == null) {
      this.cardsToVerify = new ArrayList<>();
    }
    this.cardsToVerify.add(cardsToVerifyItem);
    return this;
  }

  /**
   * Get cardsToVerify
   * @return cardsToVerify
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ZenidWebVerifyCardsRecalledRequestCardInfo> getCardsToVerify() {
    return cardsToVerify;
  }

  public void setCardsToVerify(List<ZenidWebVerifyCardsRecalledRequestCardInfo> cardsToVerify) {
    this.cardsToVerify = cardsToVerify;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ZenidWebVerifyCardsRecalledRequest zenidWebVerifyCardsRecalledRequest = (ZenidWebVerifyCardsRecalledRequest) o;
    return Objects.equals(this.cardsToVerify, zenidWebVerifyCardsRecalledRequest.cardsToVerify);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cardsToVerify);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidWebVerifyCardsRecalledRequest {\n");
    
    sb.append("    cardsToVerify: ").append(toIndentedString(cardsToVerify)).append("\n");
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

