package org.astd.rsuite.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;

public class CallArgumentUtils {

  /**
   * Log the call arguments' names and values.
   * 
   * @param args
   */
  public static void logArguments(CallArgumentList args, Log log) {
    StringBuilder sb = new StringBuilder("Parameters:");
    for (CallArgument arg : args.getAll()) {
      sb.append("\n\t\"").append(arg.getName()).append("\": ");
      if (arg.isFileItem()) {
        sb.append("[FileItem]");
      } else if (arg.isFile()) {
        sb.append("[File]");
      } else {
        sb.append("\"").append(arg.getValue()).append("\"");
      }
    }
    log.info(sb);
  }

  /**
   * Get the value of an expected String argument, or when unset, the provided default value.
   * 
   * @param props
   * @param name
   * @param defaultValue
   * @return A String argument value or the default.
   */
  public static String getFirstString(CallArgumentList args, String name, String defaultValue) {
    String value = args.getFirstString(name);
    if (StringUtils.isBlank(value)) {
      value = defaultValue;
    }
    return value;
  }
}

