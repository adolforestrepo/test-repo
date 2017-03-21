package org.astd.rsuite.service.conf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.constants.ConfigurationPropertyConstants;
import org.astd.rsuite.utils.ConfigUtils;

import com.reallysi.rsuite.api.ConfigurationProperties;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.extensions.ExecutionContext;

/**
 * Provides access to all testing service-related configuration. Only the testing service should use
 * this class.
 */
public class TestingServiceConfiguration
    implements ConfigurationPropertyConstants {

  /**
   * Class log
   */
  private static Log log = LogFactory.getLog(TestingServiceConfiguration.class);

  /**
   * Flag indicating if the service should be enabled.
   */
  private boolean enabled = false;

  /**
   * Construct an instance of the service's configuration.
   * 
   * @param context
   * @throws RSuiteException Thrown when one or more required configuration properties are not set.
   */
  public TestingServiceConfiguration(
      ExecutionContext context)
      throws RSuiteException {
    log.info(ProjectMessageResource.getMessageText("service.info.loading.conf"));
    ConfigurationProperties props = context.getConfigurationProperties();
    this.enabled = ConfigUtils.getPropertyAsBoolean(props, PROP_NAME_TESTING_SERVICE_ENABLED,
        DEFAULT_TESTING_SERVICE_ENABLED);
  }

  /**
   * Find out if the testing service may be enabled.
   * 
   * @return True if the service may be enabled; else, false.
   */
  public boolean mayEnable() {
    return enabled;
  }

}
