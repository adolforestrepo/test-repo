package org.astd.rsuite.domain;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.constants.ProjectConstants;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;

/**
 * All known qualified names used by the Java code base.
 * <p>
 * If an exception is found, it should be removed.
 */
public enum ProjectQName implements ProjectConstants {
  DITA_MAP("Map", ProjectNamespace.DITA, ELEMENT_NAME_DITA_MAP, true),
  DITA_TOPIC("Topic", ProjectNamespace.DITA, ELEMENT_NAME_DITA_TOPIC, true);

  /**
   * Class log
   */
  @SuppressWarnings("unused")
  private static Log log = LogFactory.getLog(ProjectQName.class);

  private String displayName;
  private ProjectNamespace projectNamespace;
  private QName qname;
  private boolean copyMetadataToContainer;

  private ProjectQName(
      String displayName, ProjectNamespace projectNamespace, String localName,
      boolean copyMetadataToContainer) {
    this.projectNamespace = projectNamespace;
    this.displayName = displayName;
    this.qname = new QName(projectNamespace.getNamespace().getUri(), localName, projectNamespace
        .getNamespace().getPrefix());
    this.copyMetadataToContainer = copyMetadataToContainer;
  }

  public String getDisplayName() {
    return displayName;
  }

  public ProjectNamespace getProjectNamespace() {
    return projectNamespace;
  }

  public QName getQName() {
    return qname;
  }

  /**
   * @return True when the system is to copy metadata from MOs with this QName to containers
   */
  public boolean shouldCopyMetadataToContainer() {
    return copyMetadataToContainer;
  }

  /**
   * Find out if the given MO matches this content type.
   * 
   * @param mo The managed object to test.
   * @return true when MO's namespace URI and local part match that of this content type's.
   * @throws RSuiteException
   */
  public boolean matches(ManagedObject mo) throws RSuiteException {
    return (mo != null && matches(mo.getNamespaceURI(), mo.getLocalName()));
  }

  /**
   * Find out if the given MO matches this content type.
   * 
   * @param namespaceUri
   * @param localName
   * @return true when the provided namespace URI and local name match that of this enum value.
   * @throws RSuiteException
   */
  public boolean matches(String namespaceUri, String localName) throws RSuiteException {
    String nsUri1 = this.getQName().getNamespaceURI();
    return (ProjectNamespace.areNamespaceUrisEquivalent(nsUri1, namespaceUri) && this.getQName()
        .getLocalPart().equals(localName));
  }

  /**
   * Get the project QName associated with the given MO.
   * 
   * @param mo
   * @return The project QName associated with the given MO or null when there isn't one.
   * @throws RSuiteException
   */
  public static ProjectQName get(ManagedObject mo) throws RSuiteException {
    if (mo != null) {
      for (ProjectQName pqname : ProjectQName.values()) {
        if (pqname.matches(mo)) {
          return pqname;
        }
      }
    }
    return null;
  }

  /**
   * Find out if metadata from the given MO should be copied to a container.
   * 
   * @param mo
   * @return True when metadata from the MO should be copied.
   * @throws RSuiteException
   */
  public static boolean shouldCopyMetadataToContainer(ManagedObject mo) throws RSuiteException {
    ProjectQName pqname = get(mo);
    if (pqname != null) {
      return pqname.shouldCopyMetadataToContainer();
    }
    return false;
  }

}
