package org.astd.rsuite.domain;

import java.io.File;
import java.util.zip.ZipOutputStream;

public class CopiedZipFile {

  private File zipFile;

  private ZipOutputStream zipOutputStream;

  public CopiedZipFile(
      File zipFile, ZipOutputStream zipOutputStream) {
    this.zipFile = zipFile;
    this.zipOutputStream = zipOutputStream;
  }

  /**
   * @return the zipFile
   */
  public File getZipFile() {
    return zipFile;
  }

  /**
   * @return the zipOutputStream
   */
  public ZipOutputStream getZipOutputStream() {
    return zipOutputStream;
  }

}
