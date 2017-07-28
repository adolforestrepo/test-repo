/**
 * Copyright (c) 2010, 2012 Really Strategies, Inc.
 */
package com.reallysi.rsuite.workflow.actions;

import java.io.File;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.workflow.activiti.FileWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.api.xml.DitaOpenToolkit;

/**
 * Runs the DITA for Publishers Kindle (.mobi) transform type.
 */
public class DitaOtKindleTaskRunningActionHandler extends DitaOtEPUBTaskRunningActionHandler {

	public static final String DEFAULT_TRANSTYPE = "kindle";
	public static final String KINDLEGEN_EXECUTABLE_PARAM = "kindlegenExecutable";
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
		
		// Kindle-specific arguments:
		
		setArgumentIfSpecified(context, props, "kindlegen.executable", KINDLEGEN_EXECUTABLE_PARAM);
		setEpubPropertiesIfSpecified(context, props);

		cleanOutputDir(context, getOutputDir(context));

		applyToolkitProcessToMos(context, wfLog, toolkit, transtype, props);
		
		// Make the generated EPUB the context item.
		String filename = context.getVariableAsString("exportedMapFilename");
		String outputDir = context.getVariableAsString("outputDir");
		if (filename != null && outputDir != null) {
			String epubName = FilenameUtils.getBaseName(filename) + ".mobi";
			File epubFile = new File(new File(outputDir), epubName);
			FileWorkflowObject fileWO = new FileWorkflowObject(epubFile);
			context.setFileWorkflowObject(fileWO);
			context.setVariable("epubFilename", epubName);
			context.setVariable("epubFilepath", epubFile.getAbsolutePath());
		} else {
			context.getWorkflowLog().error("No exportedMapFilename or outputDir variables set");
		}
			
		
    }
	
	public void setKindlegenExecutable(String kindlegenExecutable) {
		this.setParameter(KINDLEGEN_EXECUTABLE_PARAM, kindlegenExecutable);
	}

}
