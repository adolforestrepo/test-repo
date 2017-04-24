package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import org.activiti.engine.delegate.Expression;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.astd.rsuite.utils.ProjectAstdActionUtils;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;


/**
 * Set taxonomy classification for an MO.
 * <p>
 * The MO to set classification data for is either specified by the <tt>moId</tt> or
 * <tt>moAlias</tt> parameter. If neither parameter is specified, than context variable is used to
 * determine the ID of the MO to retrieve classification data from.
 * </p>
 * <p>
 * The classification data is specified by the <tt>data</tt> parameter. Normally, this should be a
 * variable reference since the data should be in XML format as defined by ASTD DTDs.
 * </p>
 */

public class ProjectAstdSetMoClassification
    extends BaseWorkflowAction
    implements TempWorkflowConstants {

  protected Log wfLog;

  /**
   * (Optional) Alias to MO.
   */
  public static final String MO_ALIAS_PARAM = "moAlias";

  /**
   * (Optional) Alias to MO.
   */
  public static final String MO_ID_PARAM = "moId";

  /**
   * Name of the variable containing the classification xml.
   */
  public static final String DATA_PARAM = "data";

  protected Expression moAlias;
  protected Expression moId;
  protected Expression data;


  @Override
  public void execute(WorkflowContext context) throws Exception {
    wfLog = context.getWorkflowLog();
    User user = context.getAuthorizationService().getSystemUser();

    String dataExp = resolveExpression(data);
    if (StringUtils.isBlank(dataExp)) {
      wfLog.info("No classification data provided, nothing to do");
      return;
    }

    String moIdExp = resolveExpression(moId);
    if (StringUtils.isBlank(moIdExp)) {
      String moAliasExp = resolveExpression(moAlias);
      if (!StringUtils.isBlank(moAliasExp)) {
        ManagedObject mo = context.getManagedObjectService().getObjectByAlias(user, moAliasExp);
        if (mo != null) {
          String moId = mo.getId();
        } else {
          MoListWorkflowObject moList = context.getMoListWorkflowObject();
          if (moList == null || moList.isEmpty()) {
            reportAndThrowRSuiteException("MO ID not specified to set classification");
          }
          String moId = moList.getMoList().get(0).getMoid();
        }
      }
    }


    wfLog.info("Classification data to set for " + moId + ": " + dataExp);
    try {
      ProjectAstdActionUtils.setClassificationXmlForMo(context, user, wfLog, moIdExp, dataExp);
    } catch (RSuiteException rse) {
      String msg = "Error setting classification for mo [" + moId + "]: " + rse
          .getLocalizedMessage() + rse;
      reportAndThrowRSuiteException(msg);
    }
  }

  public void setData(String s) {
    setParameter(DATA_PARAM, s);
  }

  public void setMoId(String s) {
    setParameter(MO_ID_PARAM, s);
  }

  public void setMoAlias(String s) {
    setParameter(MO_ALIAS_PARAM, s);
  }

}
