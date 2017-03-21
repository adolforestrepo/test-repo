package org.astd.rsuite.utils.mo;

import javax.xml.namespace.QName;

import org.astd.rsuite.utils.MOUtils;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;

/**
 * An managed object qualifier that accepts XML MOs with a specified QName.
 */
public class QNameManagedObjectQualifier
    implements ManagedObjectQualifier {

  private QName qname;

  public QNameManagedObjectQualifier(
      QName qname) {
    this.qname = qname;
  }

  @Override
  public boolean accept(ManagedObject mo) throws RSuiteException {
    return MOUtils.hasMatchingQName(mo, qname);
  }

}
