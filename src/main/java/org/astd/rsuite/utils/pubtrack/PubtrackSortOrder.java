package org.astd.rsuite.utils.pubtrack;

import org.astd.rsuite.utils.pubtrack.PubtrackUtils.PubtrackMappingProcessColumn;
import org.astd.rsuite.utils.pubtrack.PubtrackUtils.PubtrackResultOrder;

public class PubtrackSortOrder {

  private PubtrackResultOrder resultOrder;
  private PubtrackMappingProcessColumn processColumns[];

  public PubtrackSortOrder(
      PubtrackResultOrder resultOrder, PubtrackMappingProcessColumn... processColumns) {
    this.resultOrder = resultOrder;
    this.processColumns = processColumns;
  }

  public PubtrackResultOrder getResultOrder() {
    return resultOrder;
  }

  public PubtrackMappingProcessColumn[] getProcessColumns() {
    return processColumns;
  }

}
