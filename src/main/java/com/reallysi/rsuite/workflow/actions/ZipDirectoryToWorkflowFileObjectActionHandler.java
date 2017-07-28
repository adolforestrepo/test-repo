package com.reallysi.rsuite.workflow.actions;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.FileWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.rsicms.rsuite.helpers.utils.ZipUtil;

/**
 * Action handler to zip the contents of the workflow output directory and add
 * it as the workflow object to prepare for attaching to the browse tree.
 * 
 * @author luis
 * 
 */
public class ZipDirectoryToWorkflowFileObjectActionHandler extends BaseWorkflowAction {

	@SuppressWarnings("unused")
	private Log classLog = LogFactory.getLog(ZipDirectoryToWorkflowFileObjectActionHandler.class);

	@Override
	public void execute(WorkflowContext context) throws Exception {
		Log log = context.getWorkflowLog();
		log.info("Entered to ZipDirectoryToWO");

		String baseWorkFolder = context.getConfigurationProperties().getProperty("rsuite.workflow.baseworkfolder", "/tmp");
		String workFolderPath = baseWorkFolder + "/" + context.getWorkflowInstanceId();
		File contentsDir = new File(workFolderPath, "contents");
		contentsDir.mkdirs();
		File ditaDir = new File(workFolderPath, "dita");
		ditaDir.mkdirs();

		FileWorkflowObject wo = context.getFileWorkflowObject();
		if (wo == null) {
			log.info("Unexpected empty workflow object. Expected exactly one output dir.");
			return;
		}

		File outputDir = wo.getFile();
		File zipFile = new File(ditaDir, outputDir.getName() + ".zip");

		// check to see if this directory exists
		if (!outputDir.isDirectory()) {
			log.info("Unexpected empty workflow object. Expected exactly one output dir.");
			log.info(outputDir.getName() + " is not a directory");
		} else {
			log.info("Zipping directory " + outputDir.getAbsolutePath() + " to Zip file " + zipFile.getAbsolutePath() + "...");
			try {
				ZipUtil.zipFolder(outputDir.getAbsolutePath(), zipFile.getAbsolutePath());
			} catch (Exception e) {
				log.error(e.getClass().getSimpleName() + " exception creating Zip file: " + e.getMessage(), e);
				context.setVariable("EXCEPTION_OCCUR", "true");
			}
			log.info("Zip Complete");
		}

		String fullFileName = zipFile.getName();
		String baseFileName = FilenameUtils.getBaseName(fullFileName);
		String extension = FilenameUtils.getExtension(fullFileName);

		context.setFileWorkflowObject(new FileWorkflowObject(zipFile));

		context.setVariable("fullFileName", fullFileName);
		context.setVariable("baseFileName", baseFileName);
		context.setVariable("extension", extension);

		log.info("Done.");
	}

}
