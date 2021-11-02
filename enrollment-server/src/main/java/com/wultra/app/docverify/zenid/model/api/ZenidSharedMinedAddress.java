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
 * ZenidSharedMinedAddress
 */
@Validated


public class ZenidSharedMinedAddress   {
  @JsonProperty("ID")
  private String ID = null;

  @JsonProperty("A1")
  private String a1 = null;

  @JsonProperty("A2")
  private String a2 = null;

  @JsonProperty("A3")
  private String a3 = null;

  @JsonProperty("AdministrativeAreaLevel1")
  private String administrativeAreaLevel1 = null;

  @JsonProperty("AdministrativeAreaLevel2")
  private String administrativeAreaLevel2 = null;

  @JsonProperty("Locality")
  private String locality = null;

  @JsonProperty("Sublocality")
  private String sublocality = null;

  @JsonProperty("Suburb")
  private String suburb = null;

  @JsonProperty("Street")
  private String street = null;

  @JsonProperty("HouseNumber")
  private String houseNumber = null;

  @JsonProperty("StreetNumber")
  private String streetNumber = null;

  @JsonProperty("PostalCode")
  private String postalCode = null;

  @JsonProperty("GoogleSearchable")
  private String googleSearchable = null;

  @JsonProperty("Text")
  private String text = null;

  @JsonProperty("Confidence")
  private Integer confidence = null;

  public ZenidSharedMinedAddress ID(String ID) {
    this.ID = ID;
    return this;
  }

  /**
   * Get ID
   * @return ID
  **/
  @ApiModelProperty(value = "")


  public String getID() {
    return ID;
  }

  public void setID(String ID) {
    this.ID = ID;
  }

  public ZenidSharedMinedAddress a1(String a1) {
    this.a1 = a1;
    return this;
  }

  /**
   * physical first row of address on card
   * @return a1
  **/
  @ApiModelProperty(value = "physical first row of address on card")


  public String getA1() {
    return a1;
  }

  public void setA1(String a1) {
    this.a1 = a1;
  }

  public ZenidSharedMinedAddress a2(String a2) {
    this.a2 = a2;
    return this;
  }

  /**
   * physical second row of address on card
   * @return a2
  **/
  @ApiModelProperty(value = "physical second row of address on card")


  public String getA2() {
    return a2;
  }

  public void setA2(String a2) {
    this.a2 = a2;
  }

  public ZenidSharedMinedAddress a3(String a3) {
    this.a3 = a3;
    return this;
  }

  /**
   * physical third row of address on card
   * @return a3
  **/
  @ApiModelProperty(value = "physical third row of address on card")


  public String getA3() {
    return a3;
  }

  public void setA3(String a3) {
    this.a3 = a3;
  }

  public ZenidSharedMinedAddress administrativeAreaLevel1(String administrativeAreaLevel1) {
    this.administrativeAreaLevel1 = administrativeAreaLevel1;
    return this;
  }

  /**
   * main admin. area - in CZ - kraj
   * @return administrativeAreaLevel1
  **/
  @ApiModelProperty(value = "main admin. area - in CZ - kraj")


  public String getAdministrativeAreaLevel1() {
    return administrativeAreaLevel1;
  }

  public void setAdministrativeAreaLevel1(String administrativeAreaLevel1) {
    this.administrativeAreaLevel1 = administrativeAreaLevel1;
  }

  public ZenidSharedMinedAddress administrativeAreaLevel2(String administrativeAreaLevel2) {
    this.administrativeAreaLevel2 = administrativeAreaLevel2;
    return this;
  }

  /**
   * secondary admin. area - in CZ - okres or towns behaves also as okres - like Brno
   * @return administrativeAreaLevel2
  **/
  @ApiModelProperty(value = "secondary admin. area - in CZ - okres or towns behaves also as okres - like Brno")


  public String getAdministrativeAreaLevel2() {
    return administrativeAreaLevel2;
  }

  public void setAdministrativeAreaLevel2(String administrativeAreaLevel2) {
    this.administrativeAreaLevel2 = administrativeAreaLevel2;
  }

  public ZenidSharedMinedAddress locality(String locality) {
    this.locality = locality;
    return this;
  }

  /**
   * identification of town/city/village (if not already defined up - Brno, Praha) / OSM: boundary=administrative+ admin_level=8
   * @return locality
  **/
  @ApiModelProperty(value = "identification of town/city/village (if not already defined up - Brno, Praha) / OSM: boundary=administrative+ admin_level=8")


  public String getLocality() {
    return locality;
  }

  public void setLocality(String locality) {
    this.locality = locality;
  }

  public ZenidSharedMinedAddress sublocality(String sublocality) {
    this.sublocality = sublocality;
    return this;
  }

  /**
   * town-subdivision  CZ - čtvrť/katastrální území (Neighborhood/Cadastral place) / OSM: boundary=administrative+ admin_level=10  SK - čtvrť/katastrální území (Neighborhood/Cadastral place) / OSM: boundary=administrative+ admin_level=10  DE - stadtteil without selfgovernment / OSM: boundary=administrative+ admin_level=10  HU - admin-level 9                todo slovak: Valaská - Piesok is in addess, but Piesok is just place=village, no admin_level=10
   * @return sublocality
  **/
  @ApiModelProperty(value = "town-subdivision  CZ - čtvrť/katastrální území (Neighborhood/Cadastral place) / OSM: boundary=administrative+ admin_level=10  SK - čtvrť/katastrální území (Neighborhood/Cadastral place) / OSM: boundary=administrative+ admin_level=10  DE - stadtteil without selfgovernment / OSM: boundary=administrative+ admin_level=10  HU - admin-level 9                todo slovak: Valaská - Piesok is in addess, but Piesok is just place=village, no admin_level=10")


  public String getSublocality() {
    return sublocality;
  }

  public void setSublocality(String sublocality) {
    this.sublocality = sublocality;
  }

  public ZenidSharedMinedAddress suburb(String suburb) {
    this.suburb = suburb;
    return this;
  }

  /**
   * town-subdivision - selfgoverning - probably used only in CZ and maybe DE  CZ - městská část/obvod / OSM: addr:suburb - it can be in multiple cadastral places (parts cadastral place Trnitá is in suburb Brno-střed and Brno-jih)  DE - stadtteil without selfgovernment / OSM: boundary=administrative+ admin_level=9                todo not used outside CZ right now, so it is not searched/mined from osm, just ruian
   * @return suburb
  **/
  @ApiModelProperty(value = "town-subdivision - selfgoverning - probably used only in CZ and maybe DE  CZ - městská část/obvod / OSM: addr:suburb - it can be in multiple cadastral places (parts cadastral place Trnitá is in suburb Brno-střed and Brno-jih)  DE - stadtteil without selfgovernment / OSM: boundary=administrative+ admin_level=9                todo not used outside CZ right now, so it is not searched/mined from osm, just ruian")


  public String getSuburb() {
    return suburb;
  }

  public void setSuburb(String suburb) {
    this.suburb = suburb;
  }

  public ZenidSharedMinedAddress street(String street) {
    this.street = street;
    return this;
  }

  /**
   * in CZ - ulice
   * @return street
  **/
  @ApiModelProperty(value = "in CZ - ulice")


  public String getStreet() {
    return street;
  }

  public void setStreet(String street) {
    this.street = street;
  }

  public ZenidSharedMinedAddress houseNumber(String houseNumber) {
    this.houseNumber = houseNumber;
    return this;
  }

  /**
   * descriptive house number in town - used in Czechia, Slovakia, Austria (číslo popisné, číslo súpisné, Konskriptionsnummer)
   * @return houseNumber
  **/
  @ApiModelProperty(value = "descriptive house number in town - used in Czechia, Slovakia, Austria (číslo popisné, číslo súpisné, Konskriptionsnummer)")


  public String getHouseNumber() {
    return houseNumber;
  }

  public void setHouseNumber(String houseNumber) {
    this.houseNumber = houseNumber;
  }

  public ZenidSharedMinedAddress streetNumber(String streetNumber) {
    this.streetNumber = streetNumber;
    return this;
  }

  /**
   * descriptive number of house on the street - in CZ - číslo orientační
   * @return streetNumber
  **/
  @ApiModelProperty(value = "descriptive number of house on the street - in CZ - číslo orientační")


  public String getStreetNumber() {
    return streetNumber;
  }

  public void setStreetNumber(String streetNumber) {
    this.streetNumber = streetNumber;
  }

  public ZenidSharedMinedAddress postalCode(String postalCode) {
    this.postalCode = postalCode;
    return this;
  }

  /**
   * in CZ - poštovní směrovací číslo - PSČ
   * @return postalCode
  **/
  @ApiModelProperty(value = "in CZ - poštovní směrovací číslo - PSČ")


  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public ZenidSharedMinedAddress googleSearchable(String googleSearchable) {
    this.googleSearchable = googleSearchable;
    return this;
  }

  /**
   * Get googleSearchable
   * @return googleSearchable
  **/
  @ApiModelProperty(readOnly = true, value = "")


  public String getGoogleSearchable() {
    return googleSearchable;
  }

  public void setGoogleSearchable(String googleSearchable) {
    this.googleSearchable = googleSearchable;
  }

  public ZenidSharedMinedAddress text(String text) {
    this.text = text;
    return this;
  }

  /**
   * Get text
   * @return text
  **/
  @ApiModelProperty(value = "")


  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public ZenidSharedMinedAddress confidence(Integer confidence) {
    this.confidence = confidence;
    return this;
  }

  /**
   * Get confidence
   * @return confidence
  **/
  @ApiModelProperty(value = "")


  public Integer getConfidence() {
    return confidence;
  }

  public void setConfidence(Integer confidence) {
    this.confidence = confidence;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ZenidSharedMinedAddress zenidSharedMinedAddress = (ZenidSharedMinedAddress) o;
    return Objects.equals(this.ID, zenidSharedMinedAddress.ID) &&
        Objects.equals(this.a1, zenidSharedMinedAddress.a1) &&
        Objects.equals(this.a2, zenidSharedMinedAddress.a2) &&
        Objects.equals(this.a3, zenidSharedMinedAddress.a3) &&
        Objects.equals(this.administrativeAreaLevel1, zenidSharedMinedAddress.administrativeAreaLevel1) &&
        Objects.equals(this.administrativeAreaLevel2, zenidSharedMinedAddress.administrativeAreaLevel2) &&
        Objects.equals(this.locality, zenidSharedMinedAddress.locality) &&
        Objects.equals(this.sublocality, zenidSharedMinedAddress.sublocality) &&
        Objects.equals(this.suburb, zenidSharedMinedAddress.suburb) &&
        Objects.equals(this.street, zenidSharedMinedAddress.street) &&
        Objects.equals(this.houseNumber, zenidSharedMinedAddress.houseNumber) &&
        Objects.equals(this.streetNumber, zenidSharedMinedAddress.streetNumber) &&
        Objects.equals(this.postalCode, zenidSharedMinedAddress.postalCode) &&
        Objects.equals(this.googleSearchable, zenidSharedMinedAddress.googleSearchable) &&
        Objects.equals(this.text, zenidSharedMinedAddress.text) &&
        Objects.equals(this.confidence, zenidSharedMinedAddress.confidence);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ID, a1, a2, a3, administrativeAreaLevel1, administrativeAreaLevel2, locality, sublocality, suburb, street, houseNumber, streetNumber, postalCode, googleSearchable, text, confidence);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidSharedMinedAddress {\n");
    
    sb.append("    ID: ").append(toIndentedString(ID)).append("\n");
    sb.append("    a1: ").append(toIndentedString(a1)).append("\n");
    sb.append("    a2: ").append(toIndentedString(a2)).append("\n");
    sb.append("    a3: ").append(toIndentedString(a3)).append("\n");
    sb.append("    administrativeAreaLevel1: ").append(toIndentedString(administrativeAreaLevel1)).append("\n");
    sb.append("    administrativeAreaLevel2: ").append(toIndentedString(administrativeAreaLevel2)).append("\n");
    sb.append("    locality: ").append(toIndentedString(locality)).append("\n");
    sb.append("    sublocality: ").append(toIndentedString(sublocality)).append("\n");
    sb.append("    suburb: ").append(toIndentedString(suburb)).append("\n");
    sb.append("    street: ").append(toIndentedString(street)).append("\n");
    sb.append("    houseNumber: ").append(toIndentedString(houseNumber)).append("\n");
    sb.append("    streetNumber: ").append(toIndentedString(streetNumber)).append("\n");
    sb.append("    postalCode: ").append(toIndentedString(postalCode)).append("\n");
    sb.append("    googleSearchable: ").append(toIndentedString(googleSearchable)).append("\n");
    sb.append("    text: ").append(toIndentedString(text)).append("\n");
    sb.append("    confidence: ").append(toIndentedString(confidence)).append("\n");
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

