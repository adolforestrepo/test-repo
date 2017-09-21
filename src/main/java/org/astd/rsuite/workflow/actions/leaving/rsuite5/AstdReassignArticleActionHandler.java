package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.astd.rsuite.domain.ContainerType;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ManagedObjectReference;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.control.ContentAssemblyCreateOptions;
import com.reallysi.rsuite.api.control.ObjectAttachOptions;
import com.reallysi.rsuite.api.control.ObjectCheckInOptions;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.service.WorkflowInstanceService;

/**
 * Reassign an article.
 */

public class AstdReassignArticleActionHandler
    extends BaseWorkflowAction
    implements TempWorkflowConstants {

  private static final long serialVersionUID = 1L;

  public static final String VAR_PUB_CODE = "form.pubcode";
  public static final String VAR_TYPE = "form.type";
  public static final String VAR_VOLUME = "form.volume";
  public static final String VAR_ISSUE = "form.issue";
  public static final String VAR_SEQUENCE = "form.sequence";
  public static final String VAR_AUTHOR = "form.author";

  @Override
  public void execute(WorkflowContext context) throws Exception {
    Log wfLog = context.getWorkflowLog();
    wfLog.info("Attempting to reassign article...");

    try {
      User user = context.getAuthorizationService().getSystemUser();
      String moid = context.getVariableAsString("rsuite contents");
      wfLog.info("MOID (us): " + moid);
      ManagedObjectService mosvc = context.getManagedObjectService();
      ManagedObject mo = mosvc.getManagedObject(user, moid);
      if (!mo.isAssemblyNode()) {
        reportAndThrowRSuiteException("MO is not a content assembly");
      }

      AstdArticleFilename curName = null;
      try {
        curName = new AstdArticleFilename(mo.getDisplayName());
      } catch (Exception e) {
        reportAndThrowRSuiteException("MO does not have a valid article name: " + e
            .getLocalizedMessage());
      }

      ContentAssembly us = context.getContentAssemblyService().getContentAssembly(user, moid);
      if (AstdActionUtils.isArticleContentsLocked(us)) {
        reportAndThrowRSuiteException("Article " + curName
            + " has locked content, cannot reassign");
      }

      // Get form values: If a form value not set, we default to
      // value from current name.
      String pubCode = context.getVariableAsString(VAR_PUB_CODE);
      if (StringUtils.isEmpty(pubCode)) {
        pubCode = curName.pubCode;
      }
      String type = context.getVariableAsString(VAR_TYPE);
      if (StringUtils.isEmpty(type)) {
        type = curName.type;
      }
      String volume = context.getVariableAsString(VAR_VOLUME);
      if (StringUtils.isEmpty(volume)) {
        volume = curName.volume;
      }
      String issue = context.getVariableAsString(VAR_ISSUE);
      if (StringUtils.isEmpty(issue)) {
        issue = curName.issue;
      }
      String sequence = context.getVariableAsString(VAR_SEQUENCE);
      if (StringUtils.isEmpty(sequence)) {
        sequence = curName.sequence;
      }
      String author = context.getVariableAsString(VAR_AUTHOR);
      if (StringUtils.isEmpty(author)) {
        author = curName.author;
      }

      boolean sameIssue = curName.pubCode.equals(pubCode) && curName.issue.equals(issue)
          && curName.volume.equals(volume);
      String newName = pubCode + type + volume + issue + sequence + author;
      wfLog.info("Current CA name: " + curName);
      wfLog.info("New CA name: " + newName);

      /*******
       * NOTE: In this part we have the current CA name and the new CA name. The new conten assembly
       * should have the new CA name.
       **/

      if (!sameIssue) {
        /*
         * boolean isUnassigned = "99".equals(volume); String folderRoot = "/" + FOLDER_MAGAZINE +
         * "/" + ArticlePubCode.getPubDesc(pubCode); String folder = null; if (isUnassigned) {
         * folder = folderRoot; } else { folder = folderRoot + "/" + "Volume " + volume; }
         * wfLog.info("New folder location: " + folder); try {
         */
        /****
         * This part is deprecated as we do not create folder but CA, as you can see the resulting
         * folder is empty. This method ca be replaced for a code similar to the one found in:
         * 
         * ProjectCreateCAsActionHandler
         * 
         * As you can see, in that class the Magazine folder is created by knowing the id of the
         * root element "4". Then the id of the Magazine Ca is used to create the publication Ca,
         * then passes the resulting CA id as parent to create the Volume CA. And this Volume Ca is
         * the container you should use some lines below when creating the issue container. Notice
         * that sinse the option caCreateOp.setSilentIfExists(true); No new container will be
         * created is they exists.
         * 
         * 
         */


        /*
         * folder = AstdActionUtils.createFolder( user, folder, context); } catch (Exception e) {
         * reportAndThrowRSuiteException("Unable to create folder: " + e.getLocalizedMessage()); }
         * 
         * String caContainer = null; if (isUnassigned) { caContainer = "Unassigned"; } else {
         * caContainer = "Issue " + issue; } wfLog.info("New container CA: " + caContainer); String
         * caId = null;
         */
        /**
         * NOTE: Since the previous folder variable is empty it is why the issue is attached to the
         * root folder. Here is where you use the id of the VOlume Ca created before, intead of the
         * folder param. YOu should also use a diferent method.
         **/
        /*
         * try { caId = AstdActionUtils.createCA( context.getRepositoryService(), user, caContainer,
         * folder, context); } catch (Exception e) { reportAndThrowRSuiteException(
         * "Unable to create CA: " + e.getLocalizedMessage()); } wfLog.info("New container CA ID: "
         * +caId);
         * 
         * // Attempt to move article CA to new location ContentAssembly newParent =
         * context.getContentAssemblyService().getContentAssembly( user, caId); try { wfLog.info(
         * "Moving CA"); NOTE: Seems like this method is failing. Try to atthac to the newParent and
         * then deattach from the previous parent. context.getContentAssemblyService(). moveTo(
         * user, newParent.getId(), us.getId()); } catch (Exception e) {
         * reportAndThrowRSuiteException("Unable to move CA: " + e.getLocalizedMessage()); }
         */



        /*
         * 
         * CA creation
         * 
         * 
         */

        String VOLUME_STRING = "Volume";
        String MAGAZINES_NAME = "Magazines";
        String pubCode_NAME = pubCode;

        ContentAssembly contentCA = null;

        ContentAssemblyService caSrv = context.getContentAssemblyService();
        if (volume.equals("99")) {

          ContentAssemblyCreateOptions caCreateOp = new ContentAssemblyCreateOptions();
          caCreateOp.setSilentIfExists(true);

          caCreateOp.setType(ContainerType.FOLDER.getSystemName());
          ContentAssembly magazinesCa = caSrv.createContentAssembly(user, "4", MAGAZINES_NAME,
              caCreateOp);
          ContentAssembly tpmCa = caSrv.createContentAssembly(user, magazinesCa.getId(),
              pubCode_NAME, caCreateOp);
          caCreateOp.setType(ContainerType.CA.getSystemName());
          ContentAssembly articleCA = caSrv.createContentAssembly(user, tpmCa.getId(), newName,
              caCreateOp);

          caSrv.attach(user, articleCA.getId(), moid, new ObjectAttachOptions());
          context.setVariable(ATD_VAR_CA_ID, articleCA.getId());
          contentCA = articleCA;

        } else {

          ContentAssemblyCreateOptions caCreateOp = new ContentAssemblyCreateOptions();
          caCreateOp.setSilentIfExists(true);

          caCreateOp.setType(ContainerType.FOLDER.getSystemName());
          ContentAssembly magazinesCa = caSrv.createContentAssembly(user, "4", MAGAZINES_NAME,
              caCreateOp);
          ContentAssembly tpmCa = caSrv.createContentAssembly(user, magazinesCa.getId(),
              pubCode_NAME, caCreateOp);
          ContentAssembly volumeCa = caSrv.createContentAssembly(user, tpmCa.getId(), VOLUME_STRING
              + " " + volume, caCreateOp);

          caCreateOp.setType(ContainerType.CA.getSystemName());
          ContentAssembly monthCa = caSrv.createContentAssembly(user, volumeCa.getId(), "Issue "
              + issue, caCreateOp);
          ContentAssembly articleCA = caSrv.createContentAssembly(user, monthCa.getId(), newName,
              caCreateOp);


          @SuppressWarnings("unchecked")
          List<? extends ManagedObjectReference> moids =
              (List<? extends ManagedObjectReference>) caSrv.getContentAssembly(user, moid)
                  .getChildrenObjects();

          for (ManagedObjectReference caid : moids) {
            caSrv.attach(user, articleCA.getId(), caid.getTargetId(), new ObjectAttachOptions());
            // Add code for rename the non-xml documents

            wfLog.info("Check if need to rename " + caid.getDisplayName());
            if (AstdActionUtils.isNonXml(caid)) {

              String newDisplayName = newName + "." + FilenameUtils.getExtension(caid
                  .getDisplayName());


              wfLog.info("Attempting to rename " + caid.getDisplayName() + " to " + newDisplayName);

              context.getManagedObjectService().checkOut(user, caid.getTargetId());
              context.getManagedObjectService().setDisplayName(user, caid.getTargetId(),
                  newDisplayName);
              context.getManagedObjectService().checkIn(user, caid.getTargetId(),
                  new ObjectCheckInOptions());

              if (newDisplayName != null) {
                wfLog.info("Setting alias for " + caid.getTargetId() + " to \"" + newDisplayName
                    + "\"");
                mosvc.setAlias(user, caid.getTargetId(), newDisplayName);
              }


            }

            /*
             * context.getContentAssemblyService().renameCANode(user, caid.getTargetId(), articleCA
             * .getDisplayName());
             */
          }

        }

      }

      // Attempt to rename article CA
/*
      
      try {
        wfLog.info("Renaming CA " + moid + " to " + newName);
        context.getContentAssemblyService().renameCANode(user, moid, newName);
      } catch (Exception e) {
        reportAndThrowRSuiteException("Unable to rename CA: " + e.getLocalizedMessage());
      }

      // Re-fetch CA instance since previous operations change CA and we
      // need to make sure we are in-sync with cache.
      us = context.getContentAssemblyService().getContentAssembly(user, moid);

      // Rename non-XML child nodes that follow article naming convention
      List<? extends ContentAssemblyItem> items = us.getChildrenObjects();
      wfLog.info("CA items = " + items);
      if (items != null && items.size() > 0) {
        for (ContentAssemblyItem item : items) {
          wfLog.info("Check if need to rename " + item.getDisplayName());
          if (item instanceof ManagedObjectReference) {
            ManagedObjectReference mor = (ManagedObjectReference) item;
            // @SuppressWarnings("deprecation")

            String sid = mor.getTargetId();

            if (!AstdActionUtils.isNonXml(mor)) {
              // For xml objects, we need to set alias since
              // transform/export operations may depend on it.
              wfLog.info("Setting new alias for MO " + sid + ": " + newName + ".xml");
              mosvc.setAlias(user, sid, newName + ".xml");
              continue;
            }
            wfLog.info("Attempting to rename " + item.getDisplayName() + " to " + newName);
            String newNonXmlName = AstdActionUtils.renameArticleNonXmlMo(user, mosvc, wfLog, sid,
                newName);

            // XXX: Bug in Rsuite 3.3.1 (and earlier) where alias
            // is not updated in previous utility method, therefore,
            // we explicitly set it here (again)

            if (newNonXmlName != null) {
              wfLog.info("Setting alias for " + sid + " to \"" + newNonXmlName + "\"");
              mosvc.setAlias(user, sid, newNonXmlName);
            }
          }
        }
      }
*/
      // Update process variables
      String pid = mo.getLayeredMetadataValue(ASTD_ARTICLE_PID_LMD_FIELD);
      if (!StringUtils.isEmpty(pid)) {
        wfLog.info("Attempting to update variables for process " + pid);
        WorkflowInstanceService psvc = context.getWorkflowInstanceService();
        Object pi = psvc.getWorkflowInstance(pid);
        if (pi == null) {
          wfLog.info("No process with id " + pid + " found, clearing LMD field");
          AstdActionUtils.clearArticlePidLmdField(context, mo, user);
        } else {
          // FIXME: When API is made available to change variables, update
          // block. The following code has no effect.
          /*
           * List pVars = pi.getVariables(); for (Object obj : pVars) { VariableInfo var =
           * (VariableInfo)obj; String varName = var.getName(); wfLog.info("\t(old) "
           * +varName+"="+var.getValue()); if
           * (varName.equals(AstdWorkflowConstants.ASTD_VAR_PUB_CODE)) { var.setValue(pubCode); }
           * else if (varName.equals(AstdWorkflowConstants.ASTD_VAR_FULL_FILENAME)) {
           * var.setValue(newName+".docx"); } else if
           * (varName.equals(AstdWorkflowConstants.ASTD_VAR_SOURCE_FILENAME)) {
           * var.setValue(newName); } else if
           * (varName.equals(AstdWorkflowConstants.ASTD_VAR_ARTICLE_TYPE)) { var.setValue(type); }
           * else if (varName.equals(AstdWorkflowConstants.ASTD_VAR_VOLUME_NUMBER)) {
           * var.setValue(volume); } else if (varName.equals(AstdWorkflowConstants.ASTD_VAR_ISSUE))
           * { var.setValue(issue); } else if (varName.equals(AstdWorkflowConstants.ASTD_VAR_MONTH))
           * { var.setValue(issue); } else if
           * (varName.equals(AstdWorkflowConstants.ASTD_VAR_SEQUENCE)) { var.setValue(sequence); }
           * else if (varName.equals(AstdWorkflowConstants.ASTD_VAR_FILENAME_AUTHOR)) {
           * var.setValue(author); } wfLog.info("\t(new) "+varName+"="+var.getValue()); }
           */
        }
      }

    } catch (Exception e) {
      context.setVariable("REASSIGN_MSGS", e.getLocalizedMessage());
      throw e;
    }
  }

}
