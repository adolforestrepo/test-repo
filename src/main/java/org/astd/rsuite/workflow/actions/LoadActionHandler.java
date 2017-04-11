package org.astd.rsuite.workflow.actions;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.VersionType;
import com.reallysi.rsuite.api.control.NonXmlObjectSource;
import com.reallysi.rsuite.api.control.ObjectInsertOptions;
import com.reallysi.rsuite.api.control.ObjectSource;
import com.reallysi.rsuite.api.control.ObjectUpdateOptions;
import com.reallysi.rsuite.api.control.XmlObjectSource;
import com.reallysi.rsuite.api.i18n.MessageResources;
import org.astd.rsuite.workflow.actions.AbstractBaseActionHandler;
import com.reallysi.rsuite.api.workflow.activiti.BusinessRuleException;
import com.reallysi.rsuite.api.workflow.activiti.FileWorkflowObject;
import com.reallysi.rsuite.api.workflow.KeyedVariableMap;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoWorkflowObject;
import com.reallysi.rsuite.api.workflow.WorkflowExecutionContext;
import com.reallysi.rsuite.service.AuthorizationService;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.service.RepositoryService;
import com.reallysi.rsuite.service.SearchService;
import com.reallysi.rsuite.service.XmlApiManager;
import com.reallysi.tools.DomUtils;
import com.reallysi.tools.FileUtils;
import com.astd.rsuite.actions.workflow.StringUtil;
import com.reallysi.tools.XmlUtils;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class LoadActionHandler

{
  public static final String XML_MO_MATCH_EXPRESSIONS_PARAM = "xmlMoMatchExpressions";
  public static final String LAYERED_METADATA_MATCH_EXPRESSIONS_PARAM = "layeredMetadataMatchExpressions";
  public static final String MO_CHUNK_MATCH_EXPRESSIONS_PARAM = "moChunkMatchExpressions";
  public static final String COLLECTION_PARAM = "collection";
  public static final String INPUT_FOLDER_PARAM = "inputFolder";
  public static final String DONOT_DELETE_PARAM = "notDelete";
  public static final String IGNORE_EMPTY_ZIP_PARAM = "ignoreEmptyZip";
  public static final String COMMIT_MESSAGE_PARAM = "commitMessage";
  public static final String CREATION_POLICY_PARAM = "creationPolicy";
  public static final String NEW_ALWAYS_CREATION_POLICY = "NewAlways";
  public static final String NEW_UNIQUE_REPOSITORY_CREATION_POLICY = "NewUniqueRepository";
  public static final String UPDATE_REPOSITORY_CREATION_POLICY = "UpdateRepository";
  public static final String NEW_UNIQUE_CA_CREATION_POLICY = "NewUniqueCA";
  public static final String UPDATE_CA_CREATION_POLICY = "UpdateCA";
  public static final String REGEX_PARAM = "regex";
  private static final long serialVersionUID = -940239995583707901L;
  private static Log log = LogFactory.getLog(LoadActionHandler.class);
  
  public void setRegex(String regex)
  {
    setParameter("regex", regex);
  }
  
  public void setXmlMoMatchExpressions(String xmlMoMatchExpressions)
  {
    setParameter("xmlMoMatchExpressions", xmlMoMatchExpressions);
  }
  
  public void setLayeredMetadataMatchExpressions(String layeredMetadataMatchExpressions)
  {
    setParameter("layeredMetadataMatchExpressions", layeredMetadataMatchExpressions);
  }
  
  public void setMoChunkMatchExpressions(String moChunkMatchExpressions)
  {
    setParameter("moChunkMatchExpressions", moChunkMatchExpressions);
  }
  
  public void setCollection(String collection)
  {
    setParameter("collection", collection);
  }
  
  public void setInputFolder(String inputFolder)
  {
    setParameter("inputFolder", inputFolder);
  }
  
  public void setNotDelete(String notDelete)
  {
    setParameter("notDelete", notDelete);
  }
  
  public void setIgnoreEmptyZip(String ignoreEmptyZip)
  {
    setParameter("ignoreEmptyZip", ignoreEmptyZip);
  }
  
  public void setCreationPolicy(String creationPolicy)
  {
    setParameter("creationPolicy", creationPolicy);
  }
  
  public void setCommitMessage(String commitMessage)
  {
    setParameter("commitMessage", commitMessage);
  }
  
  public void execute(WorkflowExecutionContext executionContext)
    throws Exception
  {
    faultNoLongerSupported("The LoadActionHandler is no longer supported.");
  }
  
  protected void loadFiles(WorkflowExecutionContext context, File[] preLoadFiles)
    throws Exception
  {
    String xmlMoMatchExpressions = getParameter("xmlMoMatchExpressions");
    String layeredMetaDataMatchExpressions = getParameter("layeredMetadataMatchExpressions");
    String moChunkMatchExpressions = getParameter("moChunkMatchExpressions");
    String collection = getParameter("collection");
    String ignoreEmptyZip = getParameter("ignoreEmptyZip");
    
    String replaceExistingMO = getParameterWithDefault("creationPolicy", "NewAlways");
    
    ManagedObjectService moService = context.getManagedObjectService();
    AuthorizationService authService = context.getAuthorizationService();
    User user = authService.getSystemUser();
    
    List<MoWorkflowObject> list = new ArrayList();
    
    String[] xpathEntries = (xmlMoMatchExpressions == null) || (xmlMoMatchExpressions.trim().equals("")) ? new String[0] : xmlMoMatchExpressions.split(";");
    
    Hashtable<String, String> xpaths = new Hashtable();
    for (int i = 0; i < xpathEntries.length; i++) {
      xpaths.put("xpath_" + i, xpathEntries[i]);
    }
    String[] slaXpath = (layeredMetaDataMatchExpressions == null) || (layeredMetaDataMatchExpressions.trim().equals("")) ? null : layeredMetaDataMatchExpressions.split(";");
    for (File preLoadFile : preLoadFiles)
    {
      String[] laXpath = resolveVariables(context, slaXpath, preLoadFile);
      
      String filePath = preLoadFile.getAbsolutePath();
      String fileName = preLoadFile.getName();
      if ((!"true".equalsIgnoreCase(ignoreEmptyZip)) && (filePath.toLowerCase().endsWith(".zip")) && (new File(filePath).length() <= 22L)) {
        throw new BusinessRuleException("Empty ZIP file", getMessageResources().getMessageText("action.emptyzipfile", new Object[] { filePath }));
      }
      boolean isXml = treatAsXml(context, fileName);
      if ((isXml) && (moChunkMatchExpressions != null) && (!moChunkMatchExpressions.equals("")) && (!moChunkMatchExpressions.equals("/")))
      {
        splitFileIntoNodes(context, user, preLoadFile, filePath, xpathEntries, xpaths, laXpath, list);
      }
      else
      {
        String id = "0";
        if (isXml) {
          id = getMoIdFromXPath(context, xpaths, laXpath, filePath, null, true, "NewAlways");
        } else {
          id = getMoIdFromXPath(context, null, laXpath, filePath, null, false, replaceExistingMO);
        }
        if (!id.equals("0")) {
          updateExistingMoWithIncomingData(context, replaceExistingMO, moService, user, list, preLoadFile, filePath, isXml, id);
        } else {
          createNewMoForIncomingData(context, collection, user, list, preLoadFile, filePath, fileName, FileUtils.getFileSuffix(filePath, ""), isXml);
        }
      }
      removeFileAndLog(preLoadFile);
    }
    MoListWorkflowObject moListWorkflowObject = new MoListWorkflowObject(list);
    context.setMoListWorkflowObject(moListWorkflowObject);
    context.getVariables().put("creationPolicy", replaceExistingMO);
  }
  
  private void createNewMoForIncomingData(WorkflowExecutionContext context, String collection, User user, List<MoWorkflowObject> list, File preLoadFile, String filePath, String fileName, String suffix, boolean isXml)
    throws RSuiteException, IOException
  {
    ManagedObject newMo = null;
    String moid = null;
    if (isXml)
    {
      newMo = loadXMLFromFile(context, user, preLoadFile, "/$CONTENT$/", true, true, collection);
    }
    else
    {
      ObjectInsertOptions options = ObjectInsertOptions.constructOptionsForNonXml(filePath.substring(filePath.lastIndexOf(File.separator) + 1), null, null, collection == null ? null : collection.split(","));
      
      newMo = context.getManagedObjectService().load(user, new NonXmlObjectSource(readFileToByteArray(filePath)), options);
    }
    moid = newMo.getId();
    MoWorkflowObject moObj = new MoWorkflowObject(moid);
    moObj.setSource(new FileWorkflowObject(preLoadFile));
    logMO(context, list, moObj, moid, filePath);
    log.info(filePath + " has been loaded into System and its MOID is [" + moid + "].");
    
    moObj.setExists(false);
  }
  
  private void updateExistingMoWithIncomingData(WorkflowExecutionContext context, String replaceExistingMO, ManagedObjectService moService, User user, List<MoWorkflowObject> list, File preLoadFile, String filePath, boolean isXml, String id)
    throws RSuiteException, IOException, Exception
  {
    if (isXml)
    {
      moService.checkOut(user, id);
      
      Document doc = DomUtils.parseFromFile(new File(filePath), false);
      Element newElem = doc.getDocumentElement();
      ObjectUpdateOptions updateOptions = new ObjectUpdateOptions();
      
      moService.update(user, id, new XmlObjectSource(newElem), updateOptions);
      
      String commitMessage = getParameterWithDefault("commitMessage", "Load action handler");
      
      commitMessage = resolveVariablesAndExpressions(context, commitMessage, preLoadFile.getName());
      
      moService.checkIn(user, id, VersionType.MINOR, commitMessage, true);
    }
    else if ("NewUniqueRepository".equals(replaceExistingMO)) {}
    MoWorkflowObject obj = new MoWorkflowObject(id);
    obj.setSource(new FileWorkflowObject(preLoadFile));
    logMO(context, list, obj, id, filePath);
    log.info(" *** MO exists, id=" + id + ", file: " + filePath);
    obj.setExists(true);
  }
  
  protected void splitFileIntoNodes(WorkflowExecutionContext context, User user, File srcFile, String fileName, String[] xpathEntries, Hashtable<String, String> xpaths, String[] laXpath, List<MoWorkflowObject> list)
    throws Exception
  {
    ManagedObjectService moService = context.getManagedObjectService();
    Document doc = context.getXmlApiManager().getW3CDomFromFile(fileName, false);
    
    String xpathAbstract = getParameter("moChunkMatchExpressions");
    
    Node[] nodes = XmlUtils.executeXPathToNodeArray(xpathAbstract, doc);
    if ((nodes == null) || (nodes.length == 0)) {
      return;
    }
    DocumentType docType = doc.getDoctype();
    MoWorkflowObject obj = null;
    String id = "0";
    String collection = getParameter("collection");
    if (docType != null)
    {
      for (Node node : nodes)
      {
        id = "0";
        
        DocumentType newDoctype = DomUtils.createDocumentType(node.getNodeName(), docType.getPublicId(), docType.getSystemId());
        
        Document newDoc = DomUtils.getW3CDom(DomUtils.serializeToString(node), false);
        
        newDoc.appendChild(newDoctype);
        if ((xpathEntries != null) && (xpathEntries.length > 0)) {
          id = getMoIdFromXPath(context, xpaths, laXpath, null, newDoc.getDocumentElement(), true, "NewAlways");
        }
        if (!id.equals("0"))
        {
          moService.checkOut(user, id);
          ObjectUpdateOptions updateOptions = new ObjectUpdateOptions();
          
          moService.update(user, id, new XmlObjectSource(newDoc.getDocumentElement()), updateOptions);
          
          log.info(" *** MO exist, id=" + id);
          obj = new MoWorkflowObject(id);
          obj.setSource(new FileWorkflowObject(srcFile));
          obj.setExists(true);
          moService.checkIn(user, id, VersionType.MINOR, "ingestion work flow", true);
        }
        else
        {
          ObjectSource src = new XmlObjectSource(newDoc);
          
          ObjectInsertOptions options = new ObjectInsertOptions(fileName, null, collection == null ? null : collection.split(","), true, true);
          
          ManagedObject mo = moService.load(user, src, options);
          
          id = mo.getId();
          obj = new MoWorkflowObject(id);
          obj.setSource(new FileWorkflowObject(srcFile));
          logMO(context, list, obj, id, fileName);
          obj.setExists(false);
        }
      }
    }
    else
    {
      String namespace = "http://www.w3.org/2001/XMLSchema-instance";
      String noLocation = "noNamespaceSchemaLocation";
      String sLocation = "schemaLocation";
      Element element = doc.getDocumentElement();
      String defaultLocation = element.getAttributeNS(namespace, noLocation);
      String location = element.getAttributeNS(namespace, sLocation);
      for (Node node : nodes)
      {
        id = "0";
        if ((defaultLocation != null) && (!defaultLocation.trim().equals(""))) {
          ((Element)node).setAttributeNS(namespace, "xsi:" + noLocation, defaultLocation);
        }
        if ((location != null) && (!location.trim().equals(""))) {
          ((Element)node).setAttributeNS(namespace, "xsi:" + sLocation, location);
        }
        Document newDoc = DomUtils.getW3CDom(DomUtils.serializeToString(node), false);
        if ((xpathEntries != null) && (xpathEntries.length > 0)) {
          id = getMoIdFromXPath(context, xpaths, laXpath, null, newDoc.getDocumentElement(), true, "NewAlways");
        }
        if (!id.equals("0"))
        {
          moService.checkOut(user, id);
          ObjectUpdateOptions updateOptions = new ObjectUpdateOptions();
          
          moService.update(user, id, new XmlObjectSource(newDoc.getDocumentElement()), updateOptions);
          
          log.info(" *** MO exist, id=" + id);
          obj = new MoWorkflowObject(id);
          obj.setSource(new FileWorkflowObject(srcFile));
          obj.setExists(true);
          moService.checkIn(user, id, VersionType.MINOR, "ingestion work flow", true);
        }
        else
        {
          ObjectInsertOptions options = new ObjectInsertOptions(fileName, null, collection == null ? null : collection.split(","), true, true);
          
          ManagedObject mo = moService.load(user, new XmlObjectSource(newDoc), options);
          
          id = mo.getId();
          obj = new MoWorkflowObject(id);
          obj.setSource(new FileWorkflowObject(srcFile));
          logMO(context, list, obj, id, fileName);
          obj.setExists(false);
        }
      }
    }
  }
  
  private boolean doDelete()
  {
    String notDelete = getParameter("notDelete");
    return !"true".equalsIgnoreCase(notDelete);
  }
  
  protected File[] getPreloadFiles(File folder, String regex)
    throws RSuiteException
  {
    boolean regexNotNull = !StringUtil.isNullOrEmpty(regex);
    FileFilter filter = null;
    if (regexNotNull) {
      filter = new RegexFileFilter(regex);
    }
    File[] preLoadFiles = folder.listFiles(filter);
    if (preLoadFiles.length == 0) {
      throw new BusinessRuleException("No content found", getMessageResources().getMessageText("action.nocontentfound"));
    }
    return preLoadFiles;
  }
  
  private File extractFile(WorkflowExecutionContext executionContext)
  {
    String inputFolder = getParameter("inputFolder");
    if (!StringUtil.isNullOrEmptyOrSpace(inputFolder))
    {
      File f = new File(inputFolder);
      if (f.exists()) {
        return f;
      }
    }
    FileWorkflowObject fileBo = executionContext.getFileWorkflowObject();
    return fileBo == null ? null : fileBo.getFile();
  }
  
  private void removeFileAndLog(File file)
  {
    if (doDelete())
    {
      FileUtils.remove(file);
      log.info("{LoadAction} remove file[" + file.getAbsolutePath() + "] succeed.");
    }
  }
  
  private void logMO(WorkflowExecutionContext context, List<MoWorkflowObject> list, MoWorkflowObject obj, String moid, String fileName)
  {
    list.add(obj);
    
    context.getVariables().put(moid, null, "MOID", moid);
  }
  
  private String getMoIdFromXPath(WorkflowExecutionContext context, Hashtable<String, String> xpaths, String[] laPaths, String filePath, Element ele, boolean isXml, String creationPolicy)
    throws Exception
  {
    XmlApiManager xmlApiManager = context.getXmlApiManager();
    RepositoryService repositoryService = context.getRepositoryService();
    if (isXml) {
      return getMoIdForExistingXmlMoIfAny(xpaths, laPaths, filePath, ele, xmlApiManager, repositoryService);
    }
    return getMoIdForExistingNonXmlIfAny(filePath, creationPolicy);
  }
  
  private String getMoIdForExistingNonXmlIfAny(String filePath, String creationPolicy)
    throws RSuiteException
  {
    String displayName = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
    if ("NewAlways".equals(creationPolicy)) {
      return "0";
    }
    List<ManagedObject> mos = getSearchService().findManagedObjectsWithDisplayName(displayName);
    if ((mos == null) || (mos.size() == 0)) {
      return "0";
    }
    if ("UpdateRepository".equals(creationPolicy)) {
      if (mos.size() > 1) {
        throw new RSuiteException(1010, getMessageResources().getMessageText("action.multipleobjectsofsamename", new Object[] { displayName }));
      }
    }
    ManagedObject objectUsed = (ManagedObject)mos.get(0);
    return objectUsed.getId();
  }
  
  private String getMoIdForExistingXmlMoIfAny(Hashtable<String, String> xpaths, String[] laPaths, String filePath, Element ele, XmlApiManager xmlApiManager, RepositoryService repositoryService)
    throws RSuiteException
  {
    if ((xpaths == null) || (xpaths.size() == 0)) {
      return "0";
    }
    Hashtable<String, String> vals = new Hashtable();
    Element xmlEle = null;
    if (filePath == null) {
      xmlEle = ele;
    } else {
      xmlEle = xmlApiManager.getW3CDomFromFile(filePath, false).getDocumentElement();
    }
    for (Iterator<String> it = xpaths.keySet().iterator(); it.hasNext();)
    {
      String vname = (String)it.next();
      String xpath = (String)xpaths.get(vname);
      String value = XmlUtils.executeXPathToString(xpath, xmlEle);
      vals.put(vname, value);
    }
    String xquery = generateXQueryForXML(xmlEle.getTagName(), xpaths, vals, laPaths);
    try
    {
      log.info("getMoIdFromXPath(): xquery for repository={" + xquery + "}");
    }
    catch (Throwable e)
    {
      e.printStackTrace();
    }
    return repositoryService.queryAsString(xquery, vals, new String[0]);
  }
  
  private String generateXQueryForXML(String name, Hashtable<String, String> xpaths, Hashtable<String, String> values, String[] laPaths)
  {
    String variables = "";
    StringBuffer sb = new StringBuffer();
    String collection = getParameter("collection");
    
    String col = "collection('" + collection + "')";
    
    sb.append("let $moid := " + col + "//" + name + "[");
    for (Iterator<String> it = xpaths.keySet().iterator(); it.hasNext();)
    {
      String vname = (String)it.next();
      String value = (String)values.get(vname);
      if (value.equals(""))
      {
        sb.append("not(" + (String)xpaths.get(vname) + ")");
      }
      else
      {
        variables = variables + "define variable $" + vname + " as xs:string external" + "\r\n";
        
        sb.append((String)xpaths.get(vname) + "=$" + vname);
      }
      sb.append(" and ");
    }
    if (sb.lastIndexOf(" and ") > 0) {
      sb.delete(sb.lastIndexOf(" and "), sb.length());
    }
    sb.append("]/RSUITE:METADATA");
    if ((laPaths != null) && (laPaths.length > 0))
    {
      sb.append("[");
      for (int k = 0; k < laPaths.length; k++)
      {
        sb.append(laPaths[k]);
        if (k != laPaths.length - 1) {
          sb.append(" and ");
        }
      }
      sb.append("]");
    }
    sb.append("/RSUITE:SYSTEM/RSUITE:ID/text()");
    
    sb.append("\r\n");
    sb.append("return if($moid) then $moid");
    sb.append("\r\n");
    sb.append("       else '0'");
    return variables + sb.toString();
  }
  
  private byte[] readFileToByteArray(String filePath)
    throws IOException
  {
    return FileUtils.readFileIntoBytes(filePath);
  }
  
  private String[] resolveVariables(WorkflowExecutionContext context, String[] src, File file)
    throws RSuiteException
  {
    if (src == null) {
      return src;
    }
    String[] strs = new String[src.length];
    for (int i = 0; i < src.length; i++) {
      strs[i] = context.resolveVariables(file.getName(), strs[i]);
    }
    return strs;
  }
  
  public ManagedObject loadXMLFromFile(WorkflowExecutionContext context, User user, File xmlFile, String targetFolder, boolean validate, boolean force, String collection)
    throws RSuiteException
  {
    try
    {
      Document doc = DomUtils.parseFromFile(xmlFile.getAbsolutePath(), validate);
      
      ObjectSource src = new XmlObjectSource(doc);
      
      ObjectInsertOptions options = new ObjectInsertOptions(xmlFile.getName(), null, collection == null ? null : collection.split(","), force, validate);
      
      return context.getManagedObjectService().load(user, src, options);
    }
    catch (Exception ex)
    {
      log.warn("Error loading object", ex);
      throw new RSuiteException(1053, ex.getMessage());
    }
  }
  
  private String[] XML_FILE_EXT = null;
}
