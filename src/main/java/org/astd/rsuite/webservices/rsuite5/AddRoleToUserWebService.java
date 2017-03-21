package org.astd.rsuite.webservices.rsuite5;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.webservices.BaseWebService;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.PlainTextResult;

/**
 * Grant a role to a user, both of which are parameters. Only exists as a workaround for
 * addUserToRole() not working in RSuite 5.0.5's Groovy API (RCS-5280).
 */
public class AddRoleToUserWebService
    extends BaseWebService {

  private static Log log = LogFactory.getLog(AddRoleToUserWebService.class);

  @Override
  public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args)
      throws RSuiteException {

    // User must be an administrator.
    User user = context.getSession().getUser();
    if (!context.getAuthorizationService().isAdministrator(user)) {
      return getErrorResult(ProjectMessageResource.getMessageText("security.error.must.be.admin"));
    }

    String paramName = "receivingUserId";
    String receivingUserId = args.getFirstString(paramName);
    if (StringUtils.isBlank(receivingUserId)) {
      return getErrorResult(ProjectMessageResource.getMessageText(
          "web.service.error.required.parameter.value.not.specified", paramName));
    }

    paramName = "roleToGrant";
    String roleToGrant = args.getFirstString(paramName);
    if (StringUtils.isBlank(receivingUserId)) {
      return getErrorResult(ProjectMessageResource.getMessageText(
          "web.service.error.required.parameter.value.not.specified", paramName));
    }

    log.info(ProjectMessageResource.getMessageText("security.info.granting.role", user.getUserId(),
        roleToGrant, receivingUserId));
    // Lower case the user ID as we're interacting with a case-sensitive role manager.
    context.getAuthorizationService().getRoleManager().addUserToRole(user, roleToGrant,
        receivingUserId.toLowerCase());
    return new PlainTextResult("<ok/>");

  }

}
