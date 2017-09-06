package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.delegate.Expression;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
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

public class ProjectAstdDocxToXmlActionHandler  
extends BaseWorkflowAction
implements TempWorkflowConstants {
  
  public static final String FILE_NAME_PREFIX_PARAM = "fileNamePref";
  public static final String SUBMAP_NAME_PREFIX_PARAM = "submapNamePref";
  public static final String RESULT_FILE_NAME_PARAM = "resultFileName";
  public static final String DOCX_MO_ID_PARAM = "docxMoId";
  public static final String XFORM_REPORT_FILE_NAME_VAR_NAME_PARAM = "xformReportFileNameVarName";
  public static final String XFORM_REPORT_FILE_NAME_VARNAME = "xformReportFileName";
  public static final String XFORM_REPORT_FILENAME_VAR_NAME_PARAM = "xformReportFileName";
  public static final String MAP_FILE_NAME_VARNAME = "mapFileName";
  public static final String STYLE_MAP_URI_PARAM = "styleMapUri";
  public static final String XSLT_URI_PARAM = "xsltUri";
  public static final String XSLT_GRAPHIC_RENAME_URI_PARAM = "xsltGraphicRenameUri";
  public static final String GRAPHICS_PREFIX_PARAM = "graphicsPrefix";
  public static final String XFORM_REPORT_ID_VAR_NAME_PARAM = "xformReportIdVarName";
  public static final String XFORM_REPORT_ID_VARNAME = "xformReportId";
  public static final String DEBUG_PARAM = "debug";
  public static final String OUTPUT_PATH = "outputPath";
  
  protected Expression fileNamePref;
  protected Expression submapNamePref;
  protected Expression resultFileName;
  protected Expression docxMoId;
  protected Expression xformReportFileNameVarName;
  protected Expression xformReportFileName;
  protected Expression mapFileName;
  protected Expression styleMapUri;
  protected Expression xsltUri;
  protected Expression xsltGraphicRenameUri;
  protected Expression graphicsPrefix;
  protected Expression xformReportIdVarName;
  protected Expression xformReportId;
  protected Expression debug;
  protected Expression outputPath;
  

  @Override
  public void execute(WorkflowContext context) throws Exception {
    
    Log wfLog = context.getWorkflowLog();
    
    wfLog.info("Docx2XmlActionHandler: Starting...");
    String xsltUriVar = resolveExpression(xsltUri);
    xsltUriVar = resolveVariables(xsltUriVar);
    
    String styleMapUriVar = resolveExpression(styleMapUri);
    styleMapUriVar = resolveVariables(styleMapUriVar);
    
    String xsltGfxRenameUri = "";
    if (xsltGraphicRenameUri == null || resolveExpression(xsltGraphicRenameUri) == null || resolveVariablesAndExpressions(xsltGraphicRenameUri.getExpressionText()) == null) {
      xsltGfxRenameUri = "rsuite:/res/plugin/dita4publishers/xslt/word2dita/rewriteDocxGraphics.xsl";
    } else {
      xsltGfxRenameUri = resolveVariablesAndExpressions(xsltGraphicRenameUri.getExpressionText());
    //  xsltGfxRenameUri = resolveVariablesAndExpressions(xsltGfxRenameUri);
    }
    
    String gfxPrefix = "";
    if (graphicsPrefix == null || resolveExpression(graphicsPrefix) == null || resolveVariablesAndExpressions(graphicsPrefix.getExpressionText()) == null) {
      gfxPrefix = "";
    } else {
      gfxPrefix = resolveVariablesAndExpressions(graphicsPrefix.getExpressionText());
    }
    
    Docx2XmlBean bean = null;
    
    File outputDir = getOutputDir();
    
    wfLog.info("Effective output directory is \"" + outputDir.getAbsolutePath() + "\"");
    File docxFile = null;
    
    ManagedObject mo = null;
<<<<<<< HEAD
    String docxMoIdVar = "";
=======
    
    
    String docxMoIdVar = resolveVariablesAndExpressions(docxMoId.getExpressionText());
>>>>>>> 7085334cf63a56c9145c19a141272f39cf2d02b0
    wfLog.info("Docx MO ID "+docxMoIdVar);
    
    if (docxMoId == null || resolveExpression(docxMoId)== null) {
      MoListWorkflowObject moList = context.getMoListWorkflowObject();
      if ((moList == null) || (moList.isEmpty()) || ((mo instanceof ContentAssembly))) {
        String msg = "No managed objects in the workflow context and docxMoId parameter not specified . Nothing to do";
        reportAndThrowRSuiteException(msg);
      }
      MoWorkflowObject moObject = (MoWorkflowObject)moList.getMoList().get(0);
      docxMoIdVar = moObject.getMoid();
      mo = context.getManagedObjectService().getManagedObject(context.getAuthorizationService().getSystemUser(), docxMoIdVar);
      
      if (!Docx2Xml.isTransformationApplicable(mo)) {
        reportAndThrowRSuiteException("Transformation does not apply to the first MO in the list list (ID: " + mo.getId() + "); its content type is \"" + mo.getContentType() + "\".");
      }
    }
    else {
      docxMoIdVar = resolveExpression(docxMoId);
      mo = context.getManagedObjectService().getManagedObject(context.getAuthorizationService().getSystemUser(), docxMoIdVar);
            
      if (mo == null) {
        reportAndThrowRSuiteException("Failed to find MO with ID [" + docxMoId + "]");
      }
      if ((mo instanceof ContentAssembly)) {
        reportAndThrowRSuiteException("Specified MO [" + docxMoId + "] is a content assembly or content assembly node.");
      }
      if (!Docx2Xml.isTransformationApplicable(mo)) {
        reportAndThrowRSuiteException("Transformation does not apply to MO ID " + mo.getId() + " with a content type of \"" + mo.getContentType() + "\".");
      }
    }

    String fileNamePrefix = resolveVariablesAndExpressions(getParameterWithDefault("fileNamePref", ""));
    String submapNamePrefix = resolveVariablesAndExpressions(getParameterWithDefault("submapNamePref", "map"));
    
    String docxFilename = mo.getDisplayName();
    wfLog.info("DocxFileName "+docxFilename);
    
    String fileNameBase = context.getVariableAsString("sourceFileName");
    
    /** Added to fix a variable not set on ATD-60  */
    if(StringUtils.isEmpty(fileNameBase)){
    	fileNameBase = context.getVariableAsString("moBaseName");
    	context.setVariable("sourceFileName", fileNameBase);
    }
    
    String mapFileName = fileNameBase + ".ditamap";
    
    String topicFileName = fileNameBase + ".xml";
    
    context.setVariable("mapFileName", mapFileName);
    
    bean = new Docx2XmlBean(context, xsltUriVar, styleMapUriVar, mapFileName, topicFileName);
    if ((xsltGfxRenameUri != null) || (!"".equals(xsltGfxRenameUri))) {
      bean.setXsltGraphicRenameUri(xsltGfxRenameUri);
    }
    wfLog.info("Asset External Path "+mo.getExternalAssetPath());
    docxFile = new File(mo.getExternalAssetPath());
    
    wfLog.info("Creating map dir file "+outputDir+" "+fileNameBase);
    File mapDir = new File(outputDir, fileNameBase);
    
    String resultFileNameVar = resolveVariables(resolveExpression(resultFileName));
    File resultFile = new File(mapDir, resultFileNameVar);
    
    File resultMapFile = new File(mapDir, mapFileName);
    String reportFileName = fileNameBase + "_docx2dita_at_" + DitaOtOptions.getNowString() + ".txt";
    String xformReportIdVarName = getParameterWithDefault("xformReportIdVarName", "xformReportId");
    xformReportIdVarName = resolveVariablesAndExpressions(xformReportIdVarName);
    

    wfLog.info("Report ID is \"" + reportFileName + "\"");
    String xformReportFileNameVarName = getParameterWithDefault("xformReportFileNameVarName", "xformReportFileName");
    context.setVariable(xformReportFileNameVarName, reportFileName);
    
    LoggingSaxonMessageListener logger = context.getXmlApiManager().newLoggingSaxonMessageListener(context.getWorkflowLog());
    File copiedReportFile = null;
    Map<String, String> params = new HashMap();
    params.put("debug", resolveVariablesAndExpressions(getParameterWithDefault("debug", "false")));
    params.put("fileNamePrefix", fileNamePrefix);
    params.put("submapNamePrefix", submapNamePrefix);

    params.put("graphicFileNamePrefix", gfxPrefix + docxMoId + "_");
    
    boolean exceptionOccured = false;
    try {
      bean.generateXmlS9Api(docxFile, resultFile, logger, params); 
      } catch (RSuiteException e) { 
        StringBuilder reportStr;
        StoredReport report;
        File reportFile; exceptionOccured = true;
        String msg = "Exception transforming document: " + e.getMessage();
        reportAndThrowRSuiteException(msg);
        throw e;
      } finally {
      StringBuilder reportStr = new StringBuilder("DOCX 2 DITA Transformation report\n\n").append("Source DOCX: ").append(mo.getDisplayName()).append(" [").append(mo.getId()).append("]\n").append("Result file: ").append(resultMapFile.getAbsolutePath()).append("\n").append("Time performed: ").append(DitaOtOptions.getNowString()).append("\n\n").append(logger.getLogString());
      

      if (exceptionOccured) {
        reportStr.append(" + [ERROR] Exception occurred: ").append(context.getExceptionMessage());
      }
      else
      {
        reportStr.append(" + [INFO] Process finished normally");
      }
      StoredReport report = context.getReportManager().saveReport(reportFileName, reportStr.toString());
      copiedReportFile = new File(mapDir, report.getSuggestedFileName());
      wfLog.info("Generation report in location  \"" + copiedReportFile.getAbsolutePath() + "\"");
      context.setVariable("xformReportId", report.getId());
      File reportFile = report.getFile();
      FileUtils.copyFile(reportFile, copiedReportFile);
    }
    
    wfLog.info("XML generated in location \"" + resultMapFile.getAbsolutePath() + "\"");
    
    context.setFileWorkflowObject(new FileWorkflowObject(resultMapFile));
  }
  
  protected File getOutputDir() throws Exception {
    String outputPathVar = resolveExpression(outputPath);
    outputPathVar = resolveVariables(outputPathVar);
    
    File outputDir = null;
    if ((outputPathVar == null) || ("".equals(outputPathVar.trim()))) {
      outputDir = getWorkingDir(false);
    } else {
      outputDir = new File(outputPathVar);
      if (!outputDir.exists()) {
        outputDir.mkdirs();
      }
      if (!outputDir.exists()) {
        reportAndThrowRSuiteException("Failed to find or create output directory \"" + outputPathVar + "\"");
      }
      if (!outputDir.canWrite()) {
        reportAndThrowRSuiteException("Cannot write to output directory \"" + outputPathVar + "\"");
      }
    }
    return outputDir;
  }
  
  public void setXsltUri(String xsltUri) {
    setParameter("xsltUri", xsltUri);
  }
  
  public void setStyleMapUri(String styleMapUri) {
    setParameter("styleMapUri", styleMapUri);
  }
  
  public void setXformReportIdVarName(String xformReportIdVarName) {
    setParameter("xformReportIdVarName", xformReportIdVarName);
  }
  
  public void setXformReportFileNameVarName(String xformReportFileNameVarName) {
    setParameter("xformReportFileName", xformReportFileNameVarName);
  }
  
  public void setOutputPath(String outputPath) {
    setParameter("outputPath", outputPath);
  }
  
  public void setResultFileName(String resultFileName) {
    setParameter("resultFileName", resultFileName);
  }
  
  public void setDocxMoId(String docxMoId) {
    setParameter("docxMoId", docxMoId);
  }
  
  public void setFileNamePrefix(String fileNamePref) {
    setParameter("fileNamePref", fileNamePref);
  }
  
  public void setSubmapNamePrefix(String submapNamePref) {
    setParameter("submapNamePref", submapNamePref);
  }
  
  public void setXsltGraphicRenameUri(String s) {
    setParameter("xsltGraphicRenameUri", s);
  }
  
  public void setGraphicsPrefix(String s) {
    setParameter("graphicsPrefix", s);
  }
  
  public void setDefault(String debug) {
    setParameter("debug", debug);
  }


}
