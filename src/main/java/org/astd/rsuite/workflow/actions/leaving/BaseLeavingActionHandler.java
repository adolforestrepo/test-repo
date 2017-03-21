package org.astd.rsuite.workflow.actions.leaving;

import org.astd.rsuite.constants.WorkflowConstants;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowErrors;

/**
 * Base class for workflow actions that are to transition (leave) to the next JBPM node once
 * complete.
 * <p>
 * Workflow action handlers from the 2014 code base may not yet extend this class. New and modified
 * workflow action handlers should.
 */
public abstract class BaseLeavingActionHandler
    extends BaseWorkflowAction
    implements WorkflowConstants {

  /**
   * Serialization bit
   */
  private static final long serialVersionUID = 1L;

  /**
   * The JBPM node to go to next if operation considered successful.
   */
  protected String nextNodeUponSuccess;

  /**
   * The JBPM node to go to next if operation ended with a business exception.
   */
  protected String nextNodeUponBusinessException;

  /**
   * The JBPM node to go to next if operation ended with a system exception.
   */
  protected String nextNodeUponSystemException;

  /**
   * Set next node upon success.
   * <p>
   * RSuite will call this for us when the workflow definition includes a parameter named
   * "NextNodeUponSuccess"
   * 
   * @param nextNodeName
   */
  public void setNextNodeUponSuccess(String nextNodeName) {
    this.nextNodeUponSuccess = nextNodeName;
  }

  /**
   * @return The next node upon success
   */
  public String getNextNodeUponSuccess() {
    return nextNodeUponSuccess;
  }

  /**
   * Set next node upon business exception.
   * <p>
   * RSuite will call this for us when the workflow definition includes a parameter named
   * "NextNodeUponBusinessException"
   * 
   * @param nextNodeName
   */
  public void setNextNodeUponBusinessException(String nextNodeName) {
    this.nextNodeUponBusinessException = nextNodeName;
  }

  /**
   * @return The next node upon business exception
   */
  public String getNextNodeUponBusinessException() {
    return nextNodeUponBusinessException;
  }

  /**
   * Set next node upon system exception.
   * <p>
   * RSuite will call this for us when the workflow definition includes a parameter named
   * "NextNodeUponSystemException"
   * 
   * @param nextNodeName
   */
  public void setNextNodeUponSystemException(String nextNodeName) {
    this.nextNodeUponSystemException = nextNodeName;
  }

  /**
   * @return The next node upon system exception
   */
  public String getNextNodeUponSystemException() {
    return nextNodeUponSystemException;
  }

  /**
   * Set the next node for the process instance
   * 
   * @param context
   * @param nextWorkflowNodeName
   * @throws RSuiteException
   * @throws LNPException
   */
  protected void setNextNode(WorkflowContext context, String nextWorkflowNodeName)
      throws RSuiteException {
    if (nextWorkflowNodeName == null || nextWorkflowNodeName.isEmpty()) {
      handleWorkflowProcessInstanceError(context, "The next workflow node name is not defined.");
      return;
    }

    context.setVariable(WF_VAR_NAME_NODE_TO_TRANSITION_TO, nextWorkflowNodeName);
  }

  /**
   * Handle a workflow process instance error. Caller to stop processing by returning out of
   * execute() --not by throwing an exception.
   * 
   * @param context
   * @param msg
   * @throws Throwable
   */
  protected void handleWorkflowProcessInstanceError(WorkflowContext context, String msg)
      throws RSuiteException {
    handleWorkflowProcessInstanceError(context, msg, null);
  }

  /**
   * Handle a workflow process instance error. Caller to stop processing by returning out of
   * execute() --not by throwing an exception.
   * 
   * @param context
   * @param msg
   * @param t
   * @throws Throwable
   */
  protected void handleWorkflowProcessInstanceError(WorkflowContext context, String msg,
      Throwable t) throws RSuiteException {

    // TODO: Is this the right to do this in v5? No code is currently using this, so functionally
    // moot
    // setCommentStreamMessage(msg);
    context.addComment(context.getTask().getAssigneeUserId(), msg);
    if (t == null) {
      t = new Exception(msg);
      context.getWorkflowLog().error(msg);
    } else {
      context.getWorkflowLog().error(msg, t);
    }

    context.setVariable(WF_VAR_NAME_EXCEPTION_OCCURRED, "true");
    context.setVariable(WF_VAR_NAME_EXCEPTION_TYPE, "EXCEPTION_TYPE_SYSTEM");

    WorkflowErrors errors = new WorkflowErrors();
    errors.addSystemException(t);
    context.setVariable(WF_VAR_NAME_WORKFLOW_ERRORS_QUALIFIED_CLASSNAME, errors);

  }

}
