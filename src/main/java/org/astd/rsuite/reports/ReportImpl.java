package org.astd.rsuite.reports;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import com.reallysi.rsuite.api.report.Report;

public class ReportImpl
    implements Report {

  private String suggestedFilename = "graphical-report.bytestream";
  private String contentType = "application/octet-stream";
  private String encoding = ""; // No encoding for byte streams.
  private String label = "Graphical Report";

  // Multiple object types supported for the content / payload.
  private InputStream inputStream = null;
  private byte[] bytes = new byte[0];

  public ReportImpl() {}

  @Override
  public String getSuggestedFileName() {
    return this.suggestedFilename;
  }

  public InputStream getInputStream() throws IOException {
    if (inputStream != null) {
      return inputStream;
    }
    return new ByteArrayInputStream(bytes);
  }

  public Reader getReader() {
    try {
      return new InputStreamReader(getInputStream(), this.getEncoding());
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public String getContentType() {
    return this.contentType;
  }

  @Override
  public String getEncoding() {
    return this.encoding;
  }

  @Override
  public String getLabel() {
    return this.label;
  }

  public void setSuggestedFilename(String reportFilename) {
    this.suggestedFilename = reportFilename;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public void setContent(byte[] bytes) {
    this.bytes = bytes;
  }

  public void setContent(InputStream inputStream) {
    this.inputStream = inputStream;
  }


}
