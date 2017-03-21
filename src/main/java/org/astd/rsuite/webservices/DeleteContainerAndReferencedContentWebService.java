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
import org.astd.rsuite.utils.ContainerUtils;
import org.astd.rsuite.utils.MOUtils;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;

/**
 * A sharp knife that deletes the specified container and all of the content it references. Use with
 * care.
 */
public class DeleteContainerAndReferencedContentWebService
    extends BaseWebService
    implements WebServiceConstants, RSuiteNameAndPathConstants {

  private static Log log = LogFactory.getLog(DeleteContainerAndReferencedContentWebService.class);

  @Override
  public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args)
      throws RSuiteException {
    try {
      if (log.isDebugEnabled()) {
        CallArgumentUtils.logArguments(args, log);
      }

      User user = context.getSession().getUser();

      OperationResult opResult = new OperationResult(context.getIDGenerator().allocateId(),
          "delete", log);
      opResult.markStartOfOperation();

      List<ManagedObject> moList = args.getManagedObjects(user);
      opResult.addInfoMessage(ProjectMessageResource.getMessageText(
          "info.received.request.to.delete.containers", user.getUserId(), moList.size()));

      ContentAssemblyNodeContainer container = null;
      ManagedObject mo;
      for (ManagedObject moRef : moList) {
        mo = RSuiteUtils.getRealMo(context, user, moRef);
        if (mo.isAssemblyNode()) {
          container = context.getContentAssemblyService().getContentAssemblyNodeContainer(user, mo
              .getId());
          opResult.addSubResult(ContainerUtils.deleteContainerAndReferencedContent(context, user,
              container, opResult.getLog()));
        } else {
          opResult.addWarning(new RSuiteException(RSuiteException.ERROR_PARAM_INVALID,
              ProjectMessageResource.getMessageText("warn.skipped.non.container", MOUtils
                  .getDisplayNameQuietly(mo), mo.getId())));
        }

      }
      opResult.markEndOfOperation();

      // Serve up the response. Given our present use of this, electing to only refresh the root
      // container, and its children.
      List<String> refreshUponSuccessIds = new ArrayList<String>();
      refreshUponSuccessIds.add(context.getContentAssemblyService().getRootFolder(user).getId());
      return getWebServiceResponse(context, user, opResult, refreshUponSuccessIds, true, null,
          "info.processed.request.to.delete.container.and.its.contents", moList.size() + opResult
              .getDestroyedContentAssemblies().size(), opResult.getDestroyedManagedObjects().size(),
          opResult.getOperationDurationInSecondsQuietly());
    } catch (Exception e) {
      log.warn("Unable to complete request", e);
      return getErrorResult(ProjectMessageResource.getMessageText(
          "web.service.error.unable.to.complete", e.getMessage()));
    }
  }

}
