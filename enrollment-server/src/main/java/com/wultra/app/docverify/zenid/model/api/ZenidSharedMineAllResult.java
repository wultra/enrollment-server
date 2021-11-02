package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.wultra.app.docverify.zenid.model.api.ZenidSharedMinedAddress;
import com.wultra.app.docverify.zenid.model.api.ZenidSharedMinedDate;
import com.wultra.app.docverify.zenid.model.api.ZenidSharedMinedMaritalStatus;
import com.wultra.app.docverify.zenid.model.api.ZenidSharedMinedMrz;
import com.wultra.app.docverify.zenid.model.api.ZenidSharedMinedPhoto;
import com.wultra.app.docverify.zenid.model.api.ZenidSharedMinedRc;
import com.wultra.app.docverify.zenid.model.api.ZenidSharedMinedSex;
import com.wultra.app.docverify.zenid.model.api.ZenidSharedMinedText;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ZenidSharedMineAllResult
 */
@Validated


public class ZenidSharedMineAllResult   {
  @JsonProperty("FirstName")
  private ZenidSharedMinedText firstName = null;

  @JsonProperty("LastName")
  private ZenidSharedMinedText lastName = null;

  @JsonProperty("Address")
  private ZenidSharedMinedAddress address = null;

  @JsonProperty("BirthAddress")
  private ZenidSharedMinedText birthAddress = null;

  @JsonProperty("BirthLastName")
  private ZenidSharedMinedText birthLastName = null;

  @JsonProperty("BirthNumber")
  private ZenidSharedMinedRc birthNumber = null;

  @JsonProperty("BirthDate")
  private ZenidSharedMinedDate birthDate = null;

  @JsonProperty("ExpiryDate")
  private ZenidSharedMinedDate expiryDate = null;

  @JsonProperty("IssueDate")
  private ZenidSharedMinedDate issueDate = null;

  @JsonProperty("IdcardNumber")
  private ZenidSharedMinedText idcardNumber = null;

  @JsonProperty("DrivinglicenseNumber")
  private ZenidSharedMinedText drivinglicenseNumber = null;

  @JsonProperty("PassportNumber")
  private ZenidSharedMinedText passportNumber = null;

  @JsonProperty("Sex")
  private ZenidSharedMinedSex sex = null;

  @JsonProperty("Nationality")
  private ZenidSharedMinedText nationality = null;

  @JsonProperty("Authority")
  private ZenidSharedMinedText authority = null;

  @JsonProperty("MaritalStatus")
  private ZenidSharedMinedMaritalStatus maritalStatus = null;

  @JsonProperty("Photo")
  private ZenidSharedMinedPhoto photo = null;

  @JsonProperty("Mrz")
  private ZenidSharedMinedMrz mrz = null;

  /**
   * Code identificating document (when combining from more samples the most probable version is set)
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

  /**
   * Country associated with this document type
   */
  public enum DocumentCountryEnum {
    CZ("Cz"),
    
    SK("Sk"),
    
    AT("At"),
    
    HU("Hu"),
    
    PL("Pl"),
    
    DE("De"),
    
    HR("Hr"),
    
    RO("Ro"),
    
    RU("Ru"),
    
    UA("Ua"),
    
    IT("It"),
    
    DK("Dk"),
    
    ES("Es"),
    
    FI("Fi"),
    
    FR("Fr"),
    
    GB("Gb"),
    
    IS("Is"),
    
    NL("Nl"),
    
    SE("Se"),
    
    SI("Si"),
    
    BG("Bg");

    private String value;

    DocumentCountryEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static DocumentCountryEnum fromValue(String text) {
      for (DocumentCountryEnum b : DocumentCountryEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("DocumentCountry")
  private DocumentCountryEnum documentCountry = null;

  /**
   * General role of this document (ID card vs Passport vs Driver license etc)
   */
  public enum DocumentRoleEnum {
    IDC("Idc"),
    
    PAS("Pas"),
    
    DRV("Drv"),
    
    RES("Res"),
    
    GUN("Gun"),
    
    HIC("Hic"),
    
    STD("Std"),
    
    CAR("Car"),
    
    BIRTH("Birth");

    private String value;

    DocumentRoleEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static DocumentRoleEnum fromValue(String text) {
      for (DocumentRoleEnum b : DocumentRoleEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("DocumentRole")
  private DocumentRoleEnum documentRole = null;

  /**
   * identification of page of document
   */
  public enum PageCodeEnum {
    F("F"),
    
    B("B");

    private String value;

    PageCodeEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static PageCodeEnum fromValue(String text) {
      for (PageCodeEnum b : PageCodeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("PageCode")
  private PageCodeEnum pageCode = null;

  @JsonProperty("Height")
  private ZenidSharedMinedText height = null;

  @JsonProperty("EyesColor")
  private ZenidSharedMinedText eyesColor = null;

  @JsonProperty("CarNumber")
  private ZenidSharedMinedText carNumber = null;

  @JsonProperty("FirstNameOfParents")
  private ZenidSharedMinedText firstNameOfParents = null;

  @JsonProperty("ResidencyNumber")
  private ZenidSharedMinedText residencyNumber = null;

  @JsonProperty("ResidencyNumberPhoto")
  private ZenidSharedMinedText residencyNumberPhoto = null;

  @JsonProperty("ResidencyPermitDescription")
  private ZenidSharedMinedText residencyPermitDescription = null;

  @JsonProperty("ResidencyPermitCode")
  private ZenidSharedMinedText residencyPermitCode = null;

  @JsonProperty("GunlicenseNumber")
  private ZenidSharedMinedText gunlicenseNumber = null;

  @JsonProperty("Titles")
  private ZenidSharedMinedText titles = null;

  @JsonProperty("TitlesAfter")
  private ZenidSharedMinedText titlesAfter = null;

  @JsonProperty("SpecialRemarks")
  private ZenidSharedMinedText specialRemarks = null;

  @JsonProperty("MothersName")
  private ZenidSharedMinedText mothersName = null;

  @JsonProperty("HealthInsuranceCardNumber")
  private ZenidSharedMinedText healthInsuranceCardNumber = null;

  @JsonProperty("InsuranceCompanyCode")
  private ZenidSharedMinedText insuranceCompanyCode = null;

  @JsonProperty("IssuingCountry")
  private ZenidSharedMinedText issuingCountry = null;

  public ZenidSharedMineAllResult firstName(ZenidSharedMinedText firstName) {
    this.firstName = firstName;
    return this;
  }

  /**
   * Get firstName
   * @return firstName
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getFirstName() {
    return firstName;
  }

  public void setFirstName(ZenidSharedMinedText firstName) {
    this.firstName = firstName;
  }

  public ZenidSharedMineAllResult lastName(ZenidSharedMinedText lastName) {
    this.lastName = lastName;
    return this;
  }

  /**
   * Get lastName
   * @return lastName
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getLastName() {
    return lastName;
  }

  public void setLastName(ZenidSharedMinedText lastName) {
    this.lastName = lastName;
  }

  public ZenidSharedMineAllResult address(ZenidSharedMinedAddress address) {
    this.address = address;
    return this;
  }

  /**
   * Get address
   * @return address
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedAddress getAddress() {
    return address;
  }

  public void setAddress(ZenidSharedMinedAddress address) {
    this.address = address;
  }

  public ZenidSharedMineAllResult birthAddress(ZenidSharedMinedText birthAddress) {
    this.birthAddress = birthAddress;
    return this;
  }

  /**
   * Get birthAddress
   * @return birthAddress
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getBirthAddress() {
    return birthAddress;
  }

  public void setBirthAddress(ZenidSharedMinedText birthAddress) {
    this.birthAddress = birthAddress;
  }

  public ZenidSharedMineAllResult birthLastName(ZenidSharedMinedText birthLastName) {
    this.birthLastName = birthLastName;
    return this;
  }

  /**
   * Get birthLastName
   * @return birthLastName
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getBirthLastName() {
    return birthLastName;
  }

  public void setBirthLastName(ZenidSharedMinedText birthLastName) {
    this.birthLastName = birthLastName;
  }

  public ZenidSharedMineAllResult birthNumber(ZenidSharedMinedRc birthNumber) {
    this.birthNumber = birthNumber;
    return this;
  }

  /**
   * Get birthNumber
   * @return birthNumber
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedRc getBirthNumber() {
    return birthNumber;
  }

  public void setBirthNumber(ZenidSharedMinedRc birthNumber) {
    this.birthNumber = birthNumber;
  }

  public ZenidSharedMineAllResult birthDate(ZenidSharedMinedDate birthDate) {
    this.birthDate = birthDate;
    return this;
  }

  /**
   * Get birthDate
   * @return birthDate
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedDate getBirthDate() {
    return birthDate;
  }

  public void setBirthDate(ZenidSharedMinedDate birthDate) {
    this.birthDate = birthDate;
  }

  public ZenidSharedMineAllResult expiryDate(ZenidSharedMinedDate expiryDate) {
    this.expiryDate = expiryDate;
    return this;
  }

  /**
   * Get expiryDate
   * @return expiryDate
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedDate getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(ZenidSharedMinedDate expiryDate) {
    this.expiryDate = expiryDate;
  }

  public ZenidSharedMineAllResult issueDate(ZenidSharedMinedDate issueDate) {
    this.issueDate = issueDate;
    return this;
  }

  /**
   * Get issueDate
   * @return issueDate
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedDate getIssueDate() {
    return issueDate;
  }

  public void setIssueDate(ZenidSharedMinedDate issueDate) {
    this.issueDate = issueDate;
  }

  public ZenidSharedMineAllResult idcardNumber(ZenidSharedMinedText idcardNumber) {
    this.idcardNumber = idcardNumber;
    return this;
  }

  /**
   * identification number for id card - set only on id cards
   * @return idcardNumber
  **/
  @ApiModelProperty(value = "identification number for id card - set only on id cards")

  @Valid

  public ZenidSharedMinedText getIdcardNumber() {
    return idcardNumber;
  }

  public void setIdcardNumber(ZenidSharedMinedText idcardNumber) {
    this.idcardNumber = idcardNumber;
  }

  public ZenidSharedMineAllResult drivinglicenseNumber(ZenidSharedMinedText drivinglicenseNumber) {
    this.drivinglicenseNumber = drivinglicenseNumber;
    return this;
  }

  /**
   * identification number for driving licence - set only on driving licences
   * @return drivinglicenseNumber
  **/
  @ApiModelProperty(value = "identification number for driving licence - set only on driving licences")

  @Valid

  public ZenidSharedMinedText getDrivinglicenseNumber() {
    return drivinglicenseNumber;
  }

  public void setDrivinglicenseNumber(ZenidSharedMinedText drivinglicenseNumber) {
    this.drivinglicenseNumber = drivinglicenseNumber;
  }

  public ZenidSharedMineAllResult passportNumber(ZenidSharedMinedText passportNumber) {
    this.passportNumber = passportNumber;
    return this;
  }

  /**
   * identification number for passport - set only on passports
   * @return passportNumber
  **/
  @ApiModelProperty(value = "identification number for passport - set only on passports")

  @Valid

  public ZenidSharedMinedText getPassportNumber() {
    return passportNumber;
  }

  public void setPassportNumber(ZenidSharedMinedText passportNumber) {
    this.passportNumber = passportNumber;
  }

  public ZenidSharedMineAllResult sex(ZenidSharedMinedSex sex) {
    this.sex = sex;
    return this;
  }

  /**
   * Get sex
   * @return sex
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedSex getSex() {
    return sex;
  }

  public void setSex(ZenidSharedMinedSex sex) {
    this.sex = sex;
  }

  public ZenidSharedMineAllResult nationality(ZenidSharedMinedText nationality) {
    this.nationality = nationality;
    return this;
  }

  /**
   * Get nationality
   * @return nationality
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getNationality() {
    return nationality;
  }

  public void setNationality(ZenidSharedMinedText nationality) {
    this.nationality = nationality;
  }

  public ZenidSharedMineAllResult authority(ZenidSharedMinedText authority) {
    this.authority = authority;
    return this;
  }

  /**
   * Authority (state agency) issued this document
   * @return authority
  **/
  @ApiModelProperty(value = "Authority (state agency) issued this document")

  @Valid

  public ZenidSharedMinedText getAuthority() {
    return authority;
  }

  public void setAuthority(ZenidSharedMinedText authority) {
    this.authority = authority;
  }

  public ZenidSharedMineAllResult maritalStatus(ZenidSharedMinedMaritalStatus maritalStatus) {
    this.maritalStatus = maritalStatus;
    return this;
  }

  /**
   * Get maritalStatus
   * @return maritalStatus
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedMaritalStatus getMaritalStatus() {
    return maritalStatus;
  }

  public void setMaritalStatus(ZenidSharedMinedMaritalStatus maritalStatus) {
    this.maritalStatus = maritalStatus;
  }

  public ZenidSharedMineAllResult photo(ZenidSharedMinedPhoto photo) {
    this.photo = photo;
    return this;
  }

  /**
   * Get photo
   * @return photo
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedPhoto getPhoto() {
    return photo;
  }

  public void setPhoto(ZenidSharedMinedPhoto photo) {
    this.photo = photo;
  }

  public ZenidSharedMineAllResult mrz(ZenidSharedMinedMrz mrz) {
    this.mrz = mrz;
    return this;
  }

  /**
   * Machine readable zone
   * @return mrz
  **/
  @ApiModelProperty(value = "Machine readable zone")

  @Valid

  public ZenidSharedMinedMrz getMrz() {
    return mrz;
  }

  public void setMrz(ZenidSharedMinedMrz mrz) {
    this.mrz = mrz;
  }

  public ZenidSharedMineAllResult documentCode(DocumentCodeEnum documentCode) {
    this.documentCode = documentCode;
    return this;
  }

  /**
   * Code identificating document (when combining from more samples the most probable version is set)
   * @return documentCode
  **/
  @ApiModelProperty(value = "Code identificating document (when combining from more samples the most probable version is set)")


  public DocumentCodeEnum getDocumentCode() {
    return documentCode;
  }

  public void setDocumentCode(DocumentCodeEnum documentCode) {
    this.documentCode = documentCode;
  }

  public ZenidSharedMineAllResult documentCountry(DocumentCountryEnum documentCountry) {
    this.documentCountry = documentCountry;
    return this;
  }

  /**
   * Country associated with this document type
   * @return documentCountry
  **/
  @ApiModelProperty(value = "Country associated with this document type")


  public DocumentCountryEnum getDocumentCountry() {
    return documentCountry;
  }

  public void setDocumentCountry(DocumentCountryEnum documentCountry) {
    this.documentCountry = documentCountry;
  }

  public ZenidSharedMineAllResult documentRole(DocumentRoleEnum documentRole) {
    this.documentRole = documentRole;
    return this;
  }

  /**
   * General role of this document (ID card vs Passport vs Driver license etc)
   * @return documentRole
  **/
  @ApiModelProperty(value = "General role of this document (ID card vs Passport vs Driver license etc)")


  public DocumentRoleEnum getDocumentRole() {
    return documentRole;
  }

  public void setDocumentRole(DocumentRoleEnum documentRole) {
    this.documentRole = documentRole;
  }

  public ZenidSharedMineAllResult pageCode(PageCodeEnum pageCode) {
    this.pageCode = pageCode;
    return this;
  }

  /**
   * identification of page of document
   * @return pageCode
  **/
  @ApiModelProperty(value = "identification of page of document")


  public PageCodeEnum getPageCode() {
    return pageCode;
  }

  public void setPageCode(PageCodeEnum pageCode) {
    this.pageCode = pageCode;
  }

  public ZenidSharedMineAllResult height(ZenidSharedMinedText height) {
    this.height = height;
    return this;
  }

  /**
   * Get height
   * @return height
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getHeight() {
    return height;
  }

  public void setHeight(ZenidSharedMinedText height) {
    this.height = height;
  }

  public ZenidSharedMineAllResult eyesColor(ZenidSharedMinedText eyesColor) {
    this.eyesColor = eyesColor;
    return this;
  }

  /**
   * Get eyesColor
   * @return eyesColor
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getEyesColor() {
    return eyesColor;
  }

  public void setEyesColor(ZenidSharedMinedText eyesColor) {
    this.eyesColor = eyesColor;
  }

  public ZenidSharedMineAllResult carNumber(ZenidSharedMinedText carNumber) {
    this.carNumber = carNumber;
    return this;
  }

  /**
   * Get carNumber
   * @return carNumber
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getCarNumber() {
    return carNumber;
  }

  public void setCarNumber(ZenidSharedMinedText carNumber) {
    this.carNumber = carNumber;
  }

  public ZenidSharedMineAllResult firstNameOfParents(ZenidSharedMinedText firstNameOfParents) {
    this.firstNameOfParents = firstNameOfParents;
    return this;
  }

  /**
   * Get firstNameOfParents
   * @return firstNameOfParents
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getFirstNameOfParents() {
    return firstNameOfParents;
  }

  public void setFirstNameOfParents(ZenidSharedMinedText firstNameOfParents) {
    this.firstNameOfParents = firstNameOfParents;
  }

  public ZenidSharedMineAllResult residencyNumber(ZenidSharedMinedText residencyNumber) {
    this.residencyNumber = residencyNumber;
    return this;
  }

  /**
   * Get residencyNumber
   * @return residencyNumber
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getResidencyNumber() {
    return residencyNumber;
  }

  public void setResidencyNumber(ZenidSharedMinedText residencyNumber) {
    this.residencyNumber = residencyNumber;
  }

  public ZenidSharedMineAllResult residencyNumberPhoto(ZenidSharedMinedText residencyNumberPhoto) {
    this.residencyNumberPhoto = residencyNumberPhoto;
    return this;
  }

  /**
   * Get residencyNumberPhoto
   * @return residencyNumberPhoto
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getResidencyNumberPhoto() {
    return residencyNumberPhoto;
  }

  public void setResidencyNumberPhoto(ZenidSharedMinedText residencyNumberPhoto) {
    this.residencyNumberPhoto = residencyNumberPhoto;
  }

  public ZenidSharedMineAllResult residencyPermitDescription(ZenidSharedMinedText residencyPermitDescription) {
    this.residencyPermitDescription = residencyPermitDescription;
    return this;
  }

  /**
   * Get residencyPermitDescription
   * @return residencyPermitDescription
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getResidencyPermitDescription() {
    return residencyPermitDescription;
  }

  public void setResidencyPermitDescription(ZenidSharedMinedText residencyPermitDescription) {
    this.residencyPermitDescription = residencyPermitDescription;
  }

  public ZenidSharedMineAllResult residencyPermitCode(ZenidSharedMinedText residencyPermitCode) {
    this.residencyPermitCode = residencyPermitCode;
    return this;
  }

  /**
   * Get residencyPermitCode
   * @return residencyPermitCode
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getResidencyPermitCode() {
    return residencyPermitCode;
  }

  public void setResidencyPermitCode(ZenidSharedMinedText residencyPermitCode) {
    this.residencyPermitCode = residencyPermitCode;
  }

  public ZenidSharedMineAllResult gunlicenseNumber(ZenidSharedMinedText gunlicenseNumber) {
    this.gunlicenseNumber = gunlicenseNumber;
    return this;
  }

  /**
   * Get gunlicenseNumber
   * @return gunlicenseNumber
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getGunlicenseNumber() {
    return gunlicenseNumber;
  }

  public void setGunlicenseNumber(ZenidSharedMinedText gunlicenseNumber) {
    this.gunlicenseNumber = gunlicenseNumber;
  }

  public ZenidSharedMineAllResult titles(ZenidSharedMinedText titles) {
    this.titles = titles;
    return this;
  }

  /**
   * Get titles
   * @return titles
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getTitles() {
    return titles;
  }

  public void setTitles(ZenidSharedMinedText titles) {
    this.titles = titles;
  }

  public ZenidSharedMineAllResult titlesAfter(ZenidSharedMinedText titlesAfter) {
    this.titlesAfter = titlesAfter;
    return this;
  }

  /**
   * Get titlesAfter
   * @return titlesAfter
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getTitlesAfter() {
    return titlesAfter;
  }

  public void setTitlesAfter(ZenidSharedMinedText titlesAfter) {
    this.titlesAfter = titlesAfter;
  }

  public ZenidSharedMineAllResult specialRemarks(ZenidSharedMinedText specialRemarks) {
    this.specialRemarks = specialRemarks;
    return this;
  }

  /**
   * Get specialRemarks
   * @return specialRemarks
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getSpecialRemarks() {
    return specialRemarks;
  }

  public void setSpecialRemarks(ZenidSharedMinedText specialRemarks) {
    this.specialRemarks = specialRemarks;
  }

  public ZenidSharedMineAllResult mothersName(ZenidSharedMinedText mothersName) {
    this.mothersName = mothersName;
    return this;
  }

  /**
   * Get mothersName
   * @return mothersName
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getMothersName() {
    return mothersName;
  }

  public void setMothersName(ZenidSharedMinedText mothersName) {
    this.mothersName = mothersName;
  }

  public ZenidSharedMineAllResult healthInsuranceCardNumber(ZenidSharedMinedText healthInsuranceCardNumber) {
    this.healthInsuranceCardNumber = healthInsuranceCardNumber;
    return this;
  }

  /**
   * Get healthInsuranceCardNumber
   * @return healthInsuranceCardNumber
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getHealthInsuranceCardNumber() {
    return healthInsuranceCardNumber;
  }

  public void setHealthInsuranceCardNumber(ZenidSharedMinedText healthInsuranceCardNumber) {
    this.healthInsuranceCardNumber = healthInsuranceCardNumber;
  }

  public ZenidSharedMineAllResult insuranceCompanyCode(ZenidSharedMinedText insuranceCompanyCode) {
    this.insuranceCompanyCode = insuranceCompanyCode;
    return this;
  }

  /**
   * Get insuranceCompanyCode
   * @return insuranceCompanyCode
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getInsuranceCompanyCode() {
    return insuranceCompanyCode;
  }

  public void setInsuranceCompanyCode(ZenidSharedMinedText insuranceCompanyCode) {
    this.insuranceCompanyCode = insuranceCompanyCode;
  }

  public ZenidSharedMineAllResult issuingCountry(ZenidSharedMinedText issuingCountry) {
    this.issuingCountry = issuingCountry;
    return this;
  }

  /**
   * Get issuingCountry
   * @return issuingCountry
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ZenidSharedMinedText getIssuingCountry() {
    return issuingCountry;
  }

  public void setIssuingCountry(ZenidSharedMinedText issuingCountry) {
    this.issuingCountry = issuingCountry;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ZenidSharedMineAllResult zenidSharedMineAllResult = (ZenidSharedMineAllResult) o;
    return Objects.equals(this.firstName, zenidSharedMineAllResult.firstName) &&
        Objects.equals(this.lastName, zenidSharedMineAllResult.lastName) &&
        Objects.equals(this.address, zenidSharedMineAllResult.address) &&
        Objects.equals(this.birthAddress, zenidSharedMineAllResult.birthAddress) &&
        Objects.equals(this.birthLastName, zenidSharedMineAllResult.birthLastName) &&
        Objects.equals(this.birthNumber, zenidSharedMineAllResult.birthNumber) &&
        Objects.equals(this.birthDate, zenidSharedMineAllResult.birthDate) &&
        Objects.equals(this.expiryDate, zenidSharedMineAllResult.expiryDate) &&
        Objects.equals(this.issueDate, zenidSharedMineAllResult.issueDate) &&
        Objects.equals(this.idcardNumber, zenidSharedMineAllResult.idcardNumber) &&
        Objects.equals(this.drivinglicenseNumber, zenidSharedMineAllResult.drivinglicenseNumber) &&
        Objects.equals(this.passportNumber, zenidSharedMineAllResult.passportNumber) &&
        Objects.equals(this.sex, zenidSharedMineAllResult.sex) &&
        Objects.equals(this.nationality, zenidSharedMineAllResult.nationality) &&
        Objects.equals(this.authority, zenidSharedMineAllResult.authority) &&
        Objects.equals(this.maritalStatus, zenidSharedMineAllResult.maritalStatus) &&
        Objects.equals(this.photo, zenidSharedMineAllResult.photo) &&
        Objects.equals(this.mrz, zenidSharedMineAllResult.mrz) &&
        Objects.equals(this.documentCode, zenidSharedMineAllResult.documentCode) &&
        Objects.equals(this.documentCountry, zenidSharedMineAllResult.documentCountry) &&
        Objects.equals(this.documentRole, zenidSharedMineAllResult.documentRole) &&
        Objects.equals(this.pageCode, zenidSharedMineAllResult.pageCode) &&
        Objects.equals(this.height, zenidSharedMineAllResult.height) &&
        Objects.equals(this.eyesColor, zenidSharedMineAllResult.eyesColor) &&
        Objects.equals(this.carNumber, zenidSharedMineAllResult.carNumber) &&
        Objects.equals(this.firstNameOfParents, zenidSharedMineAllResult.firstNameOfParents) &&
        Objects.equals(this.residencyNumber, zenidSharedMineAllResult.residencyNumber) &&
        Objects.equals(this.residencyNumberPhoto, zenidSharedMineAllResult.residencyNumberPhoto) &&
        Objects.equals(this.residencyPermitDescription, zenidSharedMineAllResult.residencyPermitDescription) &&
        Objects.equals(this.residencyPermitCode, zenidSharedMineAllResult.residencyPermitCode) &&
        Objects.equals(this.gunlicenseNumber, zenidSharedMineAllResult.gunlicenseNumber) &&
        Objects.equals(this.titles, zenidSharedMineAllResult.titles) &&
        Objects.equals(this.titlesAfter, zenidSharedMineAllResult.titlesAfter) &&
        Objects.equals(this.specialRemarks, zenidSharedMineAllResult.specialRemarks) &&
        Objects.equals(this.mothersName, zenidSharedMineAllResult.mothersName) &&
        Objects.equals(this.healthInsuranceCardNumber, zenidSharedMineAllResult.healthInsuranceCardNumber) &&
        Objects.equals(this.insuranceCompanyCode, zenidSharedMineAllResult.insuranceCompanyCode) &&
        Objects.equals(this.issuingCountry, zenidSharedMineAllResult.issuingCountry);
  }

  @Override
  public int hashCode() {
    return Objects.hash(firstName, lastName, address, birthAddress, birthLastName, birthNumber, birthDate, expiryDate, issueDate, idcardNumber, drivinglicenseNumber, passportNumber, sex, nationality, authority, maritalStatus, photo, mrz, documentCode, documentCountry, documentRole, pageCode, height, eyesColor, carNumber, firstNameOfParents, residencyNumber, residencyNumberPhoto, residencyPermitDescription, residencyPermitCode, gunlicenseNumber, titles, titlesAfter, specialRemarks, mothersName, healthInsuranceCardNumber, insuranceCompanyCode, issuingCountry);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidSharedMineAllResult {\n");
    
    sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
    sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
    sb.append("    address: ").append(toIndentedString(address)).append("\n");
    sb.append("    birthAddress: ").append(toIndentedString(birthAddress)).append("\n");
    sb.append("    birthLastName: ").append(toIndentedString(birthLastName)).append("\n");
    sb.append("    birthNumber: ").append(toIndentedString(birthNumber)).append("\n");
    sb.append("    birthDate: ").append(toIndentedString(birthDate)).append("\n");
    sb.append("    expiryDate: ").append(toIndentedString(expiryDate)).append("\n");
    sb.append("    issueDate: ").append(toIndentedString(issueDate)).append("\n");
    sb.append("    idcardNumber: ").append(toIndentedString(idcardNumber)).append("\n");
    sb.append("    drivinglicenseNumber: ").append(toIndentedString(drivinglicenseNumber)).append("\n");
    sb.append("    passportNumber: ").append(toIndentedString(passportNumber)).append("\n");
    sb.append("    sex: ").append(toIndentedString(sex)).append("\n");
    sb.append("    nationality: ").append(toIndentedString(nationality)).append("\n");
    sb.append("    authority: ").append(toIndentedString(authority)).append("\n");
    sb.append("    maritalStatus: ").append(toIndentedString(maritalStatus)).append("\n");
    sb.append("    photo: ").append(toIndentedString(photo)).append("\n");
    sb.append("    mrz: ").append(toIndentedString(mrz)).append("\n");
    sb.append("    documentCode: ").append(toIndentedString(documentCode)).append("\n");
    sb.append("    documentCountry: ").append(toIndentedString(documentCountry)).append("\n");
    sb.append("    documentRole: ").append(toIndentedString(documentRole)).append("\n");
    sb.append("    pageCode: ").append(toIndentedString(pageCode)).append("\n");
    sb.append("    height: ").append(toIndentedString(height)).append("\n");
    sb.append("    eyesColor: ").append(toIndentedString(eyesColor)).append("\n");
    sb.append("    carNumber: ").append(toIndentedString(carNumber)).append("\n");
    sb.append("    firstNameOfParents: ").append(toIndentedString(firstNameOfParents)).append("\n");
    sb.append("    residencyNumber: ").append(toIndentedString(residencyNumber)).append("\n");
    sb.append("    residencyNumberPhoto: ").append(toIndentedString(residencyNumberPhoto)).append("\n");
    sb.append("    residencyPermitDescription: ").append(toIndentedString(residencyPermitDescription)).append("\n");
    sb.append("    residencyPermitCode: ").append(toIndentedString(residencyPermitCode)).append("\n");
    sb.append("    gunlicenseNumber: ").append(toIndentedString(gunlicenseNumber)).append("\n");
    sb.append("    titles: ").append(toIndentedString(titles)).append("\n");
    sb.append("    titlesAfter: ").append(toIndentedString(titlesAfter)).append("\n");
    sb.append("    specialRemarks: ").append(toIndentedString(specialRemarks)).append("\n");
    sb.append("    mothersName: ").append(toIndentedString(mothersName)).append("\n");
    sb.append("    healthInsuranceCardNumber: ").append(toIndentedString(healthInsuranceCardNumber)).append("\n");
    sb.append("    insuranceCompanyCode: ").append(toIndentedString(insuranceCompanyCode)).append("\n");
    sb.append("    issuingCountry: ").append(toIndentedString(issuingCountry)).append("\n");
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

