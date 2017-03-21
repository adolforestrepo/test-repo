package org.astd.rsuite.advisors.workflow;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.constants.WorkflowConstants;
import org.astd.rsuite.utils.WorkflowUtils;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.workflow.DefaultWorkflowExecutionAdvisor;
import com.reallysi.rsuite.api.workflow.WorkflowExecutionAdvisorBeforeExecutionContext;

/**
 * Routes new workflow process instances to a specific workflow pool.
 * <p>
 * This only applies to workflow process instances that have a workflow variable named
 * {@value org.astd.rsuite.constants.WorkflowConstants#WF_VAR_NAME_WORKFLOW_POOL_NAME}. The
 * starting process is responsible for setting this workflow variable. Else, the process instance
 * will be processed in the default workflow pool.
 */
public class AssignToPoolWorkflowExecutionAdvisor
    extends DefaultWorkflowExecutionAdvisor
    implements WorkflowConstants {

  private static Log log = LogFactory.getLog(AssignToPoolWorkflowExecutionAdvisor.class);

  @Override
  public void adviseBeforeExecution(ExecutionContext executionContext,
      WorkflowExecutionAdvisorBeforeExecutionContext context) {
    String pid = context.getProcessInstanceId();

    if (log.isDebugEnabled()) {
      log.debug(new StringBuilder("Checking workflow request ID ").append(pid).append(", \"")
          .append(context.getProcessDefinitionName()).append("\"").toString());
    }
    try {
      Map<String, Object> vars = WorkflowUtils.getVariables(executionContext
          .getWorkflowInstanceService().getWorkflowInstance(pid));

      // See if the workflow process instance is requesting a specific
      // workflow pool.
      String poolName = getPreferredPoolName(vars);

      if (log.isDebugEnabled()) {
        StringBuilder buf = new StringBuilder("Workflow process instance ID ").append(pid);
        if (StringUtils.isNotBlank(poolName)) {
          buf.append(" requesting workflow pool \"").append(poolName).append("\".");
        } else {
          buf.append(" did not specify an override workflow pool.");
        }
        log.debug(buf.toString());
      }

      // See if we need to change the pool
      if (StringUtils.isNotBlank(poolName) && !poolName.equalsIgnoreCase(context
          .getPreferredPoolName())) {
        if (log.isDebugEnabled()) {
          log.debug(new StringBuilder("Switching from the ").append(context.getPreferredPoolName())
              .append(" pool to the ").append(poolName).append(" pool.").toString());
        }

        context.setPreferredPoolName(poolName);
      }
    } catch (RSuiteException e) {

      log.warn(new StringBuilder("Unable to access the workflow variables for process instance ")
          .append(pid).append("; not overriding workflow pool.").toString(), e);
    }
  }

  /**
   * Get the preferred pool name as known by the process instance.
   * 
   * @param vars
   * @return The preferred workflow pool name, from the provided workflow variables, or null when it
   *         is not specified.
   */
  protected static String getPreferredPoolName(Map<String, Object> vars) {
    Object o = vars.get(WF_VAR_NAME_WORKFLOW_POOL_NAME);
    if (o != null && o instanceof String)
      return (String) o;
    return null;
  }

}
