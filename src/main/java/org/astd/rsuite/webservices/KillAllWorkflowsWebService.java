package org.astd.rsuite.webservices;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.constants.RSuiteNameAndPathConstants;
import org.astd.rsuite.constants.WebServiceConstants;
import org.astd.rsuite.operation.result.OperationResult;
import org.astd.rsuite.utils.CallArgumentUtils;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowInstance;
import com.reallysi.rsuite.service.WorkflowInstanceService;

/**
 * A sharp knife that kills all workflows. Use with care.
 */
public class KillAllWorkflowsWebService
    extends BaseWebService
    implements WebServiceConstants, RSuiteNameAndPathConstants {

  private static Log log = LogFactory.getLog(KillAllWorkflowsWebService.class);

  @Override
  public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args)
      throws RSuiteException {
    try {
      if (log.isDebugEnabled()) {
        CallArgumentUtils.logArguments(args, log);
      }

      User user = context.getSession().getUser();

      // Unlike KillAllWorkflowsWebService, this one doesn't use a portion of the RSuite API that
      // requires the user be an admin; yet, only admins should have access to this. Make it so.
      if (!context.getAuthorizationService().isAdministrator(user)) {
        throw new RSuiteException(RSuiteException.ERROR_PERMISSION_DENIED, ProjectMessageResource
            .getMessageText("security.error.must.be.admin"));
      }

      OperationResult opResult = new OperationResult(context.getIDGenerator().allocateId(),
          "delete", log);
      opResult.markStartOfOperation();

      opResult.addInfoMessage(ProjectMessageResource.getMessageText(
          "info.received.request.to.kill.all.workflows", user.getUserId()));

      WorkflowInstanceService wfiService = context.getWorkflowInstanceService();

      List<WorkflowInstance> wfIs = context.getWorkflowInstanceService()
          .getActiveWorkflowInstances();

      int workflowsKilled = 0;
      int workflowsFailedToKill = 0;

      for (WorkflowInstance wfI : wfIs) {
        try {
          wfiService.killWorkflowInstance(user, wfI.getId());
          workflowsKilled = workflowsKilled + 1;
        } catch (RSuiteException e) {
          workflowsFailedToKill = workflowsFailedToKill + 1;
          throw e;
        }
      }
      opResult.markEndOfOperation();

      // Serve up the response. Given our present use of this, electing to only refresh the root
      // container, and its children.
      List<String> refreshUponSuccessIds = new ArrayList<String>();
      refreshUponSuccessIds.add(context.getContentAssemblyService().getRootFolder(user).getId());
      return getWebServiceResponse(context, user, opResult, refreshUponSuccessIds, true, null,
          "info.processed.request.to.kill.all.workflows", workflowsKilled, workflowsFailedToKill,
          opResult.getOperationDurationInSecondsQuietly());
    } catch (Exception e) {
      log.warn("Unable to complete request", e);
      return getErrorResult(ProjectMessageResource.getMessageText(
          "web.service.error.unable.to.complete", e.getMessage()));
    }
  }

}
