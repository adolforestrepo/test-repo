package com.reallysi.rsuite.webservice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.IdAllocationException;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.DefaultRemoteApiHandler;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageDialogResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageType;
import com.reallysi.rsuite.api.system.AntHelper;
import com.reallysi.rsuite.api.xml.DitaOpenToolkit;
import com.reallysi.rsuite.tools.zip.MultithreadedUnzippingController;
import com.reallysi.rsuite.tools.zip.ZipListener;
import com.rsicms.rsuite.helpers.messages.ProcessMessageContainer;
import com.rsicms.rsuite.helpers.messages.impl.ProcessMessageContainerImpl;

/**
 * Provides a Web service for uploading, deploying, and integrating plugins to a
 * DITA Open Toolkit configured to RSuite.
 * 
 */
public class OpenToolkitPluginUpdaterWebService extends DefaultRemoteApiHandler {

    private static Log log = LogFactory
            .getLog(OpenToolkitPluginUpdaterWebService.class);

    public static final String ARG_CONTENT = "CONTENT";

    @Override
    public RemoteApiResult execute(
            RemoteApiExecutionContext context,
            CallArgumentList args) throws RSuiteException {

        log.info("execute(): Starting...");

        String message = "The message";

        List<FileItem> files = args.getFiles("CONTENT");
        MessageType messageType = MessageType.INFO;
        
        if (files.size() == 0) {
            message = "No files uploaded.";
        } else {
            String toolkitId = args.getFirstString(
                    "toolkitId",
                    "default");
            ProcessMessageContainer messages = new ProcessMessageContainerImpl();
            processFiles(
                    context,
                    args,
                    files,
                    toolkitId,
                    messages);
            message = messages.reportMessagesByObjectAsHtml(context
                    .getSession()
                    .getKey());
            messageType = (messages.hasFailures() ? MessageType.FAILURE : MessageType.SUCCESS);
        }

        MessageDialogResult result = new MessageDialogResult(
                messageType,
                "Open Toolkit Plugin Updater",
                message);
        log.info("execute(): Returning notification result.");
        return result;
    }

    /**
     * Process the files to do the updating.
     * 
     * @param context Execution context
     * @param args API arguments
     * @param files
     * @param toolkitId The ID of the Open Toolkit to be updated.
     * @param messages Process message container.
     */
    private void processFiles(
            RemoteApiExecutionContext context,
            CallArgumentList args,
            List<FileItem> files,
            String toolkitId,
            ProcessMessageContainer messages) {

        DitaOpenToolkit toolkit = getOpenToolkit(
                context,
                toolkitId,
                messages);
        if (toolkit == null)
            return; // Messages will have been logged.

        File toolkitDir = toolkit.getDir();
        File pluginsDir = new File(
                toolkitDir,
                "plugins");

        for (FileItem fileItem : files) {
            log.info("processFiles(): Processing file " + fileItem.getName());
            String filename = fileItem.getName();
            String extension = FilenameUtils.getExtension(filename);
            if (!"zip".equalsIgnoreCase(extension)) {
                messages.addWarningMessage(
                        "File Type",
                        filename,
                        "File is not a Zip file");
                continue;
            }

            // NOTE: See
            // http://commons.apache.org/proper/commons-compress/zip.html for
            // a discussion of why ZipFile is to be preferred over
            // ZipInputStream.
            File tempZipFile = null;
            try {
                tempZipFile = File.createTempFile(
                        "opentoolkitpluginupdater_",
                        ".zip");
                IOUtils.copy(
                        fileItem.getInputStream(),
                        new FileOutputStream(
                                tempZipFile));
            } catch (IOException e) {
                log.warn("processFiles(): I/O exception creating temp file: "
                        + e.getMessage());
                messages.addWarningMessage(
                        "Temp File",
                        filename,
                        "I/O exception creating temp file: " + e.getMessage(),
                        e);
                continue;
            }

            // Unzip the file to the plugins/ directory.
            StringBuilder buf = new StringBuilder();
            ZipListener listener = new SimpleZipListener(
                    buf);
            log.info("processFiles(): Unzipping file " + fileItem.getName()
                    + "...");
            MultithreadedUnzippingController controller = new MultithreadedUnzippingController(
                    listener);

            try {
                controller.unzip(
                        tempZipFile,
                        pluginsDir,
                        false);
            } catch (IOException e) {
                log.error("processFiles(): I/O exception unzipping file: "
                        + e.getMessage());
                messages.addWarningMessage(
                        "Unzip",
                        filename,
                        "I/O exception unzipping file: " + e.getMessage(),
                        e);
                continue;
            }
            log.info("processFiles(): File " + fileItem.getName()
                    + " unzipped.");
            log.info("processFiles():   " + buf);
        }

        // Run integrator task.

        runIntegratorTask(
                context,
                toolkitDir,
                messages);

    }

    private void runIntegratorTask(
            RemoteApiExecutionContext context,
            File toolkitDir,
            ProcessMessageContainer messages) {
        AntHelper antHelper = context
                .getConfigurationService()
                .constructAntHelper();

        File buildFile = new File(
                toolkitDir,
                "integrator.xml");
        Properties props = new Properties();
        String reportId;
        try {
            reportId = "ant-task-" + context.getIDGenerator().allocateId()
                    + ".log";
        } catch (IdAllocationException e) {
            reportId = "id-generation-failure-"
                    + Calendar.getInstance().getTimeInMillis();
        }

        try {
            antHelper.setupAndRunAntProject(
                    context,
                    log,
                    buildFile,
                    props,
                    reportId);
        } catch (RSuiteException e) {
            messages.addFailureMessage(
                    "Integrator Task",
                    "integrator.xml",
                    "Exception running integrator.xml: " + e.getMessage());
        }
    }

    /**
     * Get the Open Toolkit and check preconditions for updating it.
     * 
     * @param context Execution context
     * @param toolkitId The ID of the toolkit to get
     * @param messages Message container.
     * @return The toolkit or null if it isn't found or isn't writeable.
     */
    protected DitaOpenToolkit getOpenToolkit(
            RemoteApiExecutionContext context,
            String toolkitId,
            ProcessMessageContainer messages) {
        DitaOpenToolkit toolkit = null;
        try {
            toolkit = context
                    .getXmlApiManager()
                    .getDitaOpenToolkitManager()
                    .getToolkit(
                            toolkitId);
            if (toolkit == null) {
                messages.addFailureMessage(
                        "Open Toolkit Configuration",
                        toolkitId,
                        "Did not find Open Toolkit with ID \"" + toolkitId
                                + "\"");
                return null;
            }
        } catch (RSuiteException e) {
            messages.addFailureMessage(
                    "Open Toolkit Configuration",
                    toolkitId,
                    "Exception getting Open Toolkit from RSuite Open Toolkit manager: "
                            + e.getMessage());
            return null;
        }

        File toolkitDir = toolkit.getDir();
        if (!toolkitDir.exists()) {
            messages.addFailureMessage(
                    "Open Toolkit Configuration",
                    toolkitId,
                    "Open Toolkit directory \"" + toolkitDir.getAbsolutePath()
                            + "\" does not exist.");
            return null;
        }
        if (!toolkitDir.canWrite()) {
            messages.addFailureMessage(
                    "Open Toolkit Configuration",
                    toolkitId,
                    "Cannot write to Open Toolkit directory \""
                            + toolkitDir.getAbsolutePath() + "\".");
            return null;
        }
        return toolkit;
    }

    /**
     * This class is copied from the rsuite-extension-helper project where it's
     * a protected class.
     * 
     */
    class SimpleZipListener implements ZipListener {

        StringBuilder buf;

        public SimpleZipListener(
                StringBuilder buf) {
            this.buf = buf;
        }

        public void directoryCreated(
                ZipEntry entry,
                File dir,
                String relativePath) {
            // We don't do anything here
        }

        public void fileCreated(ZipEntry entry, File file) {
            buf.append("&nbsp;&nbsp;&nbsp;");
            buf.append(entry.getName());
            buf.append(": ");
            buf.append(String.valueOf(entry.getSize()));
            buf.append(" bytes<br/>\n");
        }
    }
}
