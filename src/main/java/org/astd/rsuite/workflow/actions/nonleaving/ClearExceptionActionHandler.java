package org.astd.rsuite.workflow.actions.nonleaving;

import org.astd.rsuite.constants.WorkflowConstants;

import com.reallysi.rsuite.api.workflow.activiti.BaseNonLeavingWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;

public class ClearExceptionActionHandler
    extends BaseNonLeavingWorkflowAction
    implements WorkflowConstants {


  /** serial uid. */
  private static final long serialVersionUID = -7209300840997626646L;

  @Override
  final public void execute(WorkflowContext executionContext) throws Exception {

    executionContext.deleteVariable(WF_VAR_NAME_EXCEPTION_OCCURRED);
    executionContext.deleteVariable(WF_VAR_NAME_EXCEPTION_TYPE);

  }



}
