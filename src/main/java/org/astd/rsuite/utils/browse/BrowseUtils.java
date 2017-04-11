package org.astd.rsuite.utils.browse;

import java.util.List;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.ReferenceInfo;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.service.ManagedObjectService;

public class BrowseUtils {

	public static String getBrowserUri (ExecutionContext context, String moId) throws RSuiteException {
		User systemUser = context.getAuthorizationService().getSystemUser();
		ManagedObjectService moServ = context.getManagedObjectService();
		List<ReferenceInfo> refernces = moServ.getDependencyTracker()
				.listAllReferences(systemUser, moId);
		
		if (refernces.size() > 0) {
			ReferenceInfo refernceInfo = refernces.get(0);
			return refernceInfo.getBrowseUri();
		}
		
		return null;
	}
}
