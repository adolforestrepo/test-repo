package org.astd.rsuite.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.constants.ConfigurationPropertyConstants;
import org.astd.rsuite.constants.DataTypeConstants;
import org.astd.rsuite.constants.LayeredMetadataConstants;
import org.astd.rsuite.constants.ProjectConstants;
import org.astd.rsuite.domain.ProjectQName;
import org.astd.rsuite.utils.mo.QNameManagedObjectQualifier;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;

/**
 * A collection of static methods related to project containers.
 */
public class ProjectContainerUtils
    implements DataTypeConstants, ProjectConstants, LayeredMetadataConstants,
    ConfigurationPropertyConstants {

  private static Log log = LogFactory.getLog(ProjectContainerUtils.class);

  /**
   * Scrub the project title such that it doesn't contain illegal characters for some contexts, such
   * as container names.
   * <p>
   * IDEA: Rename to imply a broader use. There's no reason this couldn't be used for more than
   * project titles.
   * 
   * @param projectTitle
   * @return A "sanitized" version of the project title.
   */
  public static String formatProjectTitle(String projectTitle) {
    // IMPROVE: consolidate into one statement?
    if (projectTitle.indexOf("/") > -1)
      projectTitle = projectTitle.replaceAll("\\/", "-");
    if (projectTitle.indexOf("\\") > -1)
      projectTitle = projectTitle.replaceAll("\\", "-");
    if (projectTitle.indexOf("#") > -1)
      projectTitle = projectTitle.replaceAll("\\#", "-");

    return projectTitle.trim();
  }

  /**
   * Get the first qualifying DITA MO referenced by the provided project container.
   * 
   * @param context
   * @param user
   * @param container
   * @return The project container's DITA topic or null.
   * @throws RSuiteException
   */
  public static ManagedObject getProjectContainerDitaMo(ExecutionContext context, User user,
      ContentAssemblyNodeContainer container) throws RSuiteException {
    return ContainerUtils.getFirstQualifyingReferencedManagedObject(context, user, container,
        new QNameManagedObjectQualifier(ProjectQName.DITA_TOPIC.getQName()));
  }

}
