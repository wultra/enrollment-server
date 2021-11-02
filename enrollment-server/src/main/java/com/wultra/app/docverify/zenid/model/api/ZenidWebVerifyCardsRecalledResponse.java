package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.wultra.app.docverify.zenid.model.api.ZenidWebVerifyCardsRecalledResponseVerifiedCard;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ZenidWebVerifyCardsRecalledResponse
 */
@Validated


public class ZenidWebVerifyCardsRecalledResponse   {
  @JsonProperty("VerifiedCards")
  @Valid
  private List<ZenidWebVerifyCardsRecalledResponseVerifiedCard> verifiedCards = null;

  /**
   * If throght processing some error occurs, ErrorCode property is set.
   */
  public enum ErrorCodeEnum {
    UNKNOWNSAMPLEID("UnknownSampleID"),
    
    UNKNOWNUPLOADSESSIONID("UnknownUploadSessionID"),
    
    EMPTYBODY("EmptyBody"),
    
    INTERNALSERVERERROR("InternalServerError"),
    
    INVALIDTIMESTAMP("InvalidTimeStamp"),
    
    SAMPLEININVALIDSTATE("SampleInInvalidState"),
    
    INVALIDSAMPLECOMBINATION("InvalidSampleCombination"),
    
    ACCESSDENIED("AccessDenied"),
    
    UNKNOWNPERSON("UnknownPerson"),
    
    INVALIDINPUTDATA("InvalidInputData");

    private String value;

    ErrorCodeEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static ErrorCodeEnum fromValue(String text) {
      for (ErrorCodeEnum b : ErrorCodeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("ErrorCode")
  private ErrorCodeEnum errorCode = null;

  @JsonProperty("ErrorText")
  private String errorText = null;

  @JsonProperty("MessageType")
  private String messageType = null;

  public ZenidWebVerifyCardsRecalledResponse verifiedCards(List<ZenidWebVerifyCardsRecalledResponseVerifiedCard> verifiedCards) {
    this.verifiedCards = verifiedCards;
    return this;
  }

  public ZenidWebVerifyCardsRecalledResponse addVerifiedCardsItem(ZenidWebVerifyCardsRecalledResponseVerifiedCard verifiedCardsItem) {
    if (this.verifiedCards == null) {
      this.verifiedCards = new ArrayList<>();
    }
    this.verifiedCards.add(verifiedCardsItem);
    return this;
  }

  /**
   * Get verifiedCards
   * @return verifiedCards
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ZenidWebVerifyCardsRecalledResponseVerifiedCard> getVerifiedCards() {
    return verifiedCards;
  }

  public void setVerifiedCards(List<ZenidWebVerifyCardsRecalledResponseVerifiedCard> verifiedCards) {
    this.verifiedCards = verifiedCards;
  }

  public ZenidWebVerifyCardsRecalledResponse errorCode(ErrorCodeEnum errorCode) {
    this.errorCode = errorCode;
    return this;
  }

  /**
   * If throght processing some error occurs, ErrorCode property is set.
   * @return errorCode
  **/
  @ApiModelProperty(value = "If throght processing some error occurs, ErrorCode property is set.")


  public ErrorCodeEnum getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(ErrorCodeEnum errorCode) {
    this.errorCode = errorCode;
  }

  public ZenidWebVerifyCardsRecalledResponse errorText(String errorText) {
    this.errorText = errorText;
    return this;
  }

  /**
   * Error text
   * @return errorText
  **/
  @ApiModelProperty(value = "Error text")


  public String getErrorText() {
    return errorText;
  }

  public void setErrorText(String errorText) {
    this.errorText = errorText;
  }

  public ZenidWebVerifyCardsRecalledResponse messageType(String messageType) {
    this.messageType = messageType;
    return this;
  }

  /**
   * Get messageType
   * @return messageType
  **/
  @ApiModelProperty(readOnly = true, value = "")


  public String getMessageType() {
    return messageType;
  }

  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ZenidWebVerifyCardsRecalledResponse zenidWebVerifyCardsRecalledResponse = (ZenidWebVerifyCardsRecalledResponse) o;
    return Objects.equals(this.verifiedCards, zenidWebVerifyCardsRecalledResponse.verifiedCards) &&
        Objects.equals(this.errorCode, zenidWebVerifyCardsRecalledResponse.errorCode) &&
        Objects.equals(this.errorText, zenidWebVerifyCardsRecalledResponse.errorText) &&
        Objects.equals(this.messageType, zenidWebVerifyCardsRecalledResponse.messageType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(verifiedCards, errorCode, errorText, messageType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidWebVerifyCardsRecalledResponse {\n");
    
    sb.append("    verifiedCards: ").append(toIndentedString(verifiedCards)).append("\n");
    sb.append("    errorCode: ").append(toIndentedString(errorCode)).append("\n");
    sb.append("    errorText: ").append(toIndentedString(errorText)).append("\n");
    sb.append("    messageType: ").append(toIndentedString(messageType)).append("\n");
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

