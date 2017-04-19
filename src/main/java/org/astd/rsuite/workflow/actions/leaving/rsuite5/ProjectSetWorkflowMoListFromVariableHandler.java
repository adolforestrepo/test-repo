package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;

public class ProjectSetWorkflowMoListFromVariableHandler  extends BaseWorkflowAction
implements TempWorkflowConstants {

	public static final String MO_SPECIFYING_VARIABLE_NAME_PARAMETER = "moSpecifyingVariableName";
	
	protected Log wfLog;
	
	@Override
	public void execute(WorkflowContext context) throws Exception {
		wfLog = context.getWorkflowLog();
		String moSpecifyingVariableName = getParameter(MO_SPECIFYING_VARIABLE_NAME_PARAMETER);
		
		if (StringUtils.isBlank(moSpecifyingVariableName)) {
			throw new RuntimeException("No value for required parameter 'moSpecifyingVariableName'");
		}
		
		MoListWorkflowObject newMoList = new MoListWorkflowObject();
		Object moSpecifier = context.getVariable(moSpecifyingVariableName);
		if (moSpecifier == null) {
		      throw new RSuiteException("No value in workflow context for variable named \"" + moSpecifyingVariableName + "\". This reflects an error in the workflow process definition.");
		}
		MoWorkflowObject newWFO = null;
		
		if ((moSpecifier instanceof String)) {
			String[] moIDs = ((String)moSpecifier).split(",");
			for (int i = 0; i < moIDs.length; i++) {
				newWFO = new MoWorkflowObject(moIDs[i]);
		        newMoList.addMoObject(newWFO);
			}
		} else if ((moSpecifier instanceof MoWorkflowObject)) {
		      newWFO = (MoWorkflowObject)moSpecifier;
		      newMoList.addMoObject(newWFO);
		} else {
		      throw new RSuiteException("Unexpected object type for MO specifier variable. Expected String or MoWorkflowObject, got " + moSpecifier.getClass().getName());
		}
		
		context.setMoListWorkflowObject(newMoList);
		
	}
	
	public void setMoSpecifyingVariableName(String moSpecifyingVariableName) {
	    setParameter(MO_SPECIFYING_VARIABLE_NAME_PARAMETER, moSpecifyingVariableName);
	}
}
