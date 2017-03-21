package org.astd.rsuite.webservices;

import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.constants.RSuiteNameAndPathConstants;
import org.astd.rsuite.constants.WebServiceConstants;
import org.astd.rsuite.operation.options.OperationOptions;
import org.astd.rsuite.operation.result.FileOperationResult;
import org.astd.rsuite.service.ServiceUtils;
import org.astd.rsuite.utils.CallArgumentUtils;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;

/**
 * Expose means to test a transform, providing the test service is enabled.
 * <p>
 * This web service expects two parameters:
 * <ul>
 * <li>{@value org.astd.rsuite.constants.WebServiceConstants#WS_PARAM_NAME_FILE_TO_TRANSFORM} :
 * The file to transform.</li>
 * <li>{@value org.astd.rsuite.constants.WebServiceConstants#WS_PARAM_NAME_XSL_URI}: The URI of
 * the XSL to apply.</li>
 * </ul>
 * <p>
 * When there are no errors, the web service response will be the transformation result. Else, an
 * error message will be included.
 */
public class TestTransformWebService
    extends BaseWebService
    implements WebServiceConstants, RSuiteNameAndPathConstants {

  private static Log log = LogFactory.getLog(TestTransformWebService.class);

  @Override
  public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args)
      throws RSuiteException {
    try {
      if (log.isDebugEnabled()) {
        CallArgumentUtils.logArguments(args, log);
      }

      Session session = context.getSession();
      User user = session.getUser();

      // Require exactly one file item.
      String paramName = WS_PARAM_NAME_FILE_TO_TRANSFORM;
      List<FileItem> files = args.getFiles(paramName);
      if (files == null || files.size() == 0) {
        return getErrorResult(ProjectMessageResource.getMessageText(
            "web.service.error.required.parameter.value.not.specified", paramName));
      } else if (files.size() > 1) {
        return getErrorResult(ProjectMessageResource.getMessageText(
            "web.service.error.too.many.parameter.values", paramName));
      }

      // Require an XSL URL
      paramName = WS_PARAM_NAME_XSL_URI;
      String xslUri = args.getFirstString(paramName);
      if (StringUtils.isBlank(xslUri)) {
        return getErrorResult(ProjectMessageResource.getMessageText(
            "web.service.error.required.parameter.value.not.specified", paramName));
      }

      FileOperationResult opResult = ServiceUtils.getTestingService().executeTransform(session,
          files.get(0), xslUri, new OperationOptions(log));

      // Serve up the response
      return getWebServiceResponse(context, user, opResult);

    } catch (Exception e) {
      log.warn("Unable to complete request", e);
      return getErrorResult(ProjectMessageResource.getMessageText(
          "web.service.error.unable.to.complete", e.getMessage()));
    }
  }
}
