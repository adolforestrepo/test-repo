package com.reallysi.rsuite.workflow.actions;

import static org.apache.commons.lang3.StringUtils.*;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.FileWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.tools.ditaot.DitaOTUtils;
import com.reallysi.tools.ditaot.DitaOtOptions;

/**
 * Action handler to zip the contents of the workflow output directory or select
 * single output file. Then add it as the workflow object to prepare for
 * attaching to the browse tree.
 * 
 * @author luis
 * 
 */
public class PrepareResultToWorkflowFileObjectActionHandler extends BaseWorkflowAction {

	@SuppressWarnings("unused")
	private Log classLog = LogFactory.getLog(PrepareResultToWorkflowFileObjectActionHandler.class);

	@Override
	public void execute(WorkflowContext context) throws Exception {
		Log log = context.getWorkflowLog();
		log.info("Entered to attachResultToWO");

		FileWorkflowObject wo = context.getFileWorkflowObject();
		if (wo == null) {
			log.info("Unexpected empty workflow object. Expected exactly one output dir.");
			return;
		}
		
		String uniqueId = "unknown";
		String[] rs = context.getRSuiteContents();
		if (rs != null) {
			uniqueId = rs[0];
		}
		
		File outputDir = wo.getFile();
		String outputExtension = defaultString(context.getVariableAsString(DitaOtOptions.OUTPUT_EXTENSION));
		File[] resultFiles = DitaOTUtils.getTransformedFiles(outputDir, outputExtension);
		
		String zipOutput = context.getVariableAsString(DitaOtOptions.ZIP_OUTPUT_PARAM);
		if (zipOutput == null || StringUtils.equals(zipOutput, "false")) {
			attachResultToWO(context, resultFiles[0]);
		} else if (StringUtils.equals(zipOutput, "true")) {
			log.info("Entered to zipDirectoryToWO");

			String baseWorkFolder = context.getConfigurationProperties().getProperty("rsuite.workflow.baseworkfolder", "/tmp");
			String workFolderPath = baseWorkFolder + "/" + context.getWorkflowInstanceId();
			DitaOTUtils.zipDirectoryToWO(context, outputDir, uniqueId, workFolderPath, log);
		}		
	}

	public void attachResultToWO(WorkflowContext context, File resultFile) throws Exception {
		Log log = context.getWorkflowLog();
		log.info("Entered to attachResultToWO");

		log.info("adding to workflow: " + resultFile.getName());

		context.setFileWorkflowObject(new FileWorkflowObject(resultFile));
		context.setVariable("fullFileName", resultFile.getName());
		context.setVariable("baseFileName", FilenameUtils.getBaseName(resultFile.getName()));
		context.setVariable("extension", FilenameUtils.getExtension(resultFile.getName()));

		log.info("Done.");
	}

}
