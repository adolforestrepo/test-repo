package org.astd.rsuite.constants;

/**
 * All configuration property-related constants used by this project. Property names should not be
 * hard-coded throughout the code base.
 */
public interface ConfigurationPropertyConstants {

  /**
   * Property name identifying the how long a notification should be displayed, in seconds.
   */
  String PROP_NAME_NOTIFICATION_DURATION_IN_SECONDS = "astd.notification.duration.in.seconds";

  /**
   * Default value for the {@link #PROP_NAME_NOTIFICATION_DURATION_IN_SECONDS} property.
   */
  int DEFAULT_NOTIFICATION_DURATION_IN_SECONDS = 10;

  /**
   * Property name to determine if the testing service should be enabled.
   */
  String PROP_NAME_TESTING_SERVICE_ENABLED = "astd.testing.service.enabled";

  /**
   * Default value for the {@link #PROP_NAME_TESTING_SERVICE_ENABLED} property.
   */
  boolean DEFAULT_TESTING_SERVICE_ENABLED = false;

  /**
   * The name of the property defining the protocol to use for external absolute RSuite URLs.
   */
  String PROP_NAME_RSUITE_EXTERNAL_PROTOCOL = "rsuite.external.protocol";

  /**
   * The default external port.
   */
  String DEFAULT_EXTERNAL_PROTOCOL = "http";

  /**
   * The name of the property defining RSuite's externally accessible host.
   */
  String PROP_NAME_RSUITE_EXTERNAL_HOST = "rsuite.external.host";

  /**
   * The default external host.
   */
  String DEFAULT_EXTERNAL_HOST = "localhost";

  /**
   * The name of the property defining RSuite's externally accessible port.
   */
  String PROP_NAME_RSUITE_EXTERNAL_PORT = "rsuite.external.port";

  /**
   * The default external port.
   */
  int DEFAULT_EXTERNAL_PORT = 8080;

}
