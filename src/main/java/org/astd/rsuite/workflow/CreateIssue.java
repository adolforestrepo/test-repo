package org.astd.rsuite.workflow;



import static org.astd.rsuite.constants.AstdConstants.PARAM_RSUITE_ID;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;



import org.astd.rsuite.constants.CaConstants;
import org.astd.rsuite.constants.LmdConstants;
import org.astd.rsuite.utils.CAUtils;
import org.astd.rsuite.utils.ContentDisplayUtils;
import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.control.ContentAssemblyCreateOptions;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageType;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.rsicms.rsuite.helpers.webservice.RemoteApiHandlerBase;

public class CreateIssue extends RemoteApiHandlerBase {

	@Override
	public RemoteApiResult execute(
			RemoteApiExecutionContext context,
			CallArgumentList args) throws RSuiteException {
		ContentAssemblyService caSrv = context.getContentAssemblyService();
		User user = context.getSession().getUser();

		String volume = args.getFirstString("volume");
		String issue = args.getFirstString("issue");
		String caId = args.getFirstValue(PARAM_RSUITE_ID.getName());

		ContentAssemblyCreateOptions caCreateOp = new ContentAssemblyCreateOptions();
		caCreateOp.setSilentIfExists(true);

		caCreateOp.setType(CaConstants.CA_TYPE_VOLUME.getName());		
		ContentAssembly volumeCa = caSrv.createContentAssembly(user, caId, volume, caCreateOp);

		caCreateOp.setType(CaConstants.CA_TYPE_ISSUE.getName());
		ContentAssembly issueCa = caSrv.createContentAssembly(user, volumeCa.getId(), issue, caCreateOp);

		
		ContentAssemblyItem prod = CAUtils.getAncestorCAbyType(context, caId, CaConstants.CA_TYPE_PRODUCT.getName());

		String prodParentId = CAUtils.getPaterntId(context, user, prod.getId());

		String journalCode = prod.getLayeredMetadataValue(LmdConstants.LMD_NAME_PRODUCT_CODE.getName());

		if (StringUtils.isBlank(journalCode)) {
			throw new RSuiteException("Product " + prod.getDisplayName() + " [" + prod.getId() +"] needs to have journal_code layered metadata.");
		}

		ContentAssembly prodParent = caSrv.getContentAssembly(user, prodParentId);		

		List<MetaDataItem> issueMetadataList = new ArrayList<MetaDataItem>();
		issueMetadataList.add(new MetaDataItem(LmdConstants.LMD_NAME_FINALIZED.getName(), LmdConstants.LMD_VALUE_NO.getName()));
		if (prodParent.getDisplayName().equals(CaConstants.CA_NAME_IN_PROGERSS.getName())) {
			if (journalCode.equals(LmdConstants.LMD_VALUE_ASTD.getName())) {
				issueMetadataList.add(new MetaDataItem(LmdConstants.LMD_NAME_FINALIZING_TYPE.getName(), LmdConstants.LMD_VALUE_ALL_AT_ONCE.getName()));
			} else {
				throw new RSuiteException("Under In Progress browse tree, issue containers are just suppoused to be created for JBJS"); 
			}
		} else if (prodParent.getDisplayName().equals(CaConstants.CA_NAME_PUBLISHED.getName())) {
			if (!journalCode.equals(LmdConstants.LMD_VALUE_ASTD.getName())) {
				issueMetadataList.add(new MetaDataItem(LmdConstants.LMD_NAME_FINALIZING_TYPE.getName(), LmdConstants.LMD_VALUE_CONTINUOUS.getName()));
			} else {
				throw new RSuiteException("Under Published browse tree, issue containers are not suppoused to be created for JBJS");
			}
		} else {
			throw new RSuiteException("Issues need to be created either into 'In Progress' or 'Published' contexts.");
		}

		context.getManagedObjectService().addMetaDataEntries(user, issueCa.getId(), issueMetadataList);
		
		return ContentDisplayUtils.getResultWithLabelRefreshing(
				MessageType.SUCCESS, "Create Issue", "Issue has been created", "500", caId);
	}

}
