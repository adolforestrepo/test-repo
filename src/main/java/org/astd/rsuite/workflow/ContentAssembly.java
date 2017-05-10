package org.astd.rsuite.workflow;

import java.util.HashMap;
import java.util.Map;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;

public class ContentAssembly  {
	
	 public static void setVariables(WorkflowContext context) throws RSuiteException {
		 Map<String, ContentAssemblyNodeContainer> caMap = new HashMap<String, ContentAssemblyNodeContainer>();	
			ContentAssemblyNodeContainer container = caMap.get(context);
		context.setVariable(CAInterface.WF_VAR_NAME_RSUITE_CONTENTS, container.getId());
		
	}
}
