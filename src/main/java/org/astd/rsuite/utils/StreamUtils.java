package org.astd.rsuite.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.astd.rsuite.operation.result.OperationResult;

import com.googlecode.streamflyer.core.Modifier;
import com.googlecode.streamflyer.core.ModifyingReader;
import com.googlecode.streamflyer.regex.fast.FastRegexModifier;
import com.reallysi.rsuite.api.extensions.ExecutionContext;

/**
 * A collection of static, stream-related utility methods.
 */
public class StreamUtils {

  /**
   * Copy the provided input stream to the provided output stream, after removing any RSuite IDs
   * that may be present.
   * 
   * @param inputStream
   * @param outputStream
   * @throws IOException
   */
  public static void copyLessRSuiteIds(InputStream inputStream, OutputStream outputStream)
      throws IOException {

    Reader originalReader = null;
    Reader modifyingReader = null;
    try {
      originalReader = new InputStreamReader(inputStream);

      /*
       * Replace all RSuite ID attributes with a space.
       * 
       * Presumes XML namespace prefixes are only made out of alphanumeric characters and that
       * rsuiteId is only in one namespace.
       * 
       * Helpful regex-debugging and testing site:
       * http://www.regexplanet.com/advanced/java/index.html
       */
      Modifier myModifier = new FastRegexModifier("\\s\\p{Alnum}+:rsuiteId\\s*=\\s*(['\"]).*?\\1",
          0, StringUtils.SPACE);

      // create the modifying reader that wraps the original reader
      modifyingReader = new ModifyingReader(originalReader, myModifier);

      IOUtils.copy(modifyingReader, outputStream);

    } finally {
      IOUtils.closeQuietly(modifyingReader);
      IOUtils.closeQuietly(originalReader);
    }

  }

  /**
   * A debug method to write a byte array output stream to a temp file.
   * 
   * @param context
   * @param filenamePrefix
   * @param filenameExt
   * @param outputStream
   * @param opResult
   * @return the temp file, or null if unable to create the file.
   * @throws IOException
   */
  public static File writeToTempFile(ExecutionContext context, String filenamePrefix,
      String filenameExt, ByteArrayOutputStream outputStream, OperationResult opResult)
      throws IOException {
    File f = File.createTempFile(filenamePrefix, filenameExt, context.getRSuiteServerConfiguration()
        .getTmpDir());
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(f);
      fos.write(outputStream.toByteArray());
      fos.flush();
      opResult.addInfoMessage("Temp file: " + f.getAbsolutePath());
      return f;
    } catch (Exception e) {
      opResult.addWarning(e);
      return null;
    } finally {
      IOUtils.closeQuietly(fos);
    }
  }

  /**
   * A debug method to write a byte array output stream to file while specifying a subdirectory
   * 
   * @param context
   * @param subdir
   * @param filenamePrefix
   * @param filenameExt
   * @param outputStream
   * @param opResult
   * @return the temp file, or null if unable to create the file.
   * @throws IOException
   */
  public static File writeToTempFile(ExecutionContext context, String subdir, String filenamePrefix,
      String filenameExt, ByteArrayOutputStream outputStream, OperationResult opResult)
      throws IOException {
    File subdirPath = new File(context.getRSuiteServerConfiguration().getTmpDir().getAbsoluteFile()
        + File.separator + subdir);
    subdirPath.mkdir();
    File f = File.createTempFile(filenamePrefix, filenameExt, subdirPath);
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(f);
      fos.write(outputStream.toByteArray());
      fos.flush();
      opResult.addInfoMessage("Temp file: " + f.getAbsolutePath());
      return f;
    } catch (Exception e) {
      opResult.addWarning(e);
      return null;
    } finally {
      IOUtils.closeQuietly(fos);
    }
  }

  /**
   * A method for writing input streams to file specifying a subdirectory Also closes the input
   * stream
   * 
   * @param context
   * @param subdir
   * @param filenamePrefix
   * @param filenameExt
   * @param inputStream
   * @param opResult
   * @return the temp file, or null if unable to create the file.
   * @throws IOException
   */
  public static File writeInputToFile(ExecutionContext context, String subdir,
      String filenamePrefix, String filenameExt, InputStream inputStream, OperationResult opResult)
      throws IOException {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    IOUtils.copy(inputStream, outputStream);

    File result = StreamUtils.writeToTempFile(context, subdir, filenamePrefix, filenameExt,
        outputStream, opResult);
    outputStream.close();
    inputStream.close();
    return result;
  }

}
