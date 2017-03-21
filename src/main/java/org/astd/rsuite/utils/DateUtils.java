package org.astd.rsuite.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;

import com.reallysi.rsuite.api.RSuiteException;

public class DateUtils {

  /**
   * Date format that includes the date and time, through milliseconds, explicitly in the UTC time
   * zone.
   */
  public static final SimpleDateFormat DATE_FORMAT_DATE_AND_TIME_IN_UTC = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  /**
   * The time zone RSuite uses. It's important to use the same time zone when retaining dates in
   * LMD. If a future version of RSuite changes, this code needs to as well.
   */
  public static final TimeZone RSUITE_TIME_ZONE = TimeZone.getTimeZone("UTC");

  /**
   * The date format used in RSuite metadata (system and layered). Includes milliseconds.
   */
  public static final String DATE_FORMAT_RSUITE_METADATA = "yyyy-MM-dd'T'HH:mm:ss.SSS";

  /**
   * A date format that looks like "2014-08-21"
   */
  public static final SimpleDateFormat DATE_FORMAT_YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd");

  /**
   * A date and time format that goes through seconds and includes the time zone.
   */
  public static final SimpleDateFormat DATE_FORMAT_YYYY_MM_DD_HH_mm_ss_Z = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ssZ");

  /**
   * A time format that includes hours through seconds
   */
  public static final SimpleDateFormat DATE_FORMAT_HH_mm_ss = new SimpleDateFormat("HH:mm:ss");

  /**
   * An abbreviated date format that can accept month, day, then year, delimited by slashes, whereby
   * each only needs to be one digit in length.
   */
  public static final SimpleDateFormat DATE_FORMAT_M_d_y_WITH_SLASH_DELIM = new SimpleDateFormat(
      "M/d/y");

  /**
   * An abbreviated date format that can accept year, month, then day, delimited by hyphens, whereby
   * each only needs to be one digit in length.
   */
  public static final SimpleDateFormat DATE_FORMAT_y_M_d_WITH_HYPHEN_DELIM = new SimpleDateFormat(
      "y-M-d");

  /**
   * A date format that looks like "20140821"
   */
  public static final SimpleDateFormat DATE_FORMAT_YYYYMMDD = new SimpleDateFormat("yyyyMMdd");

  /**
   * A date and time format that looks like "2014-08-21 232215"
   */
  public static final SimpleDateFormat DATE_FORMAT_YYYYMMDD_HHMMSS = new SimpleDateFormat(
      "yyyy-MM-dd HHmmss");

  /**
   * A date and time format that looks like "2014-08-21 23:22:15"
   */
  public static final String DATE_FORMAT_YMD_HMS = "yyyy-MM-dd HH:mm:ss";

  /**
   * A date and time format that looks like "2014-08-21 23:22:15"
   */
  public static final SimpleDateFormat DATE_FORMAT_YYYYMMDD_HH_MM_SS = new SimpleDateFormat(
      DATE_FORMAT_YMD_HMS);

  /**
   * Convert a date from java.util.Date to String
   * 
   * @param date
   * @param sdf
   * @return The given <code>Date</code> formatted as a <code>String</code> using the given
   *         <code>SimpleDateFormat</code>.
   * @throws RSuiteException
   */
  public static String dateToString(Date date, SimpleDateFormat sdf) {
    String strDate = null;

    if (date != null) {
      strDate = sdf.format(date);
    }

    return strDate;
  }

  /**
   * Convert a string-formatted date into a <code>Date</code>.
   * 
   * @param strDate The string-formatted date.
   * @param formats Date formats to try.
   * @return Date if able to parse against one of the provided formats. Null returned when the
   *         provided date is blank.
   * @throws RSuiteException Throw when the provided date is not blank and does not match any
   *         provided date format.
   */
  public static Date stringToDate(String strDate, SimpleDateFormat... formats)
      throws RSuiteException {
    Date date = null;

    ParseException parseException = null;
    if (StringUtils.isNotBlank(strDate)) {
      for (SimpleDateFormat format : formats) {
        try {
          date = format.parse(strDate);
          if (parseException != null)
            parseException = null;
          break;
        } catch (ParseException ex) {
          parseException = ex;
          continue;
        }
      }

      if (parseException != null) {
        throw new RSuiteException(RSuiteException.ERROR_NOT_DEFINED, "Unable to parse date",
            parseException);
      }
    }

    return date;
  }

  /**
   * Get a date as a string, after making sure its using the same time zone as RSuite.
   * 
   * @param d
   * @param pattern
   * @return Date formatted as a string after applying RSuite's time zone and the given pattern.
   */
  public static String getDateInRSuiteTimezoneAsString(Date d, String pattern) {
    return getDateInRSuiteTimezoneAsString(d, new SimpleDateFormat(pattern));
  }

  /**
   * Get a date as a string, after making sure its using the same time zone as RSuite.
   * 
   * @param d
   * @param fmt
   * @return Date formatted as a string after applying RSuite's time zone and the given format.
   */
  public static String getDateInRSuiteTimezoneAsString(Date d, SimpleDateFormat fmt) {
    if (d != null) {
      GregorianCalendar calendar = new GregorianCalendar(RSUITE_TIME_ZONE);
      calendar.setTime(d);
      fmt.setCalendar(calendar);
      return fmt.format(calendar.getTime());
    }
    return new String();
  }

  /**
   * Get a date from a string, after making sure its using the same time zone as RSuite.
   * 
   * @param value
   * @param pattern
   * @return Date from a string after applying RSuite's time zone and the given pattern.
   * @throws ParseException
   */
  public static Date getDateInRSuiteTimezoneAsDate(String value, String pattern)
      throws ParseException {
    SimpleDateFormat fmt = new SimpleDateFormat(pattern);
    fmt.setTimeZone(RSUITE_TIME_ZONE);
    return fmt.parse(value);
  }

  /**
   * Convert the date into a string that may be retained as a LMD value, with the same time zone
   * RSuite uses, and may be indexed in MarkLogic (dateTime).
   * 
   * @param d
   * @return String representation of date to be retained as LMD.
   */
  public static String getLayeredMetadataDateAsString(Date d) {
    return getDateInRSuiteTimezoneAsString(d, DATE_FORMAT_RSUITE_METADATA);
  }

  /**
   * Convert the string into a date, without applying a time zone.
   * 
   * @param value
   * @param pattern
   * @return Date
   * @throws ParseException
   */
  public static Date getDate(String value, String pattern) throws ParseException {
    return (new SimpleDateFormat(pattern)).parse(value);
  }


  /**
   * Convert a LMD value to a date.
   * 
   * @param value LMD value
   * @return Date representation of the string.
   * @throws ParseException
   */
  public static Date getLayeredMetadataStringAsDate(String value) throws ParseException {
    return getDateInRSuiteTimezoneAsDate(value, DATE_FORMAT_RSUITE_METADATA);
  }

  /**
   * Add seconds to a date
   * 
   * @param d
   * @param seconds
   * @return Given date place the specified number of seconds
   */
  public static Date addSeconds(Date d, long seconds) {
    return addMilliseconds(d, seconds / 1000);
  }

  /**
   * Add milliseconds to a date
   * 
   * @param d
   * @param milliseconds
   * @return Given date place the specified number of milliseconds
   */
  public static Date addMilliseconds(Date d, long milliseconds) {
    return new Date(d.getTime() + milliseconds);
  }

  /**
   * Subtract months from given date.
   * 
   * @param d
   * @param months
   * @return Given date less specified number of months.
   */
  public static Date subtractMonths(Date d, int months) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(d);
    cal.add(Calendar.MONTH, months * -1);
    return cal.getTime();
  }

  /**
   * Find out if the two dates have the same year, month, and day. Time differences are ignored.
   * 
   * @param d1
   * @param d2
   * @return True if and only if both are not null and the year, month, and day match.
   */
  public static boolean haveSameDate(Date d1, Date d2) {
    if (d1 != null && d2 != null) {
      Calendar c1 = Calendar.getInstance();
      c1.setTime(d1);
      c1.set(Calendar.HOUR_OF_DAY, 0);
      c1.set(Calendar.MINUTE, 0);
      c1.set(Calendar.SECOND, 0);
      c1.set(Calendar.MILLISECOND, 0);

      Calendar c2 = Calendar.getInstance();
      c2.setTime(d2);
      c2.set(Calendar.HOUR_OF_DAY, 0);
      c2.set(Calendar.MINUTE, 0);
      c2.set(Calendar.SECOND, 0);
      c2.set(Calendar.MILLISECOND, 0);

      return c1.compareTo(c2) == 0;
    }
    return false;
  }

  /**
   * Find out if the given dates have different years, months, or days. Time differences are
   * ignored.
   * 
   * @param d1
   * @param d2
   * @return True if either is null, both are null, or one has a different year, month, or day.
   */
  public static boolean haveDifferentDates(Date d1, Date d2) {
    return !haveSameDate(d1, d2);
  }

  /**
   * Get a date in a string that conforms to xs:dateTime.
   * 
   * @param d
   * @return String in xs:dateTime format
   */
  public String convertDateToXsDateTimeFormat(Date d) {
    /**
     * The DateFormat code compliments of StackOverflow: http://stackoverflow.com/a/10615059/1320627
     * (Namely this makes sure the resulting timezone offset has a correctly placed colon)
     */
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ") {
      /**
       * Eclipse wanted this serialVersionUID added.
       */
      private static final long serialVersionUID = 1L;

      public Date parse(String source, ParsePosition pos) {
        return super.parse(source.replaceFirst(":(?=[0-9]{2}$)", ""), pos);
      }
    };
    String dateStringInXsDateTimeFormat = df.format(d);
    dateStringInXsDateTimeFormat = dateStringInXsDateTimeFormat.substring(0,
        dateStringInXsDateTimeFormat.length() - 2) + ":" + dateStringInXsDateTimeFormat.substring(
            dateStringInXsDateTimeFormat.length() - 2, dateStringInXsDateTimeFormat.length());
    return dateStringInXsDateTimeFormat;
  }
}
