package org.astd.rsuite.workflow.actions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.astd.rsuite.domain.ArticlePubCode;

/**
 * Constant definitions for ASTD workflow variable names.
 */
public interface AstdWorkflowConstants {
	public static final String ASTD_VAR_PUB_CODE = "pubCode";
	public static final String ASTD_VAR_FULL_FILENAME = "fullFileName";
	public static final String ASTD_VAR_SOURCE_FILENAME = "sourceFileName";
	public static final String ASTD_VAR_ARTICLE_TYPE = "articleType";
	public static final String ASTD_VAR_VOLUME_NUMBER = "volumeNumber";
    public static final String ASTD_VAR_ISSUE = "issue";
    /** Month and issue refer to the same item. */
    public static final String ASTD_VAR_MONTH = "month";
    public static final String ASTD_VAR_SEQUENCE = "sequence";
    public static final String ASTD_VAR_FILENAME_AUTHOR = "fileNameAuthor";
    
    public static final String ASTD_VAR_XML_MO_ID = "xmlMoId";
    
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
	public static final String FOLDER_PATH_TD = "/" + FOLDER_MAGAZINE + "/" +
												ArticlePubCode.TD.getPubDesc();
	
	public static final String RSUITE_NS_URI = "http://www.reallysi.com/";
	public static final String RSUITE_NON_XML_LOCAL_NAME = "NONXML";
	
	public static final String ASTD_ARTICLE_PID_LMD_FIELD = "article-process-id";
	
	public static final String ASTD_REPORT_CSS_URL =
		"/rsuite/rest/v1/static/astd/css/report.css";
}
