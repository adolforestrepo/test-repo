package com.rsicms.rsuite.client.http;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpResponse {

  private int code;

  private String responseText;

  private Map<String, List<String>> responseHeaders = new HashMap<String, List<String>>();

  public HttpResponse(
      int code, String responseText) {
    super();
    this.code = code;
    this.responseText = responseText;
  }

  public HttpResponse(
      int code, String responseText, Map<String, List<String>> responseHeaders) {
    super();
    this.code = code;
    this.responseText = responseText;
    this.responseHeaders = responseHeaders;
  }

  public int getCode() {
    return code;
  }

  public String getResponseText() {
    return responseText;
  }

  public Map<String, List<String>> getResponseHeaders() {
    return responseHeaders;
  }

  @Override
  public String toString() {
    StringBuilder resp = new StringBuilder();
    resp.append("http code: ").append(code).append("\n");
    resp.append("message: ").append(responseText);
    return resp.toString();
  }
}
