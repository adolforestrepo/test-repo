package org.astd.rsuite.domain;

import org.apache.commons.lang3.StringUtils;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;

/**
 * All container types known by these customizations.
 * <p>
 * Some values may have the same system name but different display names. This could cause some
 * problems for {@link #hasSameType(ContentAssemblyNodeContainer)}. In Nov 2016, the only uses of
 * this method dealt with product containers, and the product container type didn't share the system
 * name as any other enum value.
 * <p>
 * Alleviates the need to hard-code container type names in the Java, *and* enables us to vary
 * system behavior by container type.
 */
public enum ContainerType {
  /**
   * Default, and built-in container type.
   */
  CA("ca", "Container"),

  /**
   * Another built-in container type.
   */
  FOLDER("folder", "Folder");

  /**
   * Container type known by RSuite
   */
  private String systemName;

  /**
   * User-friendly display name for the container type.
   */
  private String displayName;

  /**
   * Construct a value for this enum.
   * 
   * @param systemName Container type known by RSuite
   * @param displayName User-friendly display name for the container type
   */
  private ContainerType(
      String systemName, String displayName) {
    this.systemName = systemName;
    this.displayName = displayName;
  }

  /**
   * @return the systemName
   */
  public String getSystemName() {
    return systemName;
  }

  /**
   * @return the displayName
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Find out if the given container has the same type as this enum value.
   * <p>
   * WARNING: At least two enum values share the same system name.
   * 
   * @param container Container to test type of.
   * @return True if given container has the same type as this enum value.
   */
  public boolean hasSameType(ContentAssemblyNodeContainer container) {
    if (container != null) {
      String type = container.getType();
      return getSystemName().equalsIgnoreCase(type);
    }
    return false;
  }

  /**
   * Get an enum value by container.
   * 
   * @param container
   * @return enum value, or null when there is no match
   */
  public static ContainerType get(ContentAssemblyNodeContainer container) {
    if (container != null && StringUtils.isNotBlank(container.getType())) {
      for (ContainerType val : ContainerType.values()) {
        if (val.getSystemName().equalsIgnoreCase(container.getType())) {
          return val;
        }
      }
    }
    return null;
  }

}
