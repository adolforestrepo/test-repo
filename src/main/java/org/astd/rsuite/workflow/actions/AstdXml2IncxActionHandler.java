package org.astd.rsuite.workflow.actions;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;

import org.astd.rsuite.workflow.bean.XML2InDesignBean;
import com.reallysi.rsuite.api.Alias;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.report.StoredReport;
import com.reallysi.rsuite.api.workflow.activiti.FileWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoWorkflowObject;
import com.reallysi.rsuite.api.workflow.WorkflowExecutionContext;
import com.reallysi.rsuite.api.xml.LoggingSaxonMessageListener;

/**
 * Converts an ASTD XML MO to Incx using the specified transform
 * and style catalog.
 */
public class AstdXml2IncxActionHandler extends AstdActionHandlerBase {
	
	/**
	 * (Optional) MO ID of XML file to translate.
	 */
	public static final String XML_MO_ID_PARAM = "xmlMoId";

	/**
	 * Optional. Specifies the base name of the INCX file to generate.
	 * If the XML MO to process is not in the workflow context, also
	 * used to construct an alias for the XML in order to find the
	 * XML (e.g., when the original input to the workflow was a non-XML
	 * file.
	 */
	public static final String FILE_NAME_PARAM = "fileName";
	/**
	 * URL of the XSLT that does the transforming from XML to INCX
	 */
	public static final String XSLT_URI_PARAM = "xsltUri";
	/**
	 * URL of the style catalog to use for the result INCX file.
	 */
	public static final String STYLE_CATALOG_URI_PARAM = "styleCatalogUri";

	/**
	 * Optional. Name of the variable that holds the filename of the stored transform report.
	 */
	public static final String XFORM_REPORT_FILE_NAME_VAR_NAME_PARAM = "xformReportFileNameVarName";

	/**
	 * Default variable name for the workflow variable that holds the name of the
	 * transform report.
	 */
	public static final String XFORM_REPORT_FILE_NAME_VARNAME = "xformReportFileName";

	/**
	 * Optional. Name of the variable to hold the filename of the transformation report.
	 */
	public static final String XFORM_REPORT_FILENAME_VAR_NAME_PARAM = "xformReportFileName";

	/**
	 * Optional. Name of the workflow variable to hold the ID of the
	 * generated report (used to construct REST URLs for accessing
	 * the stored report).
	 */
	public static final String XFORM_REPORT_ID_VAR_NAME_PARAM = "xformReportIdVarName";

	/**
	 * The name of the workflow variable to hold the transform report ID so it can
	 * be retrieved later.
	 */
	public static final String XFORM_REPORT_ID_VARNAME = "xformReportId";

	/**
	 * Optional. Output directory to put the generated XML into.
	 * If not specified, uses a random temporary directory.
	 * Set this if you want to be able to see the generated XML
	 * before it is imported into RSuite, for example to enable
	 * debugging of transformation problems.
	 */
	public static final String OUTPUT_PATH_PARAM = "outputPath";
	
	/**
	 * (Optional) Name of variable to hold the pathname of the generated
	 * executive summary INCX file.
	 * <p>If not specified and an executive summary file is generated during
	 * conversion, then the variable <tt>incxExecSummaryPath</tt> is used.
	 * </p>
	 */
	public static final String EXEC_SUMMARY_PATH_VAR_NAME_PARAM =
		"execSummaryPathVarName";

	/**
	 * (Optional) Name of variable to hold the basename of the generated
	 * executive summary INCX file.
	 * <p>If not specified and an executive summary file is generated during
	 * conversion, then the variable <tt>incxExecSummaryFile</tt> is used.
	 * </p>
	 */
	public static final String EXEC_SUMMARY_FILE_VAR_NAME_PARAM =
		"execSummaryFileVarName";

	public static final String DEFAULT_EXEC_SUMMARY_PATH_VAR_NAME =
		"incxExecSummaryPath";

	public static final String DEFAULT_EXEC_SUMMARY_FILE_VAR_NAME =
		"incxExecSummaryFile";
	
	private static final long serialVersionUID = 1L;
	private XML2InDesignBean bean;

	public void setFileName(String fileName) {
		setParameter(FILE_NAME_PARAM, fileName);
	}

	@Override
	public void execute(WorkflowExecutionContext context) throws Exception {

		Log wfLog = context.getWorkflowLog();

		checkParamsNotEmptyOrNull(STYLE_CATALOG_URI_PARAM, XSLT_URI_PARAM);

		String styleCatalogUri = getParameter(STYLE_CATALOG_URI_PARAM);
		String xsltUri = getParameter(XSLT_URI_PARAM);
		String fileName = getParameter(FILE_NAME_PARAM);

		String xmlMoId = resolveVariables(context, getParameter(XML_MO_ID_PARAM));
		styleCatalogUri = resolveVariables(context, styleCatalogUri);
		xsltUri = resolveVariables(context, xsltUri);

		fileName = resolveVariables(context, fileName);

		bean = new XML2InDesignBean(context, xsltUri,
				styleCatalogUri);

		File workFolder = getOutputDir(context);
		
		ManagedObject mo = null;
		if (xmlMoId == null || "".equals(xmlMoId)) {
			MoListWorkflowObject moList = context.getMoListWorkflowObject();
			if (moList == null || moList.isEmpty()) {
				mo = context.getManagedObjectService().getObjectByAlias(getSystemUser(), fileName + ".xml");	
				if (mo == null) {
					reportAndThrowRSuiteException(context, "Failed to get an MO using the fileName parameter with the value \"" + fileName + "\"");
				}
			} else {
				MoWorkflowObject moObject = moList.getMoList().get(0); // First item in list should be course script MO.
				mo = context.getManagedObjectService().getManagedObject(getSystemUser(), moObject.getMoid());
				if (mo == null) {
					reportAndThrowRSuiteException(context, "Failed to get an MO from the workflow context");
				}
				// Assume first alias is a filename:
				if (fileName == null || "".equals(fileName)) {
					Alias[] aliases = mo.getAliases();
					if (aliases.length > 0) {
						//fileName = FilenameUtils.getBaseName(aliases[0].toString());
						fileName = FilenameUtils.getBaseName(aliases[0].getText());
					} else {
						fileName = "article_" + mo.getId();
					}
				}
			}
		} else {
			mo = context.getManagedObjectService().getManagedObject(getSystemUser(), xmlMoId);
			if (mo == null) {
				reportAndThrowRSuiteException(context, "Failed to find MO with ID ["+xmlMoId+"]");
			}
		}
				
		File resultInxFile = new File(workFolder, fileName + ".incx");

		String reportFileName = fileName + "_docx2dita_at_" + getNowString() + ".txt";
		String xformReportIdVarName = getParameterWithDefault(XFORM_REPORT_ID_VAR_NAME_PARAM, XFORM_REPORT_ID_VARNAME);
		xformReportIdVarName = resolveVariablesAndExpressions(context, xformReportIdVarName);


		wfLog.info("Report ID is \"" + reportFileName + "\"");
	    String xformReportFileNameVarName = getParameterWithDefault(XFORM_REPORT_FILE_NAME_VAR_NAME_PARAM, XFORM_REPORT_FILE_NAME_VARNAME);
		context.setVariable(xformReportFileNameVarName, reportFileName);

	    File copiedReportFile = null;

		LoggingSaxonMessageListener logger = context.getXmlApiManager()
				.newLoggingSaxonMessageListener(context.getWorkflowLog());

		boolean exceptionOccured = false;
		try {
			bean.generateInDesignFromTopic(mo, resultInxFile, logger);

			// If executive summary file created, set workflow variables.
			File resultInxExecSummaryFile = new File(workFolder,
					fileName + "-execsum.incx");
			if (resultInxExecSummaryFile.exists()) {
				wfLog.info("Executive summary exists: "+
						resultInxExecSummaryFile);
				String pathVarName = getParameterWithDefault(
						EXEC_SUMMARY_PATH_VAR_NAME_PARAM,
						DEFAULT_EXEC_SUMMARY_PATH_VAR_NAME);
				pathVarName = resolveVariablesAndExpressions(
						context, pathVarName);
				context.setVariable(pathVarName,
						resultInxExecSummaryFile.getAbsolutePath());

				String fileVarName = getParameterWithDefault(
						EXEC_SUMMARY_FILE_VAR_NAME_PARAM,
						DEFAULT_EXEC_SUMMARY_FILE_VAR_NAME);
				fileVarName = resolveVariablesAndExpressions(
						context, fileVarName);
				context.setVariable(fileVarName,
						resultInxExecSummaryFile.getName());
			}
		} catch (Exception e) {
			exceptionOccured = true;
			reportAndThrowRSuiteException(context, "Exception generating InDesign from mo: " + e.getLocalizedMessage());
		} finally {
			StringBuilder reportStr = new StringBuilder("XML to InCopy Transformation report\n\n")
			.append("Source XML: ").append( mo.getDisplayName())
			.append(" [").append(mo.getId()).append("]\n")
			.append("Result file: ")
			.append(resultInxFile.getAbsolutePath())
			.append("\n")
			.append("Time performed: ")
			.append(getNowString())
			.append("\n\n")
			.append(logger.getLogString())
			.append("\n");
			if (exceptionOccured) {
				reportStr
				.append(" + [ERROR] Exception occurred: ")
				.append(context.getVariable(AstdWorkflowConstants.EXCEPTION_MESSAGE_VAR));
			} else {
				reportStr.append(" + [INFO] Process finished normally");
			}
			
		    StoredReport report = context.getReportManager().saveReport(reportFileName, reportStr.toString());
		    copiedReportFile = new File(resultInxFile.getParentFile(), report.getSuggestedFileName());
		    wfLog.info("Generation report in location  \"" + copiedReportFile.getAbsolutePath() + "\"");	
		    context.setVariable(XFORM_REPORT_ID_VARNAME, report.getId());
		    File reportFile = report.getFile();
		    FileUtils.copyFile(reportFile, copiedReportFile);
		}

		wfLog.info("XML generated in temporary file \""
				+ resultInxFile.getAbsolutePath() + "\"");

		context.setFileWorkflowObject(new FileWorkflowObject(resultInxFile));

	}

	protected File getOutputDir(WorkflowExecutionContext context) throws Exception, RSuiteException {
		String outputPath = getParameter(OUTPUT_PATH_PARAM);
		outputPath = resolveVariables(context, outputPath);
		
		File outputDir = null;
		if (outputPath == null || "".equals(outputPath.trim())) {
			outputDir = bean.getWorkingDir(false);
		} else {
			outputDir = new File(outputPath);
			if (!outputDir.exists()) {
				outputDir.mkdirs();
			}
			if (!outputDir.exists()) {
				reportAndThrowRSuiteException(context, "Failed to find or create output directory \"" + outputPath + "\"");
			}
			if (!outputDir.canWrite()) {
				reportAndThrowRSuiteException(context, "Cannot write to output directory \"" + outputPath + "\"");
			}
		}
		return outputDir;
	}
	

	public void setStyleCatalogUri(String styleCatalogUri) {
		setParameter(STYLE_CATALOG_URI_PARAM, styleCatalogUri);
	}

	public void setXsltUri(String xsltUri) {
		setParameter(XSLT_URI_PARAM, xsltUri);
	}
	
	public void setXformReportIdVarName(String xformReportIdVarName) {
		setParameter(XFORM_REPORT_ID_VAR_NAME_PARAM, xformReportIdVarName);
	}
	
	public void setXformReportFileNameVarName(String xformReportFileNameVarName) {
		setParameter(XFORM_REPORT_FILENAME_VAR_NAME_PARAM, xformReportFileNameVarName);
	}
	
	public void setOutputPath(String outputPath) {
		this.setParameter(OUTPUT_PATH_PARAM, outputPath);
	}

	public void setXmlMoId(String xmlMoId) {
		this.setParameter(XML_MO_ID_PARAM, xmlMoId);
	}

	public void setExecSummaryPathVarName(String s) {
		this.setParameter(EXEC_SUMMARY_PATH_VAR_NAME_PARAM, s);
	}

	public void setExecSummaryFileVarName(String s) {
		this.setParameter(EXEC_SUMMARY_FILE_VAR_NAME_PARAM, s);
	}
}
