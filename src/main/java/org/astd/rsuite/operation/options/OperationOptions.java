package org.astd.rsuite.operation.options;

import java.util.List;

import org.apache.commons.logging.Log;

public class OperationOptions {

  private boolean mayUseSystemUser;
  private List<String> authorizedRoleNames;
  private Log log;

  public OperationOptions(
      Log log) {
    this.log = log;
  }

  public boolean mayUseSystemUser() {
    return mayUseSystemUser;
  }

  public void setMayUseSystemUser(boolean mayUseSystemUser) {
    this.mayUseSystemUser = mayUseSystemUser;
  }

  public List<String> getAuthorizedRoleNames() {
    return authorizedRoleNames;
  }

  public void setAuthorizedRoleNames(List<String> authorizedRoleNames) {
    this.authorizedRoleNames = authorizedRoleNames;
  }

  public Log getLog() {
    return log;
  }

  /**
   * @param defaultLog
   * @return This instance's log when not null, else the given log is returned.
   */
  public Log getLog(Log defaultLog) {
    return (log == null ? defaultLog : log);
  }

}
