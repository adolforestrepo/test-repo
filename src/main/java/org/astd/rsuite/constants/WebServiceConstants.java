package org.astd.rsuite.constants;

/**
 * A collection of web service-related constants, including web service parameter names.
 */
public interface WebServiceConstants {

  /**
   * Web service parameter name for an RSuite ID.
   */
  final String WS_PARAM_NAME_RSUITE_ID = "rsuiteId";

  /**
   * Web service parameter name identifying a specific version of an object.
   */
  final String WS_PARAM_NAME_VERSION = "version";

  /**
   * Web service parameter name for the web service API name (Remote API definition ID).
   */
  final String WS_PARAM_NAME_API_NAME = "apiName";

  /**
   * Web service parameter name for the RSuite session key.
   */
  final String WS_PARAM_NAME_SESSION_KEY = "skey";

  /**
   * Web service parameter name for terminating the RSuite session.
   */
  final String WS_PARAM_NAME_TERMINATE_SESSION = "terminateSession";

  /**
   * The web service parameter name for the uploaded file to transform
   */
  final String WS_PARAM_NAME_FILE_TO_TRANSFORM = "fileToTransform";

  /**
   * The web service parameter name for the XSL URI.
   */
  final String WS_PARAM_NAME_XSL_URI = "xslUri";

  /**
   * The web service parameter name for use system user or not.
   */
  final String WS_PARAM_NAME_MAY_USE_SYSTEM_USER = "mayUseSystemUser";

  /**
   * The web service parameter name for authorized role names.
   */
  final String WS_PARAM_NAME_AUTHORIZED_ROLE_NAMES = "authorizedRoleNames";

  /**
   * The web service parameter role prefix.
   */
  final String ROLE_PREFIX = "role^";

  /**
   * The web service parameter lmd prefix.
   */
  final String LMD_PREFIX = "lmd";

  /**
   * Web service parameter name for the lmd value
   */
  final String WS_PARAM_NAME_LMD_VALUE = "lmdValue";

  /**
   * Web service parameter name for transform param for showing track changes
   */
  final String WS_PARAM_NAME_TRANSFORM_PARAM_SHOW_TRACK_CHANGES = "transformParamShowTrackChanges";

  /**
   * Web service parameter name for transform param for showing comments
   */
  final String WS_PARAM_NAME_TRANSFORM_PARAM_SHOW_COMMENTS = "transformParamShowComments";


}
