package com.wultra.app.docverify.zenid.model.api;

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
 * Short description of investigation (its ID, State and CUstomData)
 */
@ApiModel(description = "Short description of investigation (its ID, State and CUstomData)")
@Validated


public class ZenidWebListInvestigationsResponseInvestigateItem   {
  @JsonProperty("InvestigationID")
  private Integer investigationID = null;

  @JsonProperty("CustomData")
  private String customData = null;

  /**
   * State of the investigation
   */
  public enum StateEnum {
    NOTDONE("NotDone"),
    
    DONE("Done"),
    
    ERROR("Error"),
    
    OPERATOR("Operator"),
    
    REJECTED("Rejected");

    private String value;

    StateEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StateEnum fromValue(String text) {
      for (StateEnum b : StateEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("State")
  private StateEnum state = null;

  public ZenidWebListInvestigationsResponseInvestigateItem investigationID(Integer investigationID) {
    this.investigationID = investigationID;
    return this;
  }

  /**
   * DB ID of investigation
   * @return investigationID
  **/
  @ApiModelProperty(value = "DB ID of investigation")


  public Integer getInvestigationID() {
    return investigationID;
  }

  public void setInvestigationID(Integer investigationID) {
    this.investigationID = investigationID;
  }

  public ZenidWebListInvestigationsResponseInvestigateItem customData(String customData) {
    this.customData = customData;
    return this;
  }

  /**
   * CustomData attribute (copied from Request)
   * @return customData
  **/
  @ApiModelProperty(value = "CustomData attribute (copied from Request)")


  public String getCustomData() {
    return customData;
  }

  public void setCustomData(String customData) {
    this.customData = customData;
  }

  public ZenidWebListInvestigationsResponseInvestigateItem state(StateEnum state) {
    this.state = state;
    return this;
  }

  /**
   * State of the investigation
   * @return state
  **/
  @ApiModelProperty(value = "State of the investigation")


  public StateEnum getState() {
    return state;
  }

  public void setState(StateEnum state) {
    this.state = state;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ZenidWebListInvestigationsResponseInvestigateItem zenidWebListInvestigationsResponseInvestigateItem = (ZenidWebListInvestigationsResponseInvestigateItem) o;
    return Objects.equals(this.investigationID, zenidWebListInvestigationsResponseInvestigateItem.investigationID) &&
        Objects.equals(this.customData, zenidWebListInvestigationsResponseInvestigateItem.customData) &&
        Objects.equals(this.state, zenidWebListInvestigationsResponseInvestigateItem.state);
  }

  @Override
  public int hashCode() {
    return Objects.hash(investigationID, customData, state);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidWebListInvestigationsResponseInvestigateItem {\n");
    
    sb.append("    investigationID: ").append(toIndentedString(investigationID)).append("\n");
    sb.append("    customData: ").append(toIndentedString(customData)).append("\n");
    sb.append("    state: ").append(toIndentedString(state)).append("\n");
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

