/**
 * 
 */
package com.reallysi.rsuite.workflow.actions;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.report.StoredReport;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.FileWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.workflow.actions.beans.WorkflowTransformSupportBean;
import com.reallysi.tools.dita.conversion.beans.TransformSupportBean;
import com.reallysi.tools.ditaot.DitaOtAntTaskCommand;
import com.reallysi.tools.ditaot.DitaOtOptions;
import com.rsicms.rsuite.helpers.messages.ProcessMessageContainer;

/**
 * Manages generic setup and execution of DITA Open Toolkit process.
 * <p>
 * </p>
 */
public class DitaOtRunningActionHandler extends BaseWorkflowAction {

	public static final String PARENT_MOID_PARAM = "parentMoId";
	
	public static final String MAX_JAVA_MEMORY_PARAM = "maxJavaMemory";

	public static final String DITAVAL_FILEPATH_PARAM = "ditavalFilepath";

	public static final String CLEAN_TEMP_PARAM = "cleanTemp";

	public static final String XSL_PARAM = "xsl";

	public static final String DRAFT_PARAM = "draft";

	public static final String ONLY_TOPIC_IN_MAP_PARAM = "onlyTopicInMap";

	private static final long serialVersionUID = 1L;

	/**
	 * Specifies the MO ID of the map to process. If not specified, the MO workflow
	 * list is used.
	 */
	public static final String MAP_MO_ID_PARAM = "mapMoId";

	/**
	 * Path of directory to hold exported map. If not specified a temporary
	 * directory is created.
	 */
	public static final String MAP_EXPORT_PATH_PARAM = "mapExportPath";

	/**
	 * Parameter to control cleaning of the output directory before running the
	 * process. Default is "true".
	 */
	public static final String CLEAN_OUTPUT_DIR_PARAM = "cleanOutputDir";

	public static final String EXPORTED_MAP_FILENAME_VARNAME = "exportedMapFilename";

	/**
	 * transtype parameter, e.g. "xhtml", "pdf", "scorm", etc.
	 */
	public static final String TRANSTYPE_PARAM = "transtype";

	/**
	 * The name of the configured Open Toolkit to use. Default is "default". Open
	 * Toolkits are configured in the DITA Open Toolkit properties file in the
	 * RSuite conf/ directory.
	 * <p>
	 * The maps to be processed are listed in the workflow variable
	 * "exportedMapFiles", which is a list of WorkflowFileObjects, one for each
	 * map.
	 * </p>
	 */
	public static final String OPEN_TOOLKIT_NAME_PARAM = "openToolkitName";

	public DitaOtRunningActionHandler() {
		super();
	}

	@Override
	public void execute(WorkflowContext context) throws Exception {
		Log wfLog = context.getWorkflowLog();
		context.setVariable(DitaOtOptions.EXCEPTION_OCCUR, "false");

		String transtype = getVariableWithDefault(context, "transtype", DitaOtOptions.DEFAULT_TRANSTYPE);
		String xsltUrlString = getParameterWithDefault(DitaOtOptions.XSLT_URI_PARAM, DitaOtOptions.DEFAULT_CA_TO_MAP_TRANSFORM_URL);
		String caNodeMoId = resolveVariablesAndExpressions(getParameterWithDefault(DitaOtOptions.CA_NODE_ID_PARAM, ""));
        String forceNewMapStr = resolveVariables(getParameterWithDefault(DitaOtOptions.FORCE_NEW_MAP_PARAM, "false"));
        
		ManagedObjectService moSvc = context.getManagedObjectService();
		User user = context.getAuthorizationService().getSystemUser();
		DitaOtOptions ditaotOptions = new DitaOtOptions(context, user, transtype);
		ManagedObject mo;

		/**
		 * Stream to hold report output
		 */
		StringWriter stringWriter = new StringWriter();

		PrintWriter writer = new PrintWriter(stringWriter);

		ditaotOptions.setMessageWriter(writer);
		ditaotOptions.setLog(wfLog);
		
		File outputDir = getOutputDir(context);

		try {
			cleanOutputDir(context, outputDir);
			ditaotOptions.setOutputDir(outputDir);

			Properties props = new Properties();
			setArgumentIfSpecified(context, props, "onlytopic.in.map", ONLY_TOPIC_IN_MAP_PARAM);
			setArgumentIfSpecified(context, props, "args.draft", DRAFT_PARAM);
			setArgumentIfSpecified(context, props, "args.xsl", XSL_PARAM);
			setArgumentIfSpecified(context, props, "clean.temp", CLEAN_TEMP_PARAM);
			setArgumentIfSpecified(context, props, "dita.input.valfile", DITAVAL_FILEPATH_PARAM);
			setArgumentIfSpecified(context, props, "maxJavaMemory", MAX_JAVA_MEMORY_PARAM);
			props.setProperty(DitaOtOptions.XSLT_URI_PARAM, xsltUrlString);
			props.setProperty(DitaOtOptions.FORCE_NEW_MAP_PARAM, forceNewMapStr);
			props.setProperty(DitaOtOptions.CA_NODE_ID_PARAM, caNodeMoId);
			ditaotOptions.addProperties(props);

			Map<String, Object> vars = context.getWorkflowVariables();
			for (String varKey : vars.keySet()) {
				if (varKey.startsWith("ditaot.")) {
					String propName = varKey.replaceFirst("ditaot.", "");
					String propValue = vars.get(varKey).toString();
					if (StringUtils.isNotEmpty(propValue)) {
						ditaotOptions.setProperty(propName, propValue);
					}
				}
			}

			DitaOtAntTaskCommand ditaot = new DitaOtAntTaskCommand();

			List<ManagedObject> moList = new ArrayList<ManagedObject>();

			String moId = resolveVariablesAndExpressions(getParameter(MAP_MO_ID_PARAM));
			if (StringUtils.isEmpty(moId)) {
				MoListWorkflowObject wfMoList = context.getMoListWorkflowObject();
				for (MoWorkflowObject obj : wfMoList.getMoList()) {
					moId = obj.getMoid();
					mo = moSvc.getManagedObject(user, moId);
					moList.add(mo);
				}
			} else {
				mo = moSvc.getManagedObject(user, moId);
				moList.add(mo);
			}
			
			if (moList.size() > 0 && moList.get(0).isAssemblyNode()) {
				context.setVariable("parentMoId", moList.get(0).getId());
			} else if (moList.size() > 0) {
				//Need a core variable for parent id
				ManagedObject primaryMo = moList.get(0);
				//If Mo is refd by multiple folders, this will catch all of them
				List<String> refs = primaryMo.getDirectReferenceIds();
				if (refs.size() > 0) {
					String refId = refs.get(0);
					ManagedObject refMo = context.getManagedObjectService().getManagedObject(user, refId);
					String[] ancIds = refMo.getAncestorIds();
					if (ancIds != null) {
						context.setVariable("parentMoId", (String) ancIds[0]);
					}
				}
			}

			wfLog.info("execute(): Generating output from MO...");
			ditaot.execute(moList, ditaotOptions);
			wfLog.info("execute(): " + transtype + " generated");

			// Make the output directory the new workflow file object so we can do
			// something with
			// it in a follow-on task, such as copy it or zip it up or whatever.
			context.setFileWorkflowObject(new FileWorkflowObject(outputDir));
			
			StoredReport report = ditaotOptions.getStoredReport();
			ditaotOptions.addInfoMessage("ditaot", transtype, "report log: '/rsuite/rest/v1/report/generated/" + report.getId() + "");
			context.setVariable("outputDir", outputDir.getAbsolutePath());
			context.setVariable("antReportId", report.getId());
			context.setVariable("antReportFileName", "result.log");

		} catch (RSuiteException e) {
			ditaotOptions.addFailureMessage("RSuiteException", transtype, "Unexpected RSuite exception: " + e.getMessage(), e);
			context.setVariable(DitaOtOptions.EXCEPTION_OCCUR, "true");
		} catch (Throwable t) {
			wfLog.error("execute(): Unexpected " + t.getClass().getSimpleName() + ": " + t.getMessage(), t);
			ditaotOptions.addFailureMessage("RSuiteException", transtype,
																			t.getClass().getSimpleName() + ": " + t.getMessage());
			context.setVariable(DitaOtOptions.EXCEPTION_OCCUR, "true");
		}

		ProcessMessageContainer messages = ditaotOptions.getMessages();

		wfLog.info("ditaot end().");
	}

	protected File getOutputDir(WorkflowContext context) throws Exception, RSuiteException {
		String outputPath = context.getVariableAsString(DitaOtOptions.OUTPUT_PATH_PARAM);
		if (outputPath == null || outputPath.isEmpty()) {
			String transtype = context.getVariableAsString("transtype");
			File tempDir = null;
			try {
				tempDir = context.getRSuiteServerConfiguration().getTmpDir();
			} catch (Exception e) {
				reportAndThrowRSuiteException("Failed to get a temporary directory: " + e.getMessage());
			} 
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
			//Include plain transtype at end so zipped uploads will have a useful name
			outputPath = tempDir.getAbsolutePath() + File.separator + transtype + "-" + sdf.format(new Date()) + File.separator + transtype;
			context.setVariable(DitaOtOptions.OUTPUT_PATH_PARAM, outputPath);
		}
		File outputDir = null;

		if (outputPath == null || "".equals(outputPath.trim())) {
			outputDir = WorkflowTransformSupportBean.getWorkingDir(context, false);
		} else {
			outputDir = new File(outputPath);
			if (!outputDir.exists()) {
				outputDir.mkdirs();
			}
			if (!outputDir.exists()) {
				reportAndThrowRSuiteException("Failed to find or create output directory \"" + outputPath + "\"");
			}
			if (!outputDir.canWrite()) {
				reportAndThrowRSuiteException("Cannot write to output directory \"" + outputPath + "\"");
			}
		}
		return outputDir;
	}

	protected File getExportDir(WorkflowContext context, ManagedObject mo) throws RSuiteException {
		String outputPath = context.getVariableAsString(MAP_EXPORT_PATH_PARAM);
		File exportDir = null;

		if (outputPath == null || "".equals(outputPath.trim())) {
			try {
				exportDir = WorkflowTransformSupportBean.getWorkingDir(context, false);
			} catch (Exception e) {
				reportAndThrowRSuiteException("Failed to get working directory");
			}
		} else {
			// Create a map-specific directory in the specified output directory:
			exportDir = new File(outputPath);
			String mapName = "map_" + mo.getId();
			// If there's an alias, assume it's the map's file name:
			if (mo.getAliases().length > 0) {
				// FIXME: This bit is to handle 3.3.1/3.3.2 API change
				// for mo.getAliases() from String[] to Alias[];
				Object alias = mo.getAliases()[0];
				String aliasStr = alias.toString();
				mapName = FilenameUtils.getBaseName(aliasStr);
			}

			exportDir = new File(exportDir, mapName);

			if (!exportDir.exists()) {
				exportDir.mkdirs();
			}
			if (!exportDir.exists()) {
				reportAndThrowRSuiteException("Failed to find or create output directory \"" + exportDir.getAbsolutePath()
																							+ "\"");
			}
			if (!exportDir.canWrite()) {
				reportAndThrowRSuiteException("Cannot write to output directory \"" + exportDir.getAbsolutePath()
																								+ "\"");
			}
		}

		return exportDir;
	}

	/**
	 * @param context
	 * 
	 */
	public void cleanExportDir(WorkflowContext context, ManagedObject mo) {
		try {
			String cleanOutputDirStr = getVariableWithDefault(context, CLEAN_OUTPUT_DIR_PARAM, "true");
			boolean cleanOutputDir = Boolean.valueOf(cleanOutputDirStr);
			if (cleanOutputDir) {
				File exportDir = getExportDir(context, mo);
				context.getWorkflowLog().info("Cleaning directory " + exportDir.getAbsolutePath() + "...");
				FileUtils.deleteDirectory(exportDir);
				exportDir.mkdirs();
			}
		} catch (Exception e) {
			context.getWorkflowLog().error(	"Unexpected exception cleaning directory: " + e.getClass().getSimpleName()
																							+ " - " + e.getMessage(), e);
		}
	}

	/**
	 * @param context
	 * 
	 */
	public void cleanOutputDir(WorkflowContext context, File outputDir) {
		try {
			String cleanOutputDirStr = getVariableWithDefault(context, CLEAN_OUTPUT_DIR_PARAM, "true");
			boolean cleanOutputDir = Boolean.valueOf(cleanOutputDirStr);
			if (cleanOutputDir) {
				context.getWorkflowLog().info("Cleaning directory " + outputDir.getAbsolutePath() + "...");
				FileUtils.deleteDirectory(outputDir);
				outputDir.mkdirs();
			}
		} catch (Exception e) {
			context.getWorkflowLog().error(	"Unexpected exception cleaning directory: " + e.getClass().getSimpleName()
																							+ " - " + e.getMessage(), e);
		}
	}

	public String getVariableWithDefault(WorkflowContext context, String varName, String defaultValue) throws RSuiteException {
		String val = context.getVariableAsString(varName);
		if (StringUtils.isEmpty(val)) {
			val = defaultValue;
		}
		return val;
	}

	public void setArgumentIfSpecified(WorkflowContext context, Properties props, String toolkitParamName,
																			String propParamName) {
		String value = context.getVariableAsString(propParamName);
		if (value != null && !"".equals(value.trim())) {
			context.getWorkflowLog().info("Setting parameter " + toolkitParamName + " to value \"" + value + "\"");
			props.setProperty(toolkitParamName, value);
		}
	}

	public void setParentMoId(String parentMoId) {
		this.setParameter(PARENT_MOID_PARAM, parentMoId);
	}
	
	public void setTranstype(String transtype) {
		this.setParameter(TRANSTYPE_PARAM, transtype);
	}

	public void setOutputPath(String outputPath) {
		this.setParameter(DitaOtOptions.OUTPUT_PATH_PARAM, outputPath);
	}

	public void setOpenToolkitName(String openToolkitName) {
		this.setParameter(OPEN_TOOLKIT_NAME_PARAM, openToolkitName);
	}

	public void setMapExportPath(String mapExportPath) {
		this.setParameter(MAP_EXPORT_PATH_PARAM, mapExportPath);
	}

	public void setCleanOuptutDir(String cleanOutputDir) {
		this.setParameter(CLEAN_OUTPUT_DIR_PARAM, cleanOutputDir);
	}

	public void setMapMoId(String mapMoId) {
		this.setParameter(MAP_MO_ID_PARAM, mapMoId);
	}

	public void setMaxJavaMemory(String maxJavaMemory) {
		this.setParameter(MAX_JAVA_MEMORY_PARAM, maxJavaMemory);
	}

	public void setForceNewMap(String forceNewMap) {
		this.setParameter(DitaOtOptions.FORCE_NEW_MAP_PARAM, forceNewMap);
	}

	public void setXsltUri(String xsltUri) {
		this.setParameter(DitaOtOptions.XSLT_URI_PARAM, xsltUri);
	}

	public void setCaNodeId(String caNodeId) {
		this.setParameter(DitaOtOptions.CA_NODE_ID_PARAM, caNodeId);
	}
}