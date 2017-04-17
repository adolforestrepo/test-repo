package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;

public class ProjectSetParentBrowseTreeNodeToVariableHandler 
extends BaseWorkflowAction
implements TempWorkflowConstants {
	
	public static final String RSUITE_BROWSE_URI_VARNAME = "rsuiteBrowseUri";
	public static final String VARIABLE_NAME_PARAM = "variableName";
	public static final String VARIABLE_NAME_VARNAME = "parentMoId";

	@Override
	public void execute(WorkflowContext context) throws Exception {
		
		Log wfLog = context.getWorkflowLog();
	    wfLog.info("Starting SetParentBrowseTreeNodeToVariable...");
	    
	    String varName = resolveVariablesAndExpressions(getParameterWithDefault("variableName", "parentMoId"));
	    String browseUri = context.getVariableAsString("rsuiteBrowseUri");
	    if ((browseUri == null) || ("".equals(browseUri.trim()))) {
	      reportAndThrowRSuiteException("Expected variable rsuiteBrowseUri not set. This is set automatically for workflows started by context menu items.");
	    }
	    
	    String parentMoId = null;
	    String[] pathTokens = browseUri.split("/");
	    for (int i = pathTokens.length - 1; i > 0; i--) {
	      String pathToken = pathTokens[i];
	      String nodeType = "folder";
	      if (pathToken.contains(":")) {
	        String[] parts = pathToken.split(":");
	        nodeType = parts[0];
	        if ((nodeType.equals("canode")) || (nodeType.equals("ca"))) {
	          parentMoId = parts[1];
	          break;
	        }
	      }
	    }
	    
	    if (parentMoId == null) {
	      reportAndThrowRSuiteException("Did not find a CA or CA Node in the browse path \"" + browseUri + "\"");
	    }
	    wfLog.info("Setting variable " + varName + " to value \"" + parentMoId + "\"");
	    context.setVariable(varName, parentMoId);
	    wfLog.info("SetParentBrowseTreeNodeToVariable: Done.");
		
	}
	
	public void setVariableName(String variableName) {
	    setParameter("variableName", variableName);
	}
}
