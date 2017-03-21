package org.astd.rsuite.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.constants.ProjectConstants;
import org.astd.rsuite.operation.result.OperationResult;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.reallysi.rsuite.api.Alias;
import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ManagedObjectReference;
import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.UserAgent;
import com.reallysi.rsuite.api.VersionEntry;
import com.reallysi.rsuite.api.VersionHistory;
import com.reallysi.rsuite.api.VersionSpecifier;
import com.reallysi.rsuite.api.VersionType;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisor;
import com.reallysi.rsuite.api.control.NonXmlObjectSource;
import com.reallysi.rsuite.api.control.ObjectCheckInOptions;
import com.reallysi.rsuite.api.control.ObjectInsertOptions;
import com.reallysi.rsuite.api.control.ObjectMetaDataSetOptions;
import com.reallysi.rsuite.api.control.ObjectRollbackOptions;
import com.reallysi.rsuite.api.control.ObjectSource;
import com.reallysi.rsuite.api.control.ObjectUpdateOptions;
import com.reallysi.rsuite.api.control.XmlObjectSource;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.extensions.Plugin;
import com.reallysi.rsuite.api.security.ACL;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.service.SecurityService;
import com.reallysi.rsuite.service.SessionService;
import com.rsicms.rsuite.utils.search.NameValuesPair;
import com.rsicms.rsuite.utils.search.SearchUtils;
import com.rsicms.rsuite.utils.xml.DomUtils;
import com.rsicms.rsuite.utils.xml.TransformUtils;

/**
 * A collection of static MO utility methods.
 */
public class MOUtils
    implements ProjectConstants {

  private static Log log = LogFactory.getLog(MOUtils.class);

  /**
   * Find out if the provided MO is a top-level MO.
   * 
   * @param moService
   * @param user
   * @param mo
   * @see #isSubMo(ManagedObjectService, User, ManagedObject)
   * @return True if a top-level MO; else, false. True returned for containers and top-level MOs,
   *         including non-XML MOs. Not sure about references.
   * @throws RSuiteException
   */
  public boolean isTopLevelMo(ManagedObjectService moService, User user, ManagedObject mo)
      throws RSuiteException {
    return !isSubMo(moService, user, mo);
  }

  /**
   * Find out if the provided MO is a sub-MO.
   * 
   * @param moService
   * @param user
   * @param mo
   * @see #isTopLevelMo(ManagedObjectService, User, ManagedObject)
   * @return True if a sub-MO; else, false. False returned for containers and top-level MOs,
   *         including non-XML MOs. Not sure about references.
   * @throws RSuiteException
   */
  public boolean isSubMo(ManagedObjectService moService, User user, ManagedObject mo)
      throws RSuiteException {
    return !mo.getId().equals(moService.getRootManagedObjectId(user, mo.getId()));
  }

  /**
   * Get the input stream of an MO, exposing options RSuite's ManagedObjectService does not.
   * 
   * @param context
   * @param mo
   * @param includeXMLDeclaration
   * @param includeDoctypeDeclaration
   * @param encoding
   * @return The MO's input stream, after applying options RSuite's API doesn't offer.
   * @throws RSuiteException
   * @throws UnsupportedEncodingException
   * @throws TransformerException
   */
  public static InputStream getInputStream(ExecutionContext context, ManagedObject mo,
      boolean includeXMLDeclaration, boolean includeDoctypeDeclaration, String encoding)
      throws RSuiteException, UnsupportedEncodingException, TransformerException {
    Element elem = mo.getElement();
    String str = DomUtils.serializeToString(context.getXmlApiManager().getTransformer((File) null),
        elem, includeXMLDeclaration, includeDoctypeDeclaration, encoding);
    return new ByteArrayInputStream(str.getBytes(encoding));
  }

  /**
   * Get a display name for the MO. Fails over to local name when display name is null.
   * 
   * @param mo
   * @return a display name for the MO.
   * @throws RSuiteException
   */
  public static String getDisplayName(ManagedObject mo) throws RSuiteException {
    return StringUtils.isBlank(mo.getDisplayName()) ? mo.getLocalName() : mo.getDisplayName();
  }

  /**
   * Get the basename alias for an MO if present.
   * 
   * @param mo
   * @return the basename alias for the MO if present, else return the empty string.
   * @throws RSuiteException
   */
  public static String getBasenameAlias(ManagedObject mo) throws RSuiteException {
    Alias[] basenameAliases = mo.getAliases("basename");
    if (basenameAliases != null && basenameAliases.length > 0) {
      return StringUtils.isBlank(mo.getAliases("basename")[0].getText()) ? "" : mo.getAliases(
          "basename")[0].getText();
    }
    return "";
  }

  /**
   * Get the qualified element name of an MO.
   * 
   * @param mo
   * @return the qualified element name.
   * @throws RSuiteException Thrown if unable to determine the given MO's qualified element name.
   */
  public static String getQualifiedElementName(ManagedObject mo) throws RSuiteException {
    StringBuilder sb = new StringBuilder();
    if (StringUtils.isNotBlank(mo.getNamespaceURI())) {
      sb.append(mo.getNamespaceURI()).append(":");
    }
    return sb.append(mo.getLocalName()).toString();
  }

  /**
   * Get the XML @id value of an MO.
   * 
   * @param mo
   * @return the @id value (or empty string if it does not exist)
   * @throws RSuiteException Thrown if unable to determine the given MO's @id value.
   */
  public static String getIdAttributeValue(ManagedObject mo) throws RSuiteException {
    return mo.getElement().getAttribute("id");
  }

  /**
   * Get a display name for the MO, without throwing an exception.
   * 
   * @param mo
   * @return a display name for the MO, or an empty string if an exception is encountered.
   */
  public static String getDisplayNameQuietly(ManagedObject mo) {
    try {
      return getDisplayName(mo);
    } catch (Exception e) {
      return StringUtils.EMPTY;
    }
  }

  /**
   * Get a managed object from a CA item.
   * 
   * @param context
   * @param user
   * @param caItem
   * @deprecated Use {@link #getManagedObject(ManagedObjectService, User, ContentAssemblyItem)}
   *             instead.
   * @return Container or null. Null return when the CA item is not a content assembly, content
   *         assembly reference, or CANode.
   * @throws RSuiteException
   */
  public static ManagedObject getManagedObject(ExecutionContext context, User user,
      ContentAssemblyItem caItem) throws RSuiteException {
    return getManagedObject(context.getManagedObjectService(), user, caItem);
  }

  /**
   * Get a managed object from a CA item.
   * 
   * @param moService
   * @param user
   * @param caItem
   * @return Container or null. Null return when the CA item is not a content assembly, content
   *         assembly reference, or CANode.
   * @throws RSuiteException
   */
  public static ManagedObject getManagedObject(ManagedObjectService moService, User user,
      ContentAssemblyItem caItem) throws RSuiteException {
    ManagedObject mo = null;
    if (caItem instanceof ManagedObject) {
      mo = (ManagedObject) caItem;
    } else if (caItem instanceof ManagedObjectReference) {
      mo = moService.getManagedObject(user, ((ManagedObjectReference) caItem).getTargetId());
    }
    return mo;
  }

  /**
   * Get an <code>ObjectSource</code> from a <code>File</code>.
   * 
   * @param context
   * @param filename
   * @param content
   * @param encoding Only used if the file is believed to be XML.
   * @return Either an instance of <code>XmlObjectSource</code> or <code>NonXmlObjectSource</code>.
   * @throws IOException Thrown if unable to get bytes from given file.
   */
  public static ObjectSource getObjectSource(ExecutionContext context, String filename,
      File content, String encoding) throws IOException {
    FileInputStream fis = new FileInputStream(content);
    try {
      return getObjectSource(context, filename, fis, encoding);
    } finally {
      IOUtils.closeQuietly(fis);
    }
  }

  /**
   * Get an <code>ObjectSource</code> from an <code>InputStream</code>.
   * 
   * @param context
   * @param filename
   * @param content
   * @param encoding Only used if the file is believed to be XML.
   * @return Either an instance of <code>XmlObjectSource</code> or <code>NonXmlObjectSource</code>.
   * @throws IOException Thrown if unable to get bytes from input stream.
   */
  public static ObjectSource getObjectSource(ExecutionContext context, String filename,
      InputStream content, String encoding) throws IOException {
    return getObjectSource(context, filename, IOUtils.toByteArray(content), encoding);
  }

  /**
   * Get an <code>ObjectSource</code> from a <code>String</code>.
   * 
   * @param context
   * @param filename
   * @param content
   * @param encoding Only used if the file is believed to be XML.
   * @return Either an instance of <code>XmlObjectSource</code> or <code>NonXmlObjectSource</code>.
   * @throws IOException Thrown if unable to get bytes from given file.
   */
  public static ObjectSource getObjectSource(ExecutionContext context, String filename,
      String content, String encoding) throws IOException {
    return getObjectSource(context, filename, IOUtils.toByteArray(new StringReader(content),
        encoding), encoding);
  }

  /**
   * Get an <code>ObjectSource</code> from a <code>byte</code> array.
   * 
   * @param context
   * @param filename
   * @param content
   * @param encoding Only used if the file is believed to be XML.
   * @return Either an instance of <code>XmlObjectSource</code> or <code>NonXmlObjectSource</code>.
   * @throws IOException Thrown if unable to get bytes from given file.
   */
  public static ObjectSource getObjectSource(ExecutionContext context, String filename,
      byte[] content, String encoding) throws IOException {
    if (context.getRSuiteServerConfiguration().isTreatAsXmlFileExtension(FilenameUtils.getExtension(
        filename))) {
      return new XmlObjectSource(content, encoding);
    } else {
      return new NonXmlObjectSource(content);
    }
  }

  /**
   * Get the insert options for the given object source and name.
   * 
   * @param context
   * @param objectSource
   * @param objectName
   * @param advisor A local MO advisor to use. May be null.
   * @return The insert options for either a new XML MO or non-XML MO.
   */
  public static ObjectInsertOptions getObjectInsertOptions(ExecutionContext context,
      ObjectSource objectSource, String objectName, ManagedObjectAdvisor advisor) {

    // In 4.1.12 an additional constructor was added to ObjectInsertOptions
    // There are constructors for Alias[] and String[] so a null no longer
    // works. Using String[] because the 4.1.9 constructor used String[].
    String[] aliases = null;

    ObjectInsertOptions insertOptions = new ObjectInsertOptions(objectName, aliases, // aliases:
                                                                                     // handled by
                                                                                     // MO advisor;
        null, // collections
        true, // force the document to be loaded?
        true); // validate?

    insertOptions.setAdvisor(advisor);

    insertOptions.setContentType(context.getConfigurationService().getMimeMappingCatalog()
        .getMimeTypeByExtension(FilenameUtils.getExtension(objectName)));

    if (objectSource instanceof NonXmlObjectSource) {
      insertOptions.setExternalFileName(objectName);
      insertOptions.setFileName(objectName);
      insertOptions.setDisplayName(objectName);
    }

    return insertOptions;

  }

  /**
   * Get the update options for the given object source and name.
   * 
   * @param context
   * @param objectSource
   * @param objectName
   * @param advisor
   * @return The update options for either an existing XML MO or non-XML MO.
   */
  public static ObjectUpdateOptions getObjectUpdateOptions(ExecutionContext context,
      ObjectSource objectSource, String objectName, ManagedObjectAdvisor advisor) {
    ObjectUpdateOptions options = new ObjectUpdateOptions();
    options.setExternalFileName(objectName);
    options.setDisplayName(objectName);
    options.setValidate(true);
    options.setAdvisor(advisor);
    return options;
  }

  /**
   * Determine if an MO, and optionally, its sub-MOs, are checked out.
   * 
   * @param context
   * @param user
   * @param id
   * @param includeSubMos Submit true to check the MO's sub-MOs.
   * @return True if the MO is checked out. When checkSubMos is true, may also return true when a
   *         sub MO is checked out.
   * @throws RSuiteException
   */
  public static boolean isCheckedOut(ExecutionContext context, User user, String id,
      boolean includeSubMos) throws RSuiteException {
    ManagedObjectService moService = context.getManagedObjectService();
    if (moService.isCheckedOut(user, id)) {
      return true;
    }
    if (includeSubMos) {
      ManagedObject mo = moService.getManagedObject(user, id);
      if (mo.hasChildren()) {
        for (ManagedObject subMo : mo.listDescendantManagedObjects()) {
          if (subMo.isCheckedout())
            return true;
        }
      }
    }
    return false;
  }

  /**
   * Cancel the check out of the specified MO. This method will attempt it even if the checkout is
   * not owned by the specified user; in that case, the user passed it will need to be an admin.
   * 
   * @param context
   * @param user
   * @param id
   * @throws RSuiteException
   */
  public static void cancelCheckout(ExecutionContext context, User user, String id)
      throws RSuiteException {

    ManagedObjectService moService = context.getManagedObjectService();
    ManagedObject mo = moService.getManagedObject(user, id);

    if (mo.isCheckedout()) {
      VersionSpecifier versionSpecifier = getCurrentVersionSpecifier(context, user, id);
      log.info("Current revision: " + versionSpecifier.getRevision());
      moService.rollback(user, versionSpecifier, new ObjectRollbackOptions());
    }

  }

  /**
   * Check out the MO, if able to. If already checked out by another user, an exception is thrown.
   * If already checked out to the specified user, no action is performed.
   * 
   * @param context
   * @param user
   * @param id
   * @return true if this method checked the MO out; false if the MO was already checked out to the
   *         specified user.
   * @throws RSuiteException
   */
  public static boolean checkout(ExecutionContext context, User user, String id)
      throws RSuiteException {
    ManagedObjectService moService = context.getManagedObjectService();
    if (!moService.isCheckedOut(user, id)) {
      moService.checkOut(user, id);
      return true;
    } else {
      if (moService.isCheckedOutButNotByUser(user, id)) {
        throw new RSuiteException(RSuiteException.ERROR_INTERNAL_ERROR, ProjectMessageResource
            .getMessageText("error.mo.checked.out.by.different.user", moService.getCheckOutInfo(id)
                .getUserId(), id));
      }
      return false;
    }
  }

  /**
   * Get a list of the MO's previous version entries, optionally excluding purged versions.
   * 
   * @param mo The MO to retrieve the previous versions of.
   * @param excludePurgedVersion Submit true to omit purged versions from the return.
   * @return The MO's previous version entries, conditionally excluding purged version entries.
   * @throws RSuiteException Bubbled up from RSuite.
   */
  public List<VersionEntry> getPreviousVersionEntries(ManagedObject mo,
      boolean excludePurgedVersion) throws RSuiteException {
    List<VersionEntry> previousEntries = new ArrayList<VersionEntry>();
    List<VersionEntry> allEntries = mo.getVersionHistory().getVersionEntries();

    // This code presumes entry with index 0 is the current version; skip it.
    VersionEntry ve;
    for (int i = 1; i < allEntries.size(); i++) {
      ve = allEntries.get(i);
      if (excludePurgedVersion && StringUtils.isNotBlank(ve.getPurgedByUserId())) {
        continue;
      }
      previousEntries.add(ve);
    }

    return previousEntries;
  }

  /**
   * Get the version specifier for the version of the MO that is immediately before the current
   * version.
   * 
   * @param context
   * @param user
   * @param id
   * @return Version specifier for the identified MO that immediately precedes the current version,
   *         or null if there is only one version of the MO.
   * @throws RSuiteException
   */
  public static VersionSpecifier getPreviousVersionSpecifier(ExecutionContext context, User user,
      String id) throws RSuiteException {

    VersionHistory vh = context.getManagedObjectService().getVersionHistory(user, id);

    if (vh.size() >= 2) {
      /*
       * Use the second entry in the list of versions, as the first (index=0) is the current
       * version.
       */
      return new VersionSpecifier(id, vh.getVersionEntries().get(1).getRevisionNumber());
    }

    // The current version is the latest version.
    return null;

  }

  /**
   * Get the MO's current version specifier. Handy for canceling a checkout.
   * 
   * @param context
   * @param user
   * @param id
   * @return Version specifier for the MO's current version.
   * @throws RSuiteException
   */
  public static VersionSpecifier getCurrentVersionSpecifier(ExecutionContext context, User user,
      String id) throws RSuiteException {

    return new VersionSpecifier(id, context.getManagedObjectService().getVersionHistory(user, id)
        .getVersionEntries().get(0).getRevisionNumber());

  }

  /**
   * Get all of the MO's LMD values for the given LMD name. This method does not de-dup the values.
   * 
   * @param mo
   * @param name
   * @return A list of zero or more qualifying LMD values. Will not be null.
   * @throws RSuiteException
   */
  public List<String> getRepeatingMetadataValues(ManagedObject mo, String name)
      throws RSuiteException {
    List<String> values = new ArrayList<String>();
    for (MetaDataItem mdItem : mo.getMetaDataItems()) {
      if (mdItem.getName().equals(name)) {
        values.add(mdItem.getValue());
      }
    }
    return values;
  }

  /**
   * Add or delete a specific name-value pair from the MO's LMD.
   * <p>
   * If LMD is versioned, caller is responsible for checking the MO out.
   * 
   * @param moService
   * @param user
   * @param mo
   * @param lmdName
   * @param lmdValue
   * @param add Submit true to add the LMD (when not present) and false to have it removed (if
   *        present).
   * @return True if metadata was added or deleted (as instructed); false if no adjustment was
   *         necessary.
   * @throws RSuiteException
   */
  public boolean adjustRepeatingMetadata(ManagedObjectService moService, User user,
      ManagedObject mo, String lmdName, String lmdValue, boolean add) throws RSuiteException {
    boolean adjusted = false;
    for (MetaDataItem mdItem : mo.getMetaDataItems()) {
      if (mdItem.getName().equals(lmdName) && mdItem.getValue().equals(lmdValue)) {
        if (add) {
          return false;
        }

        // Highly unlikely we'd have to remove the same name-value pair from the same MO; thus, not
        // bothering to find a way to remove all in one API call but will allow rest of metadata
        // entries to be reviewed.
        moService.removeMetaDataEntry(user, mo.getId(), mdItem);
        adjusted = true;
      }
    }

    if (add) {
      ObjectMetaDataSetOptions options = new ObjectMetaDataSetOptions();
      options.setAddNewItem(true);
      moService.addMetaDataEntry(user, mo.getId(), new MetaDataItem(lmdName, lmdValue), options);
      adjusted = true;
    }
    return adjusted;
  }

  /**
   * Set metadata on the specified MO, starting from an array of name-values pairs. Supports
   * repeating LMD.
   * 
   * @param user
   * @param moService
   * @param moid
   * @param metadataPairArr Required as an array to avoid erasure error with
   *        {@link #setMetadataEntries(User, ManagedObjectService, String, List)}.
   * @throws RSuiteException
   */
  public static void setMetadataEntries(User user, ManagedObjectService moService, String moid,
      NameValuesPair[] metadataPairArr) throws RSuiteException {
    if (metadataPairArr != null && metadataPairArr.length > 0) {
      List<MetaDataItem> mdList = new ArrayList<MetaDataItem>();
      for (NameValuesPair pairing : metadataPairArr) {
        for (String value : pairing.getValues()) {
          mdList.add(new MetaDataItem(pairing.getName(), value));
        }
      }
      setMetadataEntries(user, moService, moid, mdList);
    }
  }

  /**
   * Set metadata entries for the given moid.
   * 
   * @param user
   * @param moService
   * @param moid
   * @param metaDataItems
   * @throws RSuiteException
   */
  public static void setMetadataEntries(User user, ManagedObjectService moService, String moid,
      List<MetaDataItem> metaDataItems) throws RSuiteException {
    moService.setMetaDataEntries(user, moid, metaDataItems);
  }

  /**
   * Delete metadata from an MO by metadata name. If the metadata repeats, all metadata items with
   * the specified metadata name will be deleted.
   * <p>
   * IMPROVE: This method could be optimized for repeating LMD by calling
   * ManagedObjectService#processMetaDataChangeSet().
   * 
   * @param user
   * @param moService
   * @param moid
   * @param lmdName
   * @throws RSuiteException
   */
  public static void deleteMetadataEntries(User user, ManagedObjectService moService, String moid,
      String lmdName) throws RSuiteException {
    if (StringUtils.isNotBlank(moid) && StringUtils.isNotBlank(lmdName)) {
      ManagedObject mo = moService.getManagedObject(user, moid);
      if (mo != null) {
        for (MetaDataItem mdItem : mo.getMetaDataItems()) {
          if (mdItem.getName().equals(lmdName)) {
            moService.removeMetaDataEntry(user, moid, mdItem);
          }
        }
      }
    }
  }

  /**
   * @param user
   * @param moService
   * @param moid
   * @param lmdName
   * @return Metadatitem that matches lmdName or null
   * @throws RSuiteException
   */
  public static MetaDataItem getMetadataEntry(User user, ManagedObjectService moService,
      String moid, String lmdName) throws RSuiteException {
    if (StringUtils.isNotBlank(moid) && StringUtils.isNotBlank(lmdName)) {
      ManagedObject mo = moService.getManagedObject(user, moid);
      if (mo != null) {
        for (MetaDataItem mdItem : mo.getMetaDataItems()) {
          if (mdItem.getName().equals(lmdName)) {
            return mdItem;
          }
        }
      }
    }
    return null;
  }

  /**
   * Load a RSuite <code>ManagedObject</code>.
   * 
   * @param context
   * @param user
   * @param filename
   * @param is
   * @param moAdvisor
   * @return The <code>ManagedObject</code> loaded in RSuite.
   * @throws IOException
   * @throws RSuiteException
   */
  public static ManagedObject load(ExecutionContext context, User user, String filename,
      InputStream is, ManagedObjectAdvisor moAdvisor) throws IOException, RSuiteException {
    return load(context, user, filename, getObjectSource(context, filename, is,
        DEFAULT_CHARACTER_ENCODING), moAdvisor);
  }

  /**
   * Create a new managed object in RSuite.
   * 
   * @param context
   * @param user
   * @param filename
   * @param objectSource
   * @param moAdvisor
   * @return The <code>ManagedObject</code> loaded in RSuite.
   * @throws RSuiteException
   */
  public static ManagedObject load(ExecutionContext context, User user, String filename,
      ObjectSource objectSource, ManagedObjectAdvisor moAdvisor) throws RSuiteException {
    return context.getManagedObjectService().load(user, objectSource, getObjectInsertOptions(
        context, objectSource, filename, moAdvisor));
  }

  /**
   * Find out if an MO has the specified QName.
   * 
   * @param mo
   * @param qname
   * @return True if the MO is an XML MO with the specified QName.
   * @throws RSuiteException
   */
  public static boolean hasMatchingQName(ManagedObject mo, QName qname) throws RSuiteException {
    return (mo != null && !mo.isNonXml() && mo.getLocalName().equals(qname.getLocalPart()) && (
    // If both are blank, they're both in the default namespace
    (StringUtils.isBlank(mo.getNamespaceURI()) && StringUtils.isBlank(qname.getNamespaceURI()))
        || mo.getNamespaceURI().equals(qname.getNamespaceURI())));
  }

  /**
   * Apply a transform to an MO and update the same MO with the tranform's result.
   * <p>
   * This method takes care of creating and destroying a session. If you already have a session, use
   * the signature that accepts <code>Session</code> instead of <code>User</code>.
   * 
   * @param context
   * @param user The user to operate as, and create a session for.
   * @param mo The managed object to check out (when not already checked out), apply the transform
   *        to, update with the transform result, and check back in.
   * @param xslUri URI of the XSL to apply.
   * @param xslParams Optional parameters to pass into the XSL. Null may be sent in. Hint: List
   *        <String> parameters are received as a sequence, at least with Saxon.
   * @param includeStandardRSuiteXslParams Submit true to ensure XSLT parameters that RSuite
   *        typically provides are included herein, specifically including the base RSuite URL and a
   *        session key.
   * @param baseRSuiteUrl Only used when includeStandardRSuiteXslParams is true.
   * @param versionNote The new MO version's note.
   */
  public static void applyTransformAndUpdate(ExecutionContext context, User user, ManagedObject mo,
      URI xslUri, Map<String, Object> xslParams, boolean includeStandardRSuiteXslParams,
      String baseRSuiteUrl, String versionNote)
      throws RSuiteException, URISyntaxException, TransformerException, SAXException, IOException {
    SessionService sessionService = context.getSessionService();
    Session session = sessionService.createSession(PLUGIN_ID_HOST, new UserAgent(
        "background-transform"), baseRSuiteUrl, user);
    try {
      applyTransformAndUpdate(context, session, mo, xslUri, xslParams,
          includeStandardRSuiteXslParams, baseRSuiteUrl, versionNote);
    } finally {
      if (session != null) {
        sessionService.removeSession(session.getKey());
      }
    }
  }

  /**
   * Apply a transform to an MO and update the same MO with the tranform's result.
   * <p>
   * If you don't already have a session, use the signature that accepts <code>User</code> instead
   * of <code>Session</code>.
   * 
   * @param context
   * @param session A valid session that identifies the user to operate as. The session's key is
   *        also passed in as an XSL parameter when includeStandardRSuiteXslParams is true.
   * @param mo The managed object to check out (when not already checked out), apply the transform
   *        to, update with the transform result, and check back in.
   * @param xslUri URI of the XSL to apply.
   * @param xslParams Optional parameters to pass into the XSL. Null may be sent in. Hint: List
   *        <String> parameters are received as a sequence, at least with Saxon.
   * @param includeStandardRSuiteXslParams Submit true to ensure XSLT parameters that RSuite
   *        typically provides are included herein, specifically including the base RSuite URL and a
   *        session key.
   * @param baseRSuiteUrl Only used when includeStandardRSuiteXslParams is true.
   * @param versionNote The new MO version's note.
   * @throws RSuiteException
   * @throws URISyntaxException
   * @throws TransformerException
   * @throws SAXException
   * @throws IOException
   */
  public static void applyTransformAndUpdate(ExecutionContext context, Session session,
      ManagedObject mo, URI xslUri, Map<String, Object> xslParams,
      boolean includeStandardRSuiteXslParams, String baseRSuiteUrl, String versionNote)
      throws RSuiteException, URISyntaxException, TransformerException, SAXException, IOException {
    User user = session.getUser();
    ManagedObjectService moService = context.getManagedObjectService();
    boolean createdCheckOut = false;
    InputStream transformResult = null;
    try {
      // Make sure the user has the check out.
      createdCheckOut = checkout(context, user, mo.getId());

      // Perform transform
      transformResult = new TransformUtils().iTransform(context, session, mo, context
          .getXmlApiManager().getTransformer(xslUri), xslParams, includeStandardRSuiteXslParams,
          baseRSuiteUrl);

      // Update the MO
      ObjectSource objectSource = getObjectSource(context, "file.xml", // Only the file extension
                                                                       // matters here.
          transformResult, DEFAULT_CHARACTER_ENCODING);
      moService.update(user, mo.getId(), objectSource, getObjectUpdateOptions(context, objectSource,
          "bogus object name", // not important for
                               // XML MOs.
          null)); // local MO advisor

      // Check in the MO
      ObjectCheckInOptions checkInOptions = new ObjectCheckInOptions();
      checkInOptions.setVersionType(VersionType.MINOR);
      checkInOptions.setVersionNote(versionNote);
      moService.checkIn(user, mo.getId(), checkInOptions);
    } finally {
      // If this method checked the MO out and it is still checked out, cancel it.
      if (createdCheckOut && moService.isCheckedOutAuthor(user, mo.getId())) {
        moService.undoCheckout(user, mo.getId());
      }

      IOUtils.closeQuietly(transformResult);
    }
  }

  /**
   * Set the ACL on a managed object and, optionally, its sub-MOs.
   * 
   * @param context
   * @param user
   * @param mo
   * @param includeSubMos Submit true to also apply the ACL to the MO's sub-MOs.
   * @param acl
   * @param opResult
   * @param messageKey
   * @throws RSuiteException
   */
  public static void setACL(ExecutionContext context, User user, ManagedObject mo,
      boolean includeSubMos, ACL acl, OperationResult opResult, String messageKey)
      throws RSuiteException {
    SecurityService securityService = context.getSecurityService();
    securityService.setACL(user, mo.getId(), acl);
    opResult.addInfoMessage(ProjectMessageResource.getMessageText(messageKey, MOUtils
        .getDisplayName(mo), mo.getId()));
    if (includeSubMos && mo.hasChildren()) {
      for (ManagedObject subMo : mo.listDescendantManagedObjects()) {
        securityService.setACL(user, subMo.getId(), acl);
        opResult.addInfoMessage(ProjectMessageResource.getMessageText(messageKey, MOUtils
            .getDisplayName(subMo), subMo.getId()));
      }
    }
  }

  /**
   * List the descendant MO IDs of the identified MO, optionally including that MO. This is pretty
   * much the same as (and uses) ManagedObjectService#listDescendantManagedObjects() but it just
   * returns the IDs.
   * 
   * @param context
   * @param user
   * @param id
   * @param includeSelf
   * @return A list of descendant MO IDs, optionally including the specified ID. The list can be
   *         empty, but not null.
   * @throws RSuiteException
   */
  public static List<String> listDescendantManagedObjectIds(ExecutionContext context, User user,
      String id, boolean includeSelf) throws RSuiteException {
    List<String> ids = new ArrayList<String>();
    if (includeSelf) {
      ids.add(id);
    }
    List<ManagedObject> moList = context.getManagedObjectService().getManagedObject(user, id)
        .listDescendantManagedObjects();
    for (ManagedObject mo : moList) {
      ids.add(mo.getId());
    }
    return ids;
  }

  /**
   * Remove all metadata items on a given MO that match a certain name
   * 
   * @param moService
   * @param user
   * @param moId
   * @param mdis List<MetaDataItem> of mdis on a given MO
   * @param lmdName The name of the lmd field to delete
   * @throws RSuiteException
   */
  public void removeAllMetadataItemsByName(ManagedObjectService moService, User user, String moId,
      List<MetaDataItem> mdis, String lmdName) throws RSuiteException {
    for (MetaDataItem curMdi : mdis) {
      if (lmdName.equals(curMdi.getName())) {
        moService.removeMetaDataEntry(user, moId, curMdi);
      }
    }
  }
  
  /**
   * Update an MO with new content provided by the user.
   * The content is updated as a draft version, not checked-in.
   * 
   * @param user
   * @param moService
   * @param moId
   * @param objectSource
   * @throws RSuiteException
   */
  public static void updateMOWithoutCheckin(
          User user,
          ManagedObjectService moService,
          String moId,
          ObjectSource objectSource)
  throws RSuiteException { 
      
      if (!moService.isCheckedOutAuthor(user, moId))
          moService.checkOut(user, moId);
      moService.update(
              user, 
              moId, 
              objectSource, 
              new ObjectUpdateOptions());
  }
  
  /**
   * Locate and read a resource in a given folder within the main plugin, and returns the inputstream.
   * The caller is required to specify from which folder it wants to read the resource.
   * 
   * @param context
   * @param folderName Plugin's folder name.
   * @param resourceName Plugin's resource name.
   * @return InputStream Inputstream of the object read from the plugin.
   */
  public static InputStream loadMoFromInternalResource(
      ExecutionContext context,
      String folderName,
      String resourceName) {
    
    Plugin plugin = context.getPluginManager().get(PLUGIN_ID_HOST);    
    File pluginPath = plugin.getLocation();
    ZipFile zipFile = null;
    try {
      zipFile = new ZipFile(pluginPath.getAbsolutePath());
      final Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
          final ZipEntry entry = entries.nextElement();
          if (entry.getName().contains(folderName) && 
                  entry.getName().contains(resourceName)) {
              
            return plugin.getResourceAsStream(entry.getName());            
          }
      }
    } catch(IOException ioEx) {
      log.error("Unable to read the requested resource in the plugin.",ioEx);
    } finally {
      try {
        zipFile.close();
      } catch (IOException e) {
        log.error("Failed to read zip file: " + e);
      }
    }
    
    return null;
  }
  
  /**
   * Check if exists a ManagedObject in the repository with the same metadata name and value assigned.
   * 
   * @param context
   * @param metadataName
   * @param metadataValue
   * @param localName
   * @return boolean
   * @throws RSuiteException
   */
  public static boolean existsManagedObjectWithMetadata(
      ExecutionContext context,
      String metadataName,
      String metadataValue,
      String localName) throws RSuiteException {
    
    NameValuesPair pair = new NameValuesPair(metadataName, metadataValue);
    QName qname = new QName(localName);

    return SearchUtils.searchForManagedObjects(
        context.getAuthorizationService().getSystemUser(), 
        context.getSearchService(), 
        qname, 
        false, 
        pair, 
        0).size() > 0;
  }
}
