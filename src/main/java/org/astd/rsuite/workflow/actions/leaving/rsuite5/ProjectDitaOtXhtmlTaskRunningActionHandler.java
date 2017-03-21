package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.util.Properties;

import org.activiti.engine.delegate.Expression;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.api.xml.DitaOpenToolkit;
import com.reallysi.rsuite.workflow.actions.DitaOtXhtmlTaskRunningActionHandlerBase;

/**
 * Runs the generic PDF transform type.
 */
public class ProjectDitaOtXhtmlTaskRunningActionHandler
    extends DitaOtXhtmlTaskRunningActionHandlerBase
    implements TempWorkflowConstants {

  public static final String DEFAULT_TRANSTYPE = "xhtml";

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

    cleanOutputDir(context, getOutputDir(context));

    // The applyToolkitProcessToMos() method manages the export of the
    // MOs to the file system, so at this point we don't know what the
    // filename of the exported map is.
    applyToolkitProcessToMos(context, wfLog, toolkit, transtype, props);

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

}

