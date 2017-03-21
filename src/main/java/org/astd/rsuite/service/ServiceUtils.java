package org.astd.rsuite.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.service.impl.TestingService;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.extensions.Plugin;

/**
 * A collection of static methods to initialize and get custom services. The plugin lifecycle
 * listener is expected to call {@link #startAll(ExecutionContext, Plugin)}. Thereafter, any other
 * code may call one of the get service methods.
 * <p>
 * To add support for a new custom service, add an enum value to {@link ServiceId}, modify
 * {@link #startAll(ExecutionContext, Plugin)} to construct an instance thereof, retain in the map,
 * and add a getter. The service itself needs to implement {@link CustomRSuiteService}.
 */
public class ServiceUtils {

  /**
   * Class log
   */
  private static Log log = LogFactory.getLog(ServiceUtils.class);

  /**
   * Map of custom service IDs to custom service instances
   */
  private static Map<ServiceId, CustomRSuiteService> services;

  /**
   * All custom services implemented and known by this plugin.
   */
  public enum ServiceId {
    TESTING_SERVICE("Testing Service");

    private String displayName;

    private ServiceId(
        String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }

  /**
   * Start all custom services
   * 
   * @param plugin
   * @throws RSuiteException
   */
  public static void startAll(ExecutionContext context, Plugin plugin) throws RSuiteException {

    ServiceUtils.services = new HashMap<ServiceId, CustomRSuiteService>();

    ServiceId serviceId = ServiceId.TESTING_SERVICE;
    log.info(ProjectMessageResource.getMessageText("service.info.starting.service", serviceId
        .getDisplayName()));
    services.put(serviceId, TestingService.getInstance(context, plugin));
    log.info(ProjectMessageResource.getMessageText("service.info.service.started", serviceId
        .getDisplayName()));

  }

  /**
   * Stop all custom services
   * 
   * @param plugin
   * @throws RSuiteException
   */
  public static void stopAll(ExecutionContext context, Plugin plugin) throws RSuiteException {

    if (services != null) {
      for (Map.Entry<ServiceId, CustomRSuiteService> entry : services.entrySet()) {
        try {
          log.info(ProjectMessageResource.getMessageText("service.info.stopping.service", entry
              .getKey().getDisplayName()));
          entry.getValue().stop(context, plugin);
        } catch (Exception e) {
          log.warn(ProjectMessageResource.getMessageText("service.warn.unable.to.cleanly.stop",
              entry.getKey().getDisplayName()), e);
        }
      }
    } else {
      log.warn(ProjectMessageResource.getMessageText("service.warn.unable.to.stop.services"));
    }

  }

  public static TestingService getTestingService() {
    return (TestingService) services.get(ServiceId.TESTING_SERVICE);
  }

}
