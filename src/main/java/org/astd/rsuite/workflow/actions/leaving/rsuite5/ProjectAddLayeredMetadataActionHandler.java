package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.util.List;

import org.activiti.engine.delegate.Expression;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;


/**
 * Adds layered metadata to the set of MOs in the workflow context.
 */
public class ProjectAddLayeredMetadataActionHandler
    extends BaseWorkflowAction
    implements TempWorkflowConstants {

  /**
   * Value to set the metadata field to.
   */
  public static final String METADATA_VALUE_PARAM = "metadataValue";

  /**
   * Name of the metadata field. The metadata field must already be configured in RSuite.
   */
  public static final String METADATA_NAME_PARAM = "metadataName";

  /**
   * MO IDs to have LMD set on them. If not specified, workflow context is used. Comma-delimited
   * list of MO IDs.
   */
  public static final String MO_LIST_PARAM = "moList";

  protected Log wfLog;

  protected Expression metadataNameFromWorkflow;
  protected Expression metadataValueFromWorkflow;

  @Override
  public void execute(WorkflowContext context) throws Exception {
    wfLog = context.getWorkflowLog();
    logActionHandlerParameters(wfLog);

    MoListWorkflowObject moWfList = null;
    List<MoWorkflowObject> moList = null;

    // RCS-67: Catch all exceptions so this action handler never throws
    // an exception to the underlying base implementation. This is a workaround
    // for a bug in the abstract base action handler where a caught exception
    // bypasses the "leave node on exit" check.
    try {
      String moListParam = resolveVariablesAndExpressions(getParameter(MO_LIST_PARAM));
      if (moListParam == null) {
        moWfList = context.getMoListWorkflowObject();
        if (moWfList == null) {
          throw new RSuiteException("MoListWorkflowObject cannot be null.");
        }
      } else {
        String[] moIds = moListParam.split(",");
        moWfList = new MoListWorkflowObject();
        for (String moId : moIds) {
          moWfList.addMoIfNotPresent(moId);
        }
      }
      if (moWfList.isEmpty())
        throw new RSuiteException("Effective list of MOs is empty.");

      moList = moWfList.getMoList();

      String metadataName = getWorkflowVariableOrParameter(context, METADATA_NAME_PARAM,
          metadataNameFromWorkflow);
      String metadataValue = getWorkflowVariableOrParameter(context, METADATA_VALUE_PARAM,
          metadataValueFromWorkflow);

      wfLog.info("To resolve: metadataName [" + metadataName + "] [" + metadataValue + "]");

      // checkParamsNotEmptyOrNull(METADATA_NAME_PARAM, METADATA_VALUE_PARAM);

      String[] metaNameArray = metadataName == null ? null : metadataName.split(";");

      String[] metaValueArray = metadataValue == null ? null : metadataValue.split(";");

      if (metaNameArray != null && metaValueArray != null
          && metaNameArray.length != metaValueArray.length) {

        throw new RSuiteException(
            "Number of metadata names does not match number of metadata values.");
      }

      doLayeredMetadataSetting(context, moList, metaNameArray, metaValueArray);
    } catch (Exception e) {
      wfLog.error(e.getClass().getSimpleName() + " setting layered metadata: " + e.getMessage(), e);
      context.setVariable(EXCEPTION_OCCUR, true);
      context.setVariable(EXCEPTION_TYPE, EXCEPTION_TYPE_SYSTEM);
    }

    wfLog.info("Done");

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
    context.getWorkflowLog().info("Resolved expression [" + resolveExpression(workflowExpression)
        + "]");
    if (StringUtils.isNotEmpty(workflowVarOrParam)) {
      return workflowVarOrParam;
    }
    workflowVarOrParam = resolveExpression(workflowExpression);
    return workflowVarOrParam;
  }

  public void doLayeredMetadataSetting(WorkflowContext context, List<MoWorkflowObject> moList,
      String[] metaNameArray, String[] metaValueArray) throws RSuiteException {
    User sys = context.getAuthorizationService().getSystemUser();
    for (int i = 0; i < moList.size(); i++) {
      MoWorkflowObject obj = moList.get(i);
      wfLog.info("Setting LMD for MO [" + obj.getMoid() + "]...");

      int length = metaNameArray == null ? 0 : metaNameArray.length;

      String[] names = new String[length];
      String[] values = new String[length];

      int index = 0;
      if (metaNameArray != null && metaNameArray.length > 0) {
        for (index = 0; index < metaNameArray.length; index++) {
          names[index] = metaNameArray[index];
          String objectSourceName = null;
          if (obj.getSource() != null) {
            objectSourceName = obj.getSource().getName();
          }
          values[index] = build(context, metaValueArray[index], null, objectSourceName);
          wfLog.info("  name: " + names[index] + ", value: \"" + values[index] + "\"");
          try {
            MetaDataItem item = new MetaDataItem(names[index], values[index]);
            context.getManagedObjectService().setMetaDataEntry(sys, obj.getMoid(), item);
          } catch (Exception e) {
            wfLog.error("Can't add the LayeredMetadata {metadataName: " + names[index]
                + ", metadataValue: \"" + metaValueArray[index] + "\", MOID: " + obj.getMoid()
                + "} Reason: " + e);
            context.setVariable(EXCEPTION_OCCUR, true);
            context.setVariable(EXCEPTION_TYPE, EXCEPTION_TYPE_SYSTEM);
          }
        }
      }
    }
  }

  private String build(WorkflowContext context, String expression, String variableContext,
      String filename) throws RSuiteException {

    // New expression syntax:
    if (expression.startsWith("${DATABASE(")) {
      variableContext = variableContext == null
          ? "^(\\${DATABASE\\(\\s*['|\"]{1})|(['|\"]{1}\\s*\\))}$" : variableContext;
      String newExp = expression.trim().replaceAll(variableContext, "");
      newExp = resolveVariables(filename, newExp);

      try {
        return context.getRepositoryService().queryAsString(newExp);
      } catch (RSuiteException e) {
        return null;
      }
    }
    // Old (2.x) expression syntax:
    if (expression.startsWith("$DATABASE(")) {
      variableContext = variableContext == null
          ? "^(\\${DATABASE\\(\\s*['|\"]{1})|(['|\"]{1}\\s*\\))$" : variableContext;
      String newExp = expression.trim().replaceAll(variableContext, "");
      newExp = resolveVariables(filename, newExp);

      try {
        return context.getRepositoryService().queryAsString(newExp);
      } catch (RSuiteException e) {
        return null;
      }
    }
    return resolveVariables(filename, expression);
  }

  public void setMoList(String moList) {
    setParameter(MO_LIST_PARAM, moList);
  }
}

