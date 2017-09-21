package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.astd.rsuite.domain.ArticlePubCode;

public interface TempWorkflowConstants {

  /**
   * Copied from com.reallysi.service.workflow.Constants
   */
  public static final String EXCEPTION_OCCUR = "EXCEPTION_OCCUR";
  public static final String EXCEPTION_TYPE = "EXCEPTION_TYPE";
  public static final String EXCEPTION_TYPE_INNER = "EXCEPTION_TYPE_INNER";
  public static final String EXCEPTION_TYPE_CUSTOMED = "EXCEPTION_TYPE_CUSTOMED";
  public static final String EXCEPTION_TYPE_BUSINESSRULE = "EXCEPTION_TYPE_BUSINESSRULE";
  public static final String EXCEPTION_TYPE_SYSTEM = "EXCEPTION_TYPE_SYSTEM";
  public static final String ARTICLE_MO_ID_PARAM = "articleCaMoId";
  public static final String TARGET_VAR_PARAM = "targetVariableName";
  
  
  public static final String OBJECT_IDS_PARAM = "objectIds";
  public static final String DESTINATION_PATH_PARAM = "destinationPath";
  public static String FAIL_IF_EMPTY_PARAM = "failIfEmpty";
  public static String USE_DISPLAY_NAMES_PARAM = "useDisplayNames";
  

  public static final String ATD_VAR_PUB_CODE = "pubCode";
  public static final String ATD_VAR_FULL_FILENAME = "fullFileName";
  public static final String ATD_VAR_SOURCE_FILENAME = "sourceFileName";
  public static final String ATD_VAR_ARTICLE_TYPE = "articleType";
  public static final String ATD_VAR_VOLUME_NUMBER = "volumeNumber";
  public static final String ATD_VAR_ISSUE = "issue";
  /** Month and issue refer to the same item. */
  public static final String ATD_VAR_MONTH = "month";
  public static final String ATD_VAR_SEQUENCE = "sequence";
  public static final String ATD_VAR_FILENAME_AUTHOR = "fileNameAuthor";

  public static final String ATD_VAR_DOCX_ID = "docxMoId";

  public static final String ATD_VAR_CA_ID = "articleCaMoId";

  /**
   * MO ID of PDF version of article.
   */
  public static final String ASTD_VAR_ARTICLE_PDF = "articlePdfMoId";

  /**
   * Parameter to take the name of the alias to use for a managed object.
   */
  public static final String ALIAS_PARAM = "alias";
  /**
   * Variable that holds exception message from last-logged exception.
   */
  public static final String EXCEPTION_MESSAGE_VAR = "exceptionMessage";
  public static final String DATE_FORMAT_STRING = "yyyyMMdd-HHmmss";
  public static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_STRING);

  public static final String FOLDER_MAGAZINE = "Magazines";
  public static final String FOLDER_PATH_TD = "/" + FOLDER_MAGAZINE + "/" + ArticlePubCode.TD.getPubDesc();

  public static final String RSUITE_NS_URI = "http://www.reallysi.com/";
  public static final String RSUITE_NON_XML_LOCAL_NAME = "NONXML";

  public static final String ASTD_ARTICLE_PID_LMD_FIELD = "article-process-id";

}
