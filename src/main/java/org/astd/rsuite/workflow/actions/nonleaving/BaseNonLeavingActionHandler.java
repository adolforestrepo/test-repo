package org.astd.rsuite.workflow.actions.nonleaving;

import org.astd.rsuite.constants.WorkflowConstants;

import com.reallysi.rsuite.api.workflow.activiti.BaseNonLeavingWorkflowAction;

/**
 * Base action handler for actions that are not to automatically move on to the next JBPM transition
 * when complete.
 */
public abstract class BaseNonLeavingActionHandler
    extends BaseNonLeavingWorkflowAction
    implements WorkflowConstants {

  private static final long serialVersionUID = 1L;

}
