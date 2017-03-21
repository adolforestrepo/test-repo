package org.astd.rsuite.domain;

import org.apache.commons.lang3.StringUtils;

/**
 * RSuite web applications known to this project.
 */
public enum RSuiteWebApplication {

  RSUITE("rsuite"),
  RSUITE_CMS("rsuite-cms"),
  NONE(StringUtils.EMPTY);

  private String name;

  private RSuiteWebApplication(
      String name) {
    this.name = name;
  }

  /**
   * @return The web application name that may be used in URLs.
   */
  public String getName() {
    return name;
  }

}
