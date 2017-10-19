package org.astd.rsuite.webservices.rsuite5;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.webservices.BaseWebService;
import org.astd.rsuite.workflow.actions.leaving.rsuite5.AstdActionUtils;

import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.PlainTextResult;
import com.reallysi.rsuite.service.ManagedObjectService;

/**
 * Converts a Content Assembly node to a Content assembly container.
 */
public class ConvertCanodeToCA extends BaseWebService {

	private static final String CA_TYPE = "ca-type";
	private static final String ARTICLE_PROCESS_ID = "article-process-id";
	private static Log log = LogFactory.getLog(ConvertCanodeToCA.class);

	@Override
	public RemoteApiResult execute(RemoteApiExecutionContext context,
			CallArgumentList args) throws RSuiteException {

		// User must be an administrator.
		User user = context.getSession().getUser();
		if (!context.getAuthorizationService().isAdministrator(user)) {
			return getErrorResult(ProjectMessageResource
					.getMessageText("security.error.must.be.admin"));
		}

		String paramName = "rsuiteId";

		String canodeId = args.getFirstString(paramName);
		if (StringUtils.isBlank(canodeId)) {
			return getErrorResult(ProjectMessageResource.getMessageText(
					"web.service.error.required.parameter.value.not.specified",
					paramName));
		}

		ManagedObjectService moSvr = context.getManagedObjectService();

		String caid = "";
		String ca_type = moSvr.getManagedObject(user, canodeId)
				.getLayeredMetadataValue(CA_TYPE);
		String process_id = moSvr.getManagedObject(user, canodeId)
				.getLayeredMetadataValue(ARTICLE_PROCESS_ID);
		/* Captuere LMD from the old Canode */

		try {
			caid = AstdActionUtils.convertCAnodeToCA(context, canodeId);
		} catch (Exception e) {
			return new PlainTextResult("Error Coverting CAnode to CA");
		}

		MetaDataItem lmd_ca_type = new MetaDataItem(CA_TYPE, ca_type);
		MetaDataItem lmd_process_id = new MetaDataItem(ARTICLE_PROCESS_ID, process_id);
		moSvr.setMetaDataEntry(user, caid, lmd_ca_type);
		moSvr.setMetaDataEntry(user, caid, lmd_process_id);

		return new PlainTextResult("New CAontent assembly id: " + caid);

	}

}
