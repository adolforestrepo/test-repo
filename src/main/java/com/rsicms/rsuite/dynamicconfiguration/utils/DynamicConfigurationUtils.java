package com.rsicms.rsuite.dynamicconfiguration.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.system.UserPropertiesCatalog;

/**
 * A collection of static utility methods and constants related to dynamic configuration
 */
public class DynamicConfigurationUtils {

  /**
   * Parameter name (form) that will specify the prefixes
   */
  public static final String FORM_PARAM_NAME_DYNAMIC_SETTINGS_PREFIXES =
      "dynamicSettingsPrefixNames";

  /**
   * Parameter name (form) that will specify the username
   */
  public static final String FORM_PARAM_NAME_DYNAMIC_CONFIGURATION_USERNAME =
      "dynamicConfigurationUsername";

  /**
   * Log this class is to use.
   */
  private static Log log = LogFactory.getLog(DynamicConfigurationUtils.class);

  /**
   * Get a setting's value if the provided name exists as a setting in the provider user's user
   * properties catalog.
   * 
   * @param context ExecutionContext
   * @param user User that holds the dynamic configuration setting
   * @param settingName The name of the setting
   */
  public static String getSettingValue(ExecutionContext context, User user, String settingName)
      throws RSuiteException {
    String value = null;

    if (user != null) {
      String username = user.getName();
      UserPropertiesCatalog upc = context.getUserService().getUserPropertiesCatalog();
      Map<String, String> settings = upc.getProperties(username);
      if (settings != null) {
        if (settings.containsKey(settingName)) {
          value = settings.get(settingName);
        } else {
          value = null;
        }
      }
    }

    return value;
  }

  /**
   * Add the name-value pair to the user properties catalog for given user unless value is blank,
   * then remove the property. Finally save the catalog.
   * 
   * @param context ExecutionContext
   * @param user User that holds the dynamic configuration setting
   * @param args List<CallArgument> of name value pairs to set
   */
  public static void setNameValuePairs(ExecutionContext context, User user, List<CallArgument> args)
      throws RSuiteException {
    UserPropertiesCatalog upc;
    if (user != null) {
      upc = context.getUserService().getUserPropertiesCatalog();
      for (CallArgument arg : args) {
        setNameValuePair(context, user, arg.getName(), arg.getValue(), upc);
      }
      upc.save();
    }
  }

  /**
   * Add the name-value pair to the user properties catalog for given user unless value is blank,
   * then remove the property
   * 
   * @param context ExecutionContext
   * @param user User that holds the dynamic configuration setting
   * @param name Name of the setting to set
   * @param value Value of the setting to set
   * @param upc UserPropertiesCatalog
   * @throws RSuiteException
   */
  private static void setNameValuePair(ExecutionContext context, User user, String name,
      String value, UserPropertiesCatalog upc) throws RSuiteException {
    if (user != null) {
      String username = user.getName();
      if (value != null && StringUtils.isNotBlank(value)) {
        upc.set(username, name, value);
      } else {
        // if blank then remove
        upc.removeProperty(username, name);
      }
    }
  }

  /**
   * Debug method for logging the settings for the provided user
   * 
   * @param context ExecutionContext
   * @param user User that holds the dynamic configuration setting
   */
  public static void logSettings(ExecutionContext context, User user) throws RSuiteException {
    if (user != null) {
      String username = user.getName();
      UserPropertiesCatalog upc = context.getUserService().getUserPropertiesCatalog();
      Map<String, String> curSettings = upc.getProperties(username);
      if (curSettings != null) {
        for (Map.Entry<String, String> entry : curSettings.entrySet()) {
          log.info(new StringBuilder("Dynamic Setting name: [").append(entry.getKey()).append(
              "] value: [").append(entry.getValue()).append("]").toString());
        }
      }
    }
  }

  /**
   * Get a map<String, String> of the settings (key value pairs) for a prefix
   * 
   * @param context ExecutionContext
   * @param user User that holds the dynamic configuration setting
   * @param prefix
   * @return Map<String, String> Map where name -> value of name-value pair
   */
  public static Map<String, String> getSettings(ExecutionContext context, User user, String prefix)
      throws RSuiteException {
    if (!prefix.endsWith(".")) {
      prefix = prefix.concat(".");
    }
    Map<String, String> prefixedSettings = new HashMap<String, String>();
    if (user != null) {
      String username = user.getName();
      UserPropertiesCatalog upc = context.getUserService().getUserPropertiesCatalog();
      Map<String, String> curSettings = upc.getProperties(username);
      if (curSettings != null) {
        for (Map.Entry<String, String> entry : curSettings.entrySet()) {
          if (entry.getKey().startsWith(prefix)) {
            prefixedSettings.put(entry.getKey(), entry.getValue());
          }
        }
      }
    }
    return prefixedSettings;
  }

  /**
   * Get a List<String> of prefixes from an expected comma delimited list
   * 
   * @param prefixes A string of prefixes, delimited by commas if more than one
   * @return List<String> of trimmed prefixes
   */
  public static List<String> getPrefixes(String prefixes) {
    List<String> trimmedPrefixes = new ArrayList<String>();
    for (String prefix : prefixes.split(",")) {
      trimmedPrefixes.add(prefix.trim());
    }
    return trimmedPrefixes;
  }
}
