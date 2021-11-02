package com.wultra.app.docverify.zenid.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.wultra.app.docverify.zenid.model.api.ZenidWebInvestigationIssueResponse;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ZenidWebInvestigationValidatorResponse
 */
@Validated


public class ZenidWebInvestigationValidatorResponse   {
  @JsonProperty("Name")
  private String name = null;

  @JsonProperty("Code")
  private Integer code = null;

  @JsonProperty("Score")
  private Integer score = null;

  @JsonProperty("AcceptScore")
  private Integer acceptScore = null;

  @JsonProperty("Issues")
  @Valid
  private List<ZenidWebInvestigationIssueResponse> issues = null;

  @JsonProperty("Ok")
  private Boolean ok = null;

  public ZenidWebInvestigationValidatorResponse name(String name) {
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

  public ZenidWebInvestigationValidatorResponse code(Integer code) {
    this.code = code;
    return this;
  }

  /**
   * Code identification of validator in external system
   * @return code
  **/
  @ApiModelProperty(value = "Code identification of validator in external system")


  public Integer getCode() {
    return code;
  }

  public void setCode(Integer code) {
    this.code = code;
  }

  public ZenidWebInvestigationValidatorResponse score(Integer score) {
    this.score = score;
    return this;
  }

  /**
   * Score of the validator for given input
   * @return score
  **/
  @ApiModelProperty(value = "Score of the validator for given input")


  public Integer getScore() {
    return score;
  }

  public void setScore(Integer score) {
    this.score = score;
  }

  public ZenidWebInvestigationValidatorResponse acceptScore(Integer acceptScore) {
    this.acceptScore = acceptScore;
    return this;
  }

  /**
   * Accept score - if score is higher than accept score, Validator response OK is set to true
   * @return acceptScore
  **/
  @ApiModelProperty(value = "Accept score - if score is higher than accept score, Validator response OK is set to true")


  public Integer getAcceptScore() {
    return acceptScore;
  }

  public void setAcceptScore(Integer acceptScore) {
    this.acceptScore = acceptScore;
  }

  public ZenidWebInvestigationValidatorResponse issues(List<ZenidWebInvestigationIssueResponse> issues) {
    this.issues = issues;
    return this;
  }

  public ZenidWebInvestigationValidatorResponse addIssuesItem(ZenidWebInvestigationIssueResponse issuesItem) {
    if (this.issues == null) {
      this.issues = new ArrayList<>();
    }
    this.issues.add(issuesItem);
    return this;
  }

  /**
   * Description of the issues of validation (why score is lower)
   * @return issues
  **/
  @ApiModelProperty(value = "Description of the issues of validation (why score is lower)")

  @Valid

  public List<ZenidWebInvestigationIssueResponse> getIssues() {
    return issues;
  }

  public void setIssues(List<ZenidWebInvestigationIssueResponse> issues) {
    this.issues = issues;
  }

  public ZenidWebInvestigationValidatorResponse ok(Boolean ok) {
    this.ok = ok;
    return this;
  }

  /**
   * Get ok
   * @return ok
  **/
  @ApiModelProperty(value = "")


  public Boolean isOk() {
    return ok;
  }

  public void setOk(Boolean ok) {
    this.ok = ok;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ZenidWebInvestigationValidatorResponse zenidWebInvestigationValidatorResponse = (ZenidWebInvestigationValidatorResponse) o;
    return Objects.equals(this.name, zenidWebInvestigationValidatorResponse.name) &&
        Objects.equals(this.code, zenidWebInvestigationValidatorResponse.code) &&
        Objects.equals(this.score, zenidWebInvestigationValidatorResponse.score) &&
        Objects.equals(this.acceptScore, zenidWebInvestigationValidatorResponse.acceptScore) &&
        Objects.equals(this.issues, zenidWebInvestigationValidatorResponse.issues) &&
        Objects.equals(this.ok, zenidWebInvestigationValidatorResponse.ok);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, code, score, acceptScore, issues, ok);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ZenidWebInvestigationValidatorResponse {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    score: ").append(toIndentedString(score)).append("\n");
    sb.append("    acceptScore: ").append(toIndentedString(acceptScore)).append("\n");
    sb.append("    issues: ").append(toIndentedString(issues)).append("\n");
    sb.append("    ok: ").append(toIndentedString(ok)).append("\n");
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

