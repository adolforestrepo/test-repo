package org.astd.rsuite.utils;

import java.io.File;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.astd.rsuite.constants.ConfigurationPropertyConstants;
import org.astd.rsuite.constants.LayeredMetadataConstants;
import org.astd.rsuite.constants.ProjectConstants;
import org.astd.rsuite.domain.RSuiteWebApplication;

import com.reallysi.rsuite.api.ConfigurationProperties;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.system.RSuiteServerConfiguration;

/**
 * A collection of utility methods that's specific to the project yet don't have a better home.
 */
public class ProjectUtils
    implements ProjectConstants, LayeredMetadataConstants, ConfigurationPropertyConstants {

  private RSuiteServerConfiguration serverConf;

  private ProjectUtils() {}

  public ProjectUtils(
      RSuiteServerConfiguration serverConf) {
    this();
    this.serverConf = serverConf;
  }

  /**
   * Get the base URL for the RSuite server.
   * 
   * @return The protocol through port, but no web app.
   */
  public String getBaseRSuiteUrl() {
    /*
     * FIXME: If there's really no way to get the protocol from RSuite's Java API, introduce a
     * configuration property.
     */
    return new StringBuilder(PROTOCOL_RSUITE).append("://").append(serverConf.getHostName())
        .append(":").append(serverConf.getPort()).toString();
  }

  /**
   * Get or create a directory within RSuite's temp directory using the given directory name prefix
   * and date.
   * 
   * @param dirNamePrefix
   * @param date
   * @param create Submit true to instruct this method to attempt to create when it doesn't exist.
   * @return A sub-directory of RSuite's temp dir which incorporates the given prefix and date in
   *         the directory name.
   */
  public File getTmpSubDir(String dirNamePrefix, Date date, boolean create) {

    String datedPath = DateUtils.DATE_FORMAT_YYYYMMDD.format(date);

    File tmpDir =
        new File(serverConf.getTmpDir(), StringUtils.isNotBlank(dirNamePrefix) ? dirNamePrefix
            .concat(datedPath) : datedPath);

    if (create && !tmpDir.exists()) {
      tmpDir.mkdirs();
    }

    return tmpDir;
  }

  /**
   * Get an absolute base external URL for this RSuite instance.
   * 
   * @param context
   * @param webApp Optionally identify the desired web app to get the correct reference to use for
   *        that web app in a URL. It's okay to send in null or WebApp.NONE.
   * @return The protocol through port, and optionally the web app name. Does not end with a slash.
   * @throws RSuiteException
   */
  public static String getBaseExternalRSuiteUrl(ExecutionContext context,
      RSuiteWebApplication webApp) throws RSuiteException {
    ConfigurationProperties props = context.getConfigurationProperties();
    StringBuilder sb =
        new StringBuilder(ConfigUtils.getProperty(props, PROP_NAME_RSUITE_EXTERNAL_PROTOCOL,
            DEFAULT_EXTERNAL_PROTOCOL)).append("://").append(
            ConfigUtils.getProperty(props, PROP_NAME_RSUITE_EXTERNAL_HOST, DEFAULT_EXTERNAL_HOST))
            .append(":").append(
                ConfigUtils.getPropertyAsInt(props, PROP_NAME_RSUITE_EXTERNAL_PORT,
                    DEFAULT_EXTERNAL_PORT));
    if (webApp != null && StringUtils.isNotBlank(webApp.getName())) {
      sb.append("/").append(webApp.getName());
    }
    return sb.toString();
  }

}
