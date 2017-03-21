package org.astd.rsuite.webservices;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.constants.ProjectConstants;
import org.astd.rsuite.constants.RSuiteNameAndPathConstants;
import org.astd.rsuite.constants.WebServiceConstants;
import org.astd.rsuite.utils.CallArgumentUtils;
import org.astd.rsuite.utils.ContainerUtils;
import org.astd.rsuite.utils.MOUtils;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.Session;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.content.ContentDisplayObject;
import com.reallysi.rsuite.api.content.ContentObjectPath;
import com.reallysi.rsuite.api.control.ContentAssemblyCreateOptions;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.NotificationAction;
import com.reallysi.rsuite.api.remoteapi.result.RestResult;
import com.reallysi.rsuite.api.system.RSuiteServerConfiguration;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowDefinition;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowInstance;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;
import com.rsicms.rsuite.helpers.webservice.RemoteApiHandlerBase;

/**
 * A user needs to generate an output using the DITA-OT from an XML MO; start workflow using a web
 * service so that we can set the parent mo id (where the output file goes) and allow for future
 * enhancements to either response messages or workflow execution
 */
public class StartDitaOtWorkflowWebService
    extends RemoteApiHandlerBase
    implements ProjectConstants, RSuiteNameAndPathConstants, WebServiceConstants {

  private static Log log = LogFactory.getLog(StartDitaOtWorkflowWebService.class);

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
  public RemoteApiResult execute(RemoteApiExecutionContext context, CallArgumentList args)
      throws RSuiteException {

    User user = context.getSession().getUser();

    String moId = args.getFirstValue("rsuiteId");

    String processDefinitionKey = args.getFirstValue("processDefinitionKey");

    // check to see if the process definition name matches an existing process def
    checkProcessDefinitionKey(context, processDefinitionKey);

    // construct map of parameters needed for workflow starting
    Map<String, Object> params = getParams(context, args, user, moId);

    ManagedObject contextMo = context.getManagedObjectService().getManagedObject(user, moId);
    ContentAssemblyNodeContainer caParentOfOutputContainer = null;

    if (contextMo != null && !contextMo.isAssemblyNode()) {
      // context MO is probably an XML MO, so we need to find the parent
      // Find the parentMo so that we can figure out if it has the existing container
      ManagedObject parentMo = getParentMo(context, args, user);

      caParentOfOutputContainer = getContainerUtils().getContentAssemblyNodeContainer(context, user,
          parentMo.getId());

    } else {
      // context MO is an assembly node, good deal
      caParentOfOutputContainer = getContainerUtils().getContentAssemblyNodeContainer(context, user,
          moId);
    }

    String nameTargetOutputContainer = "";

    nameTargetOutputContainer = CONTAINER_NAME_GENERATED_OUTPUTS;

    // look to see if the target output container exists and if it does then
    // return the id or return 0 otherwise
    String idOfTargetOutputContainer = getIdTargetOutputContainer(context, user,
        caParentOfOutputContainer, nameTargetOutputContainer);

    String idToLoadInto = null;

    // idOfTargetOutputContainer will return 0
    // if the targeted container does not exist
    if ("0".equals(idOfTargetOutputContainer)) {
      String caForParentMoId = caParentOfOutputContainer.getId();
      ContentAssembly newCa;

      newCa = createContainerForOutputs(context, user, caForParentMoId, nameTargetOutputContainer);

      idToLoadInto = newCa.getId();
    } else {
      idToLoadInto = idOfTargetOutputContainer;
    }

    params.put("parentMoId", idToLoadInto);
    params.put("parentId", idToLoadInto);
    params.put("idToLoadInto", idToLoadInto);

    // the exportMapFilename workflow variable does not handle periods in MO aliases correctly
    // given this: 1234_orice.reviewer.xml, it interprets the base name as 1234_orice
    // the following sets a new wf variable that works around that
    params.put("correctExportMapFilename", MOUtils.getBasenameAlias(contextMo));

    String transformParamShowTrackChanges = CallArgumentUtils.getFirstString(args,
        WS_PARAM_NAME_TRANSFORM_PARAM_SHOW_TRACK_CHANGES, "no");
    String transformParamShowComments = CallArgumentUtils.getFirstString(args,
        WS_PARAM_NAME_TRANSFORM_PARAM_SHOW_COMMENTS, "no");

    params.put("showTrackChanges", transformParamShowTrackChanges);
    params.put("showComments", transformParamShowComments);

    startWorkflow(context, user, processDefinitionKey, params);

    RestResult webServiceResult = new RestResult();
    NotificationAction notification = new NotificationAction("Workflow " + processDefinitionKey
        + " started successfully", "Workflow started");
    webServiceResult.addAction(notification);
    return webServiceResult;
  }

  /**
   * Sets parameters for workflow and returns the object containing the parameters.
   * 
   * @param context The execution context
   * @param args The arguments passed, presumably from a web service.
   * @param user The user
   * @param moId The id of the managed object that the workflow is being executed on
   * @return A Map<String, Object> of parameters.
   */
  public Map<String, Object> getParams(RemoteApiExecutionContext context, CallArgumentList args,
      User user, String moId) {
    Map<String, Object> params = new HashMap<String, Object>();
    String transtype = args.getValues("transtype");
    params.put("transtype", transtype);
    String browseUri = args.getValues("rsuiteBrowseUri");
    params.put("rsuiteBrowseUri", browseUri);
    params.put("rsuiteUserId", user.getUserID());
    params.put("rsuiteUserEmailAddress", user.getEmail());
    params.put("rsuiteUserFullName", user.getFullName());
    params.put("rsuite contents", moId);
    params.put("rsuiteDitaXmlExportDir", context.getRSuiteServerConfiguration()
        .getConfigurationProperties().getProperty("rsuite.ditaXmlExportDir", ""));
    Session session = context.getSession();
    params.put("rsuiteSessionId", session.getKey());

    // Set the base server URL for use in retrieving XSLTs as static resources
    RSuiteServerConfiguration serverConf = context.getRSuiteServerConfiguration();
    int port = Integer.parseInt(serverConf.getPort());
    String rsuiteBaseServer = PROTOCOL_RSUITE + "://" + serverConf.getHostName() + ":" + port;
    params.put("rsuiteBaseServer", rsuiteBaseServer);

    return params;
  }

  /**
   * Creates a container in a given parent with a given name. Returns the new container.
   * 
   * @param context The execution context
   * @param user The user.
   * @param caForParentMoId The id of the container to create the new container in.
   * @param newContainerName The name for the new container
   * @throws RSuiteException
   */
  public ContentAssembly createContainerForOutputs(RemoteApiExecutionContext context, User user,
      String caForParentMoId, String newContainerName) throws RSuiteException {
    ContentAssemblyService caSvc = context.getContentAssemblyService();
    ContentAssemblyCreateOptions options = new ContentAssemblyCreateOptions();
    ContentAssembly newCa = caSvc.createContentAssembly(user, caForParentMoId, newContainerName,
        options);
    return newCa;
  }

  /**
   * Checks to see if a process definition exists. Throws exception if it doesn't.
   * 
   * @param context The execution context
   * @param workflowDefinitionName The string to check against the list of process definitions.
   * @throws RSuiteException
   */
  public void checkProcessDefinitionKey(RemoteApiExecutionContext context,
      String workflowDefinitionKey) throws RSuiteException {

    if (workflowDefinitionKey != null && !"".equals(workflowDefinitionKey)) {
      // processDefinitionName is not null and not the empty string
      // so see if there is a workflow definition with its name
      // pull into a utility function?
      List<WorkflowDefinition> wfDefs = context.getWorkflowDefinitionService()
          .getWorkflowDefinitions();

      boolean foundWorkflow = false;

      for (WorkflowDefinition curDef : wfDefs) {
        String curDefKey = curDef.getInternalKey();
        if (workflowDefinitionKey.equals(curDefKey)) {
          foundWorkflow = true;
          break;
        }
      }

      if (!foundWorkflow) {
        // end web service the right way?
        throw new RSuiteException("You must provide a valid workflow name");
      }
    } else {
      // workflowName was null or blank so need to throw an exception
      throw new RSuiteException(
          "You must provide a valid workflow name. processDefinitionName parameter cannot be null or the empty string.");
    }

  }

  /**
   * Look to see if a container has a child container named with the param
   * nameTargetOutputContainer. If it does, return its id. If not return 0.
   * 
   * @param context The execution context
   * @param user The current user
   * @param caForParentMo The content assembly to look in
   * @param nameTargetOutputContainer The name of the container we're looking to see if it exists
   * @return Return id of child content assembly whose name matches nameTargetOutputContainer or
   *         return 0 if does not exist.
   * @throws RSuiteException
   */
  public String getIdTargetOutputContainer(RemoteApiExecutionContext context, User user,
      ContentAssemblyNodeContainer caForParentMo, String nameTargetOutputContainer)
      throws RSuiteException {

    String idForContainer = "0";

    List<? extends ContentAssemblyItem> listOfChildren = caForParentMo.getChildrenObjects();

    for (ContentAssemblyItem curChild : listOfChildren) {
      String curItemName = curChild.getDisplayName();
      if (nameTargetOutputContainer.equals(curItemName)) {
        String curItemId = curChild.getId();
        ManagedObject curItemMo = context.getManagedObjectService().getManagedObject(user,
            curItemId);
        curItemMo = RSuiteUtils.getRealMo(context, user, curItemMo);
        idForContainer = curItemMo.getId();
        break;
      }
    }
    return idForContainer;
  }

  /**
   * Start a workflow.
   * 
   * @param context The execution context
   * @param user The current user
   * @param processDefinitionKey The key attribute of the workflow process to start.
   * @param params A map of parameters to be passed to the workflow.
   * 
   */
  public void startWorkflow(RemoteApiExecutionContext context, User user,
      String processDefinitionKey, Map<String, Object> params) {
    try {
      WorkflowInstance info = context.getWorkflowInstanceService().startWorkflow(
          processDefinitionKey, params);
      log.info("Started process: " + info.getWorkflowDefinitionName() + " with wfName " + info
          .getName() + " with wfKey " + info.getWorkflowDefinitionKey() + " with id: " + info
              .getId());
    } catch (Exception e1) {
      log.warn("unable to start workflow process from StartDitaOtWorkflowWebService", e1);
    }

  }

  /**
   * Get the parent managed object (i.e., a container or container node) of an item from the browse
   * tree
   * 
   * @param context The execution context
   * @param args The call arguments
   * @param user The current user
   * @return The parent managed object (dereferenced to be the real parent mo)
   * @throws RSuiteException
   */
  public ManagedObject getParentMo(RemoteApiExecutionContext context, CallArgumentList args,
      User user) throws RSuiteException {

    List<ContentObjectPath> contentObjectPaths = args.getContentObjectPaths(user);
    ContentObjectPath firstPath = contentObjectPaths.get(0);

    ManagedObject parentMo = null;

    ContentDisplayObject parentObject = null;

    if (firstPath.getSize() > 1) {
      // parentObject will be the second to last item, which is the size of first path - 2
      parentObject = firstPath.getPathObjects().get(firstPath.getSize() - 2);
      parentMo = parentObject.getManagedObject();
      parentMo = RSuiteUtils.getRealMo(context, user, parentMo);
    } else {
      log.info("Object has no parent: [" + firstPath.getUri() + "]");

    }

    return parentMo;

  }

}
