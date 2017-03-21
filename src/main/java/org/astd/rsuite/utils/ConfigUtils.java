package org.astd.rsuite.utils;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;

import com.reallysi.rsuite.api.ConfigurationProperties;
import com.reallysi.rsuite.api.RSuiteException;

/**
 * Configuration-related utilities
 */
public class ConfigUtils {

  /**
   * Log warnings are written to.
   */
  private static Log log = LogFactory.getLog(ConfigUtils.class);

  /**
   * Defines if a configuration property is required by default.
   * <p>
   * The value is {@value #DEFAULT_PROP_IS_REQUIRED}.
   */
  public final static boolean DEFAULT_PROP_IS_REQUIRED = true;

  /**
   * Normalize a property name by a) making all letters lower case, replacing each space with one
   * period, and removing all characters that are not letters, periods, or digits.
   * 
   * @param rawPropertyName
   * @return Normalized property name.
   */
  public static String normalizePropertyName(String rawPropertyName) {
    if (StringUtils.isNotBlank(rawPropertyName)) {
      return rawPropertyName.toLowerCase().replaceAll(" ", ".").replaceAll("[^a-z0-9.]", "");
    }
    return rawPropertyName;
  }

  /**
   * Get a property value, applying {@link #DEFAULT_PROP_IS_REQUIRED}
   *
   * @param props
   * @param name
   * @return value of property
   * @throws RSuiteException
   */
  public static String getProperty(ConfigurationProperties props, String name)
      throws RSuiteException {
    return getProperty(props, name, DEFAULT_PROP_IS_REQUIRED);
  }

  /**
   * Get the value of a property, or when unset, the provided default value.
   * 
   * @param props
   * @param name
   * @param defaultValue
   * @return A property value or the default.
   * @throws RSuiteException Shouldn't be thrown by this method.
   */
  public static String getProperty(ConfigurationProperties props, String name, String defaultValue)
      throws RSuiteException {
    String value = getProperty(props, name, false);
    if (value == null)
      return defaultValue;
    return value;
  }

  /**
   * Get a property value.
   * 
   * @param props
   * @param name
   * @param required
   * @return value of property
   * @throws RSuiteException Thrown if the property value is required by not defined.
   */
  public static String getProperty(ConfigurationProperties props, String name, boolean required)
      throws RSuiteException {
    String value = StringUtils.trim(props.getProperty(name, null));

    if (value == null && required) {
      throw new RSuiteException(RSuiteException.ERROR_CONFIGURATION_PROBLEM, ProjectMessageResource
          .getMessageText("error.required.prop.not.set", name));
    }

    return value;
  }

  /**
   * Get a property value as a URI, applying {@link #DEFAULT_PROP_IS_REQUIRED}
   * 
   * @param name
   * @return Property value as a URI
   * @throws RSuiteException
   */
  public static URI getPropertyAsURI(ConfigurationProperties props, String name)
      throws RSuiteException {
    return getPropertyAsURI(props, name, DEFAULT_PROP_IS_REQUIRED);
  }

  /**
   * Get a property value as a URI
   * 
   * @param props
   * @param name
   * @param required
   * @return Property value as a URI
   * @throws RSuiteException
   */
  public static URI getPropertyAsURI(ConfigurationProperties props, String name, boolean required)
      throws RSuiteException {
    String val = getProperty(props, name, required);
    try {
      return new URI(val);
    } catch (Exception ex) {
      throw new RSuiteException(RSuiteException.ERROR_CONFIGURATION_PROBLEM, ProjectMessageResource
          .getMessageText("config.error.invalid.property.value", val, name), ex);
    }
  }

  /**
   * Get a property as an int.
   * 
   * @param props
   * @param name
   * @param defaultValue
   * @return property value
   * @throws RSuiteException
   */
  public static int getPropertyAsInt(ConfigurationProperties props, String name, int defaultValue) {
    try {
      String propValue = getProperty(props, name, false);
      if (StringUtils.isNotBlank(propValue)) {
        try {
          return Integer.parseInt(propValue);
        } catch (NumberFormatException e) {
          log.warn(ProjectMessageResource.getMessageText("config.warn.invalid.value.using.default",
              name, propValue, defaultValue), e);
        }
      }
    } catch (RSuiteException e) {
      // Ignore; we have a default value.
    }

    log.info(ProjectMessageResource.getMessageText("config.info.property.not.set.using.default",
        name, defaultValue));
    return defaultValue;
  }

  /**
   * Get a property value as a boolean, or when not set, the provided default.
   * 
   * @param props
   * @param name
   * @param defaultValue
   * @return A property value as a boolean, or the provided default.
   */
  public static boolean getPropertyAsBoolean(ConfigurationProperties props, String name,
      boolean defaultValue) {
    try {
      String value = getProperty(props, name, false);
      if (value == null)
        return defaultValue;
      return new Boolean(value).booleanValue();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * Get a delimited property value as a list of strings.
   * 
   * @param props
   * @param name Property name
   * @param delimiter Regex provided to String#split()
   * @param required Submit true if an exception is desired when the property is not set.
   * @param trim Submit true if you'd like whitespace trimmed from the individual values.
   * @return A list of values from a delimited property value. It may be an empty list (when
   *         property is not set and property is not required), but it will never be null.
   * @throws RSuiteException Throw if the property is not set and the property is required.
   */
  public static List<String> getPropertyAsStringList(ConfigurationProperties props, String name,
      String delimiter, boolean required, boolean trim) throws RSuiteException {
    List<String> list = new ArrayList<String>();
    String delimitedValue = getProperty(props, name, required);
    if (StringUtils.isNotEmpty(delimitedValue)) {
      String[] arr = delimitedValue.split(delimiter);
      if (arr != null) {
        for (String value : arr) {
          if (trim)
            value = value.trim();
          if (StringUtils.isNotEmpty(value)) {
            list.add(value);
          }
        }
      }
    }
    return list;
  }

  /**
   * Get the properties that begin with a prefix, optionally removing the prefix and swapping the
   * property values with keys. Property values are trimmed.
   * 
   * @param props All properties.
   * @param prefix The property name prefix.
   * @param removePrefix Submit true to remove the property name prefix.
   * @param valueBecomesKey Submit true to make the property values the keys, in the returned map,
   *        and the property names the map values.
   * @return Map of properties where the original property name begins with the specified prefix.
   *         Parameters control if prefix is removed from the property name, and if values are now
   *         the map's keys.
   */
  public static Map<String, String> getPropertiesWithPrefix(ConfigurationProperties props,
      String prefix, boolean removePrefix, boolean valueBecomesKey) {
    Map<String, String> propsOut = new HashMap<String, String>();
    for (Map.Entry<String, String> entry : props.getPropertiesWithPrefix(prefix).entrySet()) {
      String propName = removePrefix ? entry.getKey().substring(prefix.length()) : entry.getKey();
      String propValue = StringUtils.trim(entry.getValue());
      propsOut.put(valueBecomesKey ? propValue : propName, valueBecomesKey ? propName : propValue);
    }
    return propsOut;
  }

  /**
   * Find out if a property value contains the specified sub-value.
   * <p>
   * There is no default value for the property, the property value is split by a comma, whitespace
   * is trimmed before the comparison, and the comparison is case-insensitive.
   * 
   * @param props
   * @param propName
   * @param checkFor
   * @return true if delimited property value contains the specified value.
   * @throws RSuiteException
   */
  public static boolean doesDelimitedPropertyValueContain(ConfigurationProperties props,
      String propName, String checkFor) throws RSuiteException {
    return doesDelimitedPropertyValueContain(props, propName, null, ",", checkFor, true, false);
  }

  /**
   * Find out if a property value contains the specified sub-value.
   * <p>
   * This is intended to help with delimited property values, where the caller needs to know if it
   * contains a specific value.
   * 
   * @param props
   * @param propName
   * @param defaultPropValue
   * @param propValueDelimiter Delimiter to split the property value on.
   * @param checkFor The sub-value to check for
   * @param trimWhitespace May trim whitespace before comparison?
   * @param caseSensitive Require case-sensitive comparison?
   * @return true if checkFor is in the property value; else, false.
   * @throws RSuiteException
   */
  public static boolean doesDelimitedPropertyValueContain(ConfigurationProperties props,
      String propName, String defaultPropValue, String propValueDelimiter, String checkFor,
      boolean trimWhitespace, boolean caseSensitive) throws RSuiteException {

    String propValue = getProperty(props, propName, false);
    if (propValue == null && defaultPropValue != null) {
      propValue = defaultPropValue;
    }

    if (StringUtils.isNotBlank(propValue)) {
      if (trimWhitespace) {
        checkFor = checkFor.trim();
      }
      String[] values = propValue.split(propValueDelimiter);
      for (String value : values) {
        if (trimWhitespace) {
          value = value.trim();
        }
        if ((caseSensitive && value.equals(checkFor)) || (!caseSensitive && value.equalsIgnoreCase(
            checkFor))) {
          return true;
        }
      }
    }

    return false;
  }
}
