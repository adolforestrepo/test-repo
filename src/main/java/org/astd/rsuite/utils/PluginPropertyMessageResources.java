package org.astd.rsuite.utils;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.i18n.PropertyMessageResources;

/**
 * Implementation of MessageResources that loads a properties file located in a plugin. In RSuite
 * 3.7, PropertyMessageResources supported this. This changed in RSuite 4. This class goes through
 * the PluginAccessManager. As such, the value provided to setConfig must begin with a static web
 * service. For example, "/rsuite_worldbank/wb-rsuite-messages.properties" would call upon the
 * PluginAccessManager to resolve "/rsuite_worldbank", and search within the resolved path for
 * "wb-rsuite-messages.properties.
 */
public class PluginPropertyMessageResources
    extends PropertyMessageResources {

  /**
   * Seralization version number
   */
  private static final long serialVersionUID = 1L;

  /**
   * Class log
   */
  private final static Log log = LogFactory.getLog(PluginPropertyMessageResources.class);

  /**
   * An instance of the ExecutionContext, used to get the PluginAccessManager.
   */
  private ExecutionContext context;

  /**
   * The expected file extention, of the resource file
   */
  private static final String FILE_EXT = ".properties";

  /**
   * Construct an instance of this class.
   * 
   * @param context
   * @param returnNull
   */
  public PluginPropertyMessageResources(
      ExecutionContext context, boolean returnNull) {
    super(returnNull);
    this.context = context;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.reallysi.rsuite.api.i18n.PropertyMessageResources#loadLocale(java.lang.String)
   */
  @SuppressWarnings({"unchecked"})
  @Override
  protected synchronized void loadLocale(String localeKey) {

    // Bail if already tried for this locale
    if (locales.get(localeKey) != null) {
      return;
    }

    // Mark as at least tried
    locales.put(localeKey, localeKey);

    // Finalize the path, for this locale
    String resourcePath = new String(config);
    if (!resourcePath.endsWith(FILE_EXT)) {
      resourcePath += FILE_EXT;
    }
    if (localeKey.length() > 0) {
      resourcePath = resourcePath.replaceFirst("\\" + FILE_EXT, "_" + localeKey + FILE_EXT);
    }

    InputStream inputStream = null;
    try {
      log.info("Loading resource \"" + resourcePath + "\" (config: " + config + "; locale: "
          + localeKey + ")");
      if (context != null)
        inputStream = context.getPluginAccessManager().getContent(context.getAuthorizationService()
            .getSystemUser(), resourcePath);
      if (inputStream != null) {
        Properties props = new Properties();
        props.load(inputStream);
        log.info("Property count: " + props.size());

        if (props.size() < 1) {
          return;
        }

        synchronized (messages) {
          Iterator<Object> names = props.keySet().iterator();
          while (names.hasNext()) {
            String key = (String) names.next();
            messages.put(messageKey(localeKey, key), props.getProperty(key));
          }
        }
      } else {
        if (log.isDebugEnabled())
          log.debug("Unable to find resource \"" + resourcePath + "\" (config: " + config
              + "; locale: " + localeKey + ")");
      }
    } catch (Exception e) {
      log.error("Unable to find resource \"" + resourcePath + "\" (config: " + config + "; locale: "
          + localeKey + ")", e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }

  }
}
