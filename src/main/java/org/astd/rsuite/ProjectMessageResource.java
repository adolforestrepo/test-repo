package org.astd.rsuite;

import java.util.Locale;

import org.astd.rsuite.constants.ProjectConstants;
import org.astd.rsuite.utils.PluginPropertyMessageResources;

import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.i18n.MessageResources;

/**
 * Provide access to messages that may appear in the UI and log files.
 * <p>
 * The resource path for the messages file is defined by
 * {@link ProjectConstants#PLUGIN_RESOURCE_PATH_MESSAGES_FILE}; if the static web service
 * definition, path, or file name changes, this class needs to be updated.
 * <p>
 * It is imperative {@link #setExecutionContext(ExecutionContext)} is called as the plugin is
 * loaded; until a valid execution context is provided, expect null pointer exceptions.
 */
public class ProjectMessageResource {

  /**
   * An instance to an execution context, which leads to the PluginAccessManager.
   */
  private static ExecutionContext context;

  /**
   * The messages loaded from the properties file.
   */
  private static MessageResources messageResources;

  /**
   * Set the execution context. This should be called when the plugin is loaded, before any code
   * attempts to resolve a message key.
   * 
   * @param context
   */
  public static void setExecutionContext(ExecutionContext context) {
    ProjectMessageResource.context = context;
  }

  /**
   * Get the message resources, loading the properties file when necessary.
   * 
   * @return message resources
   */
  private static MessageResources getMessageResources() {
    if (messageResources == null) {
      PluginPropertyMessageResources propResources = new PluginPropertyMessageResources(context,
          false);
      propResources.setConfig(ProjectConstants.PLUGIN_RESOURCE_PATH_MESSAGES_FILE);
      propResources.setDefaultLocale(Locale.getDefault());
      messageResources = propResources;
    }
    return messageResources;
  }

  public static String getMessageText(String key) {
    return getMessageResources().getMessageText(key);
  }

  public static String getMessageText(Locale locale, String key) {
    return getMessageResources().getMessageText(new java.util.Locale("en_US"), key);
  }

  public static String getMessageText(String key, Object... args) {
    return getMessageResources().getMessageText(key, args);
  }

  public static String getMessageText(Locale locale, String key, Object... args) {
    return getMessageResources().getMessageText(new java.util.Locale("en_US"), key, args);
  }
}
