package org.astd.rsuite.workflow.actions.leaving.rsuite5;

import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.astd.rsuite.domain.ContainerType;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ManagedObjectReference;
import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.ReferenceInfo;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.control.ContentAssemblyCreateOptions;
import com.reallysi.rsuite.api.control.ObjectAttachOptions;
import com.reallysi.rsuite.api.control.ObjectCheckInOptions;
import com.reallysi.rsuite.api.control.ObjectDetachOptions;
import com.reallysi.rsuite.api.control.ObjectMetaDataSetOptions;
import com.reallysi.rsuite.api.workflow.activiti.BaseWorkflowAction;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowContext;
import com.reallysi.rsuite.api.workflow.activiti.WorkflowInstance;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.service.WorkflowInstanceService;
import com.reallysi.tools.ditaot.DitaOtOptions;

/**
 * Reassign an article.
 */

public class AstdReassignArticleActionHandler extends BaseWorkflowAction
		implements TempWorkflowConstants {

	private static final long serialVersionUID = 1L;

	public static final String VAR_PUB_CODE = "form.pubcode";
	public static final String VAR_TYPE = "form.type";
	public static final String VAR_VOLUME = "form.volume";
	public static final String VAR_ISSUE = "form.issue";
	public static final String VAR_SEQUENCE = "form.sequence";
	public static final String VAR_AUTHOR = "form.author";

	@SuppressWarnings("unchecked")
	@Override
	public void execute(WorkflowContext context) throws Exception {
		Log wfLog = context.getWorkflowLog();
		wfLog.info("Attempting to reassign article...");
		context.setVariable(DitaOtOptions.EXCEPTION_OCCUR, "false");

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
				reportAndThrowRSuiteException("MO does not have a valid article name: "
						+ e.getLocalizedMessage());
			}

			ContentAssembly us = context.getContentAssemblyService()
					.getContentAssembly(user, moid);
			ContentAssembly articleCA = us;

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

			boolean sameIssue = (curName.pubCode.equals(pubCode)
					|| curName.pubCode.equals("CTDO") || curName.pubCode
						.equals("T and D"))
					&& curName.issue.equals(issue)
					&& curName.volume.equals(volume);

			String newName = pubCode + type + volume + issue + sequence
					+ author;
			wfLog.info("Current CA name: " + curName);
			wfLog.info("New CA name: " + newName);

			/*******
			 * NOTE: In this part we have the current CA name and the new CA
			 * name. The new conten assembly should have the new CA name.
			 **/

			ContentAssemblyService caSrv = context.getContentAssemblyService();

			if (!sameIssue) {

				/*
				 * 
				 * CA creation
				 */

				String VOLUME_STRING = "Volume";
				String MAGAZINES_NAME = "Magazines";
				String pubCode_NAME = pubCode;

				ContentAssembly contentCA = null;

				String catypeArticle = caSrv.getContentAssembly(user, moid)
						.getLayeredMetadataValue("ca-type").toString();

				System.out.println("pubCode_NAME : " + pubCode_NAME);

				if (pubCode_NAME.contentEquals("CT")) {
					System.out.println("Inside if (pubCode equals CT)");
					pubCode_NAME = "CTDO";
				} else if (pubCode_NAME.contentEquals("TD")) {
					System.out.println("Inside if (pubCode equals TD)");
					pubCode_NAME = "T and D";
				}

				if (volume.equals("99")) {

					ContentAssemblyCreateOptions caCreateOp = new ContentAssemblyCreateOptions();
					caCreateOp.setSilentIfExists(true);

					caCreateOp.setType(ContainerType.FOLDER.getSystemName());
					ContentAssembly magazinesCa = caSrv.createContentAssembly(
							user, "4", MAGAZINES_NAME, caCreateOp);
					ContentAssembly tpmCa = caSrv.createContentAssembly(user,
							magazinesCa.getId(), pubCode_NAME, caCreateOp);
					caCreateOp.setType(ContainerType.CA.getSystemName());
					articleCA = caSrv.createContentAssembly(user,
							tpmCa.getId(), newName, caCreateOp);

					caSrv.attach(user, articleCA.getId(), moid,
							new ObjectAttachOptions());
					context.setVariable(ATD_VAR_CA_ID, articleCA.getId());
					contentCA = articleCA;

				} else {

					ContentAssemblyCreateOptions caCreateOp = new ContentAssemblyCreateOptions();
					caCreateOp.setSilentIfExists(true);

					caCreateOp.setType(ContainerType.FOLDER.getSystemName());
					ContentAssembly magazinesCa = caSrv.createContentAssembly(
							user, "4", MAGAZINES_NAME, caCreateOp);
					ContentAssembly tpmCa = caSrv.createContentAssembly(user,
							magazinesCa.getId(), pubCode_NAME, caCreateOp);
					ContentAssembly volumeCa = caSrv.createContentAssembly(
							user, tpmCa.getId(), VOLUME_STRING + " " + volume,
							caCreateOp);

					caCreateOp.setType(ContainerType.CA.getSystemName());
					ContentAssembly monthCa = caSrv.createContentAssembly(user,
							volumeCa.getId(), "Issue " + issue, caCreateOp);
					/*
					 * ContentAssembly articleCA =
					 * caSrv.createContentAssembly(user, monthCa.getId(),
					 * newName, caCreateOp); // detach(User user,
					 * java.lang.String contentAssemblyId, java.lang.String
					 * referrerId, ObjectDetachOptions options)
					 */

					List<String> caRefs = context.getManagedObjectService()
							.getDependencyTracker()
							.listDirectReferenceIds(user, moid);

					String refid = caRefs.get(0);

					caSrv.detach(user, null, refid, new ObjectDetachOptions());

					caSrv.attach(user, monthCa.getId(), articleCA,
							new ObjectAttachOptions());

				}

			}

			caSrv.renameContentAssembly(user, moid, newName);

			@SuppressWarnings("unchecked")
			List<? extends ManagedObjectReference> moids = (List<? extends ManagedObjectReference>) caSrv
					.getContentAssembly(user, moid).getChildrenObjects();

			for (ManagedObjectReference caid : moids) {
				// caSrv.attach(user, articleCA.getId(), caid.getTargetId(), new
				// ObjectAttachOptions());
				// Add code for rename the non-xml documents
				wfLog.info("Check if need to Rename " + caid.getDisplayName());
				if (AstdActionUtils.isNonXml(caid)) {
					String newDisplayName = newName + "."
							+ FilenameUtils.getExtension(caid.getDisplayName());
					wfLog.info("Attempting to Rename " + caid.getDisplayName()
							+ " to " + newDisplayName);
					wfLog.info("Checking out MO (" + caid.getTargetId()
							+ ") under user " + user.getUserId());
					context.getManagedObjectService().checkOut(user,
							caid.getTargetId());
					context.getManagedObjectService().setDisplayName(user,
							caid.getTargetId(), newDisplayName);
					wfLog.info("Checking In MO (" + caid.getTargetId()
							+ ") under user " + user.getUserId());
					context.getManagedObjectService().checkIn(user,
							caid.getTargetId(), new ObjectCheckInOptions());
					if (newDisplayName != null) {
						wfLog.info("Setting Alias for " + caid.getTargetId()
								+ " to \"" + newDisplayName + "\"");
						mosvc.setAlias(user, caid.getTargetId(), newDisplayName);
					}

				}

			}
			/*
			 * TODO: Find a way to modify the Workflow variables. of the main workflow process.
			 * 
			 * 
			 * String pid = mo
			 * .getLayeredMetadataValue(ASTD_ARTICLE_PID_LMD_FIELD);
			 * 
			 * ManagedObject mo1 = mosvc.getManagedObject(user,
			 * articleCA.getId());
			 * 
			 * 
			 * 
			 * if (!StringUtils.isEmpty(pid)) {
			 * 
			 * wfLog.info("Attempting to update variables for process " + pid);
			 * WorkflowInstanceService psvc =
			 * context.getWorkflowInstanceService(); WorkflowInstance pi =
			 * psvc.getWorkflowInstance(pid);
			 * 
			 * 
			 * if (pi == null) { wfLog.info("No process with id " + pid +
			 * " found, clearing LMD field");
			 * AstdActionUtils.clearArticlePidLmdField(context, mo, user); }
			 * else {
			 * 
			 * WorkflowInstanceService wfsvr =
			 * context.getWorkflowInstanceService(); context.get
			 * 
			 * 
			 * 
			 * 
			 * } }
			 */

		} catch (Exception e) {
			context.setVariable("REASSIGN_MSGS", e.getLocalizedMessage());
			throw e;
		}

	}
}
