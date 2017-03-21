package com.rsicms.rsuite.client.http;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Convenience wrapper enabling one to submit HTTP requests. Sub-classes may specialize. For
 * example, a sub-class may elect to override {@link #getBaseRequestPath()} in order to alleviate
 * callers from providing absolute request paths with every call. This class may need to be extended
 * to expose additional functionality.
 */
public class HttpConnector {

  private static final String crlf = "\r\n";
  private static final String twoHyphens = "--";
  private static final String boundary = "*****";
  private static final String charset = "UTF-8";
  private static final int DEFAULT_TIMEOUT_IN_MILLIS = 100000;

  public int getDefaultTimeoutInMillis() {
    return DEFAULT_TIMEOUT_IN_MILLIS;
  }

  /**
   * Sub-classes to override as necessary.
   * 
   * @return The base request path to use with relative request paths.
   */
  public String getBaseRequestPath() {
    return StringUtils.EMPTY;
  }

  /**
   * Find out if an absolute request path is expected.
   * 
   * @return True if {@link #getBaseRequestPath()} returns a blank value (null or empty).
   */
  public boolean isAbsoluteRequestPathExpected() {
    return StringUtils.isBlank(getBaseRequestPath());
  }

  /**
   * @see #getBaseRequestPath()
   * @param relativeRequestPath
   * @return The absolute request path.
   */
  public String getAbsoluteRequestPath(String relativeRequestPath) {
    // Make more sophisticated once necessary.
    return getBaseRequestPath().concat("/").concat(relativeRequestPath);
  }

  /**
   * Send a GET request.
   * 
   * @param requestPath Relative or absolute request path, depending on the sub-class. Check
   *        {@link #isAbsoluteRequestPathExpected()}. The sub-class may also support including query
   *        parameters.
   * @param requestProperties The equivalent of request headers. Defaults for some headers are
   *        provided herein.
   * @return The request's response.
   * @throws Exception
   */
  public HttpResponse sendGetRequest(String requestPath, Map<String, String> requestProperties)
      throws Exception {

    String response = null;
    int code = -1;

    HttpURLConnection hyperConn = null;
    Map<String, List<String>> responseHeaders = null;

    InputStream is = null;
    try {
      URL connUrl = new URL(getAbsoluteRequestPath(requestPath));

      if ("https".equals(connUrl.getProtocol())) {
        hyperConn = (HttpsURLConnection) connUrl.openConnection();
      } else {
        hyperConn = (HttpURLConnection) connUrl.openConnection();
      }

      hyperConn.setDoOutput(true);

      // Pass on request properties provided by the caller.
      if (requestProperties != null) {
        for (Map.Entry<String, String> prop : requestProperties.entrySet()) {
          hyperConn.setRequestProperty(prop.getKey(), prop.getValue());
        }
      }

      hyperConn.setRequestMethod("GET");
      hyperConn.connect();
      hyperConn.setReadTimeout(getDefaultTimeoutInMillis());

      code = hyperConn.getResponseCode();

      if (code >= 200 && code < 300) {
        is = hyperConn.getInputStream();
        response = IOUtils.toString(is);
      }
      responseHeaders = hyperConn.getHeaderFields();

    } catch (Exception e) {
      throw new Exception("Failed get request " + requestPath, e);
    } finally {
      IOUtils.closeQuietly(is);
      if (hyperConn != null) {
        hyperConn.disconnect();
      }
    }

    return new HttpResponse(code, response, responseHeaders);
  }

  /**
   * Sends a POST request configured as multipart/form-data with a boundary. This implementation
   * results in custom RSuite web services receiving a FileItem, unlike
   * {@link #sendRequest(String, Map, Map, String)}.
   * <p>
   * This is an adaptation of
   * http://stackoverflow.com/questions/11766878/sending-files-using-post-with-httpurlconnection
   * 
   * @param requestPath Relative or absolute request path, depending on the sub-class. Check
   *        {@link #isAbsoluteRequestPathExpected()}. The sub-class may also support including query
   *        parameters.
   * @param requestProperties The equivalent of request headers. Defaults for some headers are
   *        provided herein.
   * @param attachmentName The name of the attachment the request will use.
   * @param attachmentFilename The filename of the attachment the request will use.
   * @param fileToAttach The file to attach.
   * @return The request's response.
   * @throws IOException
   */
  public HttpResponse sendPostRequest(String requestPath, Map<String, String> requestProperties,
      String attachmentName, String attachmentFilename, File fileToAttach) throws IOException {

    FileInputStream fis = null;
    BufferedReader responseStreamReader = null;
    InputStream responseStream = null;
    DataOutputStream dos = null;
    HttpURLConnection hyperConn = null;
    try {
      String url = getAbsoluteRequestPath(requestPath);

      URL connUrl = new URL(url);

      if ("https".equals(connUrl.getProtocol())) {
        hyperConn = (HttpsURLConnection) connUrl.openConnection();
      } else {
        hyperConn = (HttpURLConnection) connUrl.openConnection();
      }

      hyperConn.setUseCaches(false);
      hyperConn.setDoOutput(true);

      hyperConn.setRequestMethod("POST");
      hyperConn.setRequestProperty("Connection", "Keep-Alive");
      hyperConn.setRequestProperty("Cache-Control", "no-cache");
      hyperConn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
      // Apply any request props provided by caller
      if (requestProperties != null) {
        for (Map.Entry<String, String> prop : requestProperties.entrySet()) {
          System.out.println("Adding request property \"" + prop.getKey() + "\" with value \""
              + prop.getValue() + "\"");
          hyperConn.setRequestProperty(prop.getKey(), prop.getValue());
        }
      }

      dos = new DataOutputStream(hyperConn.getOutputStream());

      // Start content wrapper
      dos.writeBytes(twoHyphens + boundary + crlf);
      dos.writeBytes("Content-Disposition: form-data; name=\"" + attachmentName + "\";filename=\""
          + attachmentFilename + "\"" + crlf);
      dos.writeBytes(crlf);

      // Add file
      fis = new FileInputStream(fileToAttach);
      dos.write(IOUtils.toByteArray(fis));
      IOUtils.closeQuietly(fis);

      // End content wrapper.
      dos.writeBytes(crlf);
      dos.writeBytes(twoHyphens + boundary + twoHyphens + crlf);

      // Flush the output buffer
      dos.flush();
      IOUtils.closeQuietly(dos);

      // Get response
      responseStream = new BufferedInputStream(hyperConn.getInputStream());

      responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));

      String line = StringUtils.EMPTY;
      StringBuilder stringBuilder = new StringBuilder();

      while ((line = responseStreamReader.readLine()) != null) {
        stringBuilder.append(line).append("\n");
      }
      IOUtils.closeQuietly(responseStreamReader);
      IOUtils.closeQuietly(responseStream);

      System.out.println(hyperConn.getResponseCode() + " from " + url);
      HttpResponse httpResponse = new HttpResponse(hyperConn.getResponseCode(), stringBuilder
          .toString());

      return httpResponse;

    } finally {
      // Insurance
      IOUtils.closeQuietly(dos);
      IOUtils.closeQuietly(responseStreamReader);
      IOUtils.closeQuietly(responseStream);
      IOUtils.closeQuietly(fis);
      if (hyperConn != null) {
        hyperConn.disconnect();
      }
    }
  }

  /**
   * Send a post request that adds form data (if provided) to the request's output stream.
   * 
   * @see #sendPostRequest(String, Map, Map)
   * @param requestPath Relative or absolute request path, depending on the sub-class. Check
   *        {@link #isAbsoluteRequestPathExpected()}. The sub-class may also support including query
   *        parameters.
   * @param requestProperties The equivalent of request headers. Defaults for some headers are
   *        provided herein.
   * @param formData Optional. Ignored if type is DELETE. For delete requests, see
   *        {@link #sendDeleteRequest(String)}.
   * @return The request's response.
   * @throws IOException
   */
  public HttpResponse sendPostRequest(String requestPath, Map<String, String> requestProperties,
      Map<String, String> formData) throws IOException {
    return sendRequest(requestPath, requestProperties, formData, "POST");
  }

  /**
   * Send a request where form data (if provided) is added to the request's output stream. This
   * works for RSuite authentication parameters, but not for RSuite web services expecting an
   * instance of FileItem. For the latter, see
   * {@link #sendPostRequest(String, Map, String, String, File)}.
   * 
   * @param requestPath Relative or absolute request path, depending on the sub-class. Check
   *        {@link #isAbsoluteRequestPathExpected()}. The sub-class may also support including query
   *        parameters.
   * @param requestProperties The equivalent of request headers. Defaults for some headers are
   *        provided herein.
   * @param formData Optional. Ignored if type is DELETE. For delete requests, see
   *        {@link #sendDeleteRequest(String)}.
   * @param type The HTTP method.
   * @return The request's response.
   * @throws IOException
   */
  private HttpResponse sendRequest(String requestPath, Map<String, String> requestProperties,
      Map<String, String> formData, String type) throws IOException {

    String url = getAbsoluteRequestPath(requestPath);

    HttpResponse httpResponse = null;
    HttpURLConnection hyperConn = null;
    OutputStream output = null;
    InputStream response = null;
    try {

      URL connUrl = new URL(url);

      if ("https".equals(connUrl.getProtocol())) {
        hyperConn = (HttpsURLConnection) connUrl.openConnection();
      } else {
        hyperConn = (HttpURLConnection) connUrl.openConnection();
      }

      hyperConn.setReadTimeout(getDefaultTimeoutInMillis());
      hyperConn.setDoOutput(true);
      hyperConn.setRequestMethod(type);

      // Apply defaults first.
      hyperConn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml");
      hyperConn.setRequestProperty("Accept-Charset", "utf-8;q=0.7,*;q=0.7");
      hyperConn.setRequestProperty("Connection", "keep-alive");
      hyperConn.setRequestProperty("Content-Type",
          "application/x-www-form-urlencoded; charset=utf-8");

      // Apply any request props provided by caller
      if (requestProperties != null) {
        for (Map.Entry<String, String> prop : requestProperties.entrySet()) {
          hyperConn.setRequestProperty(prop.getKey(), prop.getValue());
        }
      }

      if (!"delete".equalsIgnoreCase(type)) {
        String requestData = StringUtils.EMPTY;
        if (formData != null) {
          for (Entry<String, String> entry : formData.entrySet()) {
            requestData += entry.getKey().concat("=").concat(URLEncoder.encode(entry.getValue(),
                charset)).concat("&");
          }
        }

        output = hyperConn.getOutputStream();
        output.write(requestData.getBytes(charset));
        IOUtils.closeQuietly(output);
      }

      response = hyperConn.getInputStream();
      System.out.println(hyperConn.getResponseCode() + " from " + url);
      httpResponse = new HttpResponse(hyperConn.getResponseCode(), IOUtils.toString(response,
          charset));

    } finally {
      IOUtils.closeQuietly(output);
      IOUtils.closeQuietly(response);
      if (hyperConn != null) {
        hyperConn.disconnect();
      }
    }

    return httpResponse;

  }

  /**
   * Send a delete request.
   * 
   * @param requestPath Relative or absolute request path, depending on the sub-class. Check
   *        {@link #isAbsoluteRequestPathExpected()}. The sub-class may also support including query
   *        parameters.
   * @return The request's response.
   * @throws IOException
   */
  public HttpResponse sendDeleteRequest(String requestPath) throws IOException {

    HttpURLConnection hyperConn = null;
    HttpResponse httpResponse = null;
    try {
      URL connUrl = new URL(getAbsoluteRequestPath(requestPath));

      if ("https".equals(connUrl.getProtocol())) {
        hyperConn = (HttpsURLConnection) connUrl.openConnection();
      } else {
        hyperConn = (HttpURLConnection) connUrl.openConnection();
      }

      hyperConn.setReadTimeout(getDefaultTimeoutInMillis());
      hyperConn.setDoOutput(true);
      hyperConn.setRequestMethod("DELETE");
      hyperConn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml");

      hyperConn.connect();

      httpResponse = new HttpResponse(hyperConn.getResponseCode(), IOUtils.toString(hyperConn
          .getInputStream(), charset));

    } finally {
      if (hyperConn != null) {
        hyperConn.disconnect();
      }
    }

    return httpResponse;
  }

}
