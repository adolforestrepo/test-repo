package org.astd.rsuite.webservices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.constants.RSuiteNameAndPathConstants;
import org.astd.rsuite.constants.WebServiceConstants;
import org.astd.rsuite.utils.CallArgumentUtils;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageDialogResult;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowInstance;

/**
 * A sharp knife that lists all the workflow instances.
 */
public class ListAllWorkflowsWebService
    extends BaseWebService
    implements WebServiceConstants, RSuiteNameAndPathConstants {

  private static Log log = LogFactory.getLog(ListAllWorkflowsWebService.class);

  @Override
  public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args)
      throws RSuiteException {
    try {
      if (log.isDebugEnabled()) {
        CallArgumentUtils.logArguments(args, log);
      }

      // Unlike KillAllWorkflowsWebService, this one doesn't use a portion of the RSuite API that
      // requires the user be an admin; yet, only admins should have access to this. Make it so.
      if (!context.getAuthorizationService().isAdministrator(context.getSession().getUser())) {
        throw new RSuiteException(RSuiteException.ERROR_PERMISSION_DENIED, ProjectMessageResource
            .getMessageText("security.error.must.be.admin"));
      }

      List<WorkflowInstance> wfIs = context.getWorkflowInstanceService()
          .getActiveWorkflowInstances();

      StringBuilder allWorkflows = new StringBuilder("");

      String skey = context.getSession().getKey();

      List<String> links = new ArrayList<String>();

      for (WorkflowInstance wfI : wfIs) {
        String wfIid = wfI.getId();
        links.add("<br />[" + wfI.getWorkflowDefinitionKey()
            + "] id [<a href='/rsuite/rest/v2/workflow/instance/id/" + wfIid + "/log?skey=" + skey
            + "' target='_blank'>" + wfIid + "</a>]");
      }

      Collections.reverse(links);

      for (String curLink : links) {
        allWorkflows.append(curLink);
      }

      MessageDialogResult msgResult = new MessageDialogResult("Workflow List", allWorkflows
          .toString());
      return msgResult;

    } catch (Exception e) {
      log.warn("Unable to complete request", e);

      MessageDialogResult msgResult = new MessageDialogResult("Error!", "Error listing workflows ["
          + e.getMessage());

      return msgResult;
    }
  }

}
