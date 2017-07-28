/**
 * Copyright (c) 2009 Really Strategies, Inc.
 */
package com.reallysi.rsuite.workflow.actions;

import java.util.Properties;

import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.api.xml.DitaOpenToolkit;

/**
 * Runs the generic XHTML transform type.
 */
public class DitaOtXhtmlTaskRunningActionHandler extends DitaOtXhtmlTaskRunningActionHandlerBase {

	public static final String DEFAULT_TRANSTYPE = "xhtml";
	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see com.reallysi.rsuite.api.workflow.AbstractBaseNonLeavingActionHandler#execute(com.reallysi.rsuite.api.workflow.WorkflowExecutionContext)
	 */
	@Override
	public void execute(WorkflowContext context) throws Exception {
		Log wfLog = context.getWorkflowLog();
		
		String transtype = resolveVariables(getParameterWithDefault("transtype", DEFAULT_TRANSTYPE));
		// Handle case where workflow process has <transtype>${transtype}</transtype>
		//  but ${transtype} isn't set.
		if (transtype == null || transtype.startsWith("${")) {
			transtype = DEFAULT_TRANSTYPE; 
		}

		DitaOpenToolkit toolkit = getToolkit(context, wfLog);

		Properties props = setBaseTaskProperties(context, transtype, toolkit);

		cleanOutputDir(context, getOutputDir(context));

		applyToolkitProcessToMos(context, wfLog, toolkit, transtype, props);
		
    }

}
