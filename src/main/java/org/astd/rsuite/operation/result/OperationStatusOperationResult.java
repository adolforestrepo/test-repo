package org.astd.rsuite.operation.result;

import org.apache.commons.logging.Log;
import org.astd.rsuite.operation.status.OperationStatus;

/**
 * An operation result that has counters for each <code>OperationStatus</code> enum value.
 */
public class OperationStatusOperationResult
    extends OperationResult {

  public OperationStatusOperationResult(
      String id, String defaultLabel, Log log) {
    super(id, defaultLabel, log);
  }

  public int getCount(OperationStatus status) {
    return getCount(status.getCounterName());
  }

  public void incrementCount(OperationStatus status) {
    incrementCount(status, 1);
  }

  public void incrementCount(OperationStatus status, int cnt) {
    incrementCount(status.getCounterName(), cnt);
  }

}
