/**
 * Copyright (c) 2009 Really Strategies, Inc.
 */
package com.reallysi.rsuite.workflow.actions;

import java.io.File;
import java.util.Properties;

import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.system.AntHelper;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;

/**
 *
 */
public class AntTaskRunningActionHandler extends DitaOtAntTaskRunningActionHandlerBase {

	private static final long serialVersionUID = 1L;

	
	/* (non-Javadoc)
	 * @see com.reallysi.rsuite.api.workflow.AbstractBaseNonLeavingActionHandler#execute(com.reallysi.rsuite.api.workflow.WorkflowExecutionContext)
	 */
	@Override
	public void execute(WorkflowContext context) throws Exception {
		Log wfLog = context.getWorkflowLog();
		File buildFile = getBuildFile(context, wfLog);		
	
		this.checkParamsNotEmptyOrNull(BUILD_FILE_PATH_PARAM);
		String buildTarget = getParameter(BUILD_TARGET_PARAM);
		buildTarget = resolveVariablesAndExpressions(buildTarget);
		
		String buildProperties = getParameter(BUILD_PROPERTIES_PARAM);
		buildProperties = resolveVariablesAndExpressions(buildProperties);

		String reportId = "ant-task-" + context.getWorkflowInstance().getId() + "-" + getNowString() + ".log";

		// Set up the Ant project then run it:
		
		Properties props = new Properties();
		
		props = parseBuildPropertiesString(wfLog, buildProperties);
		wfLog.info("Ant log will be report with ID \"" + reportId + "\"");
		
		String antReportIdVarName = getParameterWithDefault(ANT_REPORT_ID_VAR_NAME_PARAM, ANT_REPORT_ID_VARNAME);
		antReportIdVarName = resolveVariablesAndExpressions(antReportIdVarName);
		
		context.setVariable(antReportIdVarName, reportId);
		
		AntHelper antHelper = context.getConfigurationService().constructAntHelper();

		antHelper.setupAndRunAntProject(context, wfLog, buildFile, props, reportId);
	}
}