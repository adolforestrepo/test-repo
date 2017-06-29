package com.reallysi.tools.ditaot;

import static org.apache.commons.lang3.StringUtils.*;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.workflow.activiti.FileWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.rsicms.rsuite.helpers.utils.ZipUtil;

/**
 * Class to have several utilitarian methods used by several classes.
 * 
 * @author RSI Content Solutions.
 */
public class DitaOTUtils {
	
	/**
	 * Method to set some values to some context variables when using a Workflow.
	 * 
	 * @param wfContext The workflow context.
	 * @param zipFile The zip file generated.
	 */
	private static void setContextVariableValues(WorkflowContext wfContext, File zipFile) {
		String fullFileName = zipFile.getName();
		String baseFileName = FilenameUtils.getBaseName(fullFileName);
		String extension = FilenameUtils.getExtension(fullFileName);
		
		wfContext.setFileWorkflowObject(new FileWorkflowObject(zipFile));
		
		wfContext.setVariable("fullFileName", fullFileName);
		wfContext.setVariable("baseFileName", baseFileName);
		wfContext.setVariable("extension", extension);
	}
	
	/**
	 * Method that will ZIP a directory
	 * 
	 * @param outputDir The output directory in which the outputs will be generated.
	 * @param uniqueId  An ID (probably moid) that will be added to the zip file name to make it unique in RSuite.
	 * @param workFolderPath The path of the folder that will get compressed as a ZIP file.
	 * @param log To log the output.
	 * @return
	 * @throws Exception
	 */
	public static File zipDirectoryToWO(File outputDir, String uniqueId, String workFolderPath, Log log) throws Exception {
		return zipDirectoryToWO(null, outputDir, uniqueId, workFolderPath, log);
	}
	
	/**
	 * Method that will ZIP a directory
	 * 
	 * @param wfContext The workflow context.
	 * @param outputDir The output dir in which the file will be created.
	 * @param uniqueId  An ID (probably moid) that will be added to the zip file name to make it unique in RSuite.
	 * @param workFolderPath The path of the folder that will get compressed as a
	 *          ZIP file.
	 * @param log To log the output.
	 * @return
	 * @throws Exception
	 */
	public static File zipDirectoryToWO(WorkflowContext wfContext, File outputDir, String uniqueId, String workFolderPath, Log log)
																																																									throws Exception {
		File contentsDir = new File(workFolderPath, "contents");
		contentsDir.mkdirs();
		File ditaDir = new File(workFolderPath, "dita");
		ditaDir.mkdirs();

		File zipFile = new File(ditaDir, outputDir.getName() + "_" + uniqueId + ".zip");

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
				return null;
			}
			log.info("Zip Complete");
		}

		if (wfContext != null) {
			setContextVariableValues(wfContext, zipFile);
		}

		log.info("Done.");
		return zipFile;
	}
	
	/**
	 * Method that lists all the files inside a folder in which the transformed objects were generated.
	 * 
	 * @param outputDir The output directory.
	 * @return All the files found.
	 */
	public static File[] getTransformedFiles(File outputDir) {
		File[] resultFiles = outputDir.listFiles(new FilenameFilter() {
			String[] extensions = {DitaOtOptions.EPUB_TRANSFORMATION, DitaOtOptions.PDF_TRANSFORMATION};

			@Override
			public boolean accept(File file, String name) {
				return FilenameUtils.isExtension(name, extensions);
			}
		});
		return resultFiles;
	}
	
	/**
	 * Gets all the transformed files from the outputDir that match with outputExtension. Otherwise, 
	 * lists all the files inside a folder in which the transformed objects were generated.
	 * @param outputDir
	 * @param outputExtension
	 * @return All the files found.
	 */
	public static File[] getTransformedFiles(File outputDir, final String outputExtension) {
		if (isNotEmpty(outputExtension)) {
			File[] resultFiles = outputDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File file, String name) {
					return FilenameUtils.isExtension(name, outputExtension);
				}
			});
			return resultFiles;
		} else {
			return getTransformedFiles(outputDir);
		}
	}
}
