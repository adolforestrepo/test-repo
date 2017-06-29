
package com.reallysi.rsuite.workflow.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.lang3.StringUtils;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;

/**
 * Class that will attach the result of a OT process to the workflow.
 * 
 * @author RSI Content Solutions.
 */
public class AttachMoToWorkflowAction extends BaseWorkflowAction {

	@SuppressWarnings("unused")
	private static final long serialVersionUID = 1L;

	public static final String RESULT_FULL_FILENAME_PARAM = "fullFileName";
	
	@Override
	public void execute(WorkflowContext workflowContext) throws Exception {
		Log wfLog = workflowContext.getWorkflowLog();
		wfLog.info("Attach result to workflow...");
		
		String resultAlias = workflowContext.getVariableAsString(RESULT_FULL_FILENAME_PARAM);
		wfLog.info("Attempting to attach result with alias to workflow: " + resultAlias);
		if (resultAlias == null || resultAlias.isEmpty()) 
			return;
		
		ManagedObject resultMo = workflowContext.getManagedObjectService().getObjectByAlias(workflowContext.getAuthorizationService().getSystemUser(), resultAlias);
		if (resultMo == null) 
			return;
		wfLog.info("Result MO is " + resultMo.getDisplayName() + " [" + resultMo.getId() + "]");

		String[] rs = workflowContext.getRSuiteContents();
		String rsString = StringUtils.join(rs, ",");
		String updatedRs = rsString + "," + resultMo.getId();
		workflowContext.setVariable("rsuite contents", updatedRs);
		
	}
}