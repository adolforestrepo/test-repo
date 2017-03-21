package org.astd.rsuite.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.operation.options.OperationOptions;
import org.astd.rsuite.operation.result.OperationStatusOperationResult;
import org.astd.rsuite.operation.status.OperationStatus;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.extensions.Plugin;

/**
 * The base class for all custom RSuite services.
 */
public abstract class BaseCustomRSuiteService
    implements CustomRSuiteService {

  /**
   * Server log.
   */
  private final static Log serverLog = LogFactory.getLog(BaseCustomRSuiteService.class);

  /**
   * An instance of <code>ExecutionContext</code> the service is allowed to use.
   */
  protected ExecutionContext context;

  /**
   * The plugin associated with this instance of the service.
   */
  protected Plugin plugin;

  /**
   * The name of the service.
   */
  protected String serviceName;

  /**
   * Flag indicating if the service is available, and may be used. The sub-class is responsible for
   * setting this to true.
   */
  protected boolean serviceAvailable = false;

  /**
   * An instance of <code>Log</code> the service may default to. See
   * {@link #getLog(OperationOptions)}.
   */
  protected Log defaultLog;

  public BaseCustomRSuiteService(
      ExecutionContext context, Plugin plugin, String serviceName, Log defaultLog) {
    this.context = context;
    this.serviceName = serviceName;
    this.defaultLog = defaultLog;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ama_assn.rsuite.service.CustomRSuiteService#stop(com.reallysi.rsuite.api.extensions.
   * ExecutionContext, com.reallysi.rsuite.api.extensions.Plugin)
   */
  @Override
  public void stop(ExecutionContext context, Plugin plugin) throws RSuiteException {

    // Mark service as unavailable.
    serviceAvailable = false;

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ama_assn.rsuite.service.CustomRSuiteService#isServiceAvailable()
   */
  @Override
  public boolean isServiceAvailable() {
    return serviceAvailable;
  }

  /**
   * Throw an exception if the service is not available.
   * 
   * @throws RSuiteException Thrown if service is unavailable.
   */
  public void throwIfServiceUnavailable() throws RSuiteException {
    if (!isServiceAvailable()) {
      throw new RSuiteException(RSuiteException.ERROR_INTERNAL_ERROR, ProjectMessageResource
          .getMessageText("service.error.unavailable", serviceName));
    }
  }

  /**
   * Get an instance of a log known not to be null.
   * 
   * @param options
   * @return Non-null instance of <code>Log</code>
   */
  protected Log getLog(OperationOptions options) {
    return (options == null || options.getLog() == null) ? defaultLog : options.getLog();
  }

  /**
   * Ensure the given exception is an <code>RSuiteException</code>.
   * 
   * @param ex
   * @param messagePropertyName The name of the message property to use for the wrapper
   *        <code>RSuiteException</code> when the given exception is not an
   *        <code>RSuiteException</code>.
   * @return An <code>RSuiteException</code>
   */
  protected RSuiteException getRSuiteException(Exception ex, String messagePropertyName) {
    if (ex instanceof RSuiteException) {
      return (RSuiteException) ex;
    } else {
      return new RSuiteException(RSuiteException.ERROR_INTERNAL_ERROR, ProjectMessageResource
          .getMessageText(messagePropertyName, ex.getMessage()), ex);
    }
  }

  /**
   * Record an RSuiteException as a failure, accounting for a possible null
   * <code>OperationResult</code>.
   * <p>
   * IMPROVE: Offer a signature that can identify the specific project that encountered the
   * exception such that its display name, ID, etc. may be incorporated into the message.
   * 
   * @param ex
   * @param messagePropertyName The name of the message property to use for the wrapper
   *        <code>RSuiteException</code> when the given exception is not an
   *        <code>RSuiteException</code>.
   * @param result
   * @param incrementFailureCounter
   */
  protected void recordExceptionAsFailure(Exception ex, String messagePropertyName,
      OperationStatusOperationResult result, boolean incrementFailureCounter) {
    if (result == null) {
      serverLog.error(ex.getMessage(), ex);
    } else {
      result.addFailure(getRSuiteException(ex, messagePropertyName));
      if (incrementFailureCounter)
        result.incrementCount(OperationStatus.FAILED);
    }
  }

  /**
   * Record an RSuiteException as a warning, accounting for a possible null
   * <code>OperationResult</code>.
   * <p>
   * IMPROVE: Offer a signature that can identify the specific project that encountered the
   * exception such that its display name, ID, etc. may be incorporated into the message.
   * 
   * @param ex
   * @param messagePropertyName The name of the message property to use for the wrapper
   *        <code>RSuiteException</code> when the given exception is not an
   *        <code>RSuiteException</code>.
   * @param result
   * @param incrementFailureCounter
   */
  protected void recordExceptionAsWarning(Exception ex, String messagePropertyName,
      OperationStatusOperationResult result, boolean incrementFailureCounter) {
    if (result == null) {
      serverLog.warn(ex.getMessage(), ex);
    } else {
      result.addWarning(getRSuiteException(ex, messagePropertyName));
      if (incrementFailureCounter)
        result.incrementCount(OperationStatus.FAILED);
    }
  }

}
