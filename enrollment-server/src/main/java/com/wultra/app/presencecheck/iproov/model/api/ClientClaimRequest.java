package com.wultra.app.presencecheck.iproov.model.api;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ClientClaimRequest
 */
@Validated


public class ClientClaimRequest   {
  @JsonProperty("api_key")
  private String apiKey = null;

  @JsonProperty("client")
  private String client = null;

  @JsonProperty("resource")
  private String resource = null;

  @JsonProperty("success_url")
  private String successUrl = null;

  @JsonProperty("failure_url")
  private String failureUrl = null;

  @JsonProperty("abort_url")
  private String abortUrl = null;

  public ClientClaimRequest apiKey(String apiKey) {
    this.apiKey = apiKey;
    return this;
  }

  /**
   * The API key of the service provider
   * @return apiKey
  **/
  @ApiModelProperty(required = true, value = "The API key of the service provider")
  @NotNull


  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public ClientClaimRequest client(String client) {
    this.client = client;
    return this;
  }

  /**
   * Fingerprint or client identifier (e.g. User Agent)
   * @return client
  **/
  @ApiModelProperty(required = true, value = "Fingerprint or client identifier (e.g. User Agent)")
  @NotNull


  public String getClient() {
    return client;
  }

  public void setClient(String client) {
    this.client = client;
  }

  public ClientClaimRequest resource(String resource) {
    this.resource = resource;
    return this;
  }

  /**
   * The resource being accessed (e.g. URL)
   * @return resource
  **/
  @ApiModelProperty(required = true, value = "The resource being accessed (e.g. URL)")
  @NotNull


  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public ClientClaimRequest successUrl(String successUrl) {
    this.successUrl = successUrl;
    return this;
  }

  /**
   * The URL to redirect to on success. Note: This field has been deprecated and will be omitted in the next release.
   * @return successUrl
  **/
  @ApiModelProperty(value = "The URL to redirect to on success. Note: This field has been deprecated and will be omitted in the next release.")


  public String getSuccessUrl() {
    return successUrl;
  }

  public void setSuccessUrl(String successUrl) {
    this.successUrl = successUrl;
  }

  public ClientClaimRequest failureUrl(String failureUrl) {
    this.failureUrl = failureUrl;
    return this;
  }

  /**
   * The URL to redirect to on failure. Note: This field has been deprecated and will be omitted in the next release.
   * @return failureUrl
  **/
  @ApiModelProperty(value = "The URL to redirect to on failure. Note: This field has been deprecated and will be omitted in the next release.")


  public String getFailureUrl() {
    return failureUrl;
  }

  public void setFailureUrl(String failureUrl) {
    this.failureUrl = failureUrl;
  }

  public ClientClaimRequest abortUrl(String abortUrl) {
    this.abortUrl = abortUrl;
    return this;
  }

  /**
   * The URL to redirect to on user abort. Note: This field has been deprecated and will be omitted in the next release.
   * @return abortUrl
  **/
  @ApiModelProperty(value = "The URL to redirect to on user abort. Note: This field has been deprecated and will be omitted in the next release.")


  public String getAbortUrl() {
    return abortUrl;
  }

  public void setAbortUrl(String abortUrl) {
    this.abortUrl = abortUrl;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClientClaimRequest clientClaimRequest = (ClientClaimRequest) o;
    return Objects.equals(this.apiKey, clientClaimRequest.apiKey) &&
        Objects.equals(this.client, clientClaimRequest.client) &&
        Objects.equals(this.resource, clientClaimRequest.resource) &&
        Objects.equals(this.successUrl, clientClaimRequest.successUrl) &&
        Objects.equals(this.failureUrl, clientClaimRequest.failureUrl) &&
        Objects.equals(this.abortUrl, clientClaimRequest.abortUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiKey, client, resource, successUrl, failureUrl, abortUrl);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClientClaimRequest {\n");
    
    sb.append("    apiKey: ").append(toIndentedString(apiKey)).append("\n");
    sb.append("    client: ").append(toIndentedString(client)).append("\n");
    sb.append("    resource: ").append(toIndentedString(resource)).append("\n");
    sb.append("    successUrl: ").append(toIndentedString(successUrl)).append("\n");
    sb.append("    failureUrl: ").append(toIndentedString(failureUrl)).append("\n");
    sb.append("    abortUrl: ").append(toIndentedString(abortUrl)).append("\n");
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

