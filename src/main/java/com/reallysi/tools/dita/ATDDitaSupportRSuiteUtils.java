/**
 * 
 */
package com.reallysi.tools.dita;

import java.io.File;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.transform.Transformer;

import net.sourceforge.dita4publishers.util.conversion.ConversionConfig;
import net.sourceforge.dita4publishers.util.conversion.ConversionConfigImpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import com.reallysi.rsuite.api.Alias;
import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.report.StoredReport;
import com.reallysi.rsuite.api.vfs.BrowseTreeNode;
import com.reallysi.rsuite.api.xml.DitaOpenToolkit;
import com.reallysi.tools.dita.conversion.ATDDocx2Xml;
import com.reallysi.tools.dita.conversion.Docx2XmlOptions;
import com.reallysi.tools.dita.conversion.TransformationOptions;
import com.rsicms.rsuite.helpers.messages.ProcessMessageContainer;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;

/**
 * Utilities for working with RSuite in an Upper-Room-specific way.
 */
public class ATDDitaSupportRSuiteUtils {
	
	private static Log log = LogFactory.getLog(ATDDitaSupportRSuiteUtils.class);

	public static final String DEF_REST_URL = "/rsuite/rest/v1";

	public static String getConversionConfigIdAlias(ManagedObject mo)
			throws RSuiteException {
		Alias[] aliases = mo.getAliases();
		if (aliases != null && aliases.length > 0) {
			for (Alias cand : aliases) {
				if (RSuiteDitaSupportConstants.ALIAS_TYPE_CONFIG_ID.equals(cand.getType())) {
					log.info("getConversionConfigurationForPubCode(): Found alias of type " + RSuiteDitaSupportConstants.ALIAS_TYPE_CONFIG_ID + 
							", returning \"" + cand.getText() + "\"");
					return cand.getText();
				}
			}
			log.warn("getConversionConfigurationForPubCode(): No alias of type " + RSuiteDitaSupportConstants.ALIAS_TYPE_CONFIG_ID + 
					".");
			log.warn("getConversionConfigurationForPubCode(): Conversion configuration documents should have an alias of that type.");
			log.warn("getConversionConfigurationForPubCode(): Using first alias found.");
			return aliases[0].getText();
		} else {
			log.warn("getConversionConfigurationForPubCode(): No aliases set for conversion configuration managed object. Should have a " +
					"alias of type " + RSuiteDitaSupportConstants.ALIAS_TYPE_CONFIG_ID + "");
		}
		return null;
	}

	public static String[] getToolkitCatalogs(ExecutionContext context, Log log) throws RSuiteException {
		DitaOpenToolkit toolkit = context.getXmlApiManager().getDitaOpenToolkitManager().getToolkit("v185");
		if (toolkit == null) {
			String msg = "No default DITA Open Toolkit provided by Open Toolkit Manager. Cannot continue.";
			throw new RSuiteException(0, msg);
		}
		
		log.info("Got an Open Toolkit, located at \"" + toolkit.getPath() + "\"");
		
		String[] catalogs = new String[1];
		String catalogPath = toolkit.getCatalogPath();
		File catalogFile = new File(catalogPath);
		if (!catalogFile.exists()) {
			String msg = "Cannot find catalog file " + catalogFile.getAbsolutePath();
			throw new RSuiteException(0, msg);			
		}
		String catalogUrl;
		try {
			catalogUrl = catalogFile.toURI().toURL().toExternalForm();
		} catch (MalformedURLException e) {
			throw new RSuiteException(0, "Unexpected Malformed URL Exception for catalog URL \"" + catalogFile.toString() + "\"", e);
		}
		
		log.info("Using Toolkit catalog \"" + catalogUrl + "\"");
		catalogs[0] = catalogUrl;
		return catalogs;
	}
	
	public static ConversionConfig getConversionConfigForBrowseTreeNode(
			ExecutionContext context, 
			User user, 
			BrowseTreeNode moBrowseTreeNode,
			Log log) throws RSuiteException {
		
		String configId = com.rsicms.rsuite.helpers.utils.RSuiteUtils
				.getLmdFromAncestorOrSelf(
						context, 
						user, 
						moBrowseTreeNode, 
						RSuiteDitaSupportConstants.CONVERSION_CONFIG_ID_LMD);
		return getConversionConfigForAlias(context, user, log, configId);
	}

	public static ConversionConfig getConversionConfigForContainer(
			ExecutionContext context, 
			User user, 
			ContentAssemblyNodeContainer container,
			Log log) throws RSuiteException {
		ManagedObject mo = context.getManagedObjectService()
				.getManagedObject(user, container.getId());
		String configId = com.rsicms.rsuite.helpers.utils.RSuiteUtils
				.getLmdFromAncestorOrSelf(
						context, 
						user, 
						mo, 
						RSuiteDitaSupportConstants.CONVERSION_CONFIG_ID_LMD);
		if (configId == null) {
			throw new RSuiteException(0, "Did not find a value for LMD field" +
					" " + RSuiteDitaSupportConstants.CONVERSION_CONFIG_ID_LMD + " on the managed object or any ancestor.");
		}
		return getConversionConfigForAlias(context, user, log, configId);
	}

	public static ConversionConfig getConversionConfigForAlias(
			ExecutionContext context, User user, Log log, String configId)
			throws RSuiteException {
		ConversionConfig config;
		ManagedObject configMo = null;
		List<ManagedObject> candsByAlias = context.getManagedObjectService().getObjectsByAlias(user, configId);
		if (candsByAlias != null && candsByAlias.size() > 0) {
			if (candsByAlias.size() > 1) {
				log.warn("getConversionConfigForMo(): Found " + candsByAlias.size() + " managed objects with the config ID alias \"" + configId + "\". " +
					"There should be at most one.");
			}
			configMo = candsByAlias.get(0);
		}
		
		if (configMo == null) {
			// Try to find by @id value:
			String query = "/conversion_configuration[@id = '" + configId + "']";
			
			//List<ManagedObject> cands = context.getSearchService().executeRqlSearch(user, query, 0, 99);
			
			List<ManagedObject> cands = context.getSearchService().executeXPathSearch(user, query, 0, 100);

			if (cands == null || cands.size() == 0) {
				throw new RSuiteException(0, "Failed to find conversion_config managed object with alias or ID \"" + configId + "'");
			}
			if (cands.size() > 1) {
				log.warn("getConversionConfigForMo(): Found " + cands.size() + " conversion_config MOs with the ID or alias \"" + configId + "\". " +
						"There should be at most one.");
			}
			configMo = cands.get(0);
			log.info("getConversionConfigForMo(): Setting " + RSuiteDitaSupportConstants.ALIAS_TYPE_CONFIG_ID + " alias on MO [" + configMo.getId() + "]");
			context.getManagedObjectService().setAlias(user, configMo.getId(), new Alias(configId, RSuiteDitaSupportConstants.ALIAS_TYPE_CONFIG_ID));
		}
		
		config = new ConversionConfigImpl(configMo.getElement(), configId);
		return config;
	}

	public static File generateXmlFromContentAssembly(
			ExecutionContext context,
			String basePath,
			String outputFolderName,
			Docx2XmlOptions options,
			ContentAssemblyNodeContainer ca, 
			List<File> generatedXmlFiles
			) throws RSuiteException {
		
	    ConversionConfig convConfig;
		try {
			convConfig = getConversionConfigForContainer(
					context, 
					options.getUser(), 
					ca, 
					log);
		} catch (RSuiteException e) {
			options.addFailureMessage(
					
							"generateXmlFromContentAssembly", 
							"[" + ca.getId() + "] " + ca.getDisplayName(), 
							"RSuite exception getting conversion configuration for content assembly [" + ca.getId() + "]: " +
							e.getMessage(),
							e
					);
			throw e;
		}
		options.setConversionConfig(convConfig);

		return generateXmlFromContentAssemblyNodeContainer(
				context, 
				ca, 
				outputFolderName, 
				options, 
				generatedXmlFiles,
				log);
		
	}

	/**
	 * Generates XML from the DOCX files descended from a content
	 * assembly node container.
	 * @param context Execution context
	 * @param basePath The base path to use for output.
	 * @param outputFolderName The name of the folder to use for the output. 
	 * @param ca The content assembly node container to process.
	 * @param options Conversion options.
	 * @param generatedXmlFiles List that accumulates the list of generated
	 * XML files.
	 * @param followCarefs TODO
	 * @return The output directory the files are generated in.
	 * @throws RSuiteException
	 */
	public static File generateXmlFromContentAssembly(
			ExecutionContext context,
			String basePath, 
			String outputFolderName,
			ContentAssemblyNodeContainer ca,
			Docx2XmlOptions options, 
			List<File> generatedXmlFiles) throws RSuiteException {
	    // Set the output directory
		File baseDir = new File(basePath);
	    File outputDir = new File(new File(baseDir, options.getUser().getUserID()), outputFolderName);
	    
	    if (!outputDir.exists()) {
	        outputDir.mkdirs();
	    }
	    if (!outputDir.exists()) {
	    	options.addFailureMessage(
	    			"System", 
	    			"Initialization", 
	    			"Failed to find or create output directory \"" + outputDir.getAbsolutePath() + "\"");
	    	throw new RSuiteException("Failed to create create output directory " + outputDir.getAbsolutePath());
	    }
	    if (!outputDir.canWrite()) {
	    	options.addFailureMessage(
	    			"System", 
	    			"Initialization", 
	    			"Cannot write to output directory \"" + outputDir.getAbsolutePath() + "\"");
	    	throw new RSuiteException("Cannot write to output directory " + outputDir.getAbsolutePath());
	    }
	
	    options.setOutputDir(outputDir);
	    
	    options.getMessageWriter().write(" + [INFO] Generating XML for Word documents...\n");
	    ManagedObject mo = context.getManagedObjectService()
	    		.getManagedObject(options.getUser(), ca.getId());
		try {
			options.pushCaNode(ca);
			ATDDocx2Xml.traverseMOs(
					context, 
					mo, 
					options, 
					generatedXmlFiles);
		} catch (Exception e) {
			options.addFailureMessage(					
							"generateXmlFromContentAssembly", 
							"[" + ca.getId() + "] " + ca.getDisplayName(), 
							"Exception generating XML from Word documents: " +
							e.getClass().getSimpleName() + " - " + e.getMessage(),
							e
					);
			throw new RSuiteException(0, "Exception generating XML from Word docs", e);
		}
	
		return outputDir; 
		
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
			catalogs = getToolkitCatalogs(context, log);
		} catch (RSuiteException e) {
			log.error("Unexpected exception getting Open Toolkit catalogs: ", e);
			return false;
		}
		StringBuilder validationReport = new StringBuilder();
		BosConstructionOptions domOptions = 
			new BosConstructionOptions(log, 
					validationReport, 
					catalogs);
		
		String port = context.getRSuiteServerConfiguration().getPort();
		String host = context.getRSuiteServerConfiguration().getHostName();
		Transformer validationReportTransform;
		try {
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
								"XML Validation", 
								objectLabel, 
								"Unexpected exception doing XML validation: " + 
								validationException.getClass().getSimpleName() +
								" - " + 
								validationException.getMessage()
						);
			} else {
				
				if (exceptionOccurred || reportString.length() > 0) {
					isValid = false;
					StoredReport report = context.getReportManager().saveReport(reportFileName, reportString);
					options.addFailureMessage(
									"XML Validation", 
									objectLabel, 
									"XML validation failed. See stored report for details.",
									report
							);
				} else {
					options.addInfoMessage(
									"XML Validation", 
									objectLabel, 
									"XML validation passed."
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

	public static void setValidationStatusInvalid(
			ExecutionContext context,
			TransformationOptions options, 
			ContentAssemblyItem ca)
			throws RSuiteException {
		if (ca != null) {
			MetaDataItem item = new MetaDataItem(
					  RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_LMD, 
					  RSuiteDitaSupportConstants.XML_VALIDATION_STATUS_INVALID
					  );
			context.getManagedObjectService().
			  setMetaDataEntry(options.getUser(), 
					  ca.getId(), 
					  item);		            		
		}
	}

	/**
	 * Given a CA node container, apply the configured DOCX2XML transform to
	 * all the Word documents in the container.
	 * @param context Execution context
	 * @param ca CA node container to process 
	 * @param generatedXmlFiles2 
	 * @param outputDir The directory to write the generated files to.
	 * @param docx2XmlOptions Options that configure the transform
	 * @param log The log to write messages to separate from the message writer
	 * configured in the docxOptions.
	 */
	public static File generateXmlFromContentAssemblyNodeContainer(
			ExecutionContext context,
			ContentAssemblyNodeContainer ca,
			String outputDirPath,
			Docx2XmlOptions docxOptions,
			List<File> generatedXmlFiles, 
			Log log
			) throws RSuiteException {
		
		if (docxOptions.getUser() == null) {
			throw new RSuiteException(0, "User must be set on Docx2XmlOptions");
		}

        PrintWriter writer = docxOptions.getMessageWriter();
        writer.write(
        		"<h1>" + 
        		"Conversion Report for " + ca.getDisplayName() + " [" + ca.getId() + "]" +
        		"</h1>"
        		);
        writer.write("<div>");
        writer.flush(); // Make sure everything is written to the underlying stream.

		String basePath =
		        context.getConfigurationProperties().getProperty(
		        		RSuiteDitaSupportConstants.DITAXML_EXPORT_DIR_KEY,
		                RSuiteDitaSupportConstants.DITAXML_EXPORT_DIR_DEFAULT);
		writer.println("<pre>\n");
		File outputDir = null;
		try {
			outputDir = generateXmlFromContentAssembly(
					context,
					basePath,
					outputDirPath,
					ca,
					docxOptions,
					generatedXmlFiles);
			if (docxOptions.hasFailures()) {
				log.error("transform: Failures reported.");
			} else {
				log.info("transform: success");
			}
		} catch (Exception e) {
			docxOptions.addFailureMessage(
					e.getClass().getSimpleName(), 
					ca.getDisplayName(),
					"Unexpected exception generating XML: " + e.getMessage(),
					e
					);
		}
		writer.println("</pre>\n");
		if (outputDir != null) { 
			writer.write(" + [INFO] Generated output can be found on server file system in \"" + outputDir.getAbsolutePath() + "\"");
		}
        ProcessMessageContainer messages = docxOptions.getMessages();
        
        writer.write("</div>");
        if (messages.hasFailures()) {
        	writer.write("<p");
        	writer.write(" style=\"color: red;\"");
        	writer.write(">");
        	writer.write("Failures during conversion.");
        	writer.write("</p>\n");
        }
        
    	writer.write("<h2>" + "Messages" + "</h2>");

		messages.reportMessagesByObjectAsHtml(
				docxOptions.getUser().getName(), 
				writer);
        
        writer.flush();
        return outputDir;
	}

	public static ConversionConfig getConversionConfigForMo(
			ExecutionContext context,
			User user,
			ManagedObject mo,
			Log log) throws RSuiteException {
		String configId = com.rsicms.rsuite.helpers.utils.RSuiteUtils
				.getLmdFromAncestorOrSelf(
						context, 
						user, 
						mo, 
						RSuiteDitaSupportConstants.CONVERSION_CONFIG_ID_LMD);
		if (configId == null || "".equals(configId)) {
			String msg = "Could not determine conversion configurationi ID for Managed Object " + RSuiteUtils.formatMoId(mo);			
			throw new RSuiteException(0, msg);
		}
		
		ConversionConfig convConfig = 
				getConversionConfigForAlias(
						context, 
						user, 
						log, 
						configId);
		return convConfig;
	}

}
