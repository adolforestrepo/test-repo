package org.astd.rsuite.workflow;

//import static com.jbjs.rsuite.contants.CaConstants.CA_NAME_IN_PROGERSS;
import static org.astd.rsuite.constants.CaConstants.CA_NAME_IN_PROGERSS;

import static org.astd.rsuite.constants.AstdConstants.PARAM_RSUITE_ID;
//import static com.jbjs.rsuite.contants.JbjsConstants.PARAM_RSUITE_ID;
//import static com.jbjs.rsuite.contants.LmdConstants.LMD_NAME_FINALIZING_TYPE;
import static org.astd.rsuite.constants.LmdConstants.LMD_NAME_FINALIZING_TYPE;



//import static com.jbjs.rsuite.contants.LmdConstants.LMD_VALUE_ALL_AT_ONCE;
import static org.astd.rsuite.constants.LmdConstants.LMD_VALUE_ALL_AT_ONCE;

//import static com.jbjs.rsuite.contants.LmdConstants.LMD_VALUE_JBJS;
import static org.astd.rsuite.constants.LmdConstants.LMD_VALUE_ASTD;

//import static com.jbjs.rsuite.utils.JbjsUtils.ProductParentDisplayName.PRODUCT_PARENT_DISPLAY_NAME_PUBLISHED;
import static org.astd.rsuite.utils.AsdtUtils.ProductParentDisplayName.PRODUCT_PARENT_DISPLAY_NAME_PUBLISHED;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

//import com.jbjs.rsuite.contants.CaConstants;
import org.astd.rsuite.constants.CaConstants;
//import com.jbjs.rsuite.contants.LmdConstants;
import org.astd.rsuite.constants.LmdConstants;
//import com.jbjs.rsuite.utils.CAUtils;
import org.astd.rsuite.utils.CAUtils;
//import com.jbjs.rsuite.utils.ContentDisplayUtils;
import org.astd.rsuite.utils.ContentDisplayUtils;
//import com.jbjs.rsuite.utils.JbjsUtils;
import org.astd.rsuite.utils.AsdtUtils;
//import com.jbjs.rsuite.utils.MOUtils;
import org.astd.rsuite.utils.MOUtils;
//import com.jbjs.rsuite.utils.browse.BrowseUtils;
import org.astd.rsuite.utils.browse.BrowseUtils;
import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ContentAssemblyReference;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ManagedObjectReference;
import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.ObjectType;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.VersionSpecifier;
import com.reallysi.rsuite.api.browse.BrowseInfo;
import com.reallysi.rsuite.api.control.ContentAssemblyCreateOptions;
import com.reallysi.rsuite.api.control.ObjectAttachOptions;
import com.reallysi.rsuite.api.control.ObjectCopyOptions;
import com.reallysi.rsuite.api.control.ObjectDetachOptions;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.remoteapi.CallArgumentList;
import com.reallysi.rsuite.api.remoteapi.RemoteApiExecutionContext;
import com.reallysi.rsuite.api.remoteapi.RemoteApiResult;
import com.reallysi.rsuite.api.remoteapi.result.MessageType;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.rsicms.rsuite.helpers.utils.MoUtils;
import com.rsicms.rsuite.helpers.webservice.RemoteApiHandlerBase;

public class FinalizeIssueWebService extends RemoteApiHandlerBase {

	@Override
	public RemoteApiResult execute(RemoteApiExecutionContext context,
			CallArgumentList args) throws RSuiteException {
		
		ContentAssemblyService caSrv = context.getContentAssemblyService();
		User user = context.getSession().getUser();
		String issueId = args.getFirstValue(PARAM_RSUITE_ID.getName());
		ContentAssembly issueCa = caSrv.getContentAssembly(user, issueId);
		String issueRefId = CAUtils.getSelfRefId(context, issueId);
		String finalizaingType = issueCa.getLayeredMetadataValue(LMD_NAME_FINALIZING_TYPE.getName());
		
		ContentAssembly currentVolumeParent = null;
		ContentAssembly newVolume = null;
		if (StringUtils.isBlank(finalizaingType)) {
			throw new RSuiteException("Issue container is missing 'finalizing_type' layered metadata");
		} else if (finalizaingType.equals(LMD_VALUE_ALL_AT_ONCE.getName())) {
			String currentVolumeParentId = CAUtils.getPaterntId(context, user, issueCa.getId());
			currentVolumeParent = caSrv.getContentAssembly(user, currentVolumeParentId);
			newVolume = getPublishedVolume(context, user, currentVolumeParent.getDisplayName());
			caSrv.attach(user, newVolume.getId(), issueCa, new ObjectAttachOptions());
			caSrv.detach(user, currentVolumeParent.getId(), issueRefId, new ObjectDetachOptions());
		}

		finalize(context, user, issueCa);
		
		return ContentDisplayUtils.getResultWithLabelRefreshing(
				MessageType.SUCCESS, "Finalize Issue", "Issue has been finalized", "500", issueCa.getId());
	}

	private void finalizeTemp(ExecutionContext context, User user,
			ContentAssembly issueCa) throws RSuiteException {
		List<? extends ContentAssemblyItem> issueChildren = issueCa.getChildrenObjects();
		for (ContentAssemblyItem issueChild : issueChildren) {
			issueChild.getObjectType();
		}
	}

	private void finalize(ExecutionContext context, User user,
			ContentAssembly issueCa) throws RSuiteException {
	List<ManagedObject> childrenRef = MOUtils.getChildrenRef(context, issueCa.getId());
		
		for (ManagedObject childMoRef : childrenRef) {
			List<String> directRefs = context.getManagedObjectService().getDependencyTracker().listDirectReferenceIds(user, childMoRef.getTargetId());
			String parentMoId = null;
			ManagedObject mo = MoUtils.getRealMo(context, childMoRef.getId());
			boolean childFound = false;
			for (String directRef : directRefs) {
				String tentativeParentId = context.getManagedObjectService().getParentManagedObjectId(user, directRef);
				List<? extends ContentAssemblyItem> parentChildren = context.getContentAssemblyService().getContentAssembly(user, tentativeParentId).getChildrenObjects();
//				BrowseInfo browseInfo = context.getManagedObjectService().getChildManagedObjects(user, tentativeParentId, 0, 2000);
//				List<ManagedObject> parentChildren = browseInfo.getManagedObjects();
				for (ContentAssemblyItem parentChild : parentChildren) {
					String parentChildId = null;
					if (parentChild.getObjectType() == ObjectType.CONTENT_ASSEMBLY_REF) {
						parentChildId = ((ContentAssemblyReference)parentChild).getTargetId();
					} else if (parentChild.getObjectType() == ObjectType.MANAGED_OBJECT_REF) {
						parentChildId = ((ManagedObjectReference)parentChild).getTargetId();
					} else {
						parentChildId = parentChild.getId();
					}
						
					if (parentChildId.equals(mo.getId())) {
						parentMoId = tentativeParentId;
						childFound = true;
						break;
					}
				}
				
				if (childFound) {
					break;
				}
			}

			ContentAssemblyItem inProgressCaItem = 
					CAUtils.getAncestorCAbyDisplayName(context, parentMoId, CA_NAME_IN_PROGERSS.getName());
			if (inProgressCaItem == null) {
				ObjectCopyOptions copyOptions = new ObjectCopyOptions();
				if (childMoRef.getDisplayName() != null) {
					copyOptions.setExternalFileName(childMoRef.getDisplayName());
				}

				String childRefId = CAUtils.getSelfRefId(context, childMoRef.getTargetId());
				CAUtils.copyAndAttach(context, user, new VersionSpecifier(childMoRef.getTargetId()),
						 issueCa.getId(), copyOptions, new ObjectAttachOptions());
				context.getContentAssemblyService().detach(user, issueCa.getId(), childMoRef.getId(), new ObjectDetachOptions()); 
			}				
		}

		MetaDataItem finalizedLmd = new MetaDataItem(LmdConstants.LMD_NAME_FINALIZED.getName(), LmdConstants.LMD_VALUE_YES.getName());
		context.getManagedObjectService().setMetaDataEntry(user, issueCa.getId(), finalizedLmd);
		
	}

	
	
	private ContentAssembly getPublishedVolume(
			ExecutionContext context, User user, String volumeName) throws RSuiteException {
		ContentAssembly jbjsProd = AsdtUtils.getProduct(
				context, user, PRODUCT_PARENT_DISPLAY_NAME_PUBLISHED, LMD_VALUE_ASTD.getName());		
		ContentAssembly publishedVolume = CAUtils.getChildCaByDisplayName(context, jbjsProd, volumeName); 

		if (publishedVolume == null) {
			ContentAssemblyCreateOptions assemblyCreateOptions = new ContentAssemblyCreateOptions();
			assemblyCreateOptions.setSilentIfExists(true);
			assemblyCreateOptions.setType(CaConstants.CA_TYPE_VOLUME.getName());
			publishedVolume = context.getContentAssemblyService().createContentAssembly(user, jbjsProd.getId(), volumeName, assemblyCreateOptions);
		}

		return publishedVolume;
	}

}
