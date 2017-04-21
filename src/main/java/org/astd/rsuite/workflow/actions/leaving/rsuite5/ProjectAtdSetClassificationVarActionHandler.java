package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import org.activiti.engine.delegate.Expression;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.astd.rsuite.utils.WorkflowUtils;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;

public class ProjectAtdSetClassificationVarActionHandler
    extends BaseWorkflowAction
    implements TempWorkflowConstants {

  /**
   * (Optional) Alias to MO.
   */
  public static final String MO_ALIAS_PARAM = "moAlias";

  /**
   * (Optional) Alias to MO.
   */
  public static final String MO_ID_PARAM = "moId";

  /**
   * Name of the variable to hold the classification xml.
   */
  public static final String TARGET_VAR_PARAM = "targetVariableName";

  protected Expression moAlias;
  protected Expression moId;
  protected Expression targetVariableName;

  @Override
  public void execute(WorkflowContext context) throws Exception {
    Log wfLog = context.getWorkflowLog();
    User user = context.getAuthorizationService().getSystemUser();

    String varName = resolveExpression(targetVariableName);
    if (StringUtils.isBlank(varName)) {
      reportAndThrowRSuiteException("Target variable name not specified");

    }

    String moIdExp = resolveExpression(moId);
    if (StringUtils.isBlank(moIdExp)) {
      String moAlias = resolveVariables(getParameter(MO_ALIAS_PARAM));
      if (!StringUtils.isBlank(moAlias)) {
        ManagedObject mo = context.getManagedObjectService().getObjectByAlias(user, moAlias);
        if (mo != null) {
          String moId = mo.getId();
        }
      }
    }

    if (StringUtils.isBlank(moIdExp)) {
      MoListWorkflowObject moList = context.getMoListWorkflowObject();
      if (moList == null || moList.isEmpty()) {
        // There are cases where MO cannot be resolved
        // at this time, so we just log message and
        // quietly return
        wfLog.info("Unable to determine MO ID to get classification from");
        context.setVariable(varName, "");
        return;
      }
      String moId = moList.getMoList().get(0).getMoid();
    }

    String taxData = null;
    taxData = WorkflowUtils.getClassificationXmlOfMo(context, wfLog, moIdExp);

    // Strip out containing <RESULT> tags
    if (taxData.startsWith("<RESULT/>")) {
      // No classification data for MO
      taxData = "";
    } else {
      taxData = taxData.substring("<RESULT>".length(), taxData.length() - "</RESULT>".length());
    }

    wfLog.info("Classification data for " + moId + ": " + taxData);
    context.setVariable(varName, taxData);
  }

  public void setTargetVariableName(String s) {
    setParameter(TARGET_VAR_PARAM, s);
  }

  public void setMoId(String s) {
    setParameter(MO_ID_PARAM, s);
  }

  public void setMoAlias(String s) {
    setParameter(MO_ALIAS_PARAM, s);
  }

}
