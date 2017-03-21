package org.astd.rsuite.utils;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;

import com.reallysi.rsuite.api.IdAllocationException;
import com.reallysi.rsuite.api.extensions.ExecutionContext;


/**
 * A collection of static, temporary file/directory related utility methods.
 */
public class TempFileUtils {

  /**
   * Log for the class
   */
  private static Log log = LogFactory.getLog(TempFileUtils.class);

  /**
   * Get a String filepath for a subdirectory in the server's temp directory where the subdirectory
   * name is generated in the same manner as an operation id
   * 
   * @param context
   * @return filePath
   */
  public static String getTempFilePathForWriteOut(ExecutionContext context) {
    String tempFilePath = context.getRSuiteServerConfiguration().getTmpDir() + File.separator;

    try {
      tempFilePath = tempFilePath + context.getIDGenerator().allocateId();
    } catch (IdAllocationException e) {
      log.info(ProjectMessageResource.getMessageText("tempFileUtils.error.allocating.id", e
          .getMessage()));
    }

    return tempFilePath;
  }
}
