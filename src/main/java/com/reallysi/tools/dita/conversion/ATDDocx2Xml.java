package com.reallysi.tools.dita.conversion;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;

import net.sourceforge.dita4publishers.util.conversion.ConversionConfig;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ElementMatchingCriteria;
import com.reallysi.rsuite.api.ElementMatchingOptions;
import com.reallysi.rsuite.api.LayeredMetadataDefinition;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ManagedObjectReference;
import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.ObjectType;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.report.StoredReport;
import com.reallysi.rsuite.api.xml.LoggingSaxonMessageListener;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.tools.dita.*;
import com.reallysi.tools.dita.conversion.beans.*;
import com.rsicms.rsuite.helpers.messages.ProcessMessageContainer;
import com.rsicms.rsuite.helpers.messages.impl.GenericProcessFailureMessage;
import com.rsicms.rsuite.helpers.messages.impl.GenericProcessInfoMessage;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;

/**
 * Generates XML from from DOCX MO.
 */
public class ATDDocx2Xml {

    public static final String XSLT_PARAM_TOPIC_EXTENSION = "topicExtension";
	public static final String XSLT_PARAM_OUTPUT_DIR = "outputDir";
	public static final String XSLT_PARAM_ROOT_TOPIC_NAME = "rootTopicName";
	public static final String XSLT_PARAM_ROOT_MAP_NAME = "rootMapName";
	public static final String XSLT_PARAM_SUBMAP_NAME_PREFIX = "submapNamePrefix";
	public static final String XSLT_PARAM_FILE_NAME_PREFIX = "fileNamePrefix";
	public static final String XSLT_PARAM_DEBUG = "debug";
	public static final String CONFIG_PARAM_DOCX2DITA_XSLT = "docx2ditaXslt";
	public static final String XSLT_PARAM_DOCX_MOID = "docxMoId";
	public final static String MIME_TYPE_DOCX = 
	        "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	private static Log log = LogFactory.getLog(ATDDocx2Xml.class);

    public ATDDocx2Xml()
    {
    }
    
    /**
     * Determine if a DOCX to XML transformation is applicable
     * 
     * @param mo
     * @return
     */
    public static boolean isTransformationApplicable(
            ManagedObject mo) {
        try {
        	// getContentType() may return null if the content type
        	// is not recognized.
        	String mimeType = mo.getContentType();
        	boolean result = mo != null && 
   				 			 mo.isNonXml() &&
   				 			 mimeType != null &&
            				 (mimeType.equalsIgnoreCase(MIME_TYPE_DOCX) ||
            				  mimeType.equalsIgnoreCase("docx"));
            return result;
        } catch (RSuiteException e) {
            return false;
        }

    }
    
    public static void traverseMOs(
    		ExecutionContext context, 
    		ManagedObject mo, 
    		Docx2XmlOptions options, 
    		List<File> generatedXmlFiles)
        throws RSuiteException, IOException {
    	
    	ManagedObject realMo = mo;
    	ManagedObjectService moSvc = context.getManagedObjectService();
		if (mo.getTargetId() != null) {
    		realMo = moSvc.
    				 getManagedObject(options.getUser(), 
    						 mo.getTargetId());
    	}
    	
        if (realMo.isAssemblyNode()) {
        	ContentAssemblyNodeContainer ca = com.rsicms.rsuite.helpers.utils.RSuiteUtils
        			.getContentAssemblyNodeContainer(context, options.getUser(), realMo.getId());

            options.setLmdOptions(context, realMo);
            
            List<? extends ContentAssemblyItem> caItems = ca.getChildrenObjects();
            for (ContentAssemblyItem item : caItems) {
                if (item instanceof ManagedObjectReference) {
                    String moid = ((ManagedObjectReference) item).getTargetId();
                    ManagedObject childMo = moSvc.getManagedObject(options.getUser(), moid);
                    if (isTransformationApplicable(childMo)) {
                    	transformMo(context, childMo, options, generatedXmlFiles);
                    }
                }
                else {
                    ManagedObject childMo = moSvc.getManagedObject(options.getUser(), item.getId());
                    boolean isCaref = childMo.getObjectType() == ObjectType.CONTENT_ASSEMBLY_REF;
					if (isCaref) {
                    	item = RSuiteUtils.getContentAssemblyNodeContainer(context, options.getUser(), childMo.getTargetId());
                    }
                    options.pushCaNode((ContentAssemblyNodeContainer) item);
                	traverseMOs(context, childMo, options, generatedXmlFiles);
                    options.popCaNode();
                }
            }
        }
        else {
            transformMo(context, realMo, options, generatedXmlFiles);
        }
    }

    public static void transformMo(
    		ExecutionContext context, 
    		ManagedObject mo, 
    		Docx2XmlOptions options, 
    		List<File> generatedXmlFiles)
        throws RSuiteException, IOException {

        // Ignore non-DOCX files
		if (!isTransformationApplicable(mo)) {
            log.info(
                "The DOCX to XML transformation is not applicable to MO ID " +   
                mo.getId() +
                " with content type \"" + 
                mo.getContentType() +
                "\"; skipping the transformation.");
            return;
        }
        
        PrintWriter messageWriter = options.getMessageWriter();
        messageWriter.println(" + [INFO] Generating XML from managed object [" + mo.getId() + "] " + mo.getDisplayName() + "...");
        
        // Don't want to put transformation messages to the main message
        // writer because it's redundant with the stored report, which
        // is linked to the success and failure process messages.
        // The StringWriter is just to provide a temporary logging target.
        // Since the LoggingSaxonMessageListener also stores the log
        // for later retrieval, it's redundant as the code is currently
        // written.
        StringWriter stringWriter = new StringWriter();
        Log transformLog = new PrintWriterLogger(new PrintWriter(stringWriter));
        
        LoggingSaxonMessageListener localLogger =
            context.getXmlApiManager().newLoggingSaxonMessageListener(transformLog);
        options.setSaxonLogger(localLogger);

        setXsltAndStyleMap(context, mo, options);
        
        String docxFilename = mo.getDisplayName();
        String fileNameBase = FilenameUtils.getBaseName(docxFilename);
        log.info("transformMo(): Base filename is \"" + fileNameBase + "\"");
        // Get rid of some problematic characters:
        String newFilename = fileNameBase.replaceAll("[,\"']\\(\\)", "~");
        if (!newFilename.equals(fileNameBase)) {
        	log.info("transformMo(): Replaced characters in original filename to avoid URL syntax issues. New value is \"" + newFilename + "\"");
        	fileNameBase = newFilename;
        }

        options.setMapFileName(fileNameBase);
        String filenamePrefix = constructFilenamePrefix(fileNameBase);
        
        options.setFilenamePrefix(filenamePrefix);

        // FIXME: Rework the bean to use the options rather than separate parameters.
        ATDDocx2XmlBean bean = new ATDDocx2XmlBean(
        		context, 
        		options.getXsltUri(), 
        		options.getStyleMapUri(), 
        		options.getMapFileName(), 
        		com.rsicms.rsuite.helpers.utils.RSuiteUtils.formatMoId(mo));

        File docxFile = new File(mo.getExternalAssetPath());
        // If the mapDir were different from the output dir then we'd
        // need to make the outputDir equal to the map dir for the XSLT 
        // process to produce the correct result:
        File mapDir = options.getOutputDir();
        // NOTE: This doesn't actually set the name of the true result map, which is generated
        // via xsl:result-document() in the transform.
        String resultFileName = fileNameBase + ".xml";
        // The topic result files are always generated explicitly by the XSLT,
        // so the direct output of the XSLT is never used.
        File resultFile = new File(options.getOutputDir(), resultFileName);
        File tempFile = File.createTempFile("rsi-docx2dita-", "deleteme");
        // This is the real map file that will be produced:
        String mapFilename = options.getMapFileName() + ".ditamap";
        File resultMapFile = new File(mapDir, mapFilename);
        
        // context.setVariable(xformReportFileNameVarName, reportFileName);

        Map<String, String> params = new HashMap<String, String>();
        params.put(XSLT_PARAM_DEBUG, "false");
        params.put(XSLT_PARAM_FILE_NAME_PREFIX, options.getFilenamePrefix());
        params.put(XSLT_PARAM_SUBMAP_NAME_PREFIX, "map");
        params.put(XSLT_PARAM_ROOT_TOPIC_NAME, fileNameBase);
        params.put(XSLT_PARAM_ROOT_MAP_NAME, options.getMapFileName());
        params.put(XSLT_PARAM_OUTPUT_DIR, options.getOutputDir().toURI().toString());
        params.put(XSLT_PARAM_TOPIC_EXTENSION, ".xml"); // Default is ".dita"
        params.put(XSLT_PARAM_DOCX_MOID, mo.getId()); 

        // Parameter specifying what prefix to add to each graphic filename
        // to help insure uniqueness.
        params.put("graphicFileNamePrefix", options.getFilenamePrefix());
        
        boolean exceptionOccured = false;
        try {
			bean.generateXmlS9Api(
					docxFile, 
					tempFile, 
					localLogger, 
					params, 
					options);
			String objectLabel = com.rsicms.rsuite.helpers.utils.RSuiteUtils.formatMoId(mo);
			
			// If we generated a map file, prefer it over any topic file:ps
			
            File generatedFile = (resultMapFile.exists() ? resultMapFile : resultFile);
			if (generatedFile.exists()) {
				
	            generatedXmlFiles.add(generatedFile);
				if (validateXmlFile(context, objectLabel, generatedFile, options)) {					
		            messageWriter.println(" + [INFO]  DOCX 2 DITA: Process finished normally.");

		            setValidationStatusValid(context, options, options.peekCaNode());
		            if (options.loadXmlToRSuite()) {
		            	// NOTE: We want to log to the main log, not the transform-specific
		            	// log.
		            	exceptionOccured = doLoadToRSuite(
		            			context, 
		            			mo, 
		            			options,
		            			messageWriter, 
		            			generatedFile);
		            }
				} else {
		            setValidationStatusInvalid(context, options, options.peekCaNode());
					String msg = "Generated XML document not valid or validation attempt failed.";
					messageWriter.println(" - [ERROR] " + msg);
					options.addFailureMessage(
							new GenericProcessFailureMessage("DOCX to XML", 
									com.rsicms.rsuite.helpers.utils.RSuiteUtils.formatMoId(mo),
									msg));

		            exceptionOccured = true;
				}
			} else {
	            setValidationStatusInvalid(context, options, options.peekCaNode());
				String msg = "No result file generated by transform. Check output directory and server log.";
				messageWriter.println(" - [ERROR] " + msg);
				
				options.addFailureMessage(
						"DOCX to XML", 
						com.rsicms.rsuite.helpers.utils.RSuiteUtils.formatMoId(mo),
								msg);
	            exceptionOccured = true;
			}

		} catch (Exception e) {
            messageWriter.println(" + [ERROR] DOCX 2 DITA: Exception occurred.\n");
			log.error(e);
			// Don't report these exceptions because they are not useful to end users.
            exceptionOccured = true;
		}
		
		
		
        captureTransformReports(
        		context, 
        		mo, 
        		options, 
        		fileNameBase,
        		localLogger,
				mapDir, 
				resultFile, 
				resultMapFile, 
				exceptionOccured);
        // Now clean up the temp file:
        // tempFile.delete();
    }

	private static String constructFilenamePrefix(String fileNameBase) {
		String filenamePrefix = fileNameBase;
        if (filenamePrefix.matches("^\\d.*")) {
        	filenamePrefix = "x" + filenamePrefix;
        }
        filenamePrefix += "_";
        
        // Need to make sure the filename prefix also a valid
        // XML ID. This code is really not sufficient for that but
        // it at least catches the case of spaces, apostrophes, and
        // quotes, which are the most
        // likely.
        if (!filenamePrefix.matches("[\\w_][\\d\\w:\\._\\-]+")) {        	
        	filenamePrefix = filenamePrefix.replaceAll("[\\s\\(\\)]", "_");
        	filenamePrefix = filenamePrefix.replaceAll("\u2018", "-");
        	filenamePrefix = filenamePrefix.replaceAll("\u2019", "-");
        	filenamePrefix = filenamePrefix.replaceAll("\u201C", "-");
        	filenamePrefix = filenamePrefix.replaceAll("\u201D", "-");
        }
		return filenamePrefix;
	}

	protected static boolean doLoadToRSuite(ExecutionContext context,
			ManagedObject mo, 
			TransformationOptions options,
			PrintWriter messageWriter, 
			File resultFile) {
		boolean exceptionOccured = false;
		if (!resultFile.exists()) {
			exceptionOccured = true;
			String msg = "Result XML file " + resultFile.getAbsolutePath() + " not generated.";
		    options.addFailureMessage(
		    		new GenericProcessFailureMessage(
		    		"DOCX 2 DITA", 
		    		com.rsicms.rsuite.helpers.utils.RSuiteUtils.formatMoId(mo), 
		    		msg));
		    messageWriter.println(" + [ERROR] DOCX 2 DITA: " + msg);
		} else {
			try {
				RSuiteUtils.loadFileToRSuite(
						context, 
						resultFile.getName(), 
						options.getUser(), 
						options.peekCaNode(), 
						resultFile, 
						true, 
						messageWriter);				
			} catch (Exception e) {
				exceptionOccured = true;
				String msg = "Exception loading generated XML to RSuite: " + e.getMessage();
				options.addFailureMessage(
						new GenericProcessFailureMessage(
		        		"Load to RSuite", 
		        		com.rsicms.rsuite.helpers.utils.RSuiteUtils.formatMoId(mo), 
		        		msg));
				messageWriter.println("[ERROR] " + msg);
				e.printStackTrace(messageWriter);
			}
		}
		return exceptionOccured;
	}

	private static void captureTransformReports(
			ExecutionContext context,
			ManagedObject mo, 
			TransformationOptions options,
			String fileNameBase, 
			LoggingSaxonMessageListener logger,
			File mapDir, 
			File resultFile, 
			File resultMapFile,
			boolean exceptionOccured) throws RSuiteException, IOException {

		ProcessMessageContainer messages = options.getMessages();
        String reportFileName = fileNameBase + "_docx2dita_at_" + getNowString() + ".txt";

		File copiedReportFile;
		String logString = logger.getLogString();
		StringBuilder reportStr =
                new StringBuilder("DOCX 2 DITA Transformation report\n\n").append("Source DOCX: ")
                        .append(mo.getDisplayName()).append(" [").append(mo.getId()).append("]\n")
                        .append("Result file: ").append(resultMapFile.getAbsolutePath()).append(
                                "\n").append("Time performed: ").append(getNowString()).append(
                                "\n\n").append(logString);

		StoredReport report =
                context.getReportManager().saveReport(reportFileName, reportStr.toString());
        if (exceptionOccured) {
        	messages.addFailureMessage(
        			new GenericProcessFailureMessage(
        					"DOCX 2 DITA", 
        					com.rsicms.rsuite.helpers.utils.RSuiteUtils.formatMoId(mo), 
        					"Exceptions occurred during transform.", 
        					report));
        } else {
        	messages.addInfoMessage(
        			new GenericProcessInfoMessage(
        					"DOCX 2 DITA", 
        					com.rsicms.rsuite.helpers.utils.RSuiteUtils.formatMoId(mo),
        					"Transform completed normally.", 
        					report));
        }
        copiedReportFile = new File(mapDir, report.getSuggestedFileName());
        log.info("Generation report saved as file  \"" + copiedReportFile.getAbsolutePath() + "\"");
        File reportFile = report.getFile();
        FileUtils.copyFile(reportFile, copiedReportFile);
	}

    protected static void setXsltAndStyleMap(ExecutionContext context, ManagedObject mo, Docx2XmlOptions options) throws RSuiteException {
    	
    	ConversionConfig convConfig = options.getConversionConfig();
    	
    	String uriBase = RSuiteDitaSupportConstants.WEB_SERVICE_URI_BASE;
    	String transformPath;
		try {
			transformPath = convConfig.getStringParameter(CONFIG_PARAM_DOCX2DITA_XSLT);
		} catch (Exception e) {
			throw new RSuiteException(0, "Exception getting conversion parameter: " + e.getMessage(), e);
		}
    	if (transformPath == null || "".equals(transformPath.trim())) {
    		throw new RSuiteException(0, "Failed to get value for configuration parameter \"" + CONFIG_PARAM_DOCX2DITA_XSLT + "\"");
    	}
        URI xsltUri;
		try {
			if (transformPath.matches("^[a-z]+:.*")) {
				xsltUri = new URI(transformPath);
			} else {
				xsltUri = new URI(uriBase + transformPath);
			}
		} catch (URISyntaxException e) {
			throw new RSuiteException(0, "URI syntax exception for XSLT URI \"" + options.getXsltUriStr() + "\"");
		}
		options.setXsltUri(xsltUri);
    	// options.setTransformer(context.getXmlApiManager().getSaxonXsltTransformer(xsltUri, options.getSaxonLogger()));        
    	
    	String styleMapPath = null;;
		try {
			styleMapPath = convConfig.getStringParameter("style2tagmap");
		} catch (Exception e) {
			throw new RSuiteException(0, "Exception getting configuration parameter: " + e.getMessage(), e);
		}
		if (styleMapPath.matches("^[a-z]+:.*")) {
			options.setStyleMapUri(styleMapPath);
		} else {
	    	options.setStyleMapUri(uriBase + styleMapPath);
		}
				
    }

	/**
	 * Returns true if the XML document is valid.
	 * @param context
	 * @param objectLabel
	 * @param xmlFile
	 * @param options
	 * @return True if document is valid, otherwise false.
	 */
	public static boolean validateXmlFile(
			ExecutionContext context,
			String objectLabel, 
			File xmlFile, 
			TransformationOptions options) {
		Log log = options.getLog();
		String[] catalogs;
		try {
			catalogs = ATDDitaSupportRSuiteUtils.getToolkitCatalogs(context, log);
		} catch (RSuiteException e) {
			log.error("Unexpected exception getting Open Toolkit catalogs: ", e);
			return false;
		}
		StringBuilder validationReport = new StringBuilder();
		BosConstructionOptions domOptions = 
			new BosConstructionOptions(log, 
					validationReport, 
					catalogs);
		
		Transformer validationReportTransform;
		try {
			String host = context.getRSuiteServerConfiguration().getHostName();
			String port = context.getRSuiteServerConfiguration().getPort();
			validationReportTransform = context.getXmlApiManager().
			getTransformer(
					new URL("http://" + host + ":" + port + 
							"/rsuite/rest/v1/static/astd-plugin/xslt/validation-report/validation-report.xsl"));
		} catch (MalformedURLException e) {
			log.error("Unexpected exception getting validlation report formatting transform: ", e);
			return false;
		} catch (RSuiteException e) {
			log.error("Unexpected exception getting validlation report formatting transform: ", e);
			return false;
		}
		domOptions.setReportSerializationTransform(validationReportTransform);
		
		boolean exceptionOccurred = false;
		Exception validationException = null;
		try {
			log.info("Parsing file \"" + xmlFile.getAbsolutePath() + "\"...");
			Document dom = DomUtil.getDomForDocument(xmlFile, domOptions, true);
			if (dom == null) {
				exceptionOccurred = true;
			}
		} catch (DOMException e) {
			log.error("DOM Exception doing validation parse.");
			exceptionOccurred = true;
			validationException = e;
		} catch (Exception e) {
			// Got here because do is invalid or something else bad happened.
			log.error("Exception doing validation parse.", e);
			exceptionOccurred = true;
			validationException = e;
		}
		boolean isValid = true;
		String reportFileName = "ValidationReport_" + xmlFile.getName() + ".txt";
		try {
			String reportString = validationReport.toString();
			if (exceptionOccurred && reportString.trim().equals("")) {
				isValid = false;
				options.addFailureMessage(
						new GenericProcessFailureMessage(
								"XML Validation", 
								objectLabel, 
								"Unexpected exception doing XML validation: " + 
								validationException.getClass().getSimpleName() +
								" - " + 
								validationException.getMessage()
								)
						);
			} else {
				
				if (exceptionOccurred || reportString.length() > 0) {
					isValid = false;
					StoredReport report = context.getReportManager().saveReport(reportFileName, reportString);
					options.addFailureMessage(
							new GenericProcessFailureMessage(
									"XML Validation", 
									objectLabel, 
									"XML validation failed. See stored report for details.",
									report)
							);
				} else {
					options.addInfoMessage(
							new GenericProcessInfoMessage(
									"XML Validation", 
									objectLabel, 
									"XML validation passed.")
							);
				}
			}
		} catch (RSuiteException e1) {
			log.error("Unexpected exception getting saving stored report for validation report: ", e1);
			return false;
		}
		return isValid;
		
	}
	

	public static void setValidationStatusValid(
			ExecutionContext context,
			TransformationOptions options, 
			ContentAssemblyItem ca)
			throws RSuiteException {
		if (ca != null) {
			boolean lmdIsValid = validateValidationStatusLmdField(
					context,					
					options, 
					ca, 
					RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_VALID);

			if (lmdIsValid) {
				MetaDataItem metadataItem = new MetaDataItem(					  
						RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_LMD, 
						  RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_VALID);
				context.getManagedObjectService().
				  setMetaDataEntry(options.getUser(), 
						  ca.getId(), 
						  metadataItem
						  );
			}
		}
	}

	public static boolean validateValidationStatusLmdField(
			ExecutionContext context, TransformationOptions options,
			ContentAssemblyItem ca,
			String value) throws RSuiteException {
		boolean lmdIsValid = false;
		User user = context.getAuthorizationService().getSystemUser();
		ManagedObject mo = context.getManagedObjectService()
				.getManagedObject(
						user, 
						ca.getId());
		lmdIsValid = context.getMetaDataService().isValidLayeredMetaData(
				mo.getNamespaceURI(), 
				mo.getLocalName(), 
				RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_LMD, 
				value);
		if (!lmdIsValid) {
			// See if we can define the LMD field or add this element type to it.
			LayeredMetadataDefinition lmdDef = context.getMetaDataService()
					.getLayeredMetaDataDefinition(user, RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_LMD);
			if (lmdDef != null) {
				if (!lmdDef.isAssociatedWithElementCriteria(
						mo.getNamespaceURI(), 
						mo.getLocalName(), 
						new ElementMatchingOptions())) {
					log.info("validateValidationStatusLmdField(): Adding " + mo.getLocalName() + " to allowed elements for LMD field " + RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_LMD);
					ElementMatchingCriteria[] criteria = lmdDef.getElementCriteria();
					List<ElementMatchingCriteria> list = Arrays.asList(criteria);
					list.add(new LocalElementMatchingCriteria(
							mo.getNamespaceURI(), 
							mo.getLocalName()));
					context.getMetaDataService()
					  .setLayeredMetaDataDefinitionElementCriteria(
							  user,
							  lmdDef.getName(),
							  list
							  );
				} else {
					// Value specified must not be an allowed value. 
					log.warn("validateValidationStatusLmdField(): Value \"" + value + "\" specified for " + RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_LMD + 
							 " is not an allowed value.");
					// No obvious way to make this change through the core API.
					
				}
			} else {
				// Define new LMD field:
				List<ElementMatchingCriteria> elementCriteriaList = new ArrayList<ElementMatchingCriteria>();
				elementCriteriaList.add(new LocalElementMatchingCriteria(
						mo.getNamespaceURI(), 
						mo.getLocalName()));
				String[] values = {"unchecked", "valid", "invalid"};
				try {
					context.getMetaDataService()
					  .createLayeredMetaDataDefinition(
							  user, 
							  RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_LMD, 
							  "string", 
							  false, // Versioned 
							  false, // Allows multiple
							  true, // Allows contextual
							  elementCriteriaList, 
							  values, 
							  null, null, null, null);
					lmdIsValid = true;
				} catch (Exception e) {
					log.error("validateValidationStatusLmdField(): Unexpected exception creating LMD field " + RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_LMD + ": " + e.getMessage(), e);
				}
			}
		}
		return lmdIsValid;
	}

	public static void setValidationStatusInvalid(
			ExecutionContext context,
			TransformationOptions options, 
			ContentAssemblyItem ca)
			throws RSuiteException {
		if (ca != null) {
			
			boolean lmdIsValid = validateValidationStatusLmdField(
					context,					
					options, 
					ca, 
					RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_INVALID);

			if (lmdIsValid) {
				MetaDataItem metadataItem = new MetaDataItem(					  
						RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_LMD, 
						  RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_INVALID);
	
				context.getManagedObjectService().
				  setMetaDataEntry(options.getUser(), 
						  ca.getId(), 
						  metadataItem
						  );
			}
		}
	}

	public static void writeSimpleElement(XMLStreamWriter writer, String tagName, String content)
			throws XMLStreamException {
		writer.writeStartElement(tagName);
		writer.writeCharacters(content);
		writer.writeEndElement();
	}

    public static String getNowString() {
        return new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
    }

    
}
