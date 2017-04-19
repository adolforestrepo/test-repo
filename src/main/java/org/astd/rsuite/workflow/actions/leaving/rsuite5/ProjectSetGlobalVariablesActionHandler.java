package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import org.activiti.engine.delegate.Expression;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;

public class ProjectSetGlobalVariablesActionHandler
extends BaseWorkflowAction
implements TempWorkflowConstants {

	public static final String VALUES_PARAM = "values";
	public static final String VARIABLE_NAMES_PARAM = "variableNames";
	
	protected Expression values;
	protected Expression variableNames;
	
	@Override
	public void execute(WorkflowContext context) throws Exception {
		
		Log wfLog = context.getWorkflowLog();
	    wfLog.info("Starting Set Global Variables...");
	    try {
	      
	      String variableNamesParam = resolveExpression(variableNames);
	      String valuesParam = resolveExpression(values);
	      
	      if(StringUtils.isBlank(variableNamesParam) || StringUtils.isBlank(valuesParam)) {
				throw new RSuiteException("At least one of the parameters [" + variableNamesParam+" "+valuesParam+ "] must be specified");
			}
	      
	      String[] variableNames = variableNamesParam.split(";");
	      String[] values = valuesParam.split(";");
	      
	      if (variableNames.length != values.length) {
	        reportAndThrowRSuiteException("The parameters \"variableNames\" and \"values\" must have the same number of semicolon-separated values. Values as specified are \"" + variableNamesParam + "\" and \"" + valuesParam + "\"");
	      }
	      

	      wfLog.info("Setting " + variableNames.length + " variables...");
	      for (int i = 0; i < variableNames.length; i++) { 
	        String name = resolveVariables(variableNames[i]);
	        String value = resolveVariables(values[i]);
	        wfLog.info("[" + i + "] Setting " + name + " to \"" + value + "\"");
	        context.setVariable(name, value);
	      }
	      
	    }
	    catch (Exception e) {
	      wfLog.error(e.getClass().getSimpleName() + " setting global variables: " + e.getMessage(), e);
	      context.setVariable("EXCEPTION_OCCUR", Boolean.valueOf(true));
	      context.setVariable("EXCEPTION_TYPE", "EXCEPTION_TYPE_SYSTEM");
	    }
	    
	    wfLog.info("Set Global Variables done.");
	}
	
	  public void setVariableNames(String variableNames) {
		    setParameter("variableNames", variableNames);
	  }
		  
	  public void setValues(String values) {
		    setParameter("values", values);
	  }
}
