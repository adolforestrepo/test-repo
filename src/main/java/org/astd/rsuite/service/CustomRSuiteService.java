package org.astd.rsuite.service;

import org.astd.rsuite.constants.ProjectConstants;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.extensions.Plugin;

/**
 * Interface for all custom RSuite services. Custom RSuite services are to extend
 * {@link BaseCustomRSuiteService}, which is an abstract implementation of this interface.
 */
public interface CustomRSuiteService
    extends ProjectConstants {

  /**
   * Find out if the service is available.
   * 
   * @return True if the service is available.
   */
  public boolean isServiceAvailable();

  /**
   * Stop the service
   * 
   * @param context
   * @param plugin
   * @throws RSuiteException
   */
  public void stop(ExecutionContext context, Plugin plugin) throws RSuiteException;

}
