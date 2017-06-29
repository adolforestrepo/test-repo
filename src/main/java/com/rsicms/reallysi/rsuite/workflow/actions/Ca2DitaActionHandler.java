package com.reallysi.rsuite.workflow.actions;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import net.sourceforge.dita4publishers.util.conversion.ConversionConfig;

import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.report.StoredReport;
import com.reallysi.rsuite.api.workflow.WorkflowExecutionContext;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.tools.dita.DitaSupportRSuiteUtils;
import com.reallysi.tools.dita.conversion.Docx2XmlOptions;
import com.reallysi.tools.ditaot.DitaOtOptions;
import com.rsicms.rsuite.helpers.messages.ProcessMessageContainer;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;

/**
 * Generates XML from from DOCX MO.
 */
public class Ca2DitaActionHandler extends BaseWorkflowAction {

	public static final String PARAM_CONVERSION_CONFIG = "conversionConfig";
	public static final String EXCEPTION_MESSAGE_VAR = "exceptionMessage";
	public static final String DATE_FORMAT_STRING = "yyyyMMdd-HHmmss";
	public static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_STRING);

	/**
	 * 
	 */
	public static final String COURSE_SCRIPT_MO_ID_PARAM = "courseScriptMoId";

	/**
	 * 
	 */
	public static final String XFORM_REPORT_FILE_NAME_VAR_NAME_PARAM = "xformReportFileNameVarName";

	/**
	 * 
	 */
	public static final String XFORM_REPORT_FILE_NAME_VARNAME = "xformReportFileName";
	
	/**
	 * Variable to hold the filename of the generated map.
	 */
	public static final String MAP_FILE_NAME_VARNAME = "mapFileName";
	
	/**
	 * URI of the style map configuration document (maps Word styles to
	 * XML elements).
	 */
	public static final String STYLE_MAP_URI_PARAM = "styleMapUri";
	
	/**
	 * URL of the XSLT to apply to the DOX file.
	 */
	public static final String XSLT_URI_PARAM = "xsltUri";

	
	public static final String XFORM_REPORT_ID_VAR_NAME_PARAM = "xformReportIdVarName";
	
	/**
	 * The name of the workflow variable to hold the Ant report ID so it can
	 * be retrieved later.
	 */
	public static final String XFORM_REPORT_ID_VARNAME = "xformReportId";

	/**
	 * Variable to hold the filename of the transformation report.
	 */
	public static final String XFORM_REPORT_FILENAME_VAR_NAME_PARAM = "xformReportFileName";

	
	
	@Override
	public void execute(WorkflowContext context) throws Exception {
		Log wfLog = context.getWorkflowLog();
		
		String rsuiteId = (String) context.getVariable("rsuiteId");
		if (rsuiteId == null || "".equals(rsuiteId)) {
			throw new RSuiteException(0, "Workflow variable 'rsuiteId' not set.");			
		}
		ManagedObject caMo = context.getManagedObjectService().getManagedObject(context.getAuthorizationService().getSystemUser(), 
		                                                                        rsuiteId);
		String configId = getParameter(PARAM_CONVERSION_CONFIG);
		if (configId == null || "".equals(configId)) {
			configId = DitaSupportRSuiteUtils.getConversionConfigIdAlias(caMo);
		}
		if (configId == null || "".equals(configId)) {
			String msg = "Could not determine conversion configurationi ID for CA node " + RSuiteUtils.formatMoId(caMo);
			context.setVariable("failureDetail", msg);
			throw new RSuiteException(0, msg);
		}
		
		ConversionConfig convConfig = DitaSupportRSuiteUtils.getConversionConfigForAlias(context,
		                                                                                 context.getAuthorizationService().getSystemUser(),
		                                                                                 wfLog, configId);
		
		Docx2XmlOptions docxOptions = new Docx2XmlOptions(context.getAuthorizationService().getSystemUser());
		docxOptions.setConversionConfig(convConfig);
		// Set up message writer
		
		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		docxOptions.setMessageWriter(writer);
		
        writer.write(
        		"<h1>" + 
        		"Conversion Report for " + RSuiteUtils.formatMoId(caMo) +
        		"</h1>"
        		);
        writer.write("<div>");
        writer.flush(); // Make sure everything is written to the underlying stream.

        // Do the transform
        
        ContentAssemblyNodeContainer ca = RSuiteUtils.getContentAssemblyNodeContainer(context,
                                                                                      context.getAuthorizationService().getSystemUser(),
                                                                                      caMo.getId());
        
        List<File> generatedXmlFiles = new ArrayList<File>();
        
        try {
        	wfLog.info("Generating XML from content assembly...");
			DitaSupportRSuiteUtils
			   .generateXmlFromContentAssemblyNodeContainer(
					   context, 
					   ca, 
					   getTempDir("ca2ditaActionHandler_", false).getAbsolutePath(), 
					   docxOptions, 
					   generatedXmlFiles, 
					   context.getWorkflowLog());
		} catch (Exception e) {
        	wfLog.error("Unexpected exception generating XML: " + e.getMessage());
			docxOptions.addFailureMessage(
					e.getClass().getSimpleName(), 
					ca.getDisplayName(),
					"Unexpected exception generating XML: " + e.getMessage(),
					e
					);
		}
        
        wfLog.info("Generation complete.");
        if (docxOptions.hasFailures()) {
        	wfLog.warn("Failures from the generation process.");
        } else {
        	wfLog.info("No failures during the generation process");
        }
        
        ProcessMessageContainer messages = docxOptions.getMessages();
        
        writer.write("</div>");
        if (messages.hasFailures()) {
        	writer.write("<p");
        	writer.write(" style=\"color: red;\"");
        	writer.write(">");
        	writer.write("Failures during conversion.");
        	writer.write("</p>\n");
        }
        
    	writer.write("<h2>" + "Messages" + "</h2>");

		messages.reportMessagesByObjectAsHtml(context.getAuthorizationService().getSystemUser().getName(),
		                                      writer);
        
        writer.flush();
        writer.close();

		
		// Capture the report and set reportId workflow variable
        
        StoredReport report = context.getReportManager()
        		.saveReport("xml-generation=log_" + caMo.getId() + 
        				"_" + DATE_FORMAT.format(Calendar.getInstance().getTime()), 
        				stringWriter.toString(), 
        				"text/html");
        
        context.setVariable("reportId", report.getId());
		
		// Set isValid workflow variable
		
		context.setVariable(
				"isValid", 
				!docxOptions.hasFailures());

	}

	protected File getOutputDir(WorkflowExecutionContext context) throws Exception, RSuiteException {
		String outputPath = getParameter(DitaOtOptions.OUTPUT_PATH_PARAM);
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
				reportAndThrowRSuiteException("Failed to find or create output directory \"" + outputPath + "\"");
			}
			if (!outputDir.canWrite()) {
				reportAndThrowRSuiteException("Cannot write to output directory \"" + outputPath + "\"");
			}
		}
		return outputDir;
	}
	
	public void setXformReportIdVarName(String xformReportIdVarName) {
		setParameter(XFORM_REPORT_ID_VAR_NAME_PARAM, xformReportIdVarName);
	}
	
	public void setXformReportFileNameVarName(String xformReportFileNameVarName) {
		setParameter(XFORM_REPORT_FILENAME_VAR_NAME_PARAM, xformReportFileNameVarName);
	}
	
	public void setOutputPath(String outputPath) {
		this.setParameter(DitaOtOptions.OUTPUT_PATH_PARAM, outputPath);
	}
	
	public void setConversionConfig(String conversionConfig) {
		this.setParameter(PARAM_CONVERSION_CONFIG, conversionConfig);
	}
}
