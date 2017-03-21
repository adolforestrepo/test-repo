package org.astd.rsuite.workflow.actions.nonleaving;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.workflow.WorkflowExecutionContext;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.service.ManagedObjectService;

/**
 * Sets a piece of layered metadata on an object, without transitioning to the next JBPM node. The
 * metadata name/value pair is also retained a workflow variable, making it available for other
 * purposes (GUI, reports, search, etc.). An additional parameter may be used to only change the
 * metadata when the current task is assigned to an actor/user.
 * <p>
 * This is an alternative to
 * <code>com.reallysi.service.workflow.ingestion.action.AddLayeredMetadataActionHandler</code>,
 * which transitions to the next JBPM node --a behavior we do not want in the context of a task
 * event.
 * <p>
 * The {@value org.astd.rsuite.constants.WorkflowConstants#WF_VAR_NAME_RSUITE_CONTENTS} workflow
 * variable is to identify the object to set metadata on. The workflow definition should pass in two
 * parameters: {@value #WF_PARAM_NAME_METADATA_NAME} and {@value #WF_PARAM_NAME_METADATA_VALUE}.
 * Optionally, the workflow definition may also pass in the
 * {@value #WF_PARAM_NAME_REQUIRE_TASK_ACTOR} parameter. When the value equates to true, the
 * metadata will only be set if the current task is assigned to an actor/user.
 * <p>
 * Presently, this class is limited to setting one piece of metadata one one object. It could be
 * extended to set multiple pieces on one or more objects.
 * <p>
 * If unable to set the metadata, an error is logged. At least {@link #execute(WorkflowContext)}
 * will not throw an exception.
 * <p>
 * This code does not attempt to check the object out, meaning either a check out is not necessary,
 * or the object is to be checked out before {@link #execute(WorkflowContext)} is called. Likewise,
 * this code does not attempt to check the object in.
 */
public class SetLayeredMetadataWorkflowAction
    extends BaseNonLeavingActionHandler {

  private static final long serialVersionUID = 1L;

  /**
   * Name of workflow parameter identifying the layered metadata name.
   */
  public static final String WF_PARAM_NAME_METADATA_NAME = "metadataName";

  /**
   * Name of workflow parameter identifying the layered metadata value.
   */
  public static final String WF_PARAM_NAME_METADATA_VALUE = "metadataValue";

  /**
   * Name of workflow parameter indicating if a task actor should be required.
   */
  public static final String WF_PARAM_NAME_REQUIRE_TASK_ACTOR = "requireTaskActor";

  /**
   * Retain the metadata name to apply. Providing the workflow definition specifies the
   * {@value #WF_PARAM_NAME_METADATA_NAME} parameter into this class, this method will be called
   * automatically, before {@link #execute(WorkflowExecutionContext)}.
   * 
   * @param val
   */
  public void setMetadataName(String val) {
    this.setParameter(WF_PARAM_NAME_METADATA_NAME, val);
  }

  /**
   * Retain the metadata name to apply. Providing the workflow definition specifies the
   * {@value #WF_PARAM_NAME_METADATA_VALUE} parameter into this class, this method will be called
   * automatically, before {@link #execute(WorkflowExecutionContext)}.
   * 
   * @param val
   */
  public void setMetadataValue(String val) {
    this.setParameter(WF_PARAM_NAME_METADATA_VALUE, val);
  }

  /**
   * Retain if a task actor is required before making the requested LMD change. Providing the
   * workflow definition specifies the {@value #WF_PARAM_NAME_REQUIRE_TASK_ACTOR} parameter into
   * this class, this method will be called automatically, before
   * {@link #execute(WorkflowExecutionContext)}.
   * 
   * @param val
   */
  public void setRequireTaskActor(String val) {
    this.setParameter(WF_PARAM_NAME_REQUIRE_TASK_ACTOR, val);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.reallysi.rsuite.api.workflow.AbstractBaseNonLeavingActionHandler#execute(com.reallysi.
   * rsuite .api.workflow.WorkflowExecutionContext)
   */
  @Override
  public void execute(WorkflowContext context) throws RSuiteException {
    Log log = context.getWorkflowLog();
    try {
      String name = WF_PARAM_NAME_METADATA_NAME;
      String metadataName = getParameterWithDefault(name, null);
      if (StringUtils.isBlank(name)) {
        log.error("The \"" + name + "\" workflow parameter is blank.");
        return;
      }

      name = WF_PARAM_NAME_METADATA_VALUE;
      String metadataValue = getParameterWithDefault(name, null);
      if (StringUtils.isBlank(metadataValue)) {
        log.error("The \"" + name + "\" workflow parameter is blank.");
        return;
      }

      /*
       * IDEA: Support multiple objects assocated to the workflow process instance.
       */
      name = WF_VAR_NAME_RSUITE_CONTENTS;
      String id = context.getVariableAsString(name);
      if (StringUtils.isBlank(id)) {
        log.error("The \"" + name + "\" workflow variable is blank.");
        return;
      }

      // Enforce task actor, when instructed to do so.
      name = WF_PARAM_NAME_REQUIRE_TASK_ACTOR;
      String requireTaskActor = getParameterWithDefault(name, null);
      if (StringUtils.isNotBlank(requireTaskActor) && Boolean.parseBoolean(requireTaskActor)
          && context.getTask().getAssigneeUserId() == null) {
        log.info(
            "Skipping LMD change request. Per the workflow definition, the task must be accepted by a user first.");
        return;
      }

      ManagedObjectService moService = context.getManagedObjectService();
      User user = context.getAuthorizationService().getSystemUser();

      /*
       * If LMD values do not differ, there's no need to proceed.
       * 
       * IDEA: Support repeating LMD.
       */
      if (metadataValue.equals(moService.getManagedObject(user, id).getLayeredMetadataValue(
          metadataName))) {
        log.info("Skipping LMD change request. The \"" + metadataName + "\" is already set to \""
            + metadataValue + "\" on object ID " + id);
        return;
      }

      log.info("Setting LMD \"" + metadataName + "\" to \"" + metadataValue + "\" on object ID "
          + id);

      // OK to change the metadata.
      moService.setMetaDataEntry(user, id, new MetaDataItem(metadataName, metadataValue));

      // Also retain as a workflow variable.
      context.setVariable(metadataName, metadataValue);

    } catch (Exception e) {
      log.error("Unable to set metadata", e);
    }
  }

}
