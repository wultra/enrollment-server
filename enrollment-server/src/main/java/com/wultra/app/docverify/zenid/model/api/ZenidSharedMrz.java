package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.wultra.app.docverify.zenid.model.api.SystemValueTupleSystemInt32SystemInt32;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ZenidSharedMrz
 */
@Validated


public class ZenidSharedMrz   {
  /**
   * Gets or Sets type
   */
  public enum TypeEnum {
    ID_V2000("ID_v2000"),
    
    ID_V2012("ID_v2012"),
    
    PAS_V2006("PAS_v2006"),
    
    UNKNOWN("Unknown"),
    
    AUT_IDC2002("AUT_IDC2002"),
    
    AUT_PAS2006("AUT_PAS2006"),
    
    SVK_IDC2008("SVK_IDC2008"),
    
    SVK_DL2013("SVK_DL2013"),
    
    SVK_PAS2008("SVK_PAS2008"),
    
    POL_IDC2015("POL_IDC2015"),
    
    HRV_IDC2003("HRV_IDC2003"),
    
    CZE_RES_2011_14("CZE_RES_2011_14"),
    
    HUN_PAS_2006_12("HUN_PAS_2006_12"),
    
    HU_IDC_2000_01_12_16("HU_IDC_2000_01_12_16");

    private String value;

    TypeEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static TypeEnum fromValue(String text) {
      for (TypeEnum b : TypeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("Type")
  private TypeEnum type = null;

  /**
   * Gets or Sets subtype
   */
  public enum SubtypeEnum {
    OP("OP"),
    
    R("R"),
    
    D("D"),
    
    S("S"),
    
    DEFAULT("Default"),
    
    UNKNOWN("Unknown");

    private String value;

    SubtypeEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static SubtypeEnum fromValue(String text) {
      for (SubtypeEnum b : SubtypeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("Subtype")
  private SubtypeEnum subtype = null;

  @JsonProperty("BirthDate")
  private String birthDate = null;

  @JsonProperty("BirthDateVerified")
  private Boolean birthDateVerified = null;

  @JsonProperty("DocumentNumber")
  private String documentNumber = null;

  @JsonProperty("DocumentNumberVerified")
  private Boolean documentNumberVerified = null;

  @JsonProperty("ExpiryDate")
  private String expiryDate = null;

  @JsonProperty("ExpiryDateVerified")
  private Boolean expiryDateVerified = null;

  @JsonProperty("GivenName")
  private String givenName = null;

  @JsonProperty("ChecksumVerified")
  private Boolean checksumVerified = null;

  @JsonProperty("ChecksumDigit")
  private Integer checksumDigit = null;

  @JsonProperty("LastName")
  private String lastName = null;

  @JsonProperty("Nationality")
  private String nationality = null;

  @JsonProperty("Sex")
  private String sex = null;

  @JsonProperty("BirthNumber")
  private String birthNumber = null;

  @JsonProperty("BirthNumberChecksum")
  private Integer birthNumberChecksum = null;

  @JsonProperty("BirthNumberVerified")
  private Boolean birthNumberVerified = null;

  @JsonProperty("BirthdateChecksum")
  private Integer birthdateChecksum = null;

  @JsonProperty("DocumentNumChecksum")
  private Integer documentNumChecksum = null;

  @JsonProperty("ExpiryChecksum")
  private Integer expiryChecksum = null;

  @JsonProperty("BirthDateParsed")
  private LocalDate birthDateParsed = null;

  @JsonProperty("ExpiryDateParsed")
  private LocalDate expiryDateParsed = null;

  @JsonProperty("MrzLength")
  private SystemValueTupleSystemInt32SystemInt32 mrzLength = null;

  /**
   * Gets or Sets mrzDefType
   */
  public enum MrzDefTypeEnum {
    TD1_IDC("TD1_IDC"),
    
    TD2_IDC2000("TD2_IDC2000"),
    
    TD3_PAS("TD3_PAS"),
    
    SKDRV("SKDRV"),
    
    NONE("None");

    private String value;

    MrzDefTypeEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static MrzDefTypeEnum fromValue(String text) {
      for (MrzDefTypeEnum b : MrzDefTypeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("MrzDefType")
  private MrzDefTypeEnum mrzDefType = null;

  public ZenidSharedMrz type(TypeEnum type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
  **/
  @ApiModelProperty(value = "")


  public TypeEnum getType() {
    return type;
  }

  public void setType(TypeEnum type) {
    this.type = type;
  }

  public ZenidSharedMrz subtype(SubtypeEnum subtype) {
    this.subtype = subtype;
    return this;
  }

  /**
   * Get subtype
   * @return subtype
  **/
  @ApiModelProperty(value = "")


  public SubtypeEnum getSubtype() {
    return subtype;
  }

  public void setSubtype(SubtypeEnum subtype) {
    this.subtype = subtype;
  }

  public ZenidSharedMrz birthDate(String birthDate) {
    this.birthDate = birthDate;
    return this;
  }

  /**
   * Inner Birth date string of MRZ. Low-level data, ignore it. Use BirthDate from MineAllResult object.
   * @return birthDate
  **/
  @ApiModelProperty(value = "Inner Birth date string of MRZ. Low-level data, ignore it. Use BirthDate from MineAllResult object.")


  public String getBirthDate() {
    return birthDate;
  }

  public void setBirthDate(String birthDate) {
    this.birthDate = birthDate;
  }

  public ZenidSharedMrz birthDateVerified(Boolean birthDateVerified) {
    this.birthDateVerified = birthDateVerified;
    return this;
  }

  /**
   * Inner flag, if MRZ BirthDate checksum is ok. Low-level check, ignore it. Use Validators.
   * @return birthDateVerified
  **/
  @ApiModelProperty(value = "Inner flag, if MRZ BirthDate checksum is ok. Low-level check, ignore it. Use Validators.")


  public Boolean isBirthDateVerified() {
    return birthDateVerified;
  }

  public void setBirthDateVerified(Boolean birthDateVerified) {
    this.birthDateVerified = birthDateVerified;
  }

  public ZenidSharedMrz documentNumber(String documentNumber) {
    this.documentNumber = documentNumber;
    return this;
  }

  /**
   * Inner Document number string of MRZ. Low-level data, ignore it. Use value from MineAllResult object.
   * @return documentNumber
  **/
  @ApiModelProperty(value = "Inner Document number string of MRZ. Low-level data, ignore it. Use value from MineAllResult object.")


  public String getDocumentNumber() {
    return documentNumber;
  }

  public void setDocumentNumber(String documentNumber) {
    this.documentNumber = documentNumber;
  }

  public ZenidSharedMrz documentNumberVerified(Boolean documentNumberVerified) {
    this.documentNumberVerified = documentNumberVerified;
    return this;
  }

  /**
   * Inner flag, if MRZ DocumentNumber checksum is ok. Low-level check, ignore it. Use Validators.
   * @return documentNumberVerified
  **/
  @ApiModelProperty(value = "Inner flag, if MRZ DocumentNumber checksum is ok. Low-level check, ignore it. Use Validators.")


  public Boolean isDocumentNumberVerified() {
    return documentNumberVerified;
  }

  public void setDocumentNumberVerified(Boolean documentNumberVerified) {
    this.documentNumberVerified = documentNumberVerified;
  }

  public ZenidSharedMrz expiryDate(String expiryDate) {
    this.expiryDate = expiryDate;
    return this;
  }

  /**
   * Inner Expiry date string of MRZ. Low-level data, ignore it. Use value from MineAllResult object.
   * @return expiryDate
  **/
  @ApiModelProperty(value = "Inner Expiry date string of MRZ. Low-level data, ignore it. Use value from MineAllResult object.")


  public String getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(String expiryDate) {
    this.expiryDate = expiryDate;
  }

  public ZenidSharedMrz expiryDateVerified(Boolean expiryDateVerified) {
    this.expiryDateVerified = expiryDateVerified;
    return this;
  }

  /**
   * Inner flag, if MRZ ExpiryDate checksum is ok. Low-level check, ignore it. Use Validators.
   * @return expiryDateVerified
  **/
  @ApiModelProperty(value = "Inner flag, if MRZ ExpiryDate checksum is ok. Low-level check, ignore it. Use Validators.")


  public Boolean isExpiryDateVerified() {
    return expiryDateVerified;
  }

  public void setExpiryDateVerified(Boolean expiryDateVerified) {
    this.expiryDateVerified = expiryDateVerified;
  }

  public ZenidSharedMrz givenName(String givenName) {
    this.givenName = givenName;
    return this;
  }

  /**
   * Inner Given name string of MRZ. Low-level data, ignore it. Use value from MineAllResult object.
   * @return givenName
  **/
  @ApiModelProperty(value = "Inner Given name string of MRZ. Low-level data, ignore it. Use value from MineAllResult object.")


  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public ZenidSharedMrz checksumVerified(Boolean checksumVerified) {
    this.checksumVerified = checksumVerified;
    return this;
  }

  /**
   * Inner flag, if checksum of MRZ itself is ok. Low-level check, ignore it. Use Validators.
   * @return checksumVerified
  **/
  @ApiModelProperty(value = "Inner flag, if checksum of MRZ itself is ok. Low-level check, ignore it. Use Validators.")


  public Boolean isChecksumVerified() {
    return checksumVerified;
  }

  public void setChecksumVerified(Boolean checksumVerified) {
    this.checksumVerified = checksumVerified;
  }

  public ZenidSharedMrz checksumDigit(Integer checksumDigit) {
    this.checksumDigit = checksumDigit;
    return this;
  }

  /**
   * Inner value of global MRZ checksum.
   * @return checksumDigit
  **/
  @ApiModelProperty(value = "Inner value of global MRZ checksum.")


  public Integer getChecksumDigit() {
    return checksumDigit;
  }

  public void setChecksumDigit(Integer checksumDigit) {
    this.checksumDigit = checksumDigit;
  }

  public ZenidSharedMrz lastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  /**
   * Inner Last name string of MRZ. Low-level data, ignore it. Use value from MineAllResult object.
   * @return lastName
  **/
  @ApiModelProperty(value = "Inner Last name string of MRZ. Low-level data, ignore it. Use value from MineAllResult object.")


  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public ZenidSharedMrz nationality(String nationality) {
    this.nationality = nationality;
    return this;
  }

  /**
   * Inner Nationality string of MRZ. Low-level data, ignore it. Use value from MineAllResult object.
   * @return nationality
  **/
  @ApiModelProperty(value = "Inner Nationality string of MRZ. Low-level data, ignore it. Use value from MineAllResult object.")


  public String getNationality() {
    return nationality;
  }

  public void setNationality(String nationality) {
    this.nationality = nationality;
  }

  public ZenidSharedMrz sex(String sex) {
    this.sex = sex;
    return this;
  }

  /**
   * Inner Sex string of MRZ. Low-level data, ignore it. Use value from MineAllResult object.
   * @return sex
  **/
  @ApiModelProperty(value = "Inner Sex string of MRZ. Low-level data, ignore it. Use value from MineAllResult object.")


  public String getSex() {
    return sex;
  }

  public void setSex(String sex) {
    this.sex = sex;
  }

  public ZenidSharedMrz birthNumber(String birthNumber) {
    this.birthNumber = birthNumber;
    return this;
  }

  /**
   * Inner Birthnumber string of MRZ (used on Czech passports). Low-level data, ignore it. Use value from MineAllResult object.
   * @return birthNumber
  **/
  @ApiModelProperty(value = "Inner Birthnumber string of MRZ (used on Czech passports). Low-level data, ignore it. Use value from MineAllResult object.")


  public String getBirthNumber() {
    return birthNumber;
  }

  public void setBirthNumber(String birthNumber) {
    this.birthNumber = birthNumber;
  }

  public ZenidSharedMrz birthNumberChecksum(Integer birthNumberChecksum) {
    this.birthNumberChecksum = birthNumberChecksum;
    return this;
  }

  /**
   * Inner value of Birthnumber checksum in MRZ (on Czech passports). Low-level check, ignore it. Use Validators.
   * @return birthNumberChecksum
  **/
  @ApiModelProperty(value = "Inner value of Birthnumber checksum in MRZ (on Czech passports). Low-level check, ignore it. Use Validators.")


  public Integer getBirthNumberChecksum() {
    return birthNumberChecksum;
  }

  public void setBirthNumberChecksum(Integer birthNumberChecksum) {
    this.birthNumberChecksum = birthNumberChecksum;
  }

  public ZenidSharedMrz birthNumberVerified(Boolean birthNumberVerified) {
    this.birthNumberVerified = birthNumberVerified;
    return this;
  }

  /**
   * Inner flag, if MRZ BirthNumber checksum is ok (used on Czech passports). Low-level check, ignore it. Use Validators.
   * @return birthNumberVerified
  **/
  @ApiModelProperty(value = "Inner flag, if MRZ BirthNumber checksum is ok (used on Czech passports). Low-level check, ignore it. Use Validators.")


  public Boolean isBirthNumberVerified() {
    return birthNumberVerified;
  }

  public void setBirthNumberVerified(Boolean birthNumberVerified) {
    this.birthNumberVerified = birthNumberVerified;
  }

  public ZenidSharedMrz birthdateChecksum(Integer birthdateChecksum) {
    this.birthdateChecksum = birthdateChecksum;
    return this;
  }

  /**
   * Inner value of MRZ BirthDate checksum.
   * @return birthdateChecksum
  **/
  @ApiModelProperty(value = "Inner value of MRZ BirthDate checksum.")


  public Integer getBirthdateChecksum() {
    return birthdateChecksum;
  }

  public void setBirthdateChecksum(Integer birthdateChecksum) {
    this.birthdateChecksum = birthdateChecksum;
  }

  public ZenidSharedMrz documentNumChecksum(Integer documentNumChecksum) {
    this.documentNumChecksum = documentNumChecksum;
    return this;
  }

  /**
   * Inner value of MRZ DocumentNumber checksum.
   * @return documentNumChecksum
  **/
  @ApiModelProperty(value = "Inner value of MRZ DocumentNumber checksum.")


  public Integer getDocumentNumChecksum() {
    return documentNumChecksum;
  }

  public void setDocumentNumChecksum(Integer documentNumChecksum) {
    this.documentNumChecksum = documentNumChecksum;
  }

  public ZenidSharedMrz expiryChecksum(Integer expiryChecksum) {
    this.expiryChecksum = expiryChecksum;
    return this;
  }

  /**
   * Inner value of MRZ ExpiryDate checksum.
   * @return expiryChecksum
  **/
  @ApiModelProperty(value = "Inner value of MRZ ExpiryDate checksum.")


  public Integer getExpiryChecksum() {
    return expiryChecksum;
  }

  public void setExpiryChecksum(Integer expiryChecksum) {
    this.expiryChecksum = expiryChecksum;
  }

  public ZenidSharedMrz birthDateParsed(LocalDate birthDateParsed) {
    this.birthDateParsed = birthDateParsed;
    return this;
  }

  /**
   * Inner machine-readable value of BirthDate (in DateTime structure). Low-level data, use value from MineAllResult object.
   * @return birthDateParsed
  **/
  @ApiModelProperty(readOnly = true, value = "Inner machine-readable value of BirthDate (in DateTime structure). Low-level data, use value from MineAllResult object.")

  @Valid

  public LocalDate getBirthDateParsed() {
    return birthDateParsed;
  }

  public void setBirthDateParsed(LocalDate birthDateParsed) {
    this.birthDateParsed = birthDateParsed;
  }

  public ZenidSharedMrz expiryDateParsed(LocalDate expiryDateParsed) {
    this.expiryDateParsed = expiryDateParsed;
    return this;
  }

  /**
   * Inner machine-readable value of ExpiryDate (in DateTime structure). Low-level data, use value from MineAllResult object.
   * @return expiryDateParsed
  **/
  @ApiModelProperty(readOnly = true, value = "Inner machine-readable value of ExpiryDate (in DateTime structure). Low-level data, use value from MineAllResult object.")

  @Valid

  public LocalDate getExpiryDateParsed() {
    return expiryDateParsed;
  }

  public void setExpiryDateParsed(LocalDate expiryDateParsed) {
    this.expiryDateParsed = expiryDateParsed;
  }

  public ZenidSharedMrz mrzLength(SystemValueTupleSystemInt32SystemInt32 mrzLength) {
    this.mrzLength = mrzLength;
    return this;
  }

  /**
   * Get mrzLength
   * @return mrzLength
  **/
  @ApiModelProperty(readOnly = true, value = "")

  @Valid

  public SystemValueTupleSystemInt32SystemInt32 getMrzLength() {
    return mrzLength;
  }

  public void setMrzLength(SystemValueTupleSystemInt32SystemInt32 mrzLength) {
    this.mrzLength = mrzLength;
  }

  public ZenidSharedMrz mrzDefType(MrzDefTypeEnum mrzDefType) {
    this.mrzDefType = mrzDefType;
    return this;
  }

  /**
   * Get mrzDefType
   * @return mrzDefType
  **/
  @ApiModelProperty(value = "")


  public MrzDefTypeEnum getMrzDefType() {
    return mrzDefType;
  }

  public void setMrzDefType(MrzDefTypeEnum mrzDefType) {
    this.mrzDefType = mrzDefType;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ZenidSharedMrz zenidSharedMrz = (ZenidSharedMrz) o;
    return Objects.equals(this.type, zenidSharedMrz.type) &&
        Objects.equals(this.subtype, zenidSharedMrz.subtype) &&
        Objects.equals(this.birthDate, zenidSharedMrz.birthDate) &&
        Objects.equals(this.birthDateVerified, zenidSharedMrz.birthDateVerified) &&
        Objects.equals(this.documentNumber, zenidSharedMrz.documentNumber) &&
        Objects.equals(this.documentNumberVerified, zenidSharedMrz.documentNumberVerified) &&
        Objects.equals(this.expiryDate, zenidSharedMrz.expiryDate) &&
        Objects.equals(this.expiryDateVerified, zenidSharedMrz.expiryDateVerified) &&
        Objects.equals(this.givenName, zenidSharedMrz.givenName) &&
        Objects.equals(this.checksumVerified, zenidSharedMrz.checksumVerified) &&
        Objects.equals(this.checksumDigit, zenidSharedMrz.checksumDigit) &&
        Objects.equals(this.lastName, zenidSharedMrz.lastName) &&
        Objects.equals(this.nationality, zenidSharedMrz.nationality) &&
        Objects.equals(this.sex, zenidSharedMrz.sex) &&
        Objects.equals(this.birthNumber, zenidSharedMrz.birthNumber) &&
        Objects.equals(this.birthNumberChecksum, zenidSharedMrz.birthNumberChecksum) &&
        Objects.equals(this.birthNumberVerified, zenidSharedMrz.birthNumberVerified) &&
        Objects.equals(this.birthdateChecksum, zenidSharedMrz.birthdateChecksum) &&
        Objects.equals(this.documentNumChecksum, zenidSharedMrz.documentNumChecksum) &&
        Objects.equals(this.expiryChecksum, zenidSharedMrz.expiryChecksum) &&
        Objects.equals(this.birthDateParsed, zenidSharedMrz.birthDateParsed) &&
        Objects.equals(this.expiryDateParsed, zenidSharedMrz.expiryDateParsed) &&
        Objects.equals(this.mrzLength, zenidSharedMrz.mrzLength) &&
        Objects.equals(this.mrzDefType, zenidSharedMrz.mrzDefType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, subtype, birthDate, birthDateVerified, documentNumber, documentNumberVerified, expiryDate, expiryDateVerified, givenName, checksumVerified, checksumDigit, lastName, nationality, sex, birthNumber, birthNumberChecksum, birthNumberVerified, birthdateChecksum, documentNumChecksum, expiryChecksum, birthDateParsed, expiryDateParsed, mrzLength, mrzDefType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidSharedMrz {\n");
    
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    subtype: ").append(toIndentedString(subtype)).append("\n");
    sb.append("    birthDate: ").append(toIndentedString(birthDate)).append("\n");
    sb.append("    birthDateVerified: ").append(toIndentedString(birthDateVerified)).append("\n");
    sb.append("    documentNumber: ").append(toIndentedString(documentNumber)).append("\n");
    sb.append("    documentNumberVerified: ").append(toIndentedString(documentNumberVerified)).append("\n");
    sb.append("    expiryDate: ").append(toIndentedString(expiryDate)).append("\n");
    sb.append("    expiryDateVerified: ").append(toIndentedString(expiryDateVerified)).append("\n");
    sb.append("    givenName: ").append(toIndentedString(givenName)).append("\n");
    sb.append("    checksumVerified: ").append(toIndentedString(checksumVerified)).append("\n");
    sb.append("    checksumDigit: ").append(toIndentedString(checksumDigit)).append("\n");
    sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
    sb.append("    nationality: ").append(toIndentedString(nationality)).append("\n");
    sb.append("    sex: ").append(toIndentedString(sex)).append("\n");
    sb.append("    birthNumber: ").append(toIndentedString(birthNumber)).append("\n");
    sb.append("    birthNumberChecksum: ").append(toIndentedString(birthNumberChecksum)).append("\n");
    sb.append("    birthNumberVerified: ").append(toIndentedString(birthNumberVerified)).append("\n");
    sb.append("    birthdateChecksum: ").append(toIndentedString(birthdateChecksum)).append("\n");
    sb.append("    documentNumChecksum: ").append(toIndentedString(documentNumChecksum)).append("\n");
    sb.append("    expiryChecksum: ").append(toIndentedString(expiryChecksum)).append("\n");
    sb.append("    birthDateParsed: ").append(toIndentedString(birthDateParsed)).append("\n");
    sb.append("    expiryDateParsed: ").append(toIndentedString(expiryDateParsed)).append("\n");
    sb.append("    mrzLength: ").append(toIndentedString(mrzLength)).append("\n");
    sb.append("    mrzDefType: ").append(toIndentedString(mrzDefType)).append("\n");
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

