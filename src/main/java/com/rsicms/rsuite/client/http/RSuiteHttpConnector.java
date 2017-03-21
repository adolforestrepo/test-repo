package com.rsicms.rsuite.client.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * A wrapper to <code>HttpConnector</code> containing RSuite specifics.
 */
public class RSuiteHttpConnector
    extends HttpConnector {

  protected String baseRequestPath;
  protected int timeoutInMillis;
  private String sessionKey;



  public RSuiteHttpConnector(
      String host, String port, int timeoutInMillis) {
    // Determine base request path. Stop short of the REST version.
    // TODO: Will we need to support HTTPS?
    StringBuilder buf = new StringBuilder("http://").append(host);

    if (StringUtils.isNotBlank(port)) {
      buf.append(":").append(port);
    }
    buf.append("/rsuite/rest/");
    this.baseRequestPath = buf.toString();

    this.timeoutInMillis = timeoutInMillis;
  }

  // TODO: eventually replace all references to RSuiteHttpConnector constructor with this.
  public RSuiteHttpConnector(
      String host, String port, int timeoutInMillis, boolean useHttps) {
    StringBuilder buf;

    if (useHttps)
      buf = new StringBuilder("https://").append(host);
    else
      buf = new StringBuilder("http://").append(host);

    if (StringUtils.isNotBlank(port)) {
      buf.append(":").append(port);
    }
    buf.append("/rsuite/rest/");
    this.baseRequestPath = buf.toString();

    this.timeoutInMillis = timeoutInMillis;
  }



  /**
   * Constructor for when you already have a session key
   */
  public RSuiteHttpConnector(
      String host, String port, String skey, int timeoutInMillis) {
    this(host, port, timeoutInMillis);
    this.sessionKey = skey;
  }

  public String getSessionKey() {
    return this.sessionKey;
  }

  @Override
  public int getDefaultTimeoutInMillis() {
    return timeoutInMillis;
  }

  @Override
  public String getBaseRequestPath() {
    return baseRequestPath;
  }

  @Override
  public String getAbsoluteRequestPath(String relativeRequestPath) {
    StringBuilder sb = new StringBuilder(getBaseRequestPath()).append(relativeRequestPath);
    if (hasSession()) {
      sb.append(sb.indexOf("?") > -1 ? "&" : "?").append("skey=").append(sessionKey);
    }
    System.out.println(sb.toString());
    return sb.toString();
  }

  /**
   * @return True if instance has a session key. This method doesn't verify the session is valid.
   */
  public boolean hasSession() {
    return StringUtils.isNotBlank(sessionKey);
  }

  public void createSession(String userId, String password) throws Exception {
    Map<String, String> formData = new HashMap<String, String>();
    formData.put("user", userId);
    formData.put("pass", password);

    HttpResponse response = sendPostRequest("v2/user/session", null, formData);

    this.sessionKey = RSuiteHttpResponseParser.parseResponse(response.getResponseText(), "key");
  }

  /**
   * TODO: Do we need to support this method?
   * 
   * @throws IOException
   */
  public void destroySession() throws IOException {
    if (hasSession()) {
      HttpResponse response = sendDeleteRequest("v2/user/session");
      sessionKey = null;
      System.out.println("Response code: " + response.getCode());
      // System.out.println(response.getResponseText());
    } else {
      System.out.println("No session key.");
    }
  }

}
