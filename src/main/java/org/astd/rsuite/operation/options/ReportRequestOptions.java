package org.astd.rsuite.operation.options;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.operation.status.OperationStatus;
import org.astd.rsuite.utils.DateUtils;

import com.reallysi.rsuite.api.RSuiteException;


/**
 * Options class for requesting a report
 */
public class ReportRequestOptions
    extends FileRequestOptions {

  /**
   * Web service log
   */
  private final static Log log = LogFactory.getLog(ReportRequestOptions.class);

  /**
   * Flag indicate if this instance has a date range
   */
  private boolean dateRange = false;

  /**
   * Start of date range
   */
  private Date startDate = null;

  /**
   * End of date range
   */
  private Date endDate = null;

  /**
   * The operation statuses.
   */
  private List<OperationStatus> opStatuses = null;

  /**
   * Constructor for reports with a date range and operation status.
   * 
   * @param suggestedFileName
   * @param tabName
   * @param dateRange
   * @param operationStatuses
   * @throws RSuiteException Thrown if unable to parse the date range.
   */
  public ReportRequestOptions(
      String suggestedFileName, String tabName, String dateRange, List<String> operationStatuses,
      Log log)
      throws RSuiteException {
    super(log);
    setSuggestedFileName(suggestedFileName);
    setTabName(tabName);
    parseDateRange(dateRange);
    opStatuses = OperationStatus.getList(operationStatuses, false);
  }

  /**
   * Constructor for reports with a date range and operation status.
   * 
   * @param suggestedFileName
   * @param tabName
   * @param operationStatuses
   * @throws RSuiteException Thrown if unable to parse the date range.
   */
  public ReportRequestOptions(
      String suggestedFileName, String tabName, List<String> operationStatuses, Log log)
      throws RSuiteException {
    super(log);
    setSuggestedFileName(suggestedFileName);
    setTabName(tabName);
    opStatuses = OperationStatus.getList(operationStatuses, false);
  }

  /**
   * Parse the date range, requiring a valid start and end date.
   * 
   * @param dateRange
   * @throws RSuiteException
   */
  private void parseDateRange(String dateRange) throws RSuiteException {
    if (log.isDebugEnabled())
      log.debug("Received: '" + dateRange + "'");
    if (StringUtils.isNotBlank(dateRange)) {
      String[] dates = dateRange.split("[|]");
      if (dates.length != 2 || dates[0].length() == 0 || dates[1].length() == 0) {
        throw new RSuiteException(RSuiteException.ERROR_PARAM_INVALID, ProjectMessageResource
            .getMessageText("error.invalid.date.range"));
      }
      try {
        setStartDate(DateUtils.getDate(dates[0].concat(" 00:00:00"),
            DateUtils.DATE_FORMAT_YMD_HMS));
      } catch (ParseException e) {
        throw new RSuiteException(RSuiteException.ERROR_PARAM_INVALID, ProjectMessageResource
            .getMessageText("error.invalid.start.date", dates[0]), e);
      }
      try {
        setEndDate(DateUtils.getDate(dates[1].concat(" 23:59:59"), DateUtils.DATE_FORMAT_YMD_HMS));
      } catch (ParseException e) {
        throw new RSuiteException(RSuiteException.ERROR_PARAM_INVALID, ProjectMessageResource
            .getMessageText("error.invalid.end.date", dates[1]), e);
      }
      this.dateRange = true;
    }
  }

  /**
   * Determine if this instance has a date range.
   * 
   * @return true if there is a date range; else, false.
   */
  public boolean hasDateRange() {
    return dateRange;
  }

  /**
   * Set the start date
   * 
   * @param startDate
   */
  private void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  /**
   * @see #hasDateRange()
   * @return the start of the data range or null.
   */
  public Date getStartDate() {
    return startDate;
  }

  /**
   * Set the end date
   * 
   * @param endDate
   */
  private void setEndDate(Date endDate) {
    this.endDate = endDate;
  }

  /**
   * @see #hasDateRange()
   * @return the end of the data range or null.
   */
  public Date getEndDate() {
    return endDate;
  }

  /**
   * @return The operation status(es) associated with this request. May be null or empty, which
   *         means "any" or "not applicable".
   */
  public List<OperationStatus> getOperationStatuses() {
    return opStatuses;
  }

}
