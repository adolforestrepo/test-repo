package com.reallysi.rsuite.workflow.actions;

import org.apache.commons.lang3.StringUtils;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.UserAgent;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;

/**
 * Class that will generate the Load failure report or the one when the process succeeded.
 * 
 * @author RSI Content Solutions.
 */
public class GenerateReportAction extends BaseWorkflowAction {

	@Override
	public void execute(WorkflowContext workflowContext) throws Exception {
		String exceptionOccur = workflowContext.getVariableAsString(DitaOTSupportConstants.EXCEPTION_OCCUR);
		
		String transtype = workflowContext.getVariableAsString("transtype");
		String transMessage = "Performed transformation of type: " + transtype;
		
		String antReportId = workflowContext.getVariableAsString(DitaOTSupportConstants.ANT_REPORT_ID);
		String antReportFileName = workflowContext.getVariableAsString(DitaOTSupportConstants.ANT_REPORT_FILENAME);
		StringBuffer fullTaskDescription = new StringBuffer();
		
		if (StringUtils.equalsIgnoreCase(exceptionOccur, "true")) {
			String messageText = transMessage + "<br />Attempt to load document failed, probably because it wasn't generated. ";
			String reportUrl = "/rsuite/rest/v1/report/generated/" + antReportId + "?skey=" + getSession(workflowContext);
			String target = antReportId;
			fullTaskDescription = buildReportDescription(antReportFileName, messageText, reportUrl, target);
		} else if (antReportId == null) {
			fullTaskDescription.append("Process completed without error but no report is available. This probably means there was no output for the submitted content.");
		} else {
			String outputDir = workflowContext.getVariableAsString(DitaOTSupportConstants.OUTPUT_PATH_PARAM);
			String messageText = transMessage + "<br />See task attachments for output (or " + outputDir + "). ";
			String reportUrl = "/rsuite/rest/v1/report/generated/" + antReportId + "?skey=" + getSession(workflowContext);
			String target = antReportId;
			fullTaskDescription = buildReportDescription(antReportFileName, messageText, reportUrl, target);
		}
		workflowContext.setVariable(DitaOTSupportConstants.TASK_FULL_DESCRIPTION, fullTaskDescription.toString());
	}

	private StringBuffer buildReportDescription(String antReportFileName, String messageText, String reportUrl, String target) {
		StringBuffer fullTaskDescription = new StringBuffer();
		fullTaskDescription.append(messageText);
		fullTaskDescription.append("<br />See report <a href='" + reportUrl + "');");
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