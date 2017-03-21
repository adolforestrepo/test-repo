package org.astd.rsuite.workflow.actions.leaving;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.activiti.engine.delegate.Expression;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.astd.rsuite.domain.SecurityUtils;
import org.astd.rsuite.utils.ContainerUtils;
import org.astd.rsuite.utils.ProjectUtils;
import org.astd.rsuite.utils.UserUtils;
import org.astd.rsuite.workflow.actions.leaving.rsuite5.TempWorkflowConstants;
import org.w3c.dom.Element;

import com.reallysi.rsuite.api.Alias;
import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ObjectType;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.UserAgent;
import com.reallysi.rsuite.api.VersionType;
import com.reallysi.rsuite.api.control.ContentAssemblyItemFilter;
import com.reallysi.rsuite.api.control.ObjectCheckInOptions;
import com.reallysi.rsuite.api.control.ObjectInsertOptions;
import com.reallysi.rsuite.api.control.ObjectSource;
import com.reallysi.rsuite.api.control.ObjectUpdateOptions;
import com.reallysi.rsuite.api.control.XmlObjectSource;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.BusinessRuleException;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.api.xml.LoggingSaxonMessageListener;
import com.reallysi.tools.dita.DitaMapContainerItemFilter;
import com.reallysi.tools.dita.DitaUtil;
import com.reallysi.tools.dita.conversion.ToDitaTransformationOptions;
import com.reallysi.tools.dita.conversion.TransformationOptions;
import com.reallysi.tools.dita.conversion.beans.TransformSupportBean;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;

import net.sf.saxon.s9api.Serializer;

/*
 * Copied from rsuite-dita-support's
 * com.reallysi.rsuite.workflow.actions.EnsureMapForPublicationActionHandler Changes: -Added
 * getMapName() to provide custom map naming logic and replaced the call to caNode.displayName()
 * with it -Added getUserFromWorkflowVariable() to get the user executing the workflow so we do not
 * have to use the system user -Added a try/catch around the update() in loadResultToRSuite()
 * otherwise it was possible to have an exception bubble up and cause the workflow to explode *
 */

/**
 * When applied to a content assembly node, finds the ancestor or self publication node and then
 * checks to see if there is already a DITA map with the expected filename alias. If there is not,
 * calls the specified transform to generate the map. If there is, uses it.
 * <p>
 * If applied to an XML MO that is a DITA map, simply uses that map.
 * </p>
 * <p>
 * If applied to any other type of MO, throws an exception.
 * </p>
 *
 *
 */
public class EnsureMapActionHandler
    extends BaseWorkflowAction
    implements TempWorkflowConstants {

  protected Expression forceNewMapFromWorkflow;
  protected Expression xsltUriFromWorkflow;

  public static final String RSUITE_USER_ID_VAR_NAME = "rsuiteUserId";
  /**
   * If set to "true" or "yes", forces recreation of maps for content assemblies even if there is an
   * existing map. Default is false (don't force).
   */
  public static final String FORCE_NEW_MAP_PARAM = "forceNewMap";

  private static final long serialVersionUID = 1L;

  /**
   * URI of the XSLT transform to apply to the CA. If not specified, the default transform is used.
   */
  public static final String XSLT_URI_PARAM = "xsltUri";

  /**
   * MO ID of the CA node to be transformed into a map.
   */
  public static final String CA_NODE_ID_PARAM = "caNodeId";


  public static final String DEFAULT_CA_TO_MAP_TRANSFORM_URL =
      "rsuite:/res/plugin/rsuite-dita-support/canode2map/canode2map_shell.xsl";

  /**
   * Controls debugging messages in the transform. Set to "true" turn debugging messages on.
   */
  public static final String DEBUG_PARAM = "debug";

  /**
   * Instance of container utils this web service is to use.
   */
  protected ContainerUtils containerUtils;

  /**
   * Get the instance of container utils this web service is to use.
   * <p>
   * Makes some unit tests possible.
   * 
   * @return The instance of container utils this web service is to use.
   */
  public ContainerUtils getContainerUtils() {
    if (containerUtils == null) {
      containerUtils = new ContainerUtils();
    }
    return containerUtils;
  }

  @Override
  public void execute(WorkflowContext context) throws Exception {
    Log wfLog = context.getWorkflowLog();
    wfLog.info("Starting...");

    context.setVariable(EXCEPTION_OCCUR, false);
    ManagedObject mapMo = null;
    MoListWorkflowObject mosToProcess = context.getMoListWorkflowObject();
    if (mosToProcess.isEmpty()) {
      // No context object. There might be a caNodeId parameter.
      // If not, processing will fail.
      mapMo = handleContainer(context, wfLog);
    } else {
      // MO could be a container or a map.
      for (MoWorkflowObject moWFO : mosToProcess.getMoList()) {
        String moId = moWFO.getMoid();
        wfLog.info("Requesting object with ID " + moId);
        ManagedObject candMo = context.getManagedObjectService().getManagedObject(
            getUserFromWorkflowVariable(context), moId);
        wfLog.info("null ?= " + (candMo == null));
        if (candMo.isNonXml())
          continue;
        if (DitaUtil.isDitaMap(candMo.getElement())) {
          mapMo = candMo;
          break;
        }
        if (candMo.isAssemblyNode()) {
          mapMo = handleContainer(context, wfLog);
          break;
        }
      }
    }

    if (mapMo == null) {
      context.setVariable(EXCEPTION_OCCUR, true);
      throw new BusinessRuleException("No DITA map in the workflow context. Cannot continue.");
    }

    wfLog.info("Setting object " + RSuiteUtils.formatMoId(mapMo) + " as workflow context object.");
    List<MoWorkflowObject> moList = new ArrayList<MoWorkflowObject>();
    moList.add(new MoWorkflowObject(mapMo.getId()));
    context.setMoListWorkflowObject(new MoListWorkflowObject(moList));

    wfLog.info("Done.");
  }

  // Needed ability to mock.
  @Override
  public String getParameterWithDefault(String name, String defaultValue) {
    return super.getParameterWithDefault(name, defaultValue);
  }

  // Needed ability to mock.
  @Override
  public String resolveVariables(String text) throws RSuiteException {
    return super.resolveVariables(text);
  }

  /**
   * @return array list of MOs in the default (anonymous) MoWorkflowObject. Returns an empty list if
   *         the MoWorkflowObject is null.
   */
  protected ArrayList<MoWorkflowObject> getMOsToProcess(WorkflowContext context) {
    MoListWorkflowObject moWFO = context.getMoListWorkflowObject();
    if (moWFO == null || moWFO.getMoList() == null)
      return new ArrayList<MoWorkflowObject>();
    ArrayList<MoWorkflowObject> arrayList = new ArrayList<MoWorkflowObject>(moWFO.getMoList());
    return arrayList;
  }

  /**
   * Gets the value of a workflow variable / parameter.
   * 
   * @param context
   * @return the value.
   */
  protected String getWorkflowVariableOrParameter(WorkflowContext context,
      String workflowVariableOrParameterName, Expression workflowExpression) {
    String workflowVarOrParam = context.getVariableAsString(workflowVariableOrParameterName);
    if (StringUtils.isNotEmpty(workflowVarOrParam)) {
      return workflowVarOrParam;
    }
    workflowVarOrParam = resolveVariablesAndExpressions(getParameter(
        workflowVariableOrParameterName));
    if (StringUtils.isNotEmpty(workflowVarOrParam)) {
      return workflowVarOrParam;
    }
    workflowVarOrParam = resolveExpression(workflowExpression);
    context.getWorkflowLog().info("Resolved expression [" + workflowVarOrParam + "]");
    return workflowVarOrParam;
  }

  public ManagedObject handleContainer(WorkflowContext context, Log wfLog)
      throws BusinessRuleException, RSuiteException {

    String forceNewMapStr = resolveVariables(getParameterWithDefault(FORCE_NEW_MAP_PARAM, "false"));
    forceNewMapStr = getWorkflowVariableOrParameter(context, FORCE_NEW_MAP_PARAM,
        forceNewMapFromWorkflow);
    context.getWorkflowLog().info("Force new map [" + forceNewMapStr + "] should be ["
        + getWorkflowVariableOrParameter(context, FORCE_NEW_MAP_PARAM, forceNewMapFromWorkflow));
    boolean forceNewMap = false;
    if ("true".equals(forceNewMapStr.toLowerCase().trim()) || "yes".equals(forceNewMapStr
        .toLowerCase().trim())) {
      forceNewMap = true;
    }

    User user = getUserFromWorkflowVariable(context);

    // NOTE: Throws an exception is there is no caNodeId param and no
    // workflow context items.
    ManagedObject mo = getContainer(context);
    if (mo == null) {
      // Must be a content MO
      List<MoWorkflowObject> moList = getMOsToProcess(context);
      mo = context.getManagedObjectService().getManagedObject(user, moList.get(0).getMoid());
      if (mo.isNonXml()) {
        throw new BusinessRuleException("Managed object [" + mo.getId()
            + "] is a non-XML MO. Cannot process it.");
      }
      Element elem = mo.getElement();
      if (!DitaUtil.isDitaMap(elem)) {
        throw new BusinessRuleException("Managed object [" + mo.getId()
            + "] is an XML managed object but is not a DITA map. Cannot process it.");
      }
      wfLog.info("Incoming context object is a map, no need to generate a new one.");
      return mo;
    }

    // At this point, the context MO is a content assembly. See if there is already a map for it.
    // If not, generate one.
    ContentAssemblyNodeContainer container = getContainerUtils().getContentAssemblyNodeContainer(
        context, user, mo.getId());
    String alias = getMapAliasForCaNode(context, user, container);
    List<ManagedObject> mapMos = getMapMOs(context, user, container);
    if (mapMos.size() == 0) {
      mo = generateMapForCa(context, container, wfLog);
    } else if (!forceNewMap && mapMos.size() == 1) {
      // If there is exactly one map, use it if map creation is not forced.
      wfLog.info(
          "There is exactly one map in the container and map creation is not forced, using existing map.");
      mo = mapMos.get(0);
    } else {
      if (forceNewMap) {
        wfLog.info(
            "Found existing map for content assembly but forceNewMap parameter set to true, so regenerating...");
        mo = generateMapForCa(context, container, wfLog);
      } else {
        if (mapMos.size() > 1) {
          wfLog.warn("Found " + mapMos.size() + " managed objects with the alias \"" + alias
              + "\". There should be at most one.");
          wfLog.warn("  MO IDs:");
          int i = 0;
          for (ManagedObject mapMo : mapMos) {
            wfLog.warn(i++ + "   [" + mo.getId() + "] " + mapMo.getDisplayName());
          }
          wfLog.warn("Using first MO [" + mo.getId() + "]");
        }
        mo = mapMos.get(0);
      }

    }
    return mo;
  }

  public List<ManagedObject> getMapMOs(ExecutionContext context, User user,
      ContentAssemblyNodeContainer container) throws RSuiteException {
    List<? extends ContentAssemblyItem> mapItems = new ArrayList<ContentAssemblyItem>();
    ContentAssemblyItemFilter ditaMapFilter = new DitaMapContainerItemFilter(context);
    mapItems = context.getContentAssemblyService().getChildObjectsFiltered(user, container,
        ditaMapFilter);
    List<ManagedObject> mapMOs = new ArrayList<ManagedObject>();
    for (ContentAssemblyItem item : mapItems) {
      if (item.getObjectType().equals(ObjectType.MANAGED_OBJECT_REF)) {
        ManagedObject mo = context.getManagedObjectService().getManagedObject(user, item.getId());
        try {
          mo = RSuiteUtils.getRealMo(context, user, mo);
        } catch (Exception e) {
          // Any exception here is probably the result of a deleted
          // MO or some other data problem that we don't want to hose
          // up this process.
        }
        mapMOs.add(mo);
      }
    }

    // If didn't find a map in the folder, look to find a map associated with the folder moid, as
    // generated by the ck dita topic editor plugin
    if (mapMOs.size() == 0) {
      ManagedObject caMo = context.getManagedObjectService().getManagedObject(user, container
          .getId());
      caMo = RSuiteUtils.getRealMo(context, user, caMo);
      Alias[] aliases = caMo.getAliases("filename");
      String folderAlias = "";
      if (aliases.length > 0) {
        folderAlias = aliases[0].getText();
      }
      if (folderAlias.isEmpty()) {
        folderAlias = caMo.getId() + ".ditamap";
      }
      ManagedObject mapMo = context.getManagedObjectService().getObjectByAlias(user, folderAlias);
      if (mapMo != null) {
        mapMOs.add(mapMo);
      }
    }

    return mapMOs;
  }

  public ManagedObject generateMapForCa(WorkflowContext context,
      ContentAssemblyNodeContainer caNode, Log wfLog) throws RSuiteException {
    ManagedObject mapMo = null;
    Session session = null;
    try {
      User user = getUserFromWorkflowVariable(context);

      String xsltUrlString = getParameterWithDefault(XSLT_URI_PARAM,
          DEFAULT_CA_TO_MAP_TRANSFORM_URL);
      xsltUrlString = resolveVariables(xsltUrlString);
      xsltUrlString = getWorkflowVariableOrParameter(context, XSLT_URI_PARAM, xsltUriFromWorkflow);
      context.getWorkflowLog().info("Force new map [" + xsltUrlString + "] should be ["
          + getWorkflowVariableOrParameter(context, XSLT_URI_PARAM, xsltUriFromWorkflow));

      String baseName = getMapName(caNode); // Use custom method for baseName
      String alias = constructMapFilenameAlias(context, user, baseName);
      UserAgent userAgent = new UserAgent("rsuite-workflow-process");
      // Create a session so the transform can use the Web service via the
      // external HTTP access. This is a workaround for the fact that as of
      // RSuite 3.6.3 there is no internal URL for accessing plugin-provided
      // Web services.
      ProjectUtils projectUtils = new ProjectUtils(context.getRSuiteServerConfiguration());
      session = context.getSessionService().createSession("Realm", userAgent, projectUtils
          .getBaseRSuiteUrl() + "/rsuite/rest/v1", getUserFromWorkflowVariable(context));
      String skey = session.getKey();

      mapMo = applyTransformToCa(context, user, caNode, skey, xsltUrlString, baseName, alias,
          "false", context.getWorkflowLog());


    } catch (Exception e) {
      context.setVariable(EXCEPTION_OCCUR, true);
      throw new RSuiteException(0, "Exception applying transform to content assembly: " + e
          .getMessage(), e);
    } finally {
      if (session != null) {
        try {
          context.getSessionService().removeSession(session.getKey());
        } catch (Exception e) {
          wfLog.warn("Failed to kill RSuite session used to generate DITA map.", e);
        }
      }
    }
    return mapMo;
  }

  public ManagedObject getContainer(WorkflowContext context)
      throws BusinessRuleException, RSuiteException {
    Log wfLog = context.getWorkflowLog();

    String caNodeMoId = resolveVariablesAndExpressions(getParameter(CA_NODE_ID_PARAM));
    if (caNodeMoId == null) {
      List<MoWorkflowObject> moList = getMOsToProcess(context);
      if (moList.size() == 0) {
        context.setVariable(EXCEPTION_OCCUR, true);
        throw new BusinessRuleException(CA_NODE_ID_PARAM
            + " not set and no managed objects in the workflow context. Nothing to do.");
      }

      if (moList.size() > 1) {
        wfLog.warn("Multiple MOs in the workflow context. Processing the first one");
      }
      MoWorkflowObject moWFO = moList.get(0);
      caNodeMoId = moWFO.getMoid();
    }

    ContentAssemblyNodeContainer container = getContainerUtils().getContentAssemblyNodeContainer(
        context, getUserFromWorkflowVariable(context), caNodeMoId);
    if (container == null) {
      return null;
    }

    return context.getManagedObjectService().getManagedObject(getUserFromWorkflowVariable(context),
        container.getId());
  }

  public static String constructMapFilenameAlias(ExecutionContext context, User user,
      String baseName) throws RSuiteException {
    // If CA node is a generic node, then look up to the ancestor publication node
    // to get the appropriate display name.
    // This is a little weak as it could result in name collitions,
    // but's probably sufficient.
    String alias = baseName.replaceAll("[\\s,\"\'\u2018\u2019\u201C\u201D]", "_") + ".ditamap";
    return alias;
  }

  public static ManagedObject applyTransformToCa(ExecutionContext context, User user,
      ContentAssemblyNodeContainer caNode, String sessionKey, String xsltUrlString, String baseName,
      String alias, String debugParam, Log log)
      throws RSuiteException, URISyntaxException, IOException, FileNotFoundException {
    ManagedObject caNodeMo = context.getManagedObjectService().getManagedObject(user, caNode
        .getId());
    LoggingSaxonMessageListener logger = context.getXmlApiManager().newLoggingSaxonMessageListener(
        log);
    Map<String, String> params = new HashMap<String, String>();
    params.put("debug", debugParam);
    params.put("rsuiteSessionKey", sessionKey);
    params.put("rsuiteHost", context.getRSuiteServerConfiguration().getHostName());
    params.put("rsuitePort", context.getRSuiteServerConfiguration().getPort());
    log.info("rsuiteHost=" + context.getRSuiteServerConfiguration().getHostName());
    log.info("rsuitePort=" + context.getRSuiteServerConfiguration().getPort());
    params.put("mapTitle", baseName);

    TransformationOptions options = new ToDitaTransformationOptions();
    options.setSaxonLogger(logger);
    options.setTransformer(context.getXmlApiManager().getSaxonXsltTransformer(new URI(
        xsltUrlString), logger));

    TransformSupportBean tsBean = new TransformSupportBean(context, xsltUrlString);
    Source source = new DOMSource(caNodeMo.getElement());
    ProjectUtils projectUtils = new ProjectUtils(context.getRSuiteServerConfiguration());
    source.setSystemId(projectUtils.getBaseRSuiteUrl() + "/rsuite/rest/v1/content/" + caNodeMo
        .getId());

    ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
    Serializer dest = new Serializer();
    dest.setOutputStream(resultStream);

    String displayName = caNode.getDisplayName();
    File tempDir = null;
    try {
      tempDir = context.getRSuiteServerConfiguration().getTmpDir();
    } catch (Exception e) {
      String msg = "Failed to get a temporary directory: " + e.getMessage();
      TransformSupportBean.logAndThrowRSuiteException(log, e, msg);
    }

    log.info("Transforming CA " + caNodeMo.getDisplayName() + " [" + caNodeMo.getId()
        + "] using transform \"" + xsltUrlString + "\"...");
    tsBean.applyTransform(displayName, source, dest, params, options, log);

    File tempFile = new File(tempDir, alias);
    IOUtils.copy(new ByteArrayInputStream(resultStream.toByteArray()), new FileOutputStream(
        tempFile));
    log.info("Result saved to file " + tempFile.getAbsolutePath());

    ManagedObject loadedMo = loadResultToRSuite(context, user, caNode, resultStream, alias, log);
    return loadedMo;
  }

  /**
   * @param context
   * @param ca
   * @param resultStream
   * @param log
   * @return The new or updated MO.
   * @throws RSuiteException
   */
  public static ManagedObject loadResultToRSuite(ExecutionContext context, User user,
      ContentAssemblyNodeContainer ca, ByteArrayOutputStream resultStream, String alias, Log log)
      throws RSuiteException {
    ManagedObject existingMo = context.getManagedObjectService().getObjectByAlias(user, alias);
    ManagedObject loadedMo = null;

    SecurityUtils securityUtils = new SecurityUtils();
    ManagedObject caMo = context.getManagedObjectService().getManagedObject(user, ca.getId());
    ContentAssemblyNodeContainer productContainer = context.getContentAssemblyService()
        .getContentAssemblyNodeContainer(user, "4");
    User ampedEditUser = securityUtils.getAmpEditUser(context.getSecurityService(), context
        .getAuthorizationService(), user, caMo);
    if (existingMo == null) {
      ObjectInsertOptions insertOptions = new ObjectInsertOptions(alias, new String[] {alias,},
          null, false);
      insertOptions.setContentAssembly(ca);
      ObjectSource loadSource = new XmlObjectSource(resultStream.toByteArray());
      insertOptions.setAcl(null);
      loadedMo = context.getManagedObjectService().load(user, loadSource, insertOptions);
      context.getContentAssemblyService().attach(ampedEditUser, ca.getId(), loadedMo, null);
      log.info("Transform result loaded as object [" + loadedMo.getId() + "]");
    } else {
      loadedMo = existingMo;
      ObjectUpdateOptions updateOptions = new ObjectUpdateOptions();
      ObjectSource loadSource = new XmlObjectSource(resultStream.toByteArray());
      boolean isLocked = context.getManagedObjectService().isCheckedOut(user, existingMo.getId());
      if (!isLocked) {
        try {
          context.getManagedObjectService().checkOut(user, existingMo.getId());
        } catch (RSuiteException e) {
          log.info("Failed to check out managed object " + existingMo.getDisplayName() + " ["
              + existingMo.getId() + "]");
          throw e;
        }
      }
      if (context.getManagedObjectService().isCheckedOutButNotByUser(user, existingMo.getId())) {
        throw new RSuiteException(0, "Existing publication map " + existingMo.getDisplayName()
            + " [" + existingMo.getId() + "] checked out be a different user");
      }

      /*
       * If not encapsulated in try/catch, then exception bubbles up and causes the workflow to blow
       * up
       */
      try {
        context.getManagedObjectService().update(user, existingMo.getId(), loadSource,
            updateOptions);
      } catch (Exception e) {
        log.info("Error updating object [" + e.getMessage());
      }

      log.info("Managed object  [" + existingMo.getId() + "] updated with transform result");
      if (!isLocked) {
        ObjectCheckInOptions checkInOptions = new ObjectCheckInOptions();
        checkInOptions.setVersionType(VersionType.MINOR);
        checkInOptions.setVersionNote("Generated by XSLT transform from CA " + ca.getDisplayName()
            + " [" + ca.getId() + "]");
        context.getManagedObjectService().checkIn(user, existingMo.getId(), checkInOptions);
      }
      StringWriter writer = new StringWriter();
      PrintWriter messageWriter = new PrintWriter(writer);
      RSuiteUtils.attachIfNotInCa(context, ampedEditUser, ca, messageWriter, loadedMo);
      String msg = writer.toString();
      if (msg != null && !"".equals(msg.trim())) {
        log.info(msg);
      }

    }

    return loadedMo;

  }


  public static String getMapAliasForCaNode(ExecutionContext context, User user,
      ContentAssemblyItem ca) throws RSuiteException {
    String baseName = ca.getDisplayName();
    return constructMapFilenameAlias(context, user, baseName);
  }



  public void setForceNewMap(String forceNewMap) {
    setParameter(FORCE_NEW_MAP_PARAM, forceNewMap);
  }

  public void setXsltUri(String xsltUri) {
    setParameter(XSLT_URI_PARAM, xsltUri);
  }

  public void setCaNodeId(String caNodeId) {
    setParameter(CA_NODE_ID_PARAM, caNodeId);
  }

  /* New method */
  public static String getMapName(ContentAssemblyNodeContainer product) throws RSuiteException {
    return "_" + product.getDisplayName();
  }

  public User getUserFromWorkflowVariable(WorkflowContext context) throws RSuiteException {
    String userIdFromWorkflow = context.getVariableAsString(RSUITE_USER_ID_VAR_NAME);
    return UserUtils.getUser(context.getAuthorizationService(), userIdFromWorkflow);
  }

}
