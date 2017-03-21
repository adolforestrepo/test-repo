package org.astd.rsuite.domain;

import org.apache.commons.lang3.StringUtils;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.xml.Namespace;
import com.reallysi.rsuite.api.xml.XPathEvaluator;
import com.reallysi.rsuite.service.XmlApiManager;
import com.rsicms.rsuite.utils.xml.XPathUtils;

/**
 * All known namespaces used by the Java code base.
 * <p>
 * Exceptions from the 2014 code base may not have been eliminated yet; please do so, as you run
 * across them.
 */
public enum ProjectNamespace {

  DITA(StringUtils.EMPTY, StringUtils.EMPTY),
  DELTAXML("http://www.deltaxml.com/ns/well-formed-delta-v1", "deltaxml"),
  XS("http://www.w3.org/2001/XMLSchema", "xs");

  private Namespace namespace;

  private ProjectNamespace(
      String namespaceUri, String prefix) {
    this.namespace = new Namespace(namespaceUri, prefix);
  }

  public Namespace getNamespace() {
    return namespace;
  }

  /**
   * Find out if the given project namespace is in the same namespace as this one.
   * <p>
   * Since there are multiple enum values in the default namespace, comparing two enum values for
   * equality may not suffice.
   * 
   * @param projectNamespace
   * @return true if given project namespace is in the same one as this one.
   */
  public boolean isInSameNamespace(ProjectNamespace projectNamespace) {
    if (projectNamespace != null) {
      String nsUri1 = projectNamespace.getNamespace().getUri();
      for (ProjectNamespace val : ProjectNamespace.values()) {
        String nsUri2 = val.getNamespace().getUri();
        if (areNamespaceUrisEquivalent(nsUri1, nsUri2)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Find out if two namespace URIs are equivalent. Null and an empty string are considered
   * equivalent.
   * 
   * @param nsUri1
   * @param nsUri2
   * @return true if they are equivalents.
   */
  public static boolean areNamespaceUrisEquivalent(String nsUri1, String nsUri2) {
    return (StringUtils.isBlank(nsUri1) && StringUtils.isBlank(nsUri2)) || (nsUri1 != null && nsUri1
        .equals(nsUri2));
  }

  /**
   * Get the project namespace associated to the given MO.
   * <p>
   * Warning: since multiple enum values are in the default namespace, calls may not be able to
   * compare the return of this method to a specific enum value. Instead, use
   * {@link #isInSameNamespace(ProjectNamespace)}.
   * 
   * @param mo
   * @return The project namespace the MO is in, or null.
   * @throws RSuiteException
   */
  public static ProjectNamespace get(ManagedObject mo) throws RSuiteException {
    if (mo != null) {
      String nsUri1 = mo.getNamespaceURI();
      for (ProjectNamespace val : ProjectNamespace.values()) {
        String nsUri2 = val.getNamespace().getUri();
        if (areNamespaceUrisEquivalent(nsUri1, nsUri2)) {
          return val;
        }
      }
    }
    return null;
  }

  /**
   * Get a project namespace for the given MO restricted to the provided subset.
   * <p>
   * This is a way to avoid getting an unwanted enum value that is in the default namespace, when
   * the one that you want could also be in the default namespace.
   * 
   * @param mo
   * @param allowedSubset
   * @return A project namespace matching one from the allowed subset, or null.
   * @throws RSuiteException
   */
  public static ProjectNamespace getFromSpecifiedSubset(ManagedObject mo,
      ProjectNamespace... allowedSubset) throws RSuiteException {
    if (allowedSubset != null && allowedSubset.length > 0) {
      ProjectNamespace projectNamespace = get(mo);
      if (projectNamespace != null) {
        for (ProjectNamespace candidate : allowedSubset) {
          if (projectNamespace.isInSameNamespace(candidate)) {
            return projectNamespace;
          }
        }
      }
    }
    return null;
  }

  /**
   * Get an XPath evaluator that is aware of all of the namespaces in this enum.
   * 
   * @param xmlApiManager
   * @return XPath evaluator.
   * @throws RSuiteException
   */
  public static XPathEvaluator getXPathEvaluator(XmlApiManager xmlApiManager)
      throws RSuiteException {
    Namespace[] arr = new Namespace[ProjectNamespace.values().length];
    int i = 0;
    for (ProjectNamespace val : ProjectNamespace.values()) {
      arr[i++] = val.getNamespace();
    }
    return new XPathUtils().getXPathEvaluator(xmlApiManager, arr);
  }

}
