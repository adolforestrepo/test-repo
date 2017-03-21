package org.astd.rsuite.service.impl;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.operation.options.OperationOptions;
import org.astd.rsuite.operation.result.FileOperationResult;
import org.astd.rsuite.service.BaseCustomRSuiteService;
import org.astd.rsuite.service.ServiceUtils;
import org.astd.rsuite.service.conf.TestingServiceConfiguration;
import org.astd.rsuite.utils.ProjectUtils;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.extensions.Plugin;
import com.rsicms.rsuite.utils.xml.TransformUtils;

/**
 * A service facilitating some automated testing.
 */
public class TestingService
    extends BaseCustomRSuiteService {

  /**
   * Server log.
   */
  private final static Log serverLog = LogFactory.getLog(TestingService.class);

  /**
   * Singleton instance of this custom service.
   */
  private static TestingService thisService;

  /**
   * The service's configuration.
   */
  private static TestingServiceConfiguration serviceConf;

  /**
   * Construct an instance of the service.
   * 
   * @param context
   * @param plugin
   * @throws RSuiteException
   */
  private TestingService(
      ExecutionContext context, Plugin plugin)
      throws RSuiteException {

    super(context, plugin, ServiceUtils.ServiceId.TESTING_SERVICE.getDisplayName(), serverLog);

    TestingService.serviceConf = new TestingServiceConfiguration(context);
    if (serviceConf.mayEnable()) {
      serviceAvailable = true;
    } else {
      serviceAvailable = false;
    }
    serverLog.info(ProjectMessageResource.getMessageText("testing.service.info.enabled.status",
        serviceAvailable ? "enabled" : "disabled"));

  }

  /**
   * Get the instance of this service.
   * 
   * @param context
   * @param plugin
   * @return The instance of this service
   * @throws RSuiteException
   */
  public static TestingService getInstance(ExecutionContext context, Plugin plugin)
      throws RSuiteException {
    if (thisService == null) {
      thisService = new TestingService(context, plugin);
    }
    return thisService;
  }

  /**
   * Test a transform
   * 
   * @param session
   * @param fileItem File to apply XSL to.
   * @param xslUri URI of XSL to apply.
   * @param options
   * @return The result of the operation. When successful, the caller may wish to access the
   *         transform result via {@link FileOperationResult#getFileForDownload()} or
   *         {@link FileOperationResult#getFileAsReport()}. The caller is responsible for checking
   *         the operation result for failures and warnings.
   * @throws RSuiteException
   */
  public FileOperationResult executeTransform(Session session, FileItem fileItem, String xslUri,
      OperationOptions options) throws RSuiteException {

    throwIfServiceUnavailable();
    FileOperationResult opResult = null;

    try {
      opResult = new FileOperationResult(context.getIDGenerator().allocateId(), "transform", getLog(
          options));
      opResult.markStartOfOperation();

      // Start-off message.
      User user = session.getUser();
      opResult.addInfoMessage(ProjectMessageResource.getMessageText(
          "test.transform.info.received.request", user.getUserId(), xslUri));

      // All this map to be passed in if a particular XSL requires parameters (beyond the standard
      // ones).
      Map<String, Object> xslParams = new HashMap<String, Object>();

      InputStream transformResult = new TransformUtils().iTransform(context, session, fileItem,
          context.getXmlApiManager().getTransformer(new URI(xslUri)), xslParams, true,
          new ProjectUtils(context.getRSuiteServerConfiguration()).getBaseRSuiteUrl());

      // Even if this is XHTML, going with an XML MIME type.
      opResult.prepareFileForDownload(transformResult, MIME_TYPE_XML, "transform-result.xml");

    } catch (Exception e) {
      opResult.addFailure(opResult.conditionallyWrapThrowable(e,
          RSuiteException.ERROR_INTERNAL_ERROR, "test.transform.error.unable.to.complete.request", e
              .getMessage()));
    } finally {
      if (opResult != null) {
        opResult.markEndOfOperation();
        opResult.addInfoMessage(ProjectMessageResource.getMessageText(
            "test.transform.info.completed.request", opResult
                .getOperationDurationInMillisecondsQuietly(), opResult.getFailureCount(), opResult
                    .getWarningCount()));
      }
    }

    return opResult;
  }

  @Override
  public void stop(ExecutionContext context, Plugin plugin) throws RSuiteException {

    super.stop(context, plugin);

  }
}
