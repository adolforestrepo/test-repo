package org.astd.rsuite.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.VersionType;
import com.reallysi.rsuite.api.control.XmlObjectSource;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.service.RepositoryService;

/**
 * Utilities for creating folders and content assemblies in the repository within the context of a
 * workflow.
 */
public class ProjectAstdActionUtils {
  /**
   * Create a new CA.
   * <p>
   * This method will create any folder path components if <var>parentUri</var> defines a path does
   * not exist.
   * </p>
   * 
   * @param repositoryService Repository service.
   * @param user User making the request.
   * @param ca Name of new CA.
   * @param parentUri Location to create CA.
   * @param executionContext Workflow execution context.
   * @return Moid of CA created, or the existing CA if it already exists.
   * @throws RSuiteException If an error occurs.
   */

  public static String getMoText(ManagedObject mo, String encoding) throws RSuiteException {
    int len = (int) mo.getContentSize();
    if (len <= 0) {
      len = 4096;
    }
    if (encoding == null) {
      encoding = "UTF-8";
    }
    StringBuilder buf = new StringBuilder(len);
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(mo.getInputStream(), encoding));
      String line;
      while ((line = reader.readLine()) != null) {
        buf.append(line);
      }
    } catch (IOException ioe) {
      throw new RSuiteException("Error reading MO: " + ioe);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (Exception e) {
        }
      }
    }
    return buf.toString();
  }

  static final String CLASSIFICATION_GI = "classification";
  static final String CLASSIFICATION_OTAG = "<" + CLASSIFICATION_GI + ">";
  static final String CLASSIFICATION_ETAG = "</" + CLASSIFICATION_GI + ">";

  /**
   * Retrieve the taxonomy classification content for a given MO.
   * <p>
   * At this time, this function assumes the classifications is stored in the &lt;classification&gt;
   * child element of the &lt;prolog&gt; descendent of the root element of the MO.
   * </p>
   * <p>
   * TODO: Modify code to key off of DITA type values instead of element names.
   * </p>
   * <p>
   * The classification XML content will be wrapped in a &lt;RESULT&gt; element.
   * </p>
   * 
   * @param context Execution context.
   * @param log Log to use to print any log messages.
   * @param moid ID of MO to get classification data of.
   * @return XML string containing classification data.
   * @throws RSuiteException If an error occurs retrieving data.
   */
  public static String getClassificationXmlOfMo(ExecutionContext context, Log log, String moid)
      throws RSuiteException {
    RepositoryService rs = context.getRepositoryService();
    String moPath = rs.getXPathToObject(moid);
    StringBuilder buf = new StringBuilder(256);
    buf.append("<RESULT>").append("{").append(moPath).append("/prolog/").append(CLASSIFICATION_GI)
        .append("}").append("</RESULT>");
    log.info("Executing query: " + buf);
    String results = context.getRepositoryService().queryAsString(buf.toString());
    log.info("Query results: " + results);
    return results;
  }

  private static String[] postPrologCandidates = {"<subsection", "<sidebar", "<interview-question",
      "<glossentry", "<glossgroup"};

  /**
   * Set &lt;classification&gt; for given MO.
   * 
   * @param context Execution context.
   * @param user User to perform operations under.
   * @param log Log to use for logging messages.
   * @param moid ID of MO to set.
   * @param xml &lt;classification&gt element and its content.
   * @throws RSuiteException if an error occurs.
   */
  public static void setClassificationXmlForMo(ExecutionContext context, User user, Log log,
      String moid, String xml) throws RSuiteException {
    ManagedObjectService mos = context.getManagedObjectService();
    ManagedObject mo = mos.getManagedObject(user, moid);
    setClassificationXmlForMo(context, user, log, mo, xml);
  }

  /**
   * Set &lt;classification&gt; for given MO.
   * 
   * @param context Execution context.
   * @param user User to perform operations under.
   * @param log Log to use for logging messages.
   * @param mo MO to set.
   * @param xml &lt;classification&gt element and its content.
   * @throws RSuiteException if an error occurs.
   */
  public static void setClassificationXmlForMo(ExecutionContext context, User user, Log log,
      ManagedObject mo, String xml) throws RSuiteException {
    ManagedObjectService mos = context.getManagedObjectService();
    String moid = mo.getId();
    if (mo.isNonXml()) {
      throw new RSuiteException("Cannot set classification for non-xml object");
    }
    boolean doCheckIn = false;
    if (!mo.isCheckedout()) {
      log.info("Checking out MO (" + moid + ") under user " + user.getUserId());
      mos.checkOut(user, moid);
      doCheckIn = true;
    }
    log.info("Classification xml: " + xml);

    String moXml = getMoText(mo, "UTF-8");
    StringBuilder newXml = new StringBuilder(moXml.length() + xml.length() + 20);

    // XXX: Using basic string manipulation to update element. There is
    // some risk to this since comment declarations could throw things
    // off, but seems a waste of resource to build an entire DOM to do
    // what we want. Ideally RepositoryService would provide most
    // efficient method, but it is considered a "backdoor" and bypasses
    // RSuite application layer, which could cause problem, if not now,
    // in the future.
    int start = moXml.indexOf("<prolog");
    if (start < 0) {
      // No <prolog>, so we need to add it.
      log.info("MO has no <prolog>, trying to add...");
      start = moXml.indexOf("<body");
      if (start < 0) {
        start = moXml.indexOf("<related-links");
        if (start < 0) {
          int i, n;
          for (i = 0; i < postPrologCandidates.length; ++i) {
            start = moXml.indexOf(postPrologCandidates[i]);
            if (start >= 0) {
              log.info(postPrologCandidates[i] + " is current insertion candidate");
              break;
            }
          }
          if (i >= postPrologCandidates.length) {
            throw new RSuiteException("Cannot find insertion point for <prolog>" + " for MO ("
                + moid + ")");
          }
          for (; i < postPrologCandidates.length; ++i) {
            n = moXml.indexOf(postPrologCandidates[i]);
            if (n >= 0 && n < start) {
              log.info(postPrologCandidates[i] + " is current insertion candidate");
              start = n;
            }
          }
        }
      }
      newXml.append(moXml.substring(0, start));
      newXml.append("<prolog>");
      newXml.append(xml);
      newXml.append("</prolog>");
      newXml.append(moXml.substring(start));

    } else {
      int end = moXml.indexOf("</prolog", start);
      if (end < 0) {
        // Have an empty prolog tag
        end = moXml.indexOf("/>", start);
        if (end < 0) {
          // Should not get here(?)
          throw new RSuiteException("MO (" + moid + ") has malformed <prolog>");
        }
        newXml.append(moXml.substring(0, end));
        newXml.append('>');
        newXml.append(xml);
        newXml.append("</prolog>");
        newXml.append(moXml.substring(end + 2));

      } else {
        int n = moXml.indexOf("<" + CLASSIFICATION_GI);
        if (n < 0 || n > end) {
          // No classification element, or at least not in prolog.
          // Content model allows classification element at end
          // of prolog child list, so stick before prolog etag.
          newXml.append(moXml.substring(0, end));
          newXml.append(xml);
          newXml.append(moXml.substring(end));
        } else {
          int m = moXml.indexOf(CLASSIFICATION_ETAG, n);
          int skip = CLASSIFICATION_ETAG.length();
          if (m < 0) {
            m = moXml.indexOf("/>", n);
            skip = 2;
          }
          newXml.append(moXml.substring(0, n));
          newXml.append(xml);
          newXml.append(moXml.substring(m + skip));
        }
      }
    }
    try {
      log.info("Updating MO (" + moid + ") with new classification...");
      // log.info("New XML: "+newXml);
      mos.update(user, moid, new XmlObjectSource(newXml.toString()), null);
    } catch (Exception rse) {
      if (doCheckIn) {
        log.error("Error updating MO (" + moid + "): " + rse + " [attempting to rollback...]");
        try {
          mos.rollback(user, moid);
        } catch (Exception rse2) {
          log.error("Unable to rollback MO do to earlier error: " + rse2, rse2);
        }
      }
      if (rse instanceof RSuiteException) {
        throw (RSuiteException) rse;
      }
      throw new RSuiteException(RSuiteException.ERROR_INTERNAL_ERROR, rse.getLocalizedMessage(),
          rse);
    }

    if (doCheckIn) {
      log.info("Checking in MO (" + moid + ") under user " + user.getUserId());
      mos.checkIn(user, moid, VersionType.MINOR, "Classification change", true);
    }
  }
}
