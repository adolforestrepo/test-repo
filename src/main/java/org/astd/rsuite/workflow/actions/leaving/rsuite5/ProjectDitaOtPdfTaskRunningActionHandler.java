package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.io.File;
import java.util.Properties;

import org.activiti.engine.delegate.Expression;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.workflow.activiti.FileWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.api.xml.DitaOpenToolkit;
import com.reallysi.rsuite.workflow.actions.DitaOtPdfTaskRunningActionHandlerBase;

/**
 * Runs the generic PDF transform type.
 */
public class ProjectDitaOtPdfTaskRunningActionHandler
    extends DitaOtPdfTaskRunningActionHandlerBase
    implements TempWorkflowConstants {

  public static final String DEFAULT_TRANSTYPE = "pdf2";
  /**
   * If Antenna House XSL Formatter (AXF) is the selected formatter, specifies the install path.
   */
  public static final String AXF_PATH_PARAM = "axfPath";

  protected Expression buildPropertiesFromWorkflow;
  protected Expression transtypeFromWorkflow;
  protected Expression outputPathFromWorkflow;

  /*
   * (non-Javadoc)
   * 
   * @see com.reallysi.rsuite.api.workflow.AbstractBaseNonLeavingActionHandler#execute(com.reallysi.
   * rsuite.api.workflow.WorkflowExecutionContext)
   */
  @Override
  public void execute(WorkflowContext context) throws Exception {
    Log wfLog = context.getWorkflowLog();

    context.setVariable(EXCEPTION_OCCUR, false);
    String transtype = resolveVariables(getParameterWithDefault("transtype", DEFAULT_TRANSTYPE));
    transtype = getWorkflowVariableOrParameter(context, "transtypeFromWorkflow",
        transtypeFromWorkflow);
    // Handle case where workflow process has <transtype>${transtype}</transtype>
    // but ${transtype} isn't set.
    if (transtype == null || transtype.startsWith("${")) {
      transtype = DEFAULT_TRANSTYPE;
    }

    setBuildProperties(getWorkflowVariableOrParameter(context, "buildPropertiesFromWorkflow",
        buildPropertiesFromWorkflow));
    setOutputPath(getWorkflowVariableOrParameter(context, "outputPathFromWorkflow",
        outputPathFromWorkflow));

    DitaOpenToolkit toolkit = getToolkit(context, wfLog);

    Properties props = setBaseTaskProperties(context, transtype, toolkit);
    this.setArgumentIfSpecified(context, props, "axf.path", AXF_PATH_PARAM);

    cleanOutputDir(context, getOutputDir(context));

    // The applyToolkitProcessToMos() method manages the export of the
    // MOs to the file system, so at this point we don't know what the
    // filename of the exported map is.
    applyToolkitProcessToMos(context, wfLog, toolkit, transtype, props);

    // Set the generated PDF as the workflow context object.

    String exportedMapFilename = context.getVariableAsString(EXPORTED_MAP_FILENAME_VARNAME);
    String outputPath = context.getVariableAsString(OUTPUT_DIR_VARNAME);

    File outputDir = new File(outputPath);
    String mapNamePart = FilenameUtils.getBaseName(exportedMapFilename);
    String pdfFilename = mapNamePart + ".pdf";
    File pdfFile = new File(outputDir, pdfFilename);
    if (!pdfFile.exists()) {
      context.setVariable(EXCEPTION_OCCUR, true);
      throw new RSuiteException("Did not find expected PDF result file \"" + pdfFile
          .getAbsolutePath() + "\"");
    }
    context.setFileWorkflowObject(new FileWorkflowObject(pdfFile));

  }

  /**
   * Gets the value of a workflow variable / parameter.
   * 
   * @param context
   * @return the value.
   */
  protected String getWorkflowVariableOrParameter(WorkflowContext context,
      String workflowVariableOrParameterName, Expression workflowExpression) {
    String workflowVarOrParam = context.getVariableAsString(workflowVariableOrParameterName);
    if (StringUtils.isNotEmpty(workflowVarOrParam)) {
      return workflowVarOrParam;
    }
    workflowVarOrParam = resolveVariablesAndExpressions(getParameter(
        workflowVariableOrParameterName));
    if (StringUtils.isNotEmpty(workflowVarOrParam)) {
      return workflowVarOrParam;
    }
    workflowVarOrParam = resolveExpression(workflowExpression);
    context.getWorkflowLog().info("Resolved expression [" + workflowVarOrParam + "]");
    return workflowVarOrParam;
  }

  public void setAxfPath(String axfPath) {
    this.setParameter(AXF_PATH_PARAM, axfPath);
  }

}

