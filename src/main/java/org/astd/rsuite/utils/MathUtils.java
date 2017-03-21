package org.astd.rsuite.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.apache.commons.lang3.StringUtils;

/**
 * A collection of static math utility methods.
 */
public class MathUtils {

  public static BigDecimal ONE_HUNDRED = new BigDecimal(100);

  /**
   * Custom logic for parsing an int from a string. When unable to do so, -1 is returned such that
   * the caller can decide how to respond.
   * 
   * @param num
   * @return An int, unless the input is null, blank, or cannot be parsed as an int, in which case
   *         -1 is returned. Null and blank are purposely not treated as zero.
   */
  public static int parseInt(String num) {
    if (StringUtils.isNotBlank(num)) {
      try {
        return Integer.parseInt(num);
      } catch (NumberFormatException e) {
        // ignore (allow method to return null;
      }
    }
    return -1;
  }

  /**
   * Starting with strings, calculate a percent.
   * 
   * @param numerator
   * @param denominator
   * @see #getPercent(BigDecimal, BigDecimal)
   * @return The percent that goes out two decimal places.
   */
  public static BigDecimal getPercent(String numerator, String denominator) {
    return getPercent(new BigDecimal(numerator), new BigDecimal(denominator));
  }

  /**
   * Calculate the percent from the given numbers.
   * 
   * @param numerator
   * @param denominator
   * @return The percent that goes out two decimal places.
   */
  public static BigDecimal getPercent(BigDecimal numerator, BigDecimal denominator) {
    return numerator.divide(denominator, 4, // number of decimal places before rounding and
                                            // multiplication.
        RoundingMode.HALF_UP).multiply(ONE_HUNDRED);
  }

  /**
   * Round, applying default behavior of "half up".
   * 
   * @param num
   * @param decimalPlace
   * @return A string representation of a rounded number.
   */
  public static BigDecimal round(BigDecimal num, int decimalPlace) {
    return num.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
  }

}
