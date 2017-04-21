package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.delegate.Expression;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.astd.rsuite.domain.ArticleFileName;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;

public class AtdValidateFileNameActionHandler
    extends BaseWorkflowAction
    implements TempWorkflowConstants {

  protected Log wfLog;

  public static final String ALLOWED_EXT_PARAM = "allowedExtension";
  public static final String EXT_ERROR_TXT_PARAM = "extensionErrorMessage";
  public static final String ERROR_IF_LOCKED_PARAM = "errorIfLocked";
  public static final String ERROR_IF_EXISTS_PARAM = "errorIfExits";

  protected Expression allowedExtension;
  protected Expression extensionErrorMessage;
  protected Expression errorIfLocked;
  protected Expression errorIfExits;

  @Override
  public void execute(WorkflowContext context) throws Exception {
    wfLog = context.getWorkflowLog();
    logActionHandlerParameters(wfLog);

    String workingFolder = context.getVariableAsString(com.reallysi.rsuite.api.workflow.WorkflowConstants.VAR_WORKINGFOLDER_PATH);

    File workingDir = new File(workingFolder);

    wfLog.info("WorkingDir >> " + workingDir);

    String allowedExt = resolveExpression(allowedExtension);
    if (StringUtils.isBlank(allowedExt)) {
      allowedExt = null;
    } else {
      allowedExt = allowedExt.toUpperCase();
    }
    wfLog.info("Allowed extension: " + allowedExt);

    String s = resolveExpression(errorIfExits);
    boolean errorIfExists = (s != null && s.equals("true"));
    s = resolveExpression(errorIfLocked);
    boolean errorIfLocked = (s != null && s.equals("true"));

    String errTxt = resolveExpression(extensionErrorMessage);

    File[] files = workingDir.listFiles();

    List<String> errorMsgs = new ArrayList<String>();

    ArticleFileName astdName = null;
    for (File file : files) {
      // we're only expecting one file in the working directory
      String name = file.getName();
      wfLog.info("Checking filename: " + name);
      try {
        astdName = new ArticleFileName(name);
        if (allowedExt != null) {
          String inExt = astdName.getExtension();
          if (inExt == null || !inExt.toUpperCase().equals(allowedExt)) {
            if (!StringUtils.isBlank(errTxt)) {
              errorMsgs.add("File \"" + name + "\" rejected: " + errTxt);
            } else {
              errorMsgs.add("File \"" + name + "\" rejected: " + "File does not have extension \"" + allowedExt + "\"");
            }
          }
        }
      } catch (IllegalArgumentException iae) {
        errorMsgs.add("File \"" + name + "\" rejected: " + iae.getMessage());
      }

      if (errorIfExists || errorIfLocked) {
        wfLog.info("Checking if file already exists or is locked...");
        ManagedObject o = context.getManagedObjectService().getObjectByAlias(context.getAuthorizationService().getSystemUser(), name);
        wfLog.info("MO from alias \"" + name + "\": " + o);
        if (o != null) {
          if (errorIfExists) {
            errorMsgs.add("File \"" + name + "\" rejected: " + "File already exists");
          }
          if (errorIfLocked && o.isCheckedout()) {
            errorMsgs.add("File \"" + name + "\" rejected: " + "File exists and is currently locked");
          }
        }
      }
    }

    if (errorMsgs.size() > 0) {
      StringBuilder errorMsgBuf = new StringBuilder();
      for (String errorMsg : errorMsgs.toArray(new String[errorMsgs.size()])) {
        errorMsgBuf.append(errorMsg).append('\n');
      }
      context.setVariable("VALIDATION_MSGS", errorMsgBuf.toString());
    } else {
      context.setVariable("VALIDATION_MSGS", null);
      setWorkflowVariables(context, astdName);
    }

  }

  /**
   * Set workflow variables for the new article.
   * 
   * @param context
   * @param astdName
   */
  protected void setWorkflowVariables(WorkflowContext context, ArticleFileName astdName) {

    context.setVariable(ATD_VAR_PUB_CODE, astdName.getPubCode());
    context.setVariable(ATD_VAR_FULL_FILENAME, astdName.getFullFileName());
    context.setVariable(ATD_VAR_SOURCE_FILENAME, astdName.getSourceFileName());
    context.setVariable(ATD_VAR_ARTICLE_TYPE, astdName.getType());
    context.setVariable(ATD_VAR_VOLUME_NUMBER, astdName.getVolume());
    context.setVariable(ATD_VAR_ISSUE, astdName.getIssue());
    context.setVariable(ATD_VAR_MONTH, astdName.getIssue());
    context.setVariable(ATD_VAR_SEQUENCE, astdName.getSequence());
    context.setVariable(ATD_VAR_FILENAME_AUTHOR, astdName.getAuthor());

  }


}
