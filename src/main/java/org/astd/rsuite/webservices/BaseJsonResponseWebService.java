package org.astd.rsuite.webservices;

import java.io.StringWriter;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.operation.result.OperationResult;
import org.json.JSONWriter;

import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.PlainTextResult;
import com.rsicms.rsuite.helpers.messages.ProcessFailureMessage;
import com.rsicms.rsuite.helpers.messages.ProcessWarningMessage;

/**
 * Intended to help web services return JSON responses.
 */
public abstract class BaseJsonResponseWebService
    extends BaseWebService {

  protected RemoteApiResult getWebServiceResponse(ExecutionContext context, User user,
      OperationResult opResult) {
    if (opResult.hasFailures()) {
      return getErrorResult(opResult.getFailureMessages());
    } else if (opResult.hasWarnings()) {
      return getWarningResult(opResult.getWarningMessages());
    } else if (opResult.hasPayload()) {
      PlainTextResult result = new PlainTextResult(opResult.getPayload());
      result.setContentType(opResult.getPayloadContentType());
      return result;
    } else {
      return getErrorResult(ProjectMessageResource.getMessageText("web.service.no.payload"));
    }
  }

  protected RemoteApiResult getWarningResult(List<ProcessWarningMessage> warnings) {
    return getMessageResult("warning", warnings.get(warnings.size() - 1).getMessageText());
  }

  protected RemoteApiResult getErrorResult(List<ProcessFailureMessage> errors) {
    return getMessageResult("error", errors.get(errors.size() - 1).getMessageText());
  }

  @Override
  protected RemoteApiResult getErrorResult(String msg) {
    return getMessageResult("error", msg);
  }

  protected RemoteApiResult getMessageResult(String label, String msg) {
    PlainTextResult result = new PlainTextResult();
    StringWriter sw = new StringWriter();
    try {
      JSONWriter jw = new JSONWriter(sw);
      jw.object().key(label).value(msg).endObject();
      result.setContent(sw.toString());
      return result;
    } catch (Exception e) {
      result.setContent(msg);
    } finally {
      IOUtils.closeQuietly(sw);
    }
    return result;
  }
}
