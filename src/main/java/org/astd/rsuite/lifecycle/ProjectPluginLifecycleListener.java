package org.astd.rsuite.lifecycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.service.ServiceUtils;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.extensions.Plugin;
import com.reallysi.rsuite.api.extensions.PluginLifecycleListener;

/**
 * RSuite extension point when RSuite loads or unloads the plugin.
 * 
 * <p>
 * Upon load, this life cycle listener performs the following duties:
 * <ol>
 * <li>Initialize {@link ProjectMessageResource}</li>
 * <li>Initialize all custom RSuite services</li>
 * </ol>
 * 
 * <p>
 * Upon unload, this life cycle listener performs the following duties:
 * <ol>
 * <li>Stop all custom RSuite services.</li>
 * </ol>
 * 
 * <p>
 * <i>^Carried over from the 2014 code base. TBD if this code is operational and desired.</i>
 */
public class ProjectPluginLifecycleListener
    implements PluginLifecycleListener {

  private static Log log = LogFactory.getLog(ProjectPluginLifecycleListener.class);

  /*
   * (non-Javadoc)
   * 
   * @see com.reallysi.rsuite.api.extensions.PluginLifecycleListener#start(com.reallysi.rsuite.api.
   * extensions .ExecutionContext, com.reallysi.rsuite.api.extensions.Plugin)
   */
  @Override
  public void start(ExecutionContext context, Plugin plugin) {
    log.info("RSuite is loading the " + plugin.getId() + " plugin");

    ProjectMessageResource.setExecutionContext(context);

    try {
      ServiceUtils.startAll(context, plugin);
    } catch (Exception e) {
      log.fatal(ProjectMessageResource.getMessageText("service.fatal.unable.to.initialize.all", e
          .getMessage()), e);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.reallysi.rsuite.api.extensions.PluginLifecycleListener#stop(com.reallysi.rsuite.api.
   * extensions .ExecutionContext, com.reallysi.rsuite.api.extensions.Plugin)
   */
  @Override
  public void stop(ExecutionContext context, Plugin plugin) {
    log.info("RSuite is unloading the " + plugin.getId() + " plugin");

    try {
      ServiceUtils.stopAll(context, plugin);
    } catch (RSuiteException e) {
      log.error(ProjectMessageResource.getMessageText("service.warn.unable.to.stop.services", e
          .getMessage()), e);
    }

  }

}
