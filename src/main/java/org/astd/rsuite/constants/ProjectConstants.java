package org.astd.rsuite.constants;

import com.reallysi.rsuite.api.tools.AliasHelper;

public interface ProjectConstants {

  /**
   * The RSuite session key
   */
  String RSUITE_SESSION_KEY = "RSUITE-SESSION-KEY";

  /**
   * The RSuite rest web service path for version 1
   */
  String REST_V1_URL_ROOT = "/rsuite/rest/v1";

  /**
   * RSuite's external URL for static resources.
   */
  String RSUITE_EXTERNAL_URL_STATIC_RESOURCES = REST_V1_URL_ROOT.concat("/static/");

  /**
   * RSuite's internal URL for static resources.
   */
  String RSUITE_INTERNAL_URL_STATIC_RESOURCES = "rsuite:/res/plugin/";

  /**
   * MIME type for PLAIN
   */
  String MIME_TYPE_PLAIN = "text/plain";

  /**
   * MIME type for HTML
   */
  String MIME_TYPE_HTML = "text/html";

  /**
   * MIME type for XML
   */
  String MIME_TYPE_XML = "application/xml";

  /**
   * MIME type for JSON
   */
  String MIME_TYPE_JSON = "application/json";

  /**
   * MIME type for PDF
   */
  String MIME_TYPE_PDF = "application/pdf";

  /**
   * Character encoding for UTF-8.
   */
  String CHARACTER_ENCODING_UTF8 = "UTF-8";

  /**
   * Default character encoding
   */
  String DEFAULT_CHARACTER_ENCODING = CHARACTER_ENCODING_UTF8;

  /**
   * A hyphen, which is used as a delimiter by some of the code base.
   */
  String HYPHEN = "-";

  /**
   * An underscore, which is used as a delimiter by some of the code base.
   */
  String UNDERSCORE = "_";

  /**
   * A slash. Used between URL parts, if not more.
   */
  String SLASH = "/";

  /**
   * Protocol for (external) RSuite URLs.
   * <p>
   * IMPROVE: Make configurable by the time we have our first HTTPS environment.
   */
  String PROTOCOL_RSUITE = "http";

  /**
   * The ID of the plugin this class is defined in.
   */
  String PLUGIN_ID_HOST = "astd";

  /**
   * The name of this plugin's static web service that is configured to the WebContent directory.
   * This may be one in the same as the plugin's ID, but doesn't have to be.
   */
  String STATIC_WEB_SERVICE_NAME_WEB_CONTENT_HOST = PLUGIN_ID_HOST;

  /**
   * The beginning of a plugin resource path that resolves within the "WebContent" folder of the
   * plugin.
   * <p>
   * Keep in sync with the associated static web service defined in the plugin descriptor.
   */
  String PLUGIN_WEB_SERVICE_PATH_WEB_CONTENT = SLASH.concat(
      STATIC_WEB_SERVICE_NAME_WEB_CONTENT_HOST);

  /**
   * The plugin resource path for the messages file.
   */
  String PLUGIN_RESOURCE_PATH_MESSAGES_FILE = PLUGIN_WEB_SERVICE_PATH_WEB_CONTENT.concat(SLASH)
      .concat("project-rsuite-messages.properties");

  /**
   * The XHTML-to-DITA XSLT URI
   */
  String XSL_URI_XHTML_TO_DITA = RSUITE_INTERNAL_URL_STATIC_RESOURCES.concat(
      STATIC_WEB_SERVICE_NAME_WEB_CONTENT_HOST).concat("/xslt/xhtml2dita/xhtml2dita.xsl");

  /**
   * The DITA-to-XHTML XSLT URI
   */
  String XSL_URI_DITA_TO_XHTML = RSUITE_INTERNAL_URL_STATIC_RESOURCES.concat(
      STATIC_WEB_SERVICE_NAME_WEB_CONTENT_HOST).concat("/xslt/dita2xhtml/dita2xhtml.xsl");
    

  /**
   * File name alias type.
   */
  public final static String ALIAS_TYPE_FILENAME = AliasHelper.FILENAME_ALIAS_TYPE;

  /**
   * Name of the user account that stores the dynamic configuration settings
   */
  public final static String DYNAMIC_CONFIGURATION_USERNAME = "dconfigurator";

  /**
   * Name of dynamic config parameter for writing out all XSLT and Merge inputs and outputs
   */
  public final static String DYNAMIC_CONFIGURATION_PARAM_XSLT_AND_MERGE_ALL =
      "ama.xslt.and.merge.write.all";

}
