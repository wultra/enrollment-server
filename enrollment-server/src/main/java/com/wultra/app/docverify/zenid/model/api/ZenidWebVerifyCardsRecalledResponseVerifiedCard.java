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
 * ZenidWebVerifyCardsRecalledResponseVerifiedCard
 */
@Validated


public class ZenidWebVerifyCardsRecalledResponseVerifiedCard   {
  @JsonProperty("Recalled")
  private Boolean recalled = null;

  /**
   * Gets or Sets documentCode
   */
  public enum DocumentCodeEnum {
    IDC2("IDC2"),
    
    IDC1("IDC1"),
    
    DRV("DRV"),
    
    PAS("PAS"),
    
    SK_IDC_2008PLUS("SK_IDC_2008plus"),
    
    SK_DRV_2004_08_09("SK_DRV_2004_08_09"),
    
    SK_DRV_2013("SK_DRV_2013"),
    
    SK_DRV_2015("SK_DRV_2015"),
    
    SK_PAS_2008_14("SK_PAS_2008_14"),
    
    SK_IDC_1993("SK_IDC_1993"),
    
    SK_DRV_1993("SK_DRV_1993"),
    
    PL_IDC_2015("PL_IDC_2015"),
    
    DE_IDC_2010("DE_IDC_2010"),
    
    DE_IDC_2001("DE_IDC_2001"),
    
    HR_IDC_2013_15("HR_IDC_2013_15"),
    
    AT_IDE_2000("AT_IDE_2000"),
    
    HU_IDC_2000_01_12("HU_IDC_2000_01_12"),
    
    HU_IDC_2016("HU_IDC_2016"),
    
    AT_IDC_2002_05_10("AT_IDC_2002_05_10"),
    
    HU_ADD_2012("HU_ADD_2012"),
    
    AT_PAS_2006_14("AT_PAS_2006_14"),
    
    AT_DRV_2006("AT_DRV_2006"),
    
    AT_DRV_2013("AT_DRV_2013"),
    
    CZ_RES_2011_14("CZ_RES_2011_14"),
    
    CZ_RES_2006_T("CZ_RES_2006_T"),
    
    CZ_RES_2006_07("CZ_RES_2006_07"),
    
    CZ_GUN_2014("CZ_GUN_2014"),
    
    HU_PAS_2006_12("HU_PAS_2006_12"),
    
    HU_DRV_2012_13("HU_DRV_2012_13"),
    
    HU_DRV_2012_B("HU_DRV_2012_B"),
    
    EU_EHIC_2004_A("EU_EHIC_2004_A"),
    
    UNKNOWN("Unknown"),
    
    CZ_GUN_2017("CZ_GUN_2017"),
    
    CZ_RES_2020("CZ_RES_2020"),
    
    PL_IDC_2019("PL_IDC_2019"),
    
    IT_PAS_2006_10("IT_PAS_2006_10"),
    
    INT_ISIC_2008("INT_ISIC_2008"),
    
    DE_PAS("DE_PAS"),
    
    DK_PAS("DK_PAS"),
    
    ES_PAS("ES_PAS"),
    
    FI_PAS("FI_PAS"),
    
    FR_PAS("FR_PAS"),
    
    GB_PAS("GB_PAS"),
    
    IS_PAS("IS_PAS"),
    
    NL_PAS("NL_PAS"),
    
    RO_PAS("RO_PAS"),
    
    SE_PAS("SE_PAS"),
    
    PL_PAS("PL_PAS"),
    
    PL_DRV_2013("PL_DRV_2013"),
    
    CZ_BIRTH("CZ_BIRTH"),
    
    CZ_VEHICLE_I("CZ_VEHICLE_I"),
    
    INT_ISIC_2019("INT_ISIC_2019"),
    
    SI_PAS("SI_PAS"),
    
    SI_IDC("SI_IDC"),
    
    SI_DRV("SI_DRV"),
    
    EU_EHIC_2004_B("EU_EHIC_2004_B"),
    
    PL_IDC_2001_02_13("PL_IDC_2001_02_13"),
    
    IT_IDC_2016("IT_IDC_2016"),
    
    HR_PAS_2009_15("HR_PAS_2009_15"),
    
    HR_DRV_2013("HR_DRV_2013"),
    
    HR_IDC_2003("HR_IDC_2003"),
    
    SI_DRV_2009("SI_DRV_2009"),
    
    BG_PAS_2010("BG_PAS_2010"),
    
    BG_IDC_2010("BG_IDC_2010"),
    
    BG_DRV_2010_13("BG_DRV_2010_13"),
    
    HR_IDC_2021("HR_IDC_2021");

    private String value;

    DocumentCodeEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static DocumentCodeEnum fromValue(String text) {
      for (DocumentCodeEnum b : DocumentCodeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("DocumentCode")
  private DocumentCodeEnum documentCode = null;

  @JsonProperty("CardNumber")
  private String cardNumber = null;

  public ZenidWebVerifyCardsRecalledResponseVerifiedCard recalled(Boolean recalled) {
    this.recalled = recalled;
    return this;
  }

  /**
   * Get recalled
   * @return recalled
  **/
  @ApiModelProperty(value = "")


  public Boolean isRecalled() {
    return recalled;
  }

  public void setRecalled(Boolean recalled) {
    this.recalled = recalled;
  }

  public ZenidWebVerifyCardsRecalledResponseVerifiedCard documentCode(DocumentCodeEnum documentCode) {
    this.documentCode = documentCode;
    return this;
  }

  /**
   * Get documentCode
   * @return documentCode
  **/
  @ApiModelProperty(value = "")


  public DocumentCodeEnum getDocumentCode() {
    return documentCode;
  }

  public void setDocumentCode(DocumentCodeEnum documentCode) {
    this.documentCode = documentCode;
  }

  public ZenidWebVerifyCardsRecalledResponseVerifiedCard cardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
    return this;
  }

  /**
   * Get cardNumber
   * @return cardNumber
  **/
  @ApiModelProperty(value = "")


  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ZenidWebVerifyCardsRecalledResponseVerifiedCard zenidWebVerifyCardsRecalledResponseVerifiedCard = (ZenidWebVerifyCardsRecalledResponseVerifiedCard) o;
    return Objects.equals(this.recalled, zenidWebVerifyCardsRecalledResponseVerifiedCard.recalled) &&
        Objects.equals(this.documentCode, zenidWebVerifyCardsRecalledResponseVerifiedCard.documentCode) &&
        Objects.equals(this.cardNumber, zenidWebVerifyCardsRecalledResponseVerifiedCard.cardNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(recalled, documentCode, cardNumber);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidWebVerifyCardsRecalledResponseVerifiedCard {\n");
    
    sb.append("    recalled: ").append(toIndentedString(recalled)).append("\n");
    sb.append("    documentCode: ").append(toIndentedString(documentCode)).append("\n");
    sb.append("    cardNumber: ").append(toIndentedString(cardNumber)).append("\n");
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

