/**
 * Copyright (c) 2009 Really Strategies, Inc.
 */
package com.reallysi.rsuite.workflow.actions;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.transform.Transformer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.w3c.dom.Document;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.workflow.activiti.BusinessRuleException;
import com.reallysi.rsuite.api.workflow.activiti.FileWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.ProcessedWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.api.xml.DitaOpenToolkit;
import com.reallysi.tools.dita.BosConstructionOptions;
import com.reallysi.tools.dita.BosMember;
import com.reallysi.tools.dita.BosVisitor;
import com.reallysi.tools.dita.BoundedObjectSet;
import com.reallysi.tools.dita.BrowseTreeConstructingBosVisitor;
import com.reallysi.tools.dita.DitaMapImportOptions;
import com.reallysi.tools.dita.DitaUtil;
import com.reallysi.tools.dita.DomUtil;
import com.reallysi.tools.dita.PluginVersionConstants;
import com.reallysi.tools.dita.RSuiteDitaHelper;

/**
 * Takes one or more DITA map files and imports each map and its dependencies
 * to RSuite.
 */
public class MapImporterActionHandler extends RSuiteDitaSupportActionHandlerBase {

	/**
	 * 
	 */
	public static final String MAP_CA_NODE_NAME_PARAM = "mapCaNodeName";

	/**
	 * Path of the root map to process. Use if map roots are not in FileWorkflowObject.
	 */
	public static final String ROOT_MAP_PATH_PARAM = "rootMapPath";
	
	/**
	 * Filename of the map. Will be looked for in the working directory.
	 */
	public static final String MAP_FILENAME_PARAM = "mapFileName";
	
	/**
	 * The absolute path of the browse tree folder into which the map is
	 * imported. e.g. "/maps/mystuff"
	 */
	public static final String PARENT_BROWSE_TREE_FOLDER_PARAM = "parentBrowseTreeFolder";
	
	/**
	 * The path of the content assembly or node within the parent folder
	 * into which the map is imported, e.g. "books/reference" to create CA "books", CA Node "reference".
	 */
	public static final String PARENT_CONTAINER_PARAM = "parentCaNode";
	
	/**
	 * Optional. URI of the graphic to use for missing graphics.
	 */
	public static final String MISSING_GRAPHIC_URI_PARAM = "missingGraphicUri";

	/**
	 * (Optional) Name of topic container name to use.
	 * <p>If not specified, "<tt>content</tt>" will be used.
	 * </p>
	 */
	public static final String TOPIC_CONTAINER_NAME_PARAM = "topicContainerName";

	/**
	 * (Optional) Name of non-xml container name to use.
	 * <p>If not specified, "<tt>media</tt>" will be used.
	 * </p>
	 */
	public static final String NONXML_CONTAINER_NAME_PARAM = "nonXmlContainerName";

	/**
	 * (Optional) name of the variable to hold the MO ID of the imported
	 * root map. Default is "mapMoId".
	 */
	public static final String MAP_MO_ID_VAR_NAME_PARAM = "mapMoIdVarName";

	/**
	 * (Optional) name of the variable to hold the content assembly ID of the CA
	 * that is the direct parent of the imported map.
	 */
	public static final String MAP_CA_ID_VAR_NAME_PARAM = "mapCaIdVarName";

  /**
   * Specifies the message to use for any commits to the repository. Default
   * value is "Automatic update".
   */
	public static final String COMMIT_MESSAGE_PARAM = "commitMessage";

	/**
	 * Option to skip the generation of filaname alias. Must be set by other method.
	 */
	public static final String GENERATE_ALIAS_PARAM = "generateAlias";

	/**
	 * Option to create a new CaNode for the root map.
	 */
	public static final String CREATE_CONTAINER_FOR_ROOTMAP_PARAM = "createCaNodeForRootMap";

	/* (non-Javadoc)
	 * @see com.reallysi.rsuite.api.workflow.AbstractBaseNonLeavingActionHandler#execute(com.reallysi.rsuite.api.workflow.WorkflowExecutionContext)
	 */
	@Override
	public void execute(WorkflowContext context) throws Exception {
		Log wfLog = context.getWorkflowLog();
		wfLog.info("Using rsuite-dita-support version " + PluginVersionConstants.getVersion() + ".");
		
		this.checkParamsNotEmptyOrNull(PARENT_BROWSE_TREE_FOLDER_PARAM, PARENT_CONTAINER_PARAM);
		
		String mapCaNodeNameParam = resolveVariablesAndExpressions(getParameter(MAP_CA_NODE_NAME_PARAM));
		
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
		
		File mapFile = null;

		mapFile = findMapDoc(context, wfLog);
		if (mapFile == null || !mapFile.exists()) {
			String msg = "Cannot find map file.";
			reportAndThrowRSuiteException(msg);
		}
		
		boolean errorOccured = false;
		String errorMessage = "";
		ArrayList<ProcessedWorkflowObject> errorFiles = new ArrayList<ProcessedWorkflowObject>();
		ArrayList<MoWorkflowObject> importedMaps = new ArrayList<MoWorkflowObject>();
		
		StringBuilder validationReport = new StringBuilder("Map import validation report.\n\n")
		.append(" + [INFO] Root map document: " + mapFile.getAbsolutePath())
		.append("\n\n");
		
		BosConstructionOptions domOptions = new BosConstructionOptions(wfLog, validationReport, catalogs);
		
		String port = context.getRSuiteServerConfiguration().getPort();
		String host = context.getRSuiteServerConfiguration().getHostName();
		Transformer validationReportTransform = context.getXmlApiManager().
		getTransformer(new URL("http://" + host + ":" + port + 
				"/rsuite/rest/v1/static/rsuite-dita-support/validation-report/validation-report.xsl"));
		domOptions.setReportSerializationTransform(validationReportTransform);
		
		Document doc = null;
		try {
			wfLog.info("Parsing map file \"" + mapFile.getAbsolutePath() + "\"...");
			doc = DomUtil.getDomForDocument(mapFile, domOptions, true);
		} catch (Exception e) {
			String msg = "Exception parsing file \"" + mapFile.getAbsolutePath() + "\": " + e.getMessage();
			captureValidationReport(context, mapFile, validationReport);
			reportAndThrowRSuiteException(msg);			
		}
		
		BosMember rootMember = null; // Will hold the BOS member for the root map
		
		if (DitaUtil.isDitaMap(doc.getDocumentElement())) {
			try {
				DitaMapImportOptions importOptions = new DitaMapImportOptions();
				
				String mapContainerName = FilenameUtils.getBaseName(mapFile.getName());
				if (mapCaNodeNameParam != null && !"".equals(mapCaNodeNameParam)) {
					mapContainerName = mapCaNodeNameParam;
				}
				importOptions.setRootFolder(
						resolveVariablesAndExpressions(getParameter(PARENT_BROWSE_TREE_FOLDER_PARAM)));
				importOptions.setRootContentAssemblyPath(
						resolveVariablesAndExpressions(getParameter(PARENT_CONTAINER_PARAM)) + 
						("".equals(mapContainerName) ? "" : "/" + mapContainerName));
				
				importOptions.setMissingGraphicUri( 
						resolveVariablesAndExpressions(getParameterWithDefault(MISSING_GRAPHIC_URI_PARAM, "/rsuite-dita-support/images/missingGraphic")));
				importOptions.setTopicContainerName( 
						resolveVariablesAndExpressions(getParameterWithDefault(TOPIC_CONTAINER_NAME_PARAM,
										"content")));
				importOptions.setNonXmlContainerName( 
						resolveVariablesAndExpressions(getParameterWithDefault(NONXML_CONTAINER_NAME_PARAM,
										"media")));
				importOptions.setUser(context.getAuthorizationService().getSystemUser());

				importOptions.setGenerateAlias( "yes".equals( getParameterWithDefault( GENERATE_ALIAS_PARAM, "yes")));
				
				BoundedObjectSet bos = null;
				try {
					bos = importMap(context, doc, domOptions, importOptions);
				} catch (Exception e) {		
					wfLog.error("Unexpected exception importing map: ");
					wfLog.error(getStackTrace(e));
					reportAndThrowRSuiteException(e.getMessage());
				}
				
				if (bos.hasInvalidMembers()) {
					reportAndThrowRSuiteException("Map has invalid members.");			
				}
				
				BosVisitor visitor = new BrowseTreeConstructingBosVisitor(context,
				                                                          context.getAuthorizationService().getSystemUser(),
				                                                          importOptions, wfLog);
				// The MO in this context is the CA into which the map is loaded.
				importOptions.setCreateCaNodeForRootMap( "yes".equals( getParameterWithDefault( CREATE_CONTAINER_FOR_ROOTMAP_PARAM, "yes"))); // Create root map in specified CA node (e.g., "xml").
				ManagedObject ca = null;
				try {
					ca = RSuiteDitaHelper.loadMapToBrowseTree(context, bos, importOptions, visitor);
				} catch (Exception e) {
					reportAndThrowRSuiteException("Unexpected exception loading map to browse tree: " + e.getClass().getSimpleName() + " - " + e.getMessage());			
				}
				wfLog.info("Map imported");

				rootMember = bos.getRoot();
				if (rootMember == null)
					throw new RSuiteException("Failed to get root member for BOS following apparently-successful BOS import. This should not happen.");
				importedMaps.add(new MoWorkflowObject(ca.getId()));
			} catch (Exception e) {
				errorOccured = true;
				errorMessage = errorMessage + "Failed File: [" + mapFile.getName() + "]\r\n" + e + "\r\n\r\n";
				
				context.setVariable(EXCEPTION_MESSAGE_VAR, errorMessage);
				context.addComment("RSuite", errorMessage);
				
				errorFiles.add(new FileWorkflowObject(mapFile));
				// e.printStackTrace();
				wfLog.error(e);
				

			}
		} else {
			wfLog.info("File " + mapFile.getName() + " does not appear to be a DITA map, skipping.");
		}

		if (!isPreserveWorkflowContext(context))
			context.setMoListWorkflowObject(new MoListWorkflowObject(importedMaps));
		
		if(errorOccured) {
			wfLog.error("Errors occurred during map import, throwing exception");
			context.setAttribute("errorFiles", errorFiles.toString());
			wfLog.error("Errors from map imports");
			// FIXME: This next bit is just a placeholder for more complete error handling.
			BusinessRuleException e = new BusinessRuleException(errorFiles.toString());
			e.setFailureDetail(getParameter(FAILURE_DETAIL_PARAM));
			e.setTaskname("Import DITA maps");

			captureValidationReport(context, mapFile, validationReport);
			
			throw e;
		} else {
			wfLog.info("Maps imported successfully");
		}

		if (importedMaps.size() > 0) {
			String mapCaIdVarName = resolveVariablesAndExpressions(getParameterWithDefault(MAP_CA_ID_VAR_NAME_PARAM, "mapCaId"));
			context.setVariable(mapCaIdVarName, importedMaps.get(0).getMoid());
		}
		if (rootMember != null) {
			String mapMoIdVarName = resolveVariablesAndExpressions(getParameterWithDefault(MAP_MO_ID_VAR_NAME_PARAM, "mapMoId"));
			context.setVariable(mapMoIdVarName, rootMember.getManagedObject().getId());
		}
		
		wfLog.info("Done");
    }
	
	 public static String getStackTrace(Throwable aThrowable) {
		    final Writer result = new StringWriter();
		    final PrintWriter printWriter = new PrintWriter(result);
		    aThrowable.printStackTrace(printWriter);
		    return result.toString();
		  }



	/**
	 * If the input is in the file workflow object, it very likely
	 * came from a zip file, and the root map could be below the root path,
	 * so look for it, otherwise, just do the import.
	 * @param context
	 * @param wfLog
	 * @return
	 * @throws RSuiteException
	 */
	protected File findMapDoc(WorkflowContext context, Log wfLog)
			throws RSuiteException {
		File mapFile = null;
		String rootMapPath = resolveVariablesAndExpressions(getParameter(ROOT_MAP_PATH_PARAM));
		rootMapPath = resolveVariablesAndExpressions(rootMapPath);
		
		if (rootMapPath == null || "".equals(rootMapPath.trim())) {
			FileWorkflowObject fileWFO = context.getFileWorkflowObject();
			if (fileWFO == null) {
				String msg = "No file in the workflow context. Nothing to do";
				context.setVariable(EXCEPTION_MESSAGE_VAR, msg);
				context.addComment("RSuite", msg);
				wfLog.warn(msg);
				return null;
			}
			File candFile = fileWFO.getFile();
			if (candFile.getName().endsWith(".ditamap"))
				return candFile;
			
			if (candFile.isDirectory()) {
				candFile = candFile.getParentFile(); // Look in the work directory first, in case the zip didn't have a root directory.
				String mapFileName = resolveVariablesAndExpressions(this.getParameter(MAP_FILENAME_PARAM));
				mapFileName = resolveVariablesAndExpressions(mapFileName);
				if (mapFileName == null || "".equals(mapFileName.trim())) {
					String msg = "Must specify " + MAP_FILENAME_PARAM + " parameter if " + ROOT_MAP_PATH_PARAM + " parameter not specified.";
					reportAndThrowRSuiteException(msg);
				}
				mapFile = lookForFilenameInDirTree(candFile, mapFileName);
				// Search the directory for the target file.
			}
		} else {
			mapFile = new File(rootMapPath);
			if (!mapFile.exists()) {
				String msg = "Specified root map file \"" + rootMapPath + "\" does not exist. Nothing ";
				reportAndThrowRSuiteException(msg);
			}
		}
		
		return mapFile;
	}


	/**
	 * @param context
	 * @param domOptions 
	 * @param bos 
	 * @param outputPath 
	 * @param mo
	 */
	private BoundedObjectSet importMap(WorkflowContext wfContext, Document mapDoc, BosConstructionOptions domOptions,
	                                   DitaMapImportOptions importOptions) throws RSuiteException {
		Log wfLog = wfContext.getWorkflowLog();

		BoundedObjectSet bos = RSuiteDitaHelper.importMap(wfContext, wfLog, wfContext.getAuthorizationService().getSystemUser(), 
		                                                  mapDoc, domOptions, importOptions);
		return bos;
	}
	
	public void setRootMapPath(String rootMapPath) {
		this.setParameter(ROOT_MAP_PATH_PARAM, rootMapPath);
	}
    
	public void setMapFileName(String mapFileName) {
		this.setParameter(MAP_FILENAME_PARAM, mapFileName);
	}
    
	public void setParentBrowseTreeFolder(String parentBrowseTreeFolder) {
		this.setParameter(PARENT_BROWSE_TREE_FOLDER_PARAM, parentBrowseTreeFolder);
	}
    
	public void setParentCaNode(String parentCaNode) {
		this.setParameter(PARENT_CONTAINER_PARAM, parentCaNode);
	}
    
	public void setMapCaNodeName(String mapCaNodeName) {
		this.setParameter(MAP_CA_NODE_NAME_PARAM, mapCaNodeName);
	}
    
	public void setValidationReportIdVarName(String validationReportIdVarName) {
		setParameter(VALIDATION_REPORT_ID_VAR_NAME_PARAM, validationReportIdVarName);
	}
	
	public void setValidationReportFileNameVarName(String validationReportFileNameVarName) {
		setParameter(VALIDATION_REPORT_FILENAME_VAR_NAME_PARAM, validationReportFileNameVarName);
	}
	
	public void setMissingGraphicUri(String missingGraphicUri) {
		setParameter(MISSING_GRAPHIC_URI_PARAM, missingGraphicUri);
	}
	
	public void setTopicContainerName(String name) {
		setParameter(TOPIC_CONTAINER_NAME_PARAM, name);
	}
	
	public void setNonXmlContainerName(String name) {
		setParameter(NONXML_CONTAINER_NAME_PARAM, name);
	}
	
	public void setMapMoIdVarName(String mapMoIdVarName) {
		setParameter(MAP_MO_ID_VAR_NAME_PARAM, mapMoIdVarName);
	}
	
	public void setMapCaIdVarName(String mapCaIdVarName) {
		setParameter(MAP_CA_ID_VAR_NAME_PARAM, mapCaIdVarName);
	}
	
	public void setCommitMessage(String commitMessage) {
		setParameter(COMMIT_MESSAGE_PARAM, commitMessage);
	}

	public void setGenerateAlias(String generateAlias) {
		setParameter(GENERATE_ALIAS_PARAM, generateAlias);
	}

	public void setCreateCaNodeForRootMap(String createCaNodeForRootMap) {
		setParameter( CREATE_CONTAINER_FOR_ROOTMAP_PARAM, createCaNodeForRootMap);
	}

}
