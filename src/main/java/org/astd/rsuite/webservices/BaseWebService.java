package org.astd.rsuite.webservices;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.constants.ConfigurationPropertyConstants;
import org.astd.rsuite.constants.ProjectConstants;
import org.astd.rsuite.operation.result.FileOperationResult;
import org.astd.rsuite.operation.result.OperationResult;
import org.astd.rsuite.transform.CustomTransformer;
import org.astd.rsuite.utils.ConfigUtils;

import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.DefaultRemoteApiHandler;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageDialogResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageType;
import com.reallysi.rsuite.api.remoteapi.result.NotificationAction;
import com.reallysi.rsuite.api.remoteapi.result.RestResult;
import com.reallysi.rsuite.api.remoteapi.result.UserInterfaceAction;
import com.reallysi.rsuite.api.transformation.ManagedObjectTransformer;
import com.rsicms.rsuite.helpers.messages.ProcessMessage;

/**
 * The intended base web service for this project's custom REST web services.
 */
public abstract class BaseWebService
    extends DefaultRemoteApiHandler
    implements ProjectConstants, ConfigurationPropertyConstants {

  private static Log log = LogFactory.getLog(BaseWebService.class);

  /**
   * Get call arguments with names that begin with the specified prefix
   * 
   * @param args Call argument list to inspect.
   * @param prefix Prefix the call argument name must begin with, in order to qualify.
   * @param stripPrefix Submit true if the prefix is to be stripped from the names of returned call
   *        arguments.
   * @param trimValue Submit true if whitespace is to be trimmed from the value of qualifying call
   *        arguments.
   * @param keepBlankStrings Submit true if args whose values are the empty string should be
   *        retained
   * @return List of call arguments that qualify.
   */
  protected static List<CallArgument> getArgumentsWithSameNamePrefix(CallArgumentList args,
      String prefix, boolean stripPrefix, boolean trimValue, boolean keepBlankStrings) {
    List<CallArgument> withPrefix = new ArrayList<CallArgument>();
    if (args != null) {
      for (CallArgument arg : args.getAll()) {
        if (arg.getName().startsWith(prefix) && (keepBlankStrings ? true : StringUtils.isNotBlank(
            arg.getValue()))) {
          if (stripPrefix || trimValue) {
            withPrefix.add(new CallArgument(stripPrefix ? arg.getName().substring(prefix.length())
                : arg.getName(), trimValue ? arg.getValue().trim() : arg.getValue()));
          } else {
            withPrefix.add(arg);
          }
        }
      }
    }
    return withPrefix;
  }

  /**
   * Create a list of metadata items out of a list of call arguments.
   * 
   * @param args
   * @param prefix Optional prefix to qualify individual call arguments.
   * @param stripPrefix When true, the prefix will be stripped from the call argument name /
   *        metadata name.
   * @param trimValue When true, leading and following whitespace is stripped from the call argument
   *        value / metadata value.
   * @param keepBlankStrings Submit true if args whose values are the empty string should be
   *        retained
   * @return A list of metadata items from qualifying call arguments.
   */
  protected static List<MetaDataItem> getArgumentsWithSameNamePrefixAsMetadata(
      CallArgumentList args, String prefix, boolean stripPrefix, boolean trimValue,
      boolean keepBlankStrings) {
    List<MetaDataItem> metaDataItems = new ArrayList<MetaDataItem>();
    List<CallArgument> qualifyingArgs = getArgumentsWithSameNamePrefix(args, prefix, stripPrefix,
        trimValue, keepBlankStrings);
    if (qualifyingArgs != null) {
      for (CallArgument arg : qualifyingArgs) {
        metaDataItems.add(new MetaDataItem(arg.getName(), arg.getValue()));
      }
    }
    return metaDataItems;
  }

  /**
   * Get a 'browse from' URL for the specified ID of the desired object.
   * 
   * @param context
   * @param id
   * @return A 'browse from' URL for the specified object.
   */
  protected static String getBrowseFromUrl(ExecutionContext context, String id) {
    return new StringBuilder("/rsuite-cms/browseFrom/").append(id).toString();
  }

  /**
   * Get the duration of notifications, in seconds.
   * 
   * @param context
   * @return Duration of notifications, in seconds.
   */
  protected static int getNotificationDurationInSeconds(ExecutionContext context) {
    return ConfigUtils.getPropertyAsInt(context.getConfigurationProperties(),
        PROP_NAME_NOTIFICATION_DURATION_IN_SECONDS, DEFAULT_NOTIFICATION_DURATION_IN_SECONDS);
  }

  /**
   * Translate the operation result into a remote API result. This one is ideal for
   * <code>FileOperationResult</code> or when the subclass is overriding methods used by
   * getWebServiceResponse() in order to create a different <code>RemoteApiResult</code> than one
   * that incorporates the successMessage* parameters, and when it doesn't want to refresh any
   * objects in the CMS UI.
   * 
   * @param context
   * @param user
   * @param opResult
   * @return remote API result.
   */
  protected RemoteApiResult getWebServiceResponse(ExecutionContext context, User user,
      OperationResult opResult) {

    return getWebServiceResponse(context, user, opResult, new ArrayList<String>(), // No need to
                                                                                   // refresh RSuite
                                                                                   // object when
                                                                                   // downloading a
                                                                                   // file.
        false, // No need to refresh children thereof.
        null, // No additional UI action.
        null); // No message to display when downloading a file.

  }

  /**
   * Translate an operation result into a remote API result.
   * <p>
   * If there were failure or warnings, a message dialog box is presented, and no objects are
   * refreshed in the display. Else, a notification action is included, and objects may be refreshed
   * in the display.
   * 
   * @param context
   * @param user
   * @param opResult
   * @param refreshUponSuccessIds
   * @param refreshChildrenUponSuccess
   * @param successUserInterfaceAction
   * @param successMessagePropName
   * @param successMessageParams
   * @return remote API result.
   */
  protected RemoteApiResult getWebServiceResponse(ExecutionContext context, User user,
      OperationResult opResult, List<String> refreshUponSuccessIds,
      boolean refreshChildrenUponSuccess, UserInterfaceAction successUserInterfaceAction,
      String successMessagePropName, Object... successMessageParams) {

    /*
     * If the result is a file result and there were no errors, serve up the file.
     */
    RemoteApiResult fileDownloadResult = null;
    if (opResult instanceof FileOperationResult) {
      fileDownloadResult = ((FileOperationResult) opResult).getFileForDownload();
    }

    StringBuilder buf = new StringBuilder();
    boolean includeLessSevereMessages = true;
    beginMessage(buf, opResult);

    if (opResult.hasFailures()) {

      addFailureMessages(buf, includeLessSevereMessages, opResult);
      closeMessage(buf, opResult);

      return getErrorResult(buf.toString());

    } else if (opResult.hasWarnings()) {

      if (fileDownloadResult != null) {

        // Warnings should already be in the log.
        return fileDownloadResult;

      } else {

        addWarningMessages(buf, includeLessSevereMessages, opResult);
        closeMessage(buf, opResult);
        return getWarningResult(buf.toString());

      }
    } else {

      if (fileDownloadResult != null) {

        return fileDownloadResult;

      } else {

        closeMessage(buf, opResult);
        RestResult restResult = getInfoResult(context, user, buf.toString(), refreshUponSuccessIds,
            refreshChildrenUponSuccess, successMessagePropName, successMessageParams);
        if (successUserInterfaceAction != null) {
          restResult.addAction(successUserInterfaceAction);
        }
        return restResult;
      }

    }
  }

  /**
   * Adds the warning messages in the content message, conditionally, less severe messages.
   * 
   * @param buf the content message.
   * @param includeLessSevereMessages Submit true to include info-level messages.
   * @param opResult the Operation Result.
   */
  protected void addWarningMessages(StringBuilder buf, boolean includeLessSevereMessages,
      OperationResult opResult) {
    addMessages(buf, opResult.getWarningMessages(), "Warnings: ");
    if (includeLessSevereMessages) {
      addMessages(buf, opResult.getInfoMessages(), "Info: ");
    }
  }

  /**
   * Adds the failure messages in the content message, conditionally, less severe messages.
   * 
   * @param buf the content message.
   * @param includeLessSevereMessages Submit true to include warning- and info-level messages.
   * @param opResult the Operation Result.
   */
  protected void addFailureMessages(StringBuilder buf, boolean includeLessSevereMessages,
      OperationResult opResult) {
    addMessages(buf, opResult.getFailureMessages(), "Errors: ");
    if (includeLessSevereMessages) {
      addMessages(buf, opResult.getWarningMessages(), "Warnings: ");
      addMessages(buf, opResult.getInfoMessages(), "Info: ");
    }
  }

  /**
   * Adds a footer to the content message.
   * 
   * @param buf the content message.
   * @param opResult the Operation Result.
   */
  protected void closeMessage(StringBuilder buf, OperationResult opResult) {}

  /**
   * Adds a header to the content message.
   * 
   * @param buf the content message.
   * @param opResult the Operation Result.
   */
  protected void beginMessage(StringBuilder buf, OperationResult opResult) {}

  /**
   * Translate an operation result into a remote API result.
   * <p>
   * This signature doesn't enable the caller to control which objects are refreshed in the CMS UI.
   * <p>
   * If there were failure or warnings, a message dialog box is presented, and no objects are
   * refreshed in the display. Else, a notification action is included, and objects may be refreshed
   * in the display.
   * 
   * @param context
   * @param user
   * @param opResult
   * @param successUserInterfaceAction
   * @param successMessagePropName
   * @param successMessageParams
   * @return remote API result.
   */
  protected RemoteApiResult getWebServiceResponse(ExecutionContext context, User user,
      OperationResult opResult, UserInterfaceAction successUserInterfaceAction,
      String successMessagePropName, Object... successMessageParams) {

    return getWebServiceResponse(context, user, opResult, new ArrayList<String>(), false,
        successUserInterfaceAction, successMessagePropName, successMessageParams);
  }

  /**
   * Add list of messages to the string buffer, formatted with HTML.
   * 
   * @param buf
   * @param messages
   * @param messageType
   */
  protected static void addMessages(StringBuilder buf, List<? extends ProcessMessage> messages,
      String messageType) {
    if (messages != null && messages.size() > 0) {
      buf.append("<p><b>" + messageType + "</b></p>");
      buf.append("<ul>");
      for (ProcessMessage message : messages) {
        buf.append("<li>")
            // Message can be truncated when too long; label considered optional.
            // .append(message.getRelatedObjectLabel())
            // .append(": ")
            .append(message.getMessageText()).append("</li>");
      }
      buf.append("</ul>");
    }
  }

  /**
   * Get the web service's label, which should be suitable for users.
   * 
   * @return The remoteApiDefinition element's label attribute value
   */
  protected String getWebServiceLabel() {
    if (getRemoteApiDefinition() == null) {
      return StringUtils.EMPTY;
    }
    return getRemoteApiDefinition().getLabel();
  }

  /**
   * Get an info remote API result.
   * 
   * @param context
   * @param user
   * @param msg A message to include, incorporate, or ignore. It's up to the implementation.
   *        Subclasses may override. If ignored, the successMessage* parameters are expected to be
   *        used. This implementation ignores it, and creates a message using the successMessage*
   *        parameters.
   * @param refreshUponSuccessIds
   * @param refreshChildrenUponSuccess
   * @param successMessagePropName
   * @param successMessageParams
   * @return A remote API result appropriate when there were no warnings or failures.
   */
  protected RestResult getInfoResult(ExecutionContext context, User user, String msg,
      List<String> refreshUponSuccessIds, boolean refreshChildrenUponSuccess,
      String successMessagePropName, Object[] successMessageParams) {

    String msgText = ProjectMessageResource.getMessageText(successMessagePropName,
        successMessageParams);
    log.info(new StringBuilder(user.getUserId()).append(": ").append(msgText));

    RestResult webServiceResult = getNotificationResult(context, msgText, getWebServiceLabel());

    // Add refresh action
    if (refreshUponSuccessIds != null && refreshUponSuccessIds.size() > 0) {
      UserInterfaceAction action = new UserInterfaceAction("rsuite:refreshManagedObjects");
      action.addProperty("objects", StringUtils.join(refreshUponSuccessIds, ","));
      action.addProperty("children", refreshChildrenUponSuccess);
      webServiceResult.addAction(action);
    }

    return webServiceResult;
  }

  /**
   * Get a notification result, which honors a configurable duration and that the caller may add
   * additional actions to.
   * 
   * @param context
   * @param message
   * @param title
   * @return A <code>RestResult</code> that displays a notification in the CMS UI.
   */
  protected static RestResult getNotificationResult(ExecutionContext context, String message,
      String title) {
    RestResult webServiceResult = new RestResult();
    NotificationAction notification = new NotificationAction(message, title);
    notification.addProperty(NotificationAction.PROPERTY_DURATION, getNotificationDurationInSeconds(
        context));
    webServiceResult.addAction(notification);
    return webServiceResult;
  }

  /**
   * Common implementation to display a warning to the user, that requires them to dismiss it.
   * 
   * @param msg
   * @return message dialog result, of type error.
   */
  protected RemoteApiResult getWarningResult(String msg) {
    return new MessageDialogResult(MessageType.WARNING, getWebServiceLabel(), msg);
  }

  /**
   * Common implementation to display an error to the user, that requires them to dismiss it.
   * 
   * @param msg
   * @return message dialog result, of type error.
   */
  protected RemoteApiResult getErrorResult(String msg) {
    return new MessageDialogResult(MessageType.ERROR, getWebServiceLabel(), msg);
  }
  
  /**
   * Returns an instance of a custom transformer.
   * This method can be overwritten by a subclass to inject a different implementation to transform the managed object.
   *  
   * @param context
   * @param xsltUri
   * @return ManagedObjectTransformer
   */
  protected ManagedObjectTransformer getTransformer(
      RemoteApiExecutionContext context,
      String xsltUri) {
    
    return new CustomTransformer(context, xsltUri,"result.xml","result");
  }

}
