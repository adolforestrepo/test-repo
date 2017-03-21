package org.astd.rsuite.domain;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FilenameUtils;

public class InMemoryFile {

  protected InputStream inputStream;

  protected String filename;

  public InMemoryFile(
      InputStream inputStream, String filename)
      throws IOException {
    this.inputStream = inputStream;
    this.filename = filename;
  }

  /**
   * @return input stream
   */
  public InputStream getInputStream() {
    return inputStream;
  }

  /**
   * @return the suggested file name.
   */
  public String getSuggestedFilename() {
    return filename;
  }

  /**
   * @return the suggested file extension.
   */
  public String getSuggestedFileExt() {
    return FilenameUtils.getExtension(filename);
  }

}
