package org.astd.rsuite.utils;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.reallysi.rsuite.api.MetaDataItem;

/**
 * A collection of static metadata utility methods.
 */
public class MetadataUtils {

  /**
   * Get the value of the first metadata item that has the specified name.
   * 
   * @param mdList
   * @param name LMD name
   * @return The value of the first metadata item that has the specified name, or null when there
   *         none of the given metadata items qualify.
   */
  public static String getMetadataValue(List<MetaDataItem> mdList, String name) {
    if (mdList != null && StringUtils.isNotBlank(name)) {
      for (MetaDataItem item : mdList) {
        if (item.getName().equalsIgnoreCase(name)) {
          return item.getValue();
        }
      }
    }
    return null;
  }

  /**
   * Find out if any of the candidate match the value.
   * <p>
   * This could have been defined in StringUtils, but isn't already there. Rather than introduce
   * another class with that name, it was added to MetadataUtils as that's the first context it was
   * needed in.
   * 
   * @param caseSensitive
   * @param value
   * @param candidates
   * @return True if at least one candidate matches the given value.
   */
  public static boolean doesAnyMatch(boolean caseSensitive, String value, String... candidates) {
    if (StringUtils.isNotEmpty(value)) {
      for (String candidate : candidates) {
        if ((caseSensitive ? value.equals(candidate) : value.equalsIgnoreCase(candidate))) {
          return true;
        }
      }
    }
    return false;
  }


}
