package org.dita4publishers.rsuite.workflow.actions;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.dita4publishers.rsuite.workflow.actions.beans.Docx2XmlBean;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.report.StoredReport;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.FileWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.api.xml.LoggingSaxonMessageListener;
import com.reallysi.tools.dita.conversion.Docx2Xml;
import com.reallysi.tools.ditaot.DitaOtOptions;

/**
 * Generates XML from from DOCX MO.
 */
public class Docx2XmlActionHandler extends BaseWorkflowAction {

	/**
	 * Prefix to add to each generated filename. If not specified,
	 * there is no prefix.
	 */
	public static final String FILE_NAME_PREFIX_PARAM = "fileNamePref";

	/**
	 * Prefix to add to each submap's generated name, after the filename
	 * prefix.
	 */
	public static final String SUBMAP_NAME_PREFIX_PARAM = "submapNamePref";

	/**
	 * Optional. The name of the file generated directly by the XSLT (as opposed to
	 * the map file name when a map is generated). Specify this parameter
	 * when the transform will only generate a single topic rather than 
	 * a complete map.
	 * <p>NOTE: This is a stopgap in advance of more complete control over output
	 * file naming.</p>
	 */
	public static final String RESULT_FILE_NAME_PARAM = "resultFileName";

	/**
	 * Optional. MO ID of the DOCX file to process. If not specified, the first or only
	 * MO in the workflow context must be a DOCX file.
	 */
	public static final String DOCX_MO_ID_PARAM = "docxMoId";

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
	 * Optional. Variable to hold the filename of the generated map.
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
	/**
	 * URL of the XSLT that rewrites graphic filenames in DOCX
     * relationships file.
     * <p>The XSLT will be called with the parameter
     * <tt>graphicFileNamePrefix</tt>, and that prefix should
     * be applied to the base filename of images in the DOCX
     * relationships mapping file.
     * </p>
	 */
	public static final String XSLT_GRAPHIC_RENAME_URI_PARAM =
        "xsltGraphicRenameUri";

    /**
     * Primary prefix string to use when renaming graphics.
     * <p>The value of this parameter along with the managed
     * object ID of the DOCX object will be prepended to each
     * graphic filename.
     * </p>
     * <p>If this parameter is not specified, then the
     * default prefix of "<tt>rsi</tt>" will be used.
     * </p>
     */
    public static final String GRAPHICS_PREFIX_PARAM = "graphicsPrefix";
	
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
	 * Controls debugging in the XSLT transform. Values are "true" or "false", default is "false".
	 */
	public static final String DEBUG_PARAM = "debug";
	
	
	
	@Override
	public void execute(WorkflowContext context) throws Exception {
		Log wfLog = context.getWorkflowLog();
		
		wfLog.info("Docx2XmlActionHandler: Starting...");
		
		checkParamsNotEmptyOrNull(XSLT_URI_PARAM, STYLE_MAP_URI_PARAM);

		String xsltUri = getParameter(XSLT_URI_PARAM);
		xsltUri = resolveVariables(xsltUri);

		String styleMapUri = getParameter(STYLE_MAP_URI_PARAM);		
		styleMapUri = resolveVariables(styleMapUri);

        String xsltGfxRenameUri = getParameter(XSLT_GRAPHIC_RENAME_URI_PARAM);
        if (xsltGfxRenameUri == null) {
            xsltGfxRenameUri = "rsuite:/res/plugin/dita4publishers/xslt/word2dita/rewriteDocxGraphics.xsl";
        } else {
            xsltGfxRenameUri = resolveVariables(xsltGfxRenameUri);
        }

        String gfxPrefix = getParameter(GRAPHICS_PREFIX_PARAM);
        if (gfxPrefix == null) {
            gfxPrefix = "";
        } else {
            gfxPrefix = resolveVariables(gfxPrefix);
        }

		Docx2XmlBean bean = null; 
		
		File outputDir = getOutputDir();
		
		wfLog.info("Effective output directory is \"" + outputDir.getAbsolutePath() + "\"");
		File docxFile = null;
		
		ManagedObject mo = null;
		String docxMoId = resolveVariablesAndExpressions(getParameter(DOCX_MO_ID_PARAM));
		
		if (docxMoId == null || "".equals(docxMoId)) {
			MoListWorkflowObject moList = context.getMoListWorkflowObject();
			if (moList == null || moList.isEmpty() || mo instanceof ContentAssembly) {
				String msg = "No managed objects in the workflow context and " + DOCX_MO_ID_PARAM + " parameter not specified . Nothing to do";
				reportAndThrowRSuiteException(msg);
			} 			
			MoWorkflowObject moObject = moList.getMoList().get(0); // First item in list should be course script MO.
            docxMoId = moObject.getMoid();
			mo = context.getManagedObjectService().getManagedObject(context.getAuthorizationService().getSystemUser(), 
			                                                        docxMoId);
			if (!Docx2Xml.isTransformationApplicable(mo)) {
				reportAndThrowRSuiteException("Transformation does not apply to the first MO in the list list (ID: " + 
				    mo.getId() + 
				    "); its content type is \"" + 
				    mo.getContentType() +
				    "\".");
			}
		} else {
			mo = context.getManagedObjectService().getManagedObject(context.getAuthorizationService().getSystemUser(), 
			                                                        docxMoId);
			if (mo == null) {
				reportAndThrowRSuiteException("Failed to find MO with ID [" + docxMoId + "]");
			}
			if (mo instanceof ContentAssembly) {
				reportAndThrowRSuiteException("Specified MO [" + docxMoId + "] is a content assembly or content assembly node.");
			}
			if (!Docx2Xml.isTransformationApplicable(mo)) {
				reportAndThrowRSuiteException("Transformation does not apply to MO ID "
																			+ mo.getId()
																			+ " with a content type of \""
																			+ mo.getContentType() + "\".");
			}
		}
			
		String fileNamePrefix = resolveVariablesAndExpressions(getParameterWithDefault(FILE_NAME_PREFIX_PARAM, ""));
		String submapNamePrefix = resolveVariablesAndExpressions(getParameterWithDefault(SUBMAP_NAME_PREFIX_PARAM, "map"));
		
		String docxFilename = mo.getDisplayName();
		
		String fileNameBase = FilenameUtils.getBaseName(docxFilename);
		
		String mapFileName = fileNameBase + ".ditamap";
		
		String topicFileName = fileNameBase + ".xml";
		
		context.setVariable(MAP_FILE_NAME_VARNAME, mapFileName);
		
		bean = new Docx2XmlBean(context, xsltUri, styleMapUri, mapFileName, topicFileName);
        if (xsltGfxRenameUri != null || !"".equals(xsltGfxRenameUri)) {
            bean.setXsltGraphicRenameUri(xsltGfxRenameUri);
        }

		docxFile = new File(mo.getExternalAssetPath());
		// Put the map into a directory named for the map:
		File mapDir = new File(outputDir, fileNameBase);
		// NOTE: This doesn't actually set the name of the true result map, which is generated
		// via xsl:result-document() in the transform.
		String resultFileName = resolveVariables(getParameterWithDefault(RESULT_FILE_NAME_PARAM, "temp.garbage"));
		File resultFile = new File(mapDir, resultFileName);
		// This is the real map file that will be produced:
		File resultMapFile = new File(mapDir, mapFileName);
		String reportFileName = fileNameBase + "_docx2dita_at_" + DitaOtOptions.getNowString() + ".txt";
		String xformReportIdVarName = getParameterWithDefault(XFORM_REPORT_ID_VAR_NAME_PARAM, XFORM_REPORT_ID_VARNAME);
		xformReportIdVarName = resolveVariablesAndExpressions(xformReportIdVarName);


		wfLog.info("Report ID is \"" + reportFileName + "\"");
	    String xformReportFileNameVarName = getParameterWithDefault(XFORM_REPORT_FILE_NAME_VAR_NAME_PARAM, XFORM_REPORT_FILE_NAME_VARNAME);
		context.setVariable(xformReportFileNameVarName, reportFileName);

		LoggingSaxonMessageListener logger = context.getXmlApiManager().newLoggingSaxonMessageListener(context.getWorkflowLog());
	    File copiedReportFile = null;
	    Map<String, String> params = new HashMap<String, String>();
	    params.put("debug", resolveVariablesAndExpressions(getParameterWithDefault(DEBUG_PARAM, "false"))); 
	    params.put("fileNamePrefix", fileNamePrefix);
	    params.put("submapNamePrefix", submapNamePrefix);

        // Parameter specifying what prefix to add to each graphic filename
        // to help insure uniqueness.
        // NOTE: The prefix needs to be something that is "persistent"
        //       with multiple calls to this handler to insure that the
        //       graphic filenames are the same if docx is re-converted.
        //       To do this, we use the ID of the docx MO.
        params.put("graphicFileNamePrefix", gfxPrefix+docxMoId+"_");
	    
		boolean exceptionOccured = false;
		try {
			bean.generateXmlS9Api(docxFile, resultFile, logger, params);
		} catch (RSuiteException e) {
			exceptionOccured = true;
			String msg = "Exception transforming document: " + e.getMessage();
			reportAndThrowRSuiteException(msg);
			throw e;
		} finally {
			StringBuilder reportStr = new StringBuilder("DOCX 2 DITA Transformation report\n\n")
			.append("Source DOCX: ").append( mo.getDisplayName())
			.append(" [").append(mo.getId()).append("]\n")
			.append("Result file: ")
			.append(resultMapFile.getAbsolutePath())
			.append("\n")
			.append("Time performed: ")
			.append(DitaOtOptions.getNowString())
			.append("\n\n")
			.append(logger.getLogString());
			if (exceptionOccured) {
				reportStr
				.append(" + [ERROR] Exception occurred: ")
				.append(context.getExceptionMessage());
			} else {
				reportStr.append(" + [INFO] Process finished normally");
			}
		    StoredReport report = context.getReportManager().saveReport(reportFileName, reportStr.toString());
		    copiedReportFile = new File(mapDir, report.getSuggestedFileName());
		    wfLog.info("Generation report in location  \"" + copiedReportFile.getAbsolutePath() + "\"");	
		    context.setVariable(XFORM_REPORT_ID_VARNAME, report.getId());
		    File reportFile = report.getFile();
		    FileUtils.copyFile(reportFile, copiedReportFile);
		}
		
	    wfLog.info("XML generated in location \"" + resultMapFile.getAbsolutePath() + "\"");	
		
		context.setFileWorkflowObject(new FileWorkflowObject(resultMapFile));
	}

	protected File getOutputDir() throws Exception {
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
	
	public void setXsltUri(String xsltUri) {
		setParameter(XSLT_URI_PARAM, xsltUri);
	}

	public void setStyleMapUri(String styleMapUri) {
		setParameter(STYLE_MAP_URI_PARAM, styleMapUri);
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
	
	public void setResultFileName(String resultFileName) {
		this.setParameter(RESULT_FILE_NAME_PARAM, resultFileName);
	}
	
	public void setDocxMoId(String docxMoId) {
		this.setParameter(DOCX_MO_ID_PARAM, docxMoId);
	}
	
	public void setFileNamePrefix(String fileNamePref) {
		this.setParameter(FILE_NAME_PREFIX_PARAM, fileNamePref);
	}

	public void setSubmapNamePrefix(String submapNamePref) {
		this.setParameter(SUBMAP_NAME_PREFIX_PARAM, submapNamePref);
	}

    public void setXsltGraphicRenameUri(String s) {
        this.setParameter(XSLT_GRAPHIC_RENAME_URI_PARAM, s);
    }

    public void setGraphicsPrefix(String s) {
        this.setParameter(GRAPHICS_PREFIX_PARAM, s);
    }
    
    public void setDefault(String debug) {
    	this.setParameter(DEBUG_PARAM, debug);
    }
}
