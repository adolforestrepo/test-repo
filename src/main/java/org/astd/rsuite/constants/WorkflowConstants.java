package org.astd.rsuite.constants;

/**
 * Workflow constants.
 */
public interface WorkflowConstants {

  /**
   * The name of the workflow variable identifying the workflow pool to use.
   */
  String WF_VAR_NAME_WORKFLOW_POOL_NAME = "WorkflowPoolName";

  /**
   * The name of the workflow pool for in-bound content migration requests.
   */
  String WF_POOL_NAME_MIGRATION = "migration";

  /**
   * The name of the workflow pool for out-bound content delivery requests.
   */
  String WF_POOL_NAME_DELIVERY = "delivery";

  /**
   * The delimiter to use between multiple values stored as a single workflow variable value.
   */
  String WF_VAR_VALUE_DELIMITER = ",";

  /**
   * The initiating operation ID
   */
  String WF_VAR_NAME_INITIATING_OPERATION_ID = "initiatingOpId";

  /**
   * The batch number. This may be used by operations that split its work over multiple workflow
   * process instances, where each workflow process instance is considered a batch.
   */
  String WF_VAR_NAME_BATCH_NUMBER = "batchNumber";

  /**
   * The name of the workflow variable identifying the contents associated with the workflow.
   */
  String WF_VAR_NAME_RSUITE_CONTENTS = "rsuite contents";

  /**
   * The name of the workflow variable identifying the JBPM node to transition to.
   */
  String WF_VAR_NAME_NODE_TO_TRANSITION_TO = "TransitionTo";

  /**
   * Workflow variable to identify if any exception has ocurred during the workflow execution.
   */
  String WF_VAR_NAME_EXCEPTION_OCCURRED = "EXCEPTION_OCCUR";

  /**
   * Workflow variable to identify the exception type.
   */
  String WF_VAR_NAME_EXCEPTION_TYPE = "EXCEPTION_TYPE";

  /**
   * Workflow variable to identify the qualified class name of the workflow errors.
   */
  String WF_VAR_NAME_WORKFLOW_ERRORS_QUALIFIED_CLASSNAME =
      "com.reallysi.rsuite.api.workflow.WorkflowErrors";

  /**
   * The name of the workflow variable identifying the user ID.
   */
  String WF_VAR_NAME_USER_ID = "UserId";

}
