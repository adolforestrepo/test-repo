package org.astd.reallysi.rsuite.api.workflow;

import com.reallysi.rsuite.api.workflow.activiti.BusinessRuleException;

public class InnerException extends BusinessRuleException {
  public InnerException(String taskname, String description) {
     super(taskname, description);
  }
}

