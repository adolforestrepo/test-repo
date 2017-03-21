package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.delegate.Expression;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.ValidationException;
import com.reallysi.rsuite.api.VersionType;
import com.reallysi.rsuite.api.control.ObjectAttachOptions;
import com.reallysi.rsuite.api.control.ObjectInsertOptions;
import com.reallysi.rsuite.api.control.ObjectSource;
import com.reallysi.rsuite.api.control.ObjectUpdateOptions;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.FileWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoListWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.MoWorkflowObject;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.reallysi.rsuite.service.ManagedObjectService;

/**
 * Action handler to load a data source by alias.
 * <p>
 * If the alias already exists, the MO associated with the alias will be updated. If the alias does
 * not exist, a new MO is created and an alias is created for it.
 * </p>
 **/
public abstract class ProjectLoadByAliasBaseActionHandler
    extends BaseWorkflowAction
    implements TempWorkflowConstants {

  /**
   * Optional. Parameter to take the name of the alias to use for a managed object.
   */
  public static final String ALIAS_PARAM = "alias";

  /**
   * Optional. MO ID of the CA node to attach the loaded or updated MO to if it is not already
   * attached.
   */
  public static final String PARENT_MO_ID_PARAM = "parentMoIdFromWorkflow";

  /**
   * Specifies the message to use for any commits to the repository. Default value is "Automatic
   * update".
   */
  public static final String COMMIT_MESSAGE_PARAM = "commitMessageFromWorkflow";

  /**
   * 
   */
  public static final String FILE_TO_LOAD_PARAM = "fileToLoad";

  protected Expression parentMoIdFromWorkflow;
  protected Expression commitMessageFromWorkflow;
  protected Expression fileToLoadFromWorkflow;


  protected void doLoadOrUpdate(WorkflowContext context, Log wfLog, File inFile, ObjectSource src,
      String parentMoId) throws RSuiteException, ValidationException {
    String alias = getEffectiveAlias(context, inFile.getName());
    wfLog.info("Alias to load to: " + alias);

    ManagedObjectService moService = context.getManagedObjectService();
    ManagedObject newMo = null;
    String commitMessage = getCommitMessage(context);

    User sys = context.getAuthorizationService().getSystemUser();
    List<ManagedObject> mos = moService.getObjectsByAlias(sys, alias);
    if (mos.size() == 0) {
      wfLog.info("No existing MO with alias \"" + alias + "\", creating new MO...");
      String[] aliases = {alias};
      ObjectInsertOptions options = new ObjectInsertOptions(alias, aliases, null, true);
      options.setDisplayName(inFile.getName());
      newMo = moService.load(sys, src, options);
      wfLog.info("Created new MO \"" + newMo.getDisplayName() + "[" + newMo.getId() + "]");
    } else if (mos.size() > 1) {
      throw new RSuiteException(0, "Found " + mos.size() + " with alias \"" + alias
          + ". There should be at most one such MO.");
    } else {
      // Found an existing MO, update it with the new content.
      ManagedObject mo = mos.get(0);
      wfLog.info("Found existing MO \"" + mo.getDisplayName() + "[" + mo.getId()
          + "], updating with XML from incoming file...");
      moService.checkOut(sys, mo.getId());
      ObjectUpdateOptions options = ObjectUpdateOptions.constructOptionsForNonXml(inFile.getName(),
          FilenameUtils.getExtension(inFile.getName()));
      options.setValidate(true);
      try {
        newMo = moService.update(sys, mo.getId(), src, options);
      } catch (Exception e) {
        String msg = "Unexpected failure on update of mo [" + mo.getId() + "]: " + e.getMessage();
        reportAndThrowRSuiteException(msg);
      } finally {
        moService.checkIn(sys, newMo.getId(), VersionType.MINOR, commitMessage, true);
      }
      wfLog.info("MO updated, new display name is \"" + newMo.getDisplayName() + "\"");
    }

    if (parentMoId != null && !"".equals(parentMoId.trim())) {
      wfLog.info("Parent MO ID specified, trying to find and attach...");
      try {
        ContentAssemblyService caService = context.getContentAssemblyService();
        // Attach new MO to parent if not already there:
        ContentAssemblyNodeContainer caNode = caService.getContentAssemblyNodeContainer(sys,
            parentMoId);
        wfLog.info("Found specified parent CA Node " + caNode.getDisplayName() + " [" + caNode
            .getId() + "]");
        if (!caNode.hasChild(newMo)) {
          wfLog.info("CA Node does not already have new MO, attaching it...");
          ObjectAttachOptions options = new ObjectAttachOptions();
          List<ManagedObject> moList = new ArrayList<ManagedObject>();
          moList.add(newMo);
          caService.attach(sys, caNode.getId(), moList, options);
          wfLog.info("New MO attached to CA node.");
        } else {
          wfLog.info("New MO is already attached to the specified parent CA node.");
        }
      } catch (Exception e) {
        reportAndThrowRSuiteException(
            "Exception attempting to attach new/updated MO to browse tree: " + e.getMessage());
      }
    }

    MoWorkflowObject moWFO = new MoWorkflowObject(newMo.getId());
    MoListWorkflowObject moList = new MoListWorkflowObject();
    moList.addMoObject(moWFO);
    context.setMoListWorkflowObject(moList);
  }

  public void setAlias(String alias) {
    setParameter(ALIAS_PARAM, alias);
  }

  public void setCommitMessage(String commitMessage) {
    setParameter(COMMIT_MESSAGE_PARAM, commitMessage);
  }

  protected String getEffectiveAlias(WorkflowContext context, String defaultAlias)
      throws RSuiteException {
    String alias = getParameter(ALIAS_PARAM);
    if (!StringUtils.isBlank(alias)) {
      alias = resolveVariables(alias);
    } else {
      alias = defaultAlias;
    }
    return alias;
  }

  public void setFileToLoad(String fileToLoad) {
    setParameter(FILE_TO_LOAD_PARAM, fileToLoad);
  }

  protected File getFileToLoad(WorkflowContext context)
      throws RSuiteException, FileNotFoundException {
    File inFile;
    String fileToLoad = resolveVariables(getParameter(FILE_TO_LOAD_PARAM));
    fileToLoad = getWorkflowVariableOrParameter(context, FILE_TO_LOAD_PARAM,
        fileToLoadFromWorkflow);
    if (fileToLoad == null || "".equals(fileToLoad.trim())) {
      FileWorkflowObject fileWFO = context.getFileWorkflowObject();

      if (fileWFO == null) {
        String msg = "No file workflow object in workflow context, nothing to do.";
        reportAndThrowRSuiteException(msg);
      }

      inFile = fileWFO.getFile();
    } else {
      inFile = new File(fileToLoad);
      if (!inFile.exists()) {
        throw new FileNotFoundException("Specified file to load \"" + fileToLoad + "\" not found.");
      }
    }
    return inFile;
  }

  /**
   * Gets the parent MO ID specified in a workflow variable.
   * 
   * @param context
   * @return MO ID or null or empty string.
   */
  protected String getParentMoId(WorkflowContext context) {
    // String parentMoId = resolveExpression(parentMoIdFromWorkflow);
    String parentMoId = context.getVariableAsString(PARENT_MO_ID_PARAM);
    if (StringUtils.isNotEmpty(parentMoId)) {
      return parentMoId;
    }
    parentMoId = resolveVariablesAndExpressions(getParameter(PARENT_MO_ID_PARAM));
    if (StringUtils.isNotEmpty(parentMoId)) {
      return parentMoId;
    }
    parentMoId = resolveExpression(parentMoIdFromWorkflow);
    context.getWorkflowLog().info("Resolved expression [" + parentMoId + "]");
    return parentMoId;
  }

  /**
   * Gets the commit message specified in a workflow variable.
   * 
   * @param context
   * @return commit message or null or empty string.
   */
  protected String getCommitMessage(WorkflowContext context) {
    String commitMessage = context.getVariableAsString(COMMIT_MESSAGE_PARAM);
    if (StringUtils.isNotEmpty(commitMessage)) {
      return commitMessage;
    }
    commitMessage = resolveVariablesAndExpressions(getParameter(COMMIT_MESSAGE_PARAM));
    if (StringUtils.isNotEmpty(commitMessage)) {
      return commitMessage;
    }
    commitMessage = resolveExpression(commitMessageFromWorkflow);
    context.getWorkflowLog().info("Resolved expression [" + commitMessage + "]");
    return commitMessage;
  }

  public void setParentMoId(String parentMoId) {
    setParameter(PARENT_MO_ID_PARAM, parentMoId);
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

}
