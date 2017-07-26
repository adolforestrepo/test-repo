package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.activiti.engine.delegate.Expression;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.Alias;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.report.StoredReport;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.FileWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
// import com.reallysi.rsuite.api.workflow.WorkflowExecutionContext;
import com.reallysi.rsuite.api.xml.LoggingSaxonMessageListener;
import com.reallysi.tools.dita.conversion.InxGenerationOptions;
import com.reallysi.tools.dita.conversion.beans.XML2InDesignBean;

// import com.reallysi.rsuite.api.workflow.activiti.BaseNonLeavingWorkflowAction;

/**
 * Converts an ASTD XML MO to Incx using the specified transform and style
 * catalog.
 */
public class ProjectAstdXml2IncxActionHandler extends BaseWorkflowAction
		implements TempWorkflowConstants {

	private static final String NOW_STRING_FORMAT_STRING = "yyyyMMdd-HHmmss";
	private static final DateFormat NOW_STRING_DATE_FORMAT = new SimpleDateFormat(
			NOW_STRING_FORMAT_STRING);

	/**
	 * (Optional) MO ID of XML file to translate.
	 */
	public static final String XML_MO_ID_PARAM = "xmlMoId";

	/**
	 * Optional. Specifies the base name of the INCX file to generate. If the
	 * XML MO to process is not in the workflow context, also used to construct
	 * an alias for the XML in order to find the XML (e.g., when the original
	 * input to the workflow was a non-XML file.
	 */
	public static final String FILE_NAME_PARAM = "fileName";
	/**
	 * URL of the XSLT that does the transforming from XML to INCX
	 */
	public static final String XSLT_URI_PARAM = "xsltUriFromWorkflow";
	/**
	 * URL of the style catalog to use for the result INCX file.
	 */
	public static final String STYLE_CATALOG_URI_PARAM = "styleCatalogUri";

	/**
	 * Optional. Name of the variable that holds the filename of the stored
	 * transform report.
	 */
	public static final String XFORM_REPORT_FILE_NAME_VAR_NAME_PARAM = "xformReportFileNameVarName";

	/**
	 * Default variable name for the workflow variable that holds the name of
	 * the transform report.
	 */
	public static final String XFORM_REPORT_FILE_NAME_VARNAME = "xformReportFileName";

	/**
	 * Optional. Name of the variable to hold the filename of the transformation
	 * report.
	 */
	public static final String XFORM_REPORT_FILENAME_VAR_NAME_PARAM = "xformReportFileName";

	/**
	 * Optional. Name of the workflow variable to hold the ID of the generated
	 * report (used to construct REST URLs for accessing the stored report).
	 */
	public static final String XFORM_REPORT_ID_VAR_NAME_PARAM = "xformReportIdVarName";

	/**
	 * The name of the workflow variable to hold the transform report ID so it
	 * can be retrieved later.
	 */
	public static final String XFORM_REPORT_ID_VARNAME = "xformReportId";

	/**
	 * Optional. Output directory to put the generated XML into. If not
	 * specified, uses a random temporary directory. Set this if you want to be
	 * able to see the generated XML before it is imported into RSuite, for
	 * example to enable debugging of transformation problems.
	 */
	public static final String OUTPUT_PATH_PARAM = "outputPath";

	/**
	 * (Optional) Name of variable to hold the pathname of the generated
	 * executive summary INCX file.
	 * <p>
	 * If not specified and an executive summary file is generated during
	 * conversion, then the variable <tt>incxExecSummaryPath</tt> is used.
	 * </p>
	 */
	public static final String EXEC_SUMMARY_PATH_VAR_NAME_PARAM = "execSummaryPathVarName";

	/**
	 * (Optional) Name of variable to hold the basename of the generated
	 * executive summary INCX file.
	 * <p>
	 * If not specified and an executive summary file is generated during
	 * conversion, then the variable <tt>incxExecSummaryFile</tt> is used.
	 * </p>
	 */
	public static final String EXEC_SUMMARY_FILE_VAR_NAME_PARAM = "execSummaryFileVarName";

	public static final String DEFAULT_EXEC_SUMMARY_PATH_VAR_NAME = "incxExecSummaryPath";

	public static final String DEFAULT_EXEC_SUMMARY_FILE_VAR_NAME = "incxExecSummaryFile";

	private XML2InDesignBean bean;

	protected Expression xmlMoId;
	protected Expression styleCatalogUri;
	protected Expression xsltUriFromWorkflow;
	protected Expression fileName;

	public void setFileName(String fileName) {
		setParameter(FILE_NAME_PARAM, fileName);
	}

	@Override
  public void execute(WorkflowContext context) throws Exception {

    Log wfLog = context.getWorkflowLog();

    try{
    checkParamsNotEmptyOrNull(STYLE_CATALOG_URI_PARAM, XSLT_URI_PARAM);
    }
    catch (Exception e) {
    	wfLog.error("Checking parameters styleCatalogUri and xsltUriFromWorkflow");
    	
    }
    String styleCatalogUriExp = resolveExpression(styleCatalogUri);
    String xsltUriExp = resolveExpression(xsltUriFromWorkflow);
    String fileNameExp = resolveVariablesAndExpressions(fileName.getExpressionText());
    String xmlMoIdExp = resolveVariablesAndExpressions(xmlMoId.getExpressionText());


    // bean = new XML2InDesignBean(xsltUri, styleCatalogUri);
    bean = new XML2InDesignBean(context, xsltUriExp, styleCatalogUriExp, fileNameExp);

    File workFolder = getOutputDir(context);

    ManagedObject mo = null;
    if (xmlMoIdExp == null || "".equals(xmlMoIdExp)) {
      MoListWorkflowObject moList = context.getMoListWorkflowObject();
      if (moList == null || moList.isEmpty()) {
        mo = context.getManagedObjectService().getObjectByAlias(context.getAuthorizationService()
            .getSystemUser(), fileNameExp + ".xml");
        if (mo == null) {
          reportAndThrowRSuiteException(
              "Failed to get an MO using the fileName parameter with the value \"" + fileNameExp
                  + "\"");
        }
      } else {
        MoWorkflowObject moObject = moList.getMoList().get(0); // First item in list should be
                                                               // course script MO.
        mo = context.getManagedObjectService().getManagedObject(context.getAuthorizationService()
            .getSystemUser(), moObject.getMoid());
        if (mo == null) {
          reportAndThrowRSuiteException("Failed to get an MO from the workflow context");
        }
        // Assume first alias is a filename:
        if (fileNameExp == null || "".equals(fileNameExp)) {
          Alias[] aliases = mo.getAliases();
          if (aliases.length > 0) {
            // fileName = FilenameUtils.getBaseName(aliases[0].toString());
            fileNameExp = FilenameUtils.getBaseName(aliases[0].getText());
          } else {
            fileNameExp = "article_" + mo.getId();
          }
        }
      }
    } else {
      mo = context.getManagedObjectService().getManagedObject(context.getAuthorizationService()
          .getSystemUser(), xmlMoIdExp);
      if (mo == null) {
        reportAndThrowRSuiteException("Failed to find MO with ID [" + xmlMoIdExp + "]");
      }
    }

    File resultInxFile = new File(workFolder, fileNameExp + ".incx");

    String reportFileName = fileNameExp + "_docx2dita_at_" + getNowString() + ".txt";
    String xformReportIdVarName = getParameterWithDefault(XFORM_REPORT_ID_VAR_NAME_PARAM,
        XFORM_REPORT_ID_VARNAME);
    xformReportIdVarName = resolveVariablesAndExpressions(xformReportIdVarName);


    wfLog.info("Report ID is \"" + reportFileName + "\"");
    String xformReportFileNameVarName = getParameterWithDefault(
        XFORM_REPORT_FILE_NAME_VAR_NAME_PARAM, XFORM_REPORT_FILE_NAME_VARNAME);
    context.setVariable(xformReportFileNameVarName, reportFileName);

    File copiedReportFile = null;

    LoggingSaxonMessageListener logger = context.getXmlApiManager().newLoggingSaxonMessageListener(
        context.getWorkflowLog());

    boolean exceptionOccured = false;
    try {

      // bean.generateInDesignFromTopic(mo, resultInxFile, logger);


      InxGenerationOptions options = new InxGenerationOptions();
      bean.generateInDesignFromTopic(mo.getElement().getOwnerDocument(), resultInxFile, options);

      // If executive summary file created, set workflow variables.
      File resultInxExecSummaryFile = new File(workFolder, fileNameExp + "-execsum.incx");
      if (resultInxExecSummaryFile.exists()) {
        wfLog.info("Executive summary exists: " + resultInxExecSummaryFile);
        String pathVarName = getParameterWithDefault(EXEC_SUMMARY_PATH_VAR_NAME_PARAM,
            DEFAULT_EXEC_SUMMARY_PATH_VAR_NAME);
        pathVarName = resolveVariablesAndExpressions(pathVarName);
        context.setVariable(pathVarName, resultInxExecSummaryFile.getAbsolutePath());

        String fileVarName = getParameterWithDefault(EXEC_SUMMARY_FILE_VAR_NAME_PARAM,
            DEFAULT_EXEC_SUMMARY_FILE_VAR_NAME);
        fileVarName = resolveVariablesAndExpressions(fileVarName);
        context.setVariable(fileVarName, resultInxExecSummaryFile.getName());
      }
    } catch (Exception e) {
      exceptionOccured = true;
      reportAndThrowRSuiteException("Exception generating InDesign from mo: " + e
          .getLocalizedMessage());
    } finally {
      StringBuilder reportStr = new StringBuilder("XML to InCopy Transformation report\n\n").append(
          "Source XML: ").append(mo.getDisplayName()).append(" [").append(mo.getId()).append("]\n")
          .append("Result file: ").append(resultInxFile.getAbsolutePath()).append("\n").append(
              "Time performed: ").append(getNowString()).append("\n\n").append(logger
                  .getLogString()).append("\n");
      if (exceptionOccured) {
        reportStr.append(" + [ERROR] Exception occurred: ").append(context.getVariable(
            ProjectAstdWorkflowConstants.EXCEPTION_MESSAGE_VAR));
      } else {
        reportStr.append(" + [INFO] Process finished normally");
      }

      StoredReport report = context.getReportManager().saveReport(reportFileName, reportStr
          .toString());
      copiedReportFile = new File(resultInxFile.getParentFile(), report.getSuggestedFileName());
      wfLog.info("Generation report in location  \"" + copiedReportFile.getAbsolutePath() + "\"");
      context.setVariable(XFORM_REPORT_ID_VARNAME, report.getId());
      File reportFile = report.getFile();
      FileUtils.copyFile(reportFile, copiedReportFile);
    }

    wfLog.info("XML generated in temporary file \"" + resultInxFile.getAbsolutePath() + "\"");

    context.setFileWorkflowObject(new FileWorkflowObject(resultInxFile));

  }

	protected File getOutputDir(WorkflowContext context) throws Exception,
			RSuiteException {
		String outputPath = getParameter(OUTPUT_PATH_PARAM);
		outputPath = resolveVariables(outputPath);

		File outputDir = null;
		if (outputPath == null || "".equals(outputPath.trim())) {
			outputDir = getWorkingDir(false);
		} else {
			outputDir = new File(outputPath);
			if (!outputDir.exists()) {
				outputDir.mkdirs();
			}
			if (!outputDir.exists()) {
				reportAndThrowRSuiteException("Failed to find or create output directory \""
						+ outputPath + "\"");
			}
			if (!outputDir.canWrite()) {
				reportAndThrowRSuiteException("Cannot write to output directory \""
						+ outputPath + "\"");
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
		setParameter(XFORM_REPORT_FILENAME_VAR_NAME_PARAM,
				xformReportFileNameVarName);
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

	public static String getNowString() {
		return NOW_STRING_DATE_FORMAT.format(new Date());
	}
}
