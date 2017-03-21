package org.astd.rsuite.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.domain.CopiedZipFile;
import org.astd.rsuite.domain.InMemoryFile;
import org.astd.rsuite.operation.result.ZipOperationResult;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.rsicms.rsuite.helpers.download.ZipHelper;
import com.rsicms.rsuite.helpers.download.ZipHelperConfiguration;
import com.rsicms.rsuite.helpers.utils.ZipUtil;

public class ZipUtils {

  /**
   * Create a zip containing the provided in-memory files, content assembly nodes contents, and the
   * contents of the specified MOs.
   * 
   * @param context
   * @param user
   * @param zipConfig
   * @param inMemoryFileList
   * @param caNodeContainers
   * @param mos
   * @param filename
   * @param log
   * @return The result of the create zip operation, including access to the zip file when
   *         successful. The caller is responsible for checking for failures and warnings.
   * @throws RSuiteException
   * @throws IOException
   */
  public static ZipOperationResult createZip(ExecutionContext context, User user,
      ZipHelperConfiguration zipConfig, List<InMemoryFile> inMemoryFileList,
      List<ContentAssemblyNodeContainer> caNodeContainers, List<ManagedObject> mos, String filename,
      Log log) throws RSuiteException, IOException {
    String opLabel = "create";
    ZipFile zipFile = null;
    FileOutputStream fos = null;
    ZipOutputStream zos = null;

    ZipOperationResult result = new ZipOperationResult(context.getIDGenerator().allocateId(),
        opLabel, log);

    result.markStartOfOperation();

    try {
      File outputFile = new File(context.getRSuiteServerConfiguration().getTmpDir(), filename);
      outputFile.deleteOnExit();

      // Delete any existing temporary file.
      if (outputFile.exists()) {
        outputFile.delete();
      }

      if (!outputFile.createNewFile()) {
        result.addFailure(new RSuiteException(RSuiteException.ERROR_FILE_NOT_FOUND,
            ProjectMessageResource.getMessageText("createZip.error.unable.to.create.zip.file",
                outputFile.getAbsolutePath())));
        return result;
      }

      // Add the MOs, using ZipHelper.
      fos = new FileOutputStream(outputFile, false);

      ZipHelper zipHelper = new ZipHelper(context, zipConfig, fos);
      addManagedObjectsToZip(filename, mos, result, zipHelper, opLabel);

      // Add the CANodeContainers, using ZipHelper.
      addContentAssemblyNodeContainersToZip(filename, caNodeContainers, result, zipHelper, opLabel);
      // Closing stream here. Moved this from addManagedObjectsToZip().
      zipHelper.closeZipOutputStream();
      // Add in-memory files, after re-opening the output stream
      CopiedZipFile copiedZipFile = getOutputStreamForExistingZip(outputFile);
      outputFile = copiedZipFile.getZipFile();
      zos = copiedZipFile.getZipOutputStream();
      addInMemoryFilesToZip(filename, zos, inMemoryFileList, result, opLabel);
      IOUtils.closeQuietly(zos);

      // Set up the zip-specific part of the return.
      result.setZipFile(outputFile);
      zipFile = new ZipFile(outputFile);
      Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();

      while (zipEntries.hasMoreElements()) {
        result.addToZipFileManifest((ZipEntry) zipEntries.nextElement());
      }

    } finally {
      IOUtils.closeQuietly(fos);
      IOUtils.closeQuietly(zos);

      if (zipFile != null) {
        zipFile.close();
      }

      result.markEndOfOperation();
    }

    return result;
  }

  /**
   * Add managed objects to the output stream associated with a zip file.
   * 
   * @param zipFilename
   * @param mos
   * @param result
   * @param zipHelper
   * @param opLabel
   * @throws RSuiteException
   * @throws IOException
   */
  private static void addManagedObjectsToZip(String zipFilename, List<ManagedObject> mos,
      ZipOperationResult result, ZipHelper zipHelper, String opLabel)
      throws RSuiteException, IOException {
    String msg;
    if (mos != null && mos.size() > 0) {
      for (ManagedObject mo : mos) {
        msg = ProjectMessageResource.getMessageText(
            "createZip.info.zipping.contents.of.rsuite.object", MOUtils.getDisplayName(mo), mo
                .getId(), zipFilename);
        result.addInfoMessage(opLabel, msg);

        zipHelper.addManagedObjectToZip("", mo);
      }
    }
  }

  /**
   * Add content assemblies contents to the output stream associated with a zip file.
   * 
   * @param zipFilename
   * @param caNodeContainers
   * @param result
   * @param zipHelper
   * @param opLabel
   * @throws RSuiteException
   * @throws IOException
   */
  private static void addContentAssemblyNodeContainersToZip(String zipFilename,
      List<ContentAssemblyNodeContainer> caNodeContainers, ZipOperationResult result,
      ZipHelper zipHelper, String opLabel) throws RSuiteException, IOException {
    String msg;
    if (caNodeContainers != null && caNodeContainers.size() > 0) {
      for (ContentAssemblyNodeContainer caNodeContainer : caNodeContainers) {
        msg = ProjectMessageResource.getMessageText(
            "createZip.info.zipping.contents.of.rsuite.object", caNodeContainer.getDisplayName(),
            caNodeContainer.getId(), zipFilename);
        result.addInfoMessage(opLabel, msg);

        zipHelper.addCaNodeContentsToZip("", caNodeContainer);
      }
    }
  }

  /**
   * A helper function get the <code>ZipOutputStream</code> of an existing zip file, in order to
   * *add* files to it. This proves to to be less straightforward as it should be.
   * <p>
   * Credit:
   * http://stackoverflow.com/questions/3048669/how-can-i-add-entries-to-an-existing-zip-file
   * -in-java
   * 
   * @param zipFile
   * @since Java 1.7
   * @return <code>CopiedZipFile</code> that provides the <code>ZipOutputStream</code> and
   *         <code>File</code>.
   * @throws IOException
   */
  public static CopiedZipFile getOutputStreamForExistingZip(File zipFile) throws IOException {

    // get a temp file
    File tempFile = File.createTempFile(zipFile.getName(), null);
    // delete it, otherwise you cannot rename your existing zip to it.
    tempFile.delete();

    boolean renameOk = zipFile.renameTo(tempFile);
    if (!renameOk) {
      throw new RuntimeException("could not rename the file " + zipFile.getAbsolutePath() + " to "
          + tempFile.getAbsolutePath());
    }

    ZipInputStream zipInputStream = null;
    ZipOutputStream zipOutputStream = null;

    try {
      zipInputStream = new ZipInputStream(new FileInputStream(tempFile));
      zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));
      ZipEntry entry = zipInputStream.getNextEntry();
      byte[] buf = new byte[1024];
      while (entry != null) {
        String name = entry.getName();
        // Add ZIP entry to output stream.
        zipOutputStream.putNextEntry(new ZipEntry(name));
        // Transfer bytes from the ZIP file to the output file
        int len;
        while ((len = zipInputStream.read(buf)) > 0) {
          zipOutputStream.write(buf, 0, len);
        }
        entry = zipInputStream.getNextEntry();
      }

      return new CopiedZipFile(zipFile, zipOutputStream);

    } finally {
      IOUtils.closeQuietly(zipInputStream);
    }
  }

  /**
   * Add in-memory files to the output stream associated with a zip file.
   * 
   * @param zipFilename
   * @param zipOutputStream
   * @param inMemoryFileList
   * @param result
   * @param opLabel
   * @throws IOException
   */
  private static void addInMemoryFilesToZip(String zipFilename, ZipOutputStream zipOutputStream,
      List<InMemoryFile> inMemoryFileList, ZipOperationResult result, String opLabel)
      throws IOException {

    String msg;

    if (inMemoryFileList != null && inMemoryFileList.size() > 0) {
      InputStream inputStream;
      for (InMemoryFile inMemoryFile : inMemoryFileList) {
        msg = ProjectMessageResource.getMessageText(
            "createZip.info.zipping.contents.of.non.rsuite.object", inMemoryFile
                .getSuggestedFilename(), zipFilename);
        result.addInfoMessage(opLabel, msg);

        inputStream = inMemoryFile.getInputStream();
        ZipUtil.addStreamToZip(inMemoryFile.getSuggestedFilename(), inputStream, zipOutputStream);
        IOUtils.closeQuietly(inputStream);
      }
    }
  }

}
