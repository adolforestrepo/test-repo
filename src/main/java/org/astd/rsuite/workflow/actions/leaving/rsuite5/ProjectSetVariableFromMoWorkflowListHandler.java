package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;

public class ProjectSetVariableFromMoWorkflowListHandler extends BaseWorkflowAction
implements TempWorkflowConstants {
	
	public static final String TARGET_VARIABLE_NAME_PARAMETER = "targetVariableName";
	static Log log = LogFactory.getLog(ProjectSetVariableFromMoWorkflowListHandler.class);
	
	/**
	 * Workflow log
	 */
	protected Log wfLog;

	@Override
	public void execute(WorkflowContext context) throws Exception {
		
		wfLog = context.getWorkflowLog();
	    logActionHandlerParameters(wfLog);
	    
	    String targetVariableName = getParameter("targetVariableName");
	    
	    if (StringUtils.isBlank(targetVariableName)) {
	      throw new RuntimeException("No value for required parameter 'targetVariableName'");
	    }
	    MoListWorkflowObject moList = context.getMoListWorkflowObject();
	    StringBuilder buf = new StringBuilder();
	    int i = 0;
	    for (MoWorkflowObject mo : moList.getMoList()) {
	      if (i++ > 0) buf.append(",");
	      buf.append(mo.getMoid());
	    }
	    context.setVariable(targetVariableName, buf.toString());
		
	}
	
	public void setTargetVariableName(String targetVariableName) {
	    setParameter("targetVariableName", targetVariableName);
	}

}
