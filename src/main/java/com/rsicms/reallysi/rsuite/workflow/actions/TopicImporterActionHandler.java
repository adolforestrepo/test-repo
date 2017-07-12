/**
 * Copyright (c) 2009 Really Strategies, Inc.
 */
package com.reallysi.rsuite.workflow.actions;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.el.FixedValue;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.w3c.dom.Document;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.workflow.activiti.BusinessRuleException;
import com.reallysi.rsuite.api.workflow.activiti.FileWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.ProcessedWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.api.xml.DitaOpenToolkit;
import com.reallysi.rsuite.workflow.actions.RSuiteDitaSupportActionHandlerBase;
import com.reallysi.tools.dita.BosConstructionOptions;
import com.reallysi.tools.dita.BosVisitor;
import com.reallysi.tools.dita.BoundedObjectSet;
import com.reallysi.tools.dita.BrowseTreeConstructingBosVisitor;
import com.reallysi.tools.dita.DitaMapImportOptions;
import com.reallysi.tools.dita.DitaUtil;
import com.reallysi.tools.dita.DomUtil;
import com.reallysi.tools.dita.RSuiteDitaHelper;

/**
 * Takes a single article topic, chases down its dependencies, and imports the
 * lot into RSuite, rewriting pointers as needed.
 */
public class TopicImporterActionHandler extends RSuiteDitaSupportActionHandlerBase {


	/**
	 * 
	 */
	public static final String IMPORTED_MO_LIST_VARNAME = "importedMoList";
	 protected Expression importedMoList;
	/**
	 * 
	 */
	public static final String IMPORTED_MO_LIST_VARNAME_PARAM = "importedMoListVarname";
	protected Expression importedMoListVarname;
	/**
	 * 
	 */
	public static final String COMMIT_MESSAGE_PARAM = "commitMessage";
	protected Expression commitMessage;
	/**
	 * Path of the root topic to process. Use if topics are not in FileWorkflowObject.
	 */
	public static final String TOPIC_PATH_PARAM = "topicPath";
	protected Expression topicPath;	
	/**
	 * The absolute path of the browse tree folder into which the map is
	 * imported. e.g. "/maps/mystuff"
	 */
	public static final String PARENT_BROWSE_TREE_FOLDER_PARAM = "parentBrowseTreeFolder";
	protected Expression parentBrowseTreeFolder;
	/**
	 * The path of the content assembly or node within the parent folder
	 * into which the map is imported, e.g. "books/reference" to create CA "books", CA Node "reference".
	 */
	public static final String PARENT_CA_NODE_PARAM = "parentCaNode";
	protected Expression parentCaNode;
	/**
	 * The ID of the content assembly or node within the parent folder
	 * into which the map is to be imported, e.g. "12353".
	 */
	public static final String PARENT_CA_ID_PARAM = "parentCaId";
	protected Expression parentCaId;
	/**
	 * Optional. URI of the graphic to use for missing graphics.
	 */
	public static final String MISSING_GRAPHIC_URI_PARAM = "missingGraphicUri";
	protected Expression missingGraphicUri;
	/**
	 * The name of the configured Open Toolkit to use. Default is "default".
	 * Open Toolkits are configured in the DITA Open Toolkit properties file
	 * in the RSuite conf/ directory.
	 * <p>
	 * The maps to be processed are listed in the workflow variable "exportedMapFiles", 
	 * which is a list of WorkflowFileObjects, one for each map.
	 * </p>
	 */
	public static final String OPEN_TOOLKIT_NAME_PARAM = "openToolkitName";
	protected Expression openToolkitName;
	/**
	 * (Optional) Name of topic container name to use.
	 * <p>If not specified, "<tt>content</tt>" will be used.
	 * </p>
	 */
	public static final String TOPIC_CONTAINER_NAME_PARAM = "topicContainerName";
	protected Expression topicContainerName;
	/**
	 * (Optional) Name of non-xml container name to use.
	 * <p>If not specified, "<tt>media</tt>" will be used.
	 * </p>
	 */
	public static final String NONXML_CONTAINER_NAME_PARAM = "nonXmlContainerName";
	protected Expression nonXmlContainerName;

	private static final long serialVersionUID = 1L;

	
	/* (non-Javadoc)
	 * @see com.reallysi.rsuite.api.workflow.AbstractBaseNonLeavingActionHandler#execute(com.reallysi.rsuite.api.workflow.WorkflowExecutionContext)
	 */
	@Override
	public void execute(WorkflowContext context) throws Exception {
		Log wfLog = context.getWorkflowLog();
		
		DitaMapImportOptions importOptions = setImportOptions(context);
		
		importOptions.setMissingGraphicUri( 
				resolveVariablesAndExpressions(getParameterWithDefault(MISSING_GRAPHIC_URI_PARAM, "images/missingGraphic.jpg")));
		importOptions.setTopicContainerName( 
				resolveVariablesAndExpressions(getParameterWithDefault(TOPIC_CONTAINER_NAME_PARAM,
								"content")));
		importOptions.setNonXmlContainerName(resolveVariablesAndExpressions(getParameterWithDefault(NONXML_CONTAINER_NAME_PARAM,
								"media")));
		importOptions.setUser(context.getAuthorizationService().getSystemUser());

		/** set system user ATD-53 **/
		wfLog.info("[INFOX] Import option user: "+importOptions.getUser().getUserId());
				
		String[] catalogs = getCatalogs(context, wfLog);
		
		File topicFile = null;

		topicFile = getTopicFile(context, wfLog);
		if (topicFile == null || !topicFile.exists()) {
			String msg = "Cannot find topic file.";
			reportAndThrowRSuiteException(msg);
		}
		
		boolean errorOccured = false;
		String errorMessage = "";
		ArrayList<ProcessedWorkflowObject> errorFiles = new ArrayList<ProcessedWorkflowObject>();
		
		// This variable holds the set of content assembly nodes that contain the imported
		// topics. The topic importer always loads the imported topic to the browse tree
		// and returns the CA node, not the imported topic's XML MO. It is this list that
		// is used to set the workflow context by default.
		ArrayList<MoWorkflowObject> importedTopicCaNodes = new ArrayList<MoWorkflowObject>();

		// This variable holds the set of topic MOs that were imported.
		ArrayList<MoWorkflowObject> importedTopics = new ArrayList<MoWorkflowObject>();

		StringBuilder validationReport = new StringBuilder("Topic import validation report.");
		BosConstructionOptions bosConstructionOptions = new BosConstructionOptions(wfLog, validationReport, catalogs);
		
		Document doc = null;
		try {
			wfLog.info("Parsing topic file \"" + topicFile.getAbsolutePath() + "\"...");
			doc = DomUtil.getDomForDocument(topicFile, bosConstructionOptions, true);
		} catch (Exception e) {
			wfLog.error("ERROR Parsing topic file \"" + topicFile.getAbsolutePath() + "\"...");
			String msg = "Exception parsing file \"" + topicFile.getAbsolutePath() + "\": " + e.getMessage();
			captureValidationReport(context, topicFile, validationReport);
			reportAndThrowRSuiteException(msg);			
		}
		
		if (DitaUtil.isDitaTopic(doc.getDocumentElement())) {
			try {
				wfLog.info("Importing topic file \"" + topicFile.getAbsolutePath() + "\"...");
				MoWorkflowObject importedTopicMo = importTopic(context, doc, bosConstructionOptions, importOptions, importedTopics);
				importedTopicCaNodes.add(importedTopicMo);				
			} catch (Exception e) {
				wfLog.error("Error importing topic file \"" + topicFile.getAbsolutePath() + "\"...");
				errorOccured = true;
				errorMessage = errorMessage + "Failed File: [" + topicFile.getName() + "]\r\n" + e + "\r\n\r\n";
				
				context.setVariable(EXCEPTION_MESSAGE_VAR, errorMessage);
				context.addComment("RSuite", errorMessage);
				
				errorFiles.add(new FileWorkflowObject(topicFile));
				// e.printStackTrace();
				wfLog.error(e);
				

			}
		} else {
			wfLog.info("File " + topicFile.getName() + " does not appear to be a DITA topic, skipping.");
		}
		
		String importedMoListVarname = resolveVariablesAndExpressions(getParameterWithDefault(IMPORTED_MO_LIST_VARNAME_PARAM, IMPORTED_MO_LIST_VARNAME));
		
		wfLog.info("Setting variable " + importedMoListVarname + " to list of imported topics.");
		context.setVariable(importedMoListVarname, (new MoListWorkflowObject(importedTopics)).toString());

		if (!isPreserveWorkflowContext(context)) {
			wfLog.info("Updating workflow context");
			context.setMoListWorkflowObject(new MoListWorkflowObject(importedTopicCaNodes));
		} else {
			wfLog.info("Workflow context set to 'preserve', not modifying it.");
		}
		
		if(errorOccured) {
			handleErrors(context, wfLog, topicFile, errorFiles, validationReport);
		} else {
			wfLog.info("Topics imported successfully");
		}

		
		wfLog.info("Done");
    }


	protected void handleErrors(WorkflowContext context, Log wfLog,
			File topicFile, ArrayList<ProcessedWorkflowObject> errorFiles,
			StringBuilder validationReport) throws RSuiteException,
			BusinessRuleException {
		wfLog.error("Errors occurred during topic import, throwing exception");
		context.setAttribute("errorFiles", errorFiles.toString());
		wfLog.error("Errors from topic imports");
		// FIXME: This next bit is just a placeholder for more complete error handling.
		BusinessRuleException e = new BusinessRuleException(errorFiles.toString());
		e.setFailureDetail(getParameter(FAILURE_DETAIL_PARAM));
		e.setTaskname("Import DITA topic");

		captureValidationReport(context, topicFile, validationReport);

		
		throw e;
	}


	protected DitaMapImportOptions setImportOptions(
			WorkflowContext context) throws RSuiteException {
		DitaMapImportOptions importOptions = new DitaMapImportOptions();
		
		String parentCaId = resolveVariablesAndExpressions(this.parentCaId.getExpressionText());
			
		if (parentCaId == null || "".equals(parentCaId)) {
			try {
				this.checkParamsNotEmptyOrNull(PARENT_BROWSE_TREE_FOLDER_PARAM, PARENT_CA_NODE_PARAM);
			} catch (Exception e) {
				throw new RSuiteException(0, "Must specify either the " + PARENT_CA_ID_PARAM + " parameter or " +
						"both of " + PARENT_BROWSE_TREE_FOLDER_PARAM + " and " + PARENT_CA_NODE_PARAM + ".");				
			}
			importOptions.setRootFolder(
					resolveVariablesAndExpressions(getParameter(PARENT_BROWSE_TREE_FOLDER_PARAM)));
			importOptions.setRootContentAssemblyPath(
					resolveVariablesAndExpressions(getParameter(PARENT_CA_NODE_PARAM)));
		} else {
			ContentAssemblyNodeContainer ca = context.getContentAssemblyService().getContentAssemblyNodeContainer(context.getAuthorizationService().getSystemUser(), 
			                                                                                parentCaId);
			if (ca == null)
				throw new RSuiteException(0, "Failed to find content assembly with ID [" + parentCaId + "] specified as " + PARENT_CA_ID_PARAM +" action handler parameter.");
			importOptions.setRootCa(ca);
		}
	
		
		return importOptions;
	}


	protected String[] getCatalogs(WorkflowContext context, Log wfLog)
			throws RSuiteException, MalformedURLException {
		// We need the Open Toolkit because we use its catalog to parse the incoming files.
		String otName = resolveVariablesAndExpressions(getParameterWithDefault(OPEN_TOOLKIT_NAME_PARAM,"default"));
		wfLog.info("Using Open Toolkit named \"" + otName + "\"");
		DitaOpenToolkit toolkit = context.getXmlApiManager().getDitaOpenToolkitManager().getToolkit(otName);
		if (toolkit == null) {
			String msg = "No DITA Open Toolkit named \"" + otName + "\" provided by Open Toolkit Manager. Cannot continue.";
			reportAndThrowRSuiteException(msg);
		}
		
		wfLog.info("Got an Open Toolkit, located at \"" + toolkit.getPath() + "\"");
		
		String[] catalogs = new String[1];
		String catalogPath = toolkit.getCatalogPath();
		File catalogFile = new File(catalogPath);
		if (!catalogFile.exists()) {
			String msg = "Cannot find catalog file " + catalogFile.getAbsolutePath();
			reportAndThrowRSuiteException(msg);			
		}
		String catalogUrl = catalogFile.toURI().toURL().toExternalForm();
		
		wfLog.info("Using Toolkit catalog \"" + catalogUrl + "\"");
		catalogs[0] = catalogUrl;
		return catalogs;
	}


	/**
	 * If the input is in the file workflow object or specified by the TOPIC_PATH_PARAM
	 * parameter.
	 * @param context
	 * @param wfLog
	 * @return
	 * @throws RSuiteException
	 */
	protected File getTopicFile(WorkflowContext context, Log wfLog)
			throws RSuiteException {
		File topicFile = null;
		String topicPath = resolveVariablesAndExpressions(this.topicPath.getExpressionText());
		topicPath = resolveVariablesAndExpressions(topicPath);
		
		if (topicPath == null || "".equals(topicPath.trim())) {
			FileWorkflowObject fileWFO = context.getFileWorkflowObject();
			if (fileWFO == null) {
				String msg = "No file in the workflow context. Nothing to do";
				context.setVariable(EXCEPTION_MESSAGE_VAR, msg);
				context.addComment("RSuite", msg);
				wfLog.warn(msg);
				return null;
			}
			File candFile = fileWFO.getFile();
			String extension = FilenameUtils.getExtension(candFile.getName());
			if (extension.equalsIgnoreCase("dita") || extension.equalsIgnoreCase("xml"))
				topicFile = candFile;			
		} else {
			topicFile = new File(topicPath);
			if (!topicFile.exists()) {
				String msg = "Specified root map file \"" + topicPath + "\" does not exist. Nothing ";
				reportAndThrowRSuiteException(msg);
			}
		}
		
		return topicFile;
	}


	/**
	 * @param context
	 * @param bosConstructionOptions 
	 * @param importedTopics 
	 * @param outputPath 
	 * @param mo
	 */
	private MoWorkflowObject importTopic(WorkflowContext wfContext, Document topicDoc, 
			BosConstructionOptions bosConstructionOptions,
			DitaMapImportOptions importOptions, List<MoWorkflowObject> importedTopics) throws RSuiteException {
		Log wfLog = wfContext.getWorkflowLog();
		
		BoundedObjectSet bos = RSuiteDitaHelper.importTopic(wfContext, wfLog,
		                                                    wfContext.getAuthorizationService().getSystemUser(),
		                                                    topicDoc, bosConstructionOptions, importOptions);
		
		// BOS should always have at least one member, the topic itself, and that member should always be the root member of the BOS.
		importedTopics.add(new MoWorkflowObject(bos.getRoot().getManagedObject().getId()));
		
		// The MO in this context is the CA into which the map is loaded.
		ManagedObject mo;
		try {
			BosVisitor visitor = new BrowseTreeConstructingBosVisitor(wfContext,
			                                                          wfContext.getAuthorizationService().getSystemUser(), 
			                                                          importOptions, wfLog);
			mo = RSuiteDitaHelper.loadMapToBrowseTree(wfContext, bos, importOptions, visitor);
		} catch (Exception e) {
			wfContext.getWorkflowLog().error("Exception loading BOS to browse tree", e);
			throw new RSuiteException(0, "Error", e);
		}
		
		if (mo == null) {
			throw new RSuiteException(0, "Failed to get managed object from successful BOS construction for topic. This indicates a code bug");
		}
		MoWorkflowObject moWFO = new MoWorkflowObject(mo.getId());
		wfLog.info("Topic imported");

		return moWFO;
	}
	
	public void setTopicPath(String topicPath) {
		this.setParameter(TOPIC_PATH_PARAM, topicPath);
	}
	
	public void setTopicPath(FixedValue topicPath) {
		this.topicPath = topicPath;
	}
    
	public void setParentBrowseTreeFolder(String parentBrowseTreeFolder) {
		this.setParameter(PARENT_BROWSE_TREE_FOLDER_PARAM, parentBrowseTreeFolder);
	}
    
	public void setParentCaNode(String parentCaNode) {
		this.setParameter(PARENT_CA_NODE_PARAM, parentCaNode);
	}

	
	public void setParentCaId (FixedValue parentCaId) {
		this.parentCaId = parentCaId;
	}
	
	public void setMissingGraphicUri(String missingGraphicUri) {
		setParameter(MISSING_GRAPHIC_URI_PARAM, missingGraphicUri);
	}
	
	public void setOpenToolkitName(String openToolkitName) {
		setParameter(OPEN_TOOLKIT_NAME_PARAM, openToolkitName);
	}
	
	public void setCommitMessage(String commitMessage) {
		setParameter(COMMIT_MESSAGE_PARAM, commitMessage);
	}
	
	public void setImportedMoListVarname(String importedMoListVarname) {
		setParameter(IMPORTED_MO_LIST_VARNAME_PARAM, importedMoListVarname);
	}
	
	public void setTopicContainerName(String name) {
		setParameter(TOPIC_CONTAINER_NAME_PARAM, name);
	}

	public void setNonXmlContainerName(String name) {
		setParameter(NONXML_CONTAINER_NAME_PARAM, name);
	}
}
