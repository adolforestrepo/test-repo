package org.astd.rsuite.operation.result;

import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;

/**
 * A project container operation result.
 */
public class ProjectContainerOperationResult
    extends OperationResult {

  private ContentAssemblyNodeContainer projectContainer;

  public ProjectContainerOperationResult(
      String opId, String defaultLabel, Log log) {
    super(opId, defaultLabel, log);
  }

  /**
   * @return the projectContainer
   */
  public ContentAssemblyNodeContainer getProjectContainer() {
    return projectContainer;
  }

  /**
   * @param projectContainer the projectContainer to set
   */
  public void setProjectContainer(ContentAssemblyNodeContainer projectContainer) {
    this.projectContainer = projectContainer;
  }



}
