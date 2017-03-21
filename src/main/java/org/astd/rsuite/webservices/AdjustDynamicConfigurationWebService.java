package org.astd.rsuite.webservices;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.constants.RSuiteNameAndPathConstants;
import org.astd.rsuite.constants.WebServiceConstants;
import org.astd.rsuite.utils.CallArgumentUtils;
import org.astd.rsuite.utils.UserUtils;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.rsicms.rsuite.dynamicconfiguration.utils.DynamicConfigurationUtils;

/**
 * Set one or more configuration values. These values are set and retrieved differently than
 * traditional RSuite configuration properties. Values are available to future RSuite processes, and
 * may also be changed while RSuite is running. That's the main advantage of this feature. Values
 * may be set or retrieved via
 * {@link com.rsicms.rsuite.dynamicconfiguration.utils.DynamicConfigurationUtils}.
 * <p>
 * This web service expects one parameter:
 * <ul>
 * <li>
 * {@value com.rsicms.rsuite.dynamicconfiguration.utils.DynamicConfigurationUtils#FORM_PARAM_NAME_DYNAMIC_SETTINGS_PREFIXES}
 * : A comma-separated list of configuration name prefixes this web service is to attempt to set the
 * values of.</li>
 * </ul>
 * <p>
 * When there are no errors, the web service response will be a successful message. Else, an error
 * message will be included.
 */
public class AdjustDynamicConfigurationWebService
    extends BaseWebService
    implements WebServiceConstants, RSuiteNameAndPathConstants {

  /**
   * Log this class is to use.
   */
  private static Log log = LogFactory.getLog(AdjustDynamicConfigurationWebService.class);

  @Override
  public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args)
      throws RSuiteException {
    try {
      if (log.isDebugEnabled()) {
        CallArgumentUtils.logArguments(args, log);
      }

      Session session = context.getSession();
      User user = session.getUser();

      // Require a dynamic settings prefix
      String prefixes = args.getFirstString(
          DynamicConfigurationUtils.FORM_PARAM_NAME_DYNAMIC_SETTINGS_PREFIXES);
      if (prefixes == null) {
        return getErrorResult(ProjectMessageResource.getMessageText(
            "web.service.error.required.parameter.value.not.specified",
            DynamicConfigurationUtils.FORM_PARAM_NAME_DYNAMIC_SETTINGS_PREFIXES));
      }

      try {
        log.info(ProjectMessageResource.getMessageText(
            "adjust.dynamic.configuration.info.received.request", user.getUserId()));

        // Require user is an administrator
        if (!context.getAuthorizationService().isAdministrator(user)) {
          throw new RSuiteException(RSuiteException.ERROR_PERMISSION_DENIED, ProjectMessageResource
              .getMessageText("security.error.must.be.admin"));
        }

        User dynamicConfigUser = UserUtils.getUser(context.getAuthorizationService(),
            DYNAMIC_CONFIGURATION_USERNAME);

        // For each prefix, get all of the name value pairs and set them
        for (String prefix : DynamicConfigurationUtils.getPrefixes(prefixes)) {
          List<CallArgument> prefixedArgs = BaseWebService.getArgumentsWithSameNamePrefix(args,
              prefix, false, false, true);

          Map<String, String> curSettingsForPrefix = DynamicConfigurationUtils.getSettings(context,
              dynamicConfigUser, prefix);

          if (curSettingsForPrefix != null) {

            for (Map.Entry<String, String> entry : curSettingsForPrefix.entrySet()) {
              // for each setting for the given prefix, if there is a defined NVP in call args
              // then ignore; otherwise, this means the user has cleared the setting
              // and we should get rid of the stored setting

              String settingName = entry.getKey();
              if (!prefixedArgs.contains(settingName)) {
                prefixedArgs.add(new CallArgument(settingName, ""));
              }
            }
          }

          DynamicConfigurationUtils.setNameValuePairs(context, dynamicConfigUser, prefixedArgs);

        }
        DynamicConfigurationUtils.logSettings(context, dynamicConfigUser);
      } catch (Exception e) {
        throw new RSuiteException(RSuiteException.ERROR_INTERNAL_ERROR, ProjectMessageResource
            .getMessageText("adjust.dynamic.configuration.error.unable.to.complete").concat(e
                .getMessage() == null ? StringUtils.EMPTY : ": ".concat(e.getMessage())));
      }

      return getNotificationResult(context, ProjectMessageResource.getMessageText(
          "adjust.dynamic.configuration.info.successful"), "Adjust Dynamic Configuration Result");

    } catch (Exception e) {
      log.warn("Unable to complete request", e);
      return getErrorResult(ProjectMessageResource.getMessageText(
          "web.service.error.unable.to.complete", e.getMessage()));
    }
  }
}
