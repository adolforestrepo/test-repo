package com.reallysi.rsuite.workflow.actions;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.UserAgent;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;

/**
 * Class that will set up the information needed to be shown to the user when
 * the process fails.
 * 
 * @author RSI Content Solutions.
 */
public class GenerationFailSetUpAction extends BaseWorkflowAction {
	
	@Override
	public void execute(WorkflowContext workflowContext) throws Exception {
		String antReportId = workflowContext.getVariableAsString(DitaOTSupportConstants.ANT_REPORT_ID);
		String antReportFileName = workflowContext.getVariableAsString(DitaOTSupportConstants.ANT_REPORT_FILENAME);
		if (antReportFileName == null) {
			antReportFileName = "process log";
		}
		String transtype = workflowContext.getVariableAsString("transtype");
		String transMessage = "Performed transformation of type: " + transtype;
		
		StringBuffer fullTaskDescription = new StringBuffer();
		if (antReportId != null) {
			String messageText = transMessage + "<br />Output generation failed. ";
			String reportUrl = "/rsuite/rest/v1/report/generated/" + antReportId + "?skey=" + getSession(workflowContext);
			String target = antReportId;
			fullTaskDescription = buildReportDescription(antReportFileName, messageText, reportUrl, target);
		} else {
			String messageText = transMessage + "<br />Output generation failed before a report could be written. Try again or see your administrator.";
			fullTaskDescription.append(messageText);
		}
		
		workflowContext.setVariable(DitaOTSupportConstants.TASK_FULL_DESCRIPTION, fullTaskDescription.toString());
	}

	private StringBuffer buildReportDescription(String antReportFileName, String messageText, String reportUrl, String target) {
		StringBuffer fullTaskDescription = new StringBuffer();
		fullTaskDescription.append(messageText);
		fullTaskDescription.append("<br/>See report <a href='" + reportUrl + "');");
		fullTaskDescription.append("target='_" + target + "' ");
		fullTaskDescription.append("onClick=\"window.open('" + reportUrl + "',");
		fullTaskDescription.append("'_" + target + "','width=1000,height=600,scrollbars=yes')\">" + antReportFileName + "</a>.");
		return fullTaskDescription;
	}

	private String getSession(ExecutionContext context) throws RSuiteException {
		UserAgent userAgent = new UserAgent("rsuite-workflow-process");
		Session session = context.getSessionService().
				createSession("Realm", userAgent,
				              "http://" + context.getRSuiteServerConfiguration().getHostName() +
				              ":" + context.getRSuiteServerConfiguration().getPort() +
				              "/rsuite/rest/v1", context.getAuthorizationService().getSystemUser());
		String skey = session.getKey();
		return skey;
	}
}
