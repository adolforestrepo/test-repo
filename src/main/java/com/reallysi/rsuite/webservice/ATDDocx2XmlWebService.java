package com.reallysi.rsuite.webservice;


import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.transform.Transformer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.DefaultRemoteApiHandler;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageDialogResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageType;
import com.reallysi.rsuite.api.report.StoredReport;
import com.reallysi.rsuite.api.xml.DitaOpenToolkit;
import com.reallysi.tools.dita.BosConstructionOptions;
import com.reallysi.tools.dita.BosMember;
import com.reallysi.tools.dita.BosVisitor;
import com.reallysi.tools.dita.BoundedObjectSet;
import com.reallysi.tools.dita.BrowseTreeConstructingBosVisitor;
import com.reallysi.tools.dita.DitaMapImportOptions;
import com.reallysi.tools.dita.ATDDitaSupportRSuiteUtils;
import com.reallysi.tools.dita.DitaUtil;
import com.reallysi.tools.dita.DomUtil;
import com.reallysi.tools.dita.RSuiteDitaHelper;
import com.reallysi.tools.dita.conversion.Docx2XmlOptions;
import com.reallysi.tools.dita.conversion.InxGenerationOptions;
import com.rsicms.rsuite.helpers.messages.ProcessMessageContainer;
import com.rsicms.rsuite.helpers.messages.impl.ProcessMessageContainerImpl;
import com.rsicms.rsuite.helpers.messages.visitors.HtmlFormattingProcessMessageVisitor;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;

/**
 * Takes a publication content assembly (book or periodical) and transforms all
 * the DOCX files into XML files and then attempts to generate InDesign from the
 * XML files. A lot of this code is copied from the DocxToXmlWebService and MapImporterActionHandler
 * classes in the rsuite-dita-support project. At some point this same functionality should
 * be migrated back to the general project since it's not specific to SMP business logic.
 * 
 * 
 */
public class ATDDocx2XmlWebService extends
        DefaultRemoteApiHandler {

	// FIXME: These constants are in anticipation of future code to 
	// allow dynamic configuration of the browse-tree-constructing 
	// BOS visitor used to load the imported map to the browse tree.
    public static final String DEFAULT_ARG_BROWSE_CONSTRUCTING_BOS_VISITOR_CLASSNAME = "com.reallysi.tools.dita.BrowseTreeConstructingBosVisitor";
	public static final String ARG_BROWSE_CONSTRUCTING_BOS_VISTOR_CLASSNAME = "browseConstructingBosVistorClassname";



	public class StringLogger implements
            Log {

        private PrintWriter printWriter;

        public StringLogger(Writer logWriter) {
            this.printWriter = new PrintWriter(logWriter);
        }

        @Override
        public
                void debug(
                        Object arg0) {
            printWriter.write("[DEBUG] ");
            printWriter.write(arg0.toString());
            printWriter.write("\n");
        }

        @Override
        public
                void debug(
                        Object arg0,
                        Throwable arg1) {
            printWriter.write("[DEBUG] ");
            printWriter.write(arg0.toString());
            printWriter.write("\n");

        }

        @Override
        public
                void error(
                        Object arg0) {
            printWriter.write("[ERROR] ");
            printWriter.write(arg0.toString());
            printWriter.write("\n");

        }

        @Override
        public
                void error(
                        Object arg0,
                        Throwable arg1) {
            printWriter.write("[ERROR] ");
            printWriter.write(arg0.toString());
            printWriter.write("\n");

        }

        @Override
        public
                void fatal(
                        Object arg0) {
            printWriter.write("[FATAL] ");
            printWriter.write(arg0.toString());
            printWriter.write("\n");

        }

        @Override
        public
                void fatal(
                        Object arg0,
                        Throwable arg1) {
            printWriter.write("[FATAL] ");
            printWriter.write(arg0.toString());
            printWriter.write("\n");

        }

        @Override
        public
                void info(
                        Object arg0) {
            printWriter.write("[INFO] ");
            printWriter.write(arg0.toString());
            printWriter.write("\n");

        }

        @Override
        public
                void info(
                        Object arg0,
                        Throwable arg1) {
            printWriter.write("[INFO] ");
            printWriter.write(arg0.toString());
            printWriter.write("\n");

        }

        @Override
        public
                boolean
                isDebugEnabled() {
            return true;
        }

        @Override
        public
                boolean
                isErrorEnabled() {
            return true;
        }

        @Override
        public
                boolean
                isFatalEnabled() {
            return true;
        }

        @Override
        public
                boolean isInfoEnabled() {
            return true;
        }

        @Override
        public
                boolean
                isTraceEnabled() {
            return true;
        }

        @Override
        public
                boolean isWarnEnabled() {
            return true;
        }

        @Override
        public
                void trace(
                        Object arg0) {
            printWriter.write("[TRACE] ");
            printWriter.write(arg0.toString());
            printWriter.write("\n");

        }

        @Override
        public
                void trace(
                        Object arg0,
                        Throwable arg1) {
            printWriter.write("[TRACE] ");
            printWriter.write(arg0.toString());
            printWriter.write("\n");

        }

        @Override
        public
                void warn(
                        Object arg0) {
            printWriter.write("[WARN] ");
            printWriter.write(arg0.toString());
            printWriter.write("\n");

        }

        @Override
        public
                void warn(
                        Object arg0,
                        Throwable arg1) {
            printWriter.write("[WARN] ");
            printWriter.write(arg0.toString());
            printWriter.write("\n");

        }

    }

    private static final String WEB_SERVICE_TITLE = "Word to XML";
    private static Log log = LogFactory.getLog(ATDDocx2XmlWebService.class);
    public static final String DATE_FORMAT_STRING = "yyyyMMdd-HHmmss";
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_STRING);

    @Override
    public
            RemoteApiResult
            execute(
                    RemoteApiExecutionContext context,
                    CallArgumentList args) throws RSuiteException {
        log.info("execute(): Starting...");


        User user = context.getAuthorizationService()
                .findUser(context.getPrincipal()
                        .getName());

        ManagedObject mo = args.getFirstManagedObject(user);
        if (mo == null) {
            return new MessageDialogResult(
                    MessageType.ERROR,
                    WEB_SERVICE_TITLE,
                    "No managed object provided to Web service.");

        }
        
        String targetId = mo.getTargetId();
        if (targetId != null) {
            mo = context.getManagedObjectService()
                    .getManagedObject(user,
                            targetId);
        }
        log.info("execute(): Effective MO: "
                + RSuiteUtils.formatMoId(mo));

        InxGenerationOptions generationOptions = new InxGenerationOptions();

        List<File> generatedXmlFiles = new ArrayList<File>();

        StringWriter conReportWriter = new StringWriter();
        boolean XML_GENERATION_SUCCEEDED =  
                doXmlGeneration(context,
                                user,
                                mo,
                                generationOptions,
                                generatedXmlFiles,
                                conReportWriter);
        
        RemoteApiResult result = null;
        
        MessageType msgType = MessageType.ERROR; // Assume error

        HtmlFormattingProcessMessageVisitor visitor = 
                new HtmlFormattingProcessMessageVisitor(new PrintWriter(conReportWriter));

        if (XML_GENERATION_SUCCEEDED) {
            // For each generated XML file, if it's a map, import it
            
            ContentAssemblyNodeContainer rootContainer = 
                    RSuiteUtils.getContentAssemblyNodeContainer(
                            context, 
                            user, 
                            mo.getId());
            
            ProcessMessageContainer messages = new ProcessMessageContainerImpl();
            conReportWriter.write("\n<h2>Importing generated maps</h2>\n");
            for (File xmlFile : generatedXmlFiles) {
                try {
                    Document doc = context.getXmlApiManager()
                            .getW3CDomFromFile(xmlFile, false);
		    log.info("THE_XML_FILE: " + doc);
                    List<ManagedObject> importedMaps = new ArrayList<ManagedObject>();
                    if (DitaUtil.isDitaMap(doc.getDocumentElement())) {
                        importDitaMap(
                                context, 
                                user, 
                                xmlFile,
                                doc, 
                                rootContainer,
                                messages,
                                importedMaps);
                    }
                } catch (Exception e) {
                    messages.addFailureMessage(
                            e.getClass().getSimpleName(), 
                            xmlFile.getName(), 
                            e.getMessage(), 
                            e);
                }
                try {
                    visitor.visit(messages);
                } catch (Exception e) {
                    conReportWriter.write(e.getClass().getSimpleName() + " processing messages: " + e.getMessage());
                }
            }
            if (!messages.hasFailures()) {
                msgType = MessageType.SUCCESS;
            }
        } else {
            // Report any failure messages.
            try {
                visitor.visit(generationOptions);
            } catch (Exception e) {
                conReportWriter.write("Unexpected exception " + e.getClass().getSimpleName() + 
                        " formatting generation messages: " + e.getMessage());
            }
        }
        result = new MessageDialogResult(
                msgType,
                WEB_SERVICE_TITLE, 
                conReportWriter.toString(),
                "800");


        return result;
    }

    /**
     * Import the DITA map using the map importer from rsuite-dita-support.
     * The business logic is copied largely from the MapImporterActionHandler class.
     * @param context
     * @param user
     * @param doc
     * @param rootCaNode The CA to import the map into.
     * @param messages
     * @throws RSuiteException 
     */
    protected
            void
            importDitaMap(
                    RemoteApiExecutionContext context,
                    User user,
                    File mapFile,
                    Document doc,
                    ContentAssemblyNodeContainer rootCaNode,
                    ProcessMessageContainer messages,
                    List<ManagedObject> importedMaps) throws RSuiteException {
        
        // Capture the import log so we can write out as a stored report.
        StringWriter logWriter = new StringWriter();
        
        Log importLog = new StringLogger(logWriter);

        String[] catalogs = getCatalogs(context, user, messages);
        if (catalogs == null) return; // Message will have been logged
        
        StringBuilder validationReport = new StringBuilder("Map import validation report.\n\n")
        .append(" + [INFO] Root map document: " + mapFile.getAbsolutePath())
        .append("\n\n");
        
        BosConstructionOptions domOptions = new BosConstructionOptions(log, validationReport, catalogs);
        
        Transformer validationReportTransform = null;
        try {
            String port = context.getRSuiteServerConfiguration().getPort();
            String host = context.getRSuiteServerConfiguration().getHostName();
            validationReportTransform = context.getXmlApiManager().
                    getTransformer(new URL("http://" + host + ":" + port + 
                    		"/rsuite/rest/v1/static/astd-plugin/xslt/validation-report/validation-report.xsl"));
        } catch (Exception e) {
            messages.addFailureMessage(
                    "Load Map to Browse Tree", 
                    "Get transformer", 
                    e.getMessage(), 
                    e);
            return;
        }
        domOptions.setReportSerializationTransform(validationReportTransform);
        
        try {
            log.info("Parsing map file \"" + mapFile.getAbsolutePath() + "\"...");
            doc = DomUtil.getDomForDocument(mapFile, domOptions, true);
        } catch (Exception e) {
            StoredReport report = null;
            try {
                report = captureValidationReport(context, mapFile, validationReport);
                messages.addFailureMessage("Map Validation", mapFile.getName(), e.getMessage(), report);
            } catch (RSuiteException e1) {
                log.error("Exception capturing validation report: " + e.getMessage(), e);
            }
        }
        
        BosMember rootMember = null; // Will hold the BOS member for the root map
        
        // We shouldn't even be in this method if the doc is not a map,
        // but this is a double check.
        if (DitaUtil.isDitaMap(doc.getDocumentElement())) {
            try {
                DitaMapImportOptions importOptions = new DitaMapImportOptions();
                
                importOptions.setRootCa(rootCaNode);
                importOptions.setMissingGraphicUri("/astd-plugin/xslt/images/missingGraphic");
                importOptions.setTopicContainerName("content");
                importOptions.setNonXmlContainerName("media");
                importOptions.setUser(user);

                BoundedObjectSet bos = null;
                try {
                     bos = RSuiteDitaHelper.importMap(
                                     context, importLog, user, doc, domOptions, importOptions);
                } catch (Exception e) {     
                    messages.addFailureMessage("Map Import", mapFile.getName(), e.getMessage(), e);
                }
                
                if (bos != null && bos.hasInvalidMembers()) {
                    messages.addFailureMessage(
                            "Map Import", mapFile.getName(), "One or more BOS members failed validation.");
                    // FIXME: List failed docs?
                }
                
                ManagedObject ca = null;
                if (!messages.hasFailures()) {
	                BosVisitor visitor = new BrowseTreeConstructingBosVisitor(
	                                context, user, importOptions, importLog);
	                
	                // The MO in this context is the CA into which the map is loaded.
	                importOptions.setCreateCaNodeForRootMap(false); // Create root map in specified CA node (e.g., "xml").
	                try {
	                    ca = RSuiteDitaHelper.loadMapToBrowseTree(context, bos, importOptions, visitor);
	                    log.info("Map imported");
	                } catch (Exception e) {
	                    messages.addFailureMessage(
	                            "Load Map to Browse Tree", mapFile.getName(), e.getMessage(), e);
	                    log.error("Exception loading map to browse tree");
	                }
                }
                StoredReport importReport = captureImportReport(context, mapFile, logWriter.toString());
                if (messages.hasFailures()) {
                    messages.addFailureMessage(
                            "Import Map", mapFile.getName(), 
                            "Map imported failed in some way. See stored report for details", importReport);
                } else {
                    messages.addInfoMessage(
                            "Import Map", mapFile.getName(), 
                            "Map imported successfully. See stored report for details", importReport);
                    rootMember = bos.getRoot();
                    if (rootMember == null)
                        throw new RSuiteException("Failed to get root member for BOS following apparently-successful BOS import. This should not happen.");
                    importedMaps.add(ca);
                }
            } catch (Exception e) {
                log.error(e);
                messages.addFailureMessage("Import map", mapFile.getName(), e.getMessage(), e);
            }
        } else {
            log.info("File " + mapFile.getName() + " does not appear to be a DITA map, skipping.");
        }
    }

    protected
            String[]
            getCatalogs(
                    RemoteApiExecutionContext context,
                    User user, 
                    ProcessMessageContainer messages) throws RSuiteException {
        DitaOpenToolkit toolkit = context.getXmlApiManager().getDitaOpenToolkitManager().getToolkit("v185");
        if (toolkit == null) {
            String msg = "No default DITA Open Toolkit provided by Open Toolkit Manager. Cannot continue.";
            messages.addFailureMessage("No DITA Open Toolkit", "default", msg);
            return null;
        }
        
        log.info("Got an Open Toolkit, located at \"" + toolkit.getPath() + "\"");
        
        String catalogPath = toolkit.getCatalogPath();
        File catalogFile = new File(catalogPath);
        if (!catalogFile.exists()) {
            String msg = "Cannot find catalog file " + catalogFile.getAbsolutePath();
            messages.addFailureMessage("No entity catalog file", catalogFile.getName(), msg);
            return null;
        }
        
        String catalogUrlStr = null;
        try {
            catalogUrlStr = catalogFile.toURI().toURL().toExternalForm();
        } catch (MalformedURLException e) {
            // Should never happen since we're going from a file.
        }
        String[] catalogs = new String[1];
        catalogs[0] = catalogUrlStr;
        
        log.info("Using Toolkit catalog \"" + catalogUrlStr + "\"");
        return catalogs;
    }

    /**
     * Runs the Word-to-XML transform against a content assembly, converting
     * any Word documents it contains into XML. Any messages and completion status
     * are captured on the generationOptions object.
     * @param context Execution context 
     * @param user The user who submitted the Web service
     * @param mo The content assembly to be processed
     * @param generationOptions The options that control the conversion and 
     * capture any result messages.
     * @param logWriter writer to write the DOCX2XML log to
     * @return false if there were any failures, otherwise true, indicating success
     */
    protected
            boolean
            doXmlGeneration(
                    RemoteApiExecutionContext context,
                    User user,
                    ManagedObject mo,
                    InxGenerationOptions generationOptions,
                    List<File> generatedXmlFiles,
                    Writer logWriter
                    ) {

        PrintWriter writer = new PrintWriter(
                logWriter);
        
        // Make sure the user is set so it will be set on the 
        // Docx2XmlOptions.
        generationOptions.setUser(user);
        generationOptions.setMessageWriter(writer);
        generationOptions.setIsBuildInDesignDoc(false);
        generationOptions.setSession(context.getSession());

        Docx2XmlOptions docxOptions = new Docx2XmlOptions(generationOptions);
        try {
            // Verify that a content assembly was selected
            ContentAssemblyNodeContainer ca = RSuiteUtils.getContentAssemblyNodeContainer(context,
                    user,
                    mo.getId());
            if (ca == null) {
                throw new RSuiteException(
                        0,
                        "Failed to get a content assembly for ID ["
                                + mo.getId()
                                + "]");
            } else {
            	docxOptions.setXsltParameter("containerId", ca.getId());
            	generationOptions.setLinksDirName("links");

                File outputDir = new File(
                        context.getRSuiteServerConfiguration()
                                .getTmpDir(),
                        "transformXmlMoToInCopy_"
                                + mo.getId());
                outputDir.mkdirs();

                String basePath = outputDir.getAbsolutePath();
                generationOptions.setLoadXmlToRSuite(false);
                writer.println("<pre>\n");
                generationOptions.setMessageWriter(writer);

                try {
                    outputDir = ATDDitaSupportRSuiteUtils.generateXmlFromContentAssembly(context,
                            basePath,
                            ca.getDisplayName(),
                            docxOptions,
                            ca,
                            generatedXmlFiles);
                    if (docxOptions.hasFailures()) {
                        log.error("transform: Failures reported.");
                    } else {
                        log.info("transform: success");
                    }                    
                } catch (Exception e) {
                    docxOptions.addFailureMessage(e.getClass()
                            .getSimpleName(),
                            ca.getDisplayName(),
                            "Unexpected exception generating XML: "
                                    + e.getMessage(),
                            e);
                    // To make sure we report the failure.
                    generationOptions.addAll(docxOptions);
                }
                writer.println("</pre>\n");
                if (outputDir != null) {
                    writer.write(" + [INFO] Generated output can be found on server file system in \""
                            + outputDir.getAbsolutePath()
                            + "\"");
                }
            }

        } catch (Exception e) {
            docxOptions.addFailureMessage(
                    e.getClass().getSimpleName(), 
                    mo.getId(), 
                    e.getMessage(),
                    e);
            // Copy the failure messages into the generation options
            // because we never had an opportunity to report on them otherwise.
            generationOptions.addAll(docxOptions);
        }

        writer.write("</div>");

        writer.flush();
        writer.close();
        return !(docxOptions.hasFailures());
    }

    protected StoredReport captureValidationReport(
            ExecutionContext context,
            File bosSourceFile, 
            StringBuilder validationReport) throws RSuiteException {
            // Capture validation report
            String fileNameBase = FilenameUtils.getBaseName(bosSourceFile.getName());
        
            String reportFileName = fileNameBase + "_validation_at_" + getNowString() + ".txt";
        
            log.info("Report file name is \"" + reportFileName + "\"");
        
            StoredReport report = context.getReportManager().saveReport(reportFileName, validationReport.toString());
            log.info("Report ID name is \"" + report.getId() + "\"");
            return report;
    }
    
    protected
    StoredReport
    captureImportReport(
            RemoteApiExecutionContext context,
            File mapFile,
            String logStr) throws RSuiteException {
        String fileNameBase = FilenameUtils.getBaseName(mapFile.getName());
        
        String reportFileName = fileNameBase + "_mapimport_at_" + getNowString() + ".txt";
    
        log.info("Report file name is \"" + reportFileName + "\"");
    
        StoredReport report = context.getReportManager().saveReport(reportFileName, logStr);
        log.info("Report ID name is \"" + report.getId() + "\"");
        return report;
    }



    public static String getNowString() {
        String timeStr = DATE_FORMAT.format(new Date());
        return timeStr;
    }


}
