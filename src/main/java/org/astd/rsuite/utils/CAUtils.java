package org.astd.rsuite.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import org.astd.rsuite.utils.browse.filters.ChildCaFilter;
import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.ContentAssemblyReference;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ObjectType;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.ReferenceInfo;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.VersionSpecifier;
import com.reallysi.rsuite.api.control.DependencyTracker;
import com.reallysi.rsuite.api.control.ObjectAttachOptions;
import com.reallysi.rsuite.api.control.ObjectCopyOptions;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.reallysi.rsuite.service.ManagedObjectService;

public class CAUtils {

	public static String getPaterntId (ExecutionContext context, User user, String moId) throws RSuiteException {
		ContentAssemblyService caSvc = context.getContentAssemblyService();
		ManagedObjectService moSvc = context.getManagedObjectService();
		DependencyTracker tracker = moSvc.getDependencyTracker();
		List<ReferenceInfo> refList = tracker.listDirectReferences(user, moId);
		
		if (refList.size() == 0){
			throw new RSuiteException("Unable to localize parent. There are no references for " + moId);
		}
		
		ReferenceInfo ref = refList.get(0);
		String browseUri = ref.getParentBrowseUri();
		String[] parents = browseUri.split("\\/");
		if (parents.length == 1)
			return null;
		String refCaId = parents[parents.length - 1].split(":")[1];		
		return ((ContentAssemblyReference)caSvc.getContentAssemblyItem(user, refCaId)).getTargetId();
	}

	public static ContentAssemblyItem getAncestorCAbyType (ExecutionContext context, String caId, String caType) throws RSuiteException {
		Set<String> caTypes = new HashSet<String>();
		caTypes.add(caType);
		return getAncestorCAbyTypes(context, caId, caTypes);
	}

	public static ContentAssemblyItem getAncestorCAbyTypes(ExecutionContext context, String caId, Set<String> caTypes) throws RSuiteException {
		User user = context.getAuthorizationService().getSystemUser();
		ContentAssemblyItem ancestorCA = getCAItem(context, user, caId);
		
		if ((ancestorCA == null) || (!caTypes.contains(ancestorCA.getType()))) {
			String parentId = getPaterntId(context, user, caId);
			if (parentId != null) {				
				ancestorCA = getAncestorCAbyTypes(context, parentId, caTypes);
			} else {
				return null;
			}
		}
		
		return ancestorCA;
	}

	public static ContentAssemblyItem getCAItem (ExecutionContext context, User user, String caId) throws RSuiteException {
		ManagedObjectService moServ = context.getManagedObjectService();
		ContentAssemblyService caServ = context.getContentAssemblyService();
		ManagedObject managedObject = moServ.getManagedObject(user, caId);
		ContentAssemblyItem caItem = null;
						
		if (managedObject.getObjectType() == ObjectType.CONTENT_ASSEMBLY ||
		    managedObject.getObjectType() == ObjectType.CONTENT_ASSEMBLY_REF) {
			caItem = getCAFromMO(context, user, caId);
		} else  if (managedObject.getObjectType() == ObjectType.CONTENT_ASSEMBLY_NODE) {
			caItem = caServ.getCANode(user, managedObject.getId());
		}

		return caItem;
	}

	public static ContentAssembly getCAFromMO (ExecutionContext context, User user, String moId) throws RSuiteException {
		ManagedObjectService moServ = context.getManagedObjectService();
		ContentAssemblyService caServ = context.getContentAssemblyService();
		ManagedObject managedObject = moServ.getManagedObject(user, moId);
		ContentAssembly ca = null;
						
		if (managedObject.getObjectType() == ObjectType.CONTENT_ASSEMBLY) {
			ca = caServ.getContentAssembly(user, managedObject.getId());	
		} else  if (managedObject.getObjectType() == ObjectType.CONTENT_ASSEMBLY_REF) {
			ca = caServ.getContentAssembly(user, managedObject.getTargetId());
		}

		return ca;
	}

	/**
	 * Finds child MO by local name
	 * 
	 * @param context
	 *            the execution context
	 * @param contextCa
	 *            the context CA
	 * @param caType
	 *            caType
	 * @return Content Assembly if found otherwise null.
	 * @throws RSuiteException
	 */
	public static ContentAssembly getChildCaByDisplayName(
			ExecutionContext context, ContentAssembly contextCa,
			final String displaName) throws RSuiteException {

		ChildCaFilter filter = new ChildCaFilter() {			
			@Override
			public boolean accept(ContentAssembly ca) {
				if (ca.getDisplayName().equals(displaName)) {
					return true;
				}
				return false;
			}
		};
		
		return getFirstResult(getChildrenByFilter(context, contextCa, filter,
				true));
	}

	/**
	 * Finds child CA by type and display name
	 * 
	 * @param context
	 *            execution context
	 * @param contextCa
	 *            the context content CA
	 * @param caType
	 *            the ca type to find
	 * @param displayName
	 *            the display name to find
	 * @return Content Assembly if found otherwise null.
	 * @throws RSuiteException
	 */
	public static List<ContentAssembly> getChildrenByFilter(
			ExecutionContext context, ContentAssemblyItem contextCa,
			ChildCaFilter filter, boolean firstOnly) throws RSuiteException {

		User user = context.getAuthorizationService().getSystemUser();
		ContentAssemblyService caService = context.getContentAssemblyService();

		List<? extends ContentAssemblyItem> nodes = contextCa
				.getChildrenObjects();
		

		List<ContentAssembly> resultList = new ArrayList<ContentAssembly>();
		
		ContentAssembly ca = null;

		for (int i = nodes.size() - 1; i >= 0; i--) {

			ContentAssemblyItem caItem = nodes.get(i);

			if (caItem.getObjectType() == ObjectType.CONTENT_ASSEMBLY_REF) {

				String caId = ((ContentAssemblyReference) caItem).getTargetId();
				ca = caService.getContentAssembly(user, caId);

			} else if (caItem.getObjectType() == ObjectType.CONTENT_ASSEMBLY) {
				ca = caService.getContentAssembly(user, caItem.getId());
			}

			if (ca != null && filter.accept(ca)) {
				resultList.add(ca);
				
				if (firstOnly){
					break;
				}
			}
		}

		return resultList;
	}

	/**
	 * Gets first content assembly from the list
	 * 
	 * @param resultList
	 *            the CA list
	 * @return first content assembly from the list or null if list is empty
	 */
	private static ContentAssembly getFirstResult(
			List<ContentAssembly> resultList) {
		if (resultList.size() > 0){
			//get first
			return resultList.get(0);
		}
		
		return null;
	}

	/**
	 * Finds content assembly with given type
	 * 
	 * @param context
	 *            the execution context
	 * @param contextCa
	 *            the context CA
	 * @param caType
	 *            caType
	 * @return First found Content Assembly otherwise null.
	 * @throws RSuiteException
	 */
	public static ContentAssembly getChildCaByType(ExecutionContext context,
			ContentAssemblyItem contextCa, String caType) throws RSuiteException {

		List<ContentAssembly> caList = getChildrenCaByType(context, contextCa,
				caType);
		return getFirstResult(caList);
	}

	/**
	 * Finds child CA by caType
	 * 
	 * @param context
	 *            the execution context
	 * @param contextCa
	 *            the context CA
	 * @param caType
	 *            caType
	 * @return Content Assembly if found otherwise null.
	 * @throws RSuiteException
	 */
	public static List<ContentAssembly> getChildrenCaByType(
			ExecutionContext context, ContentAssemblyItem contextCa,
			final String caType) throws RSuiteException {

		ChildCaFilter filter = new ChildCaFilter() {			
			@Override
			public boolean accept(ContentAssembly ca) {
				if (StringUtils.equals(caType, ca.getType())) {
					return true;
				}
				return false;
			}
		};
		
		return  getChildrenByFilter(context, contextCa, filter, false);		
	}

	/**
	 * Finds child CA by caType
	 * 
	 * @param context
	 *            the execution context
	 * @param contextCa
	 *            the context CA
	 * @param caType
	 *            caType
	 * @return Content Assembly if found otherwise null.
	 * @throws RSuiteException
	 */
	public static List<ContentAssembly> getChildrenCaLMD(
			ExecutionContext context, ContentAssembly contextCa,
			final String lmdName, final String lmdValue) throws RSuiteException {

		ChildCaFilter filter = new ChildCaFilter() {			
			@Override
			public boolean accept(ContentAssembly ca) throws RSuiteException {
				
				if (StringUtils.equals(lmdValue,
						ca.getLayeredMetadataValue(lmdName))) {
					return true;
				}
				return false;
			}
		};
		
		return  getChildrenByFilter(context, contextCa, filter, false);		
	}

	public static ContentAssembly extractCaByLmd(
			List<ContentAssembly> caList, final String lmdaNme, final String lmdValue) throws RSuiteException {
		
		ChildCaFilter filter = new ChildCaFilter() {			
			@Override
			public boolean accept(ContentAssembly ca) throws RSuiteException {
				if (StringUtils.equals(lmdValue, ca.getLayeredMetadataValue(lmdaNme))) {
					return true;
				}
				return false;
			}
		};
		
		return  getFirstResult(extractCaByFilter(caList, filter, false));	
	}

	private static List<ContentAssembly> extractCaByFilter(
			List<ContentAssembly> caList, ChildCaFilter filter, boolean firstOnly) throws RSuiteException {
		List<ContentAssembly> resultList = new ArrayList<ContentAssembly>();

		for (ContentAssembly ca : caList) {
			if (ca != null && filter.accept(ca)) {
				resultList.add(ca);
				
				if (firstOnly){
					break;
				}
			}
		}

		return resultList;
	}

	public static ContentAssemblyItem getAncestorCAbyDisplayName(
			ExecutionContext context, String caId, String displayName) throws RSuiteException {
		User user = context.getAuthorizationService().getSystemUser();
		ContentAssemblyItem ancestorCA = getCAItem(context, user, caId);
		
		if ((ancestorCA == null) || (!ancestorCA.getDisplayName().equals(displayName))) {
			String parentId = getPaterntId(context, user, caId);
			if (parentId != null) {				
				ancestorCA = getAncestorCAbyDisplayName(context, parentId, displayName);
			} else {
				return null;
			}
		}
		
		return ancestorCA;
	}

	public static ContentAssemblyItem getAncestorCAbyLmd(
			ExecutionContext context, String caId, String lmdName) throws RSuiteException {
		User user = context.getAuthorizationService().getSystemUser();
		ContentAssemblyItem ancestorCA = getCAItem(context, user, caId);

		if ((ancestorCA == null) || (ancestorCA != null && StringUtils.isBlank(ancestorCA.getLayeredMetadataValue(lmdName)))) {
			String parentId = getPaterntId(context, user, caId);
			if (parentId != null) {				
				ancestorCA = getAncestorCAbyLmd(context, parentId, lmdName);
			} else {
				return null;
			}
		}
		
		return ancestorCA;
	}

	public static ManagedObject copyAndAttach(ExecutionContext context,
		    User user,
		    VersionSpecifier moSpecifier,
		    String caId,
		    ObjectCopyOptions copyOptions,
		    ObjectAttachOptions attachOptions)
		      throws RSuiteException
		  {
		    // Verify EDIT permission
		    if (!context.getSecurityService().hasEditPermission(user, caId))
		      throw new RSuiteException(
		        context.getMessageResources().getMessageText("ca.nocreatepermissionincaid", caId));
		    
		    ManagedObjectService moSvc = context.getManagedObjectService();
		    ContentAssemblyService caSvc = context.getContentAssemblyService();
		    
		    if (copyOptions == null)
		      copyOptions = new ObjectCopyOptions();
		    copyOptions.setContentAssembly(caSvc.getContentAssembly(user, caId));
		    
		    
		    ManagedObject newMo 
		      = moSvc.copy(user, moSpecifier, copyOptions);
		    
		    return caSvc.attach(user, caId, newMo, null, attachOptions);
	}

	/**
	 * Build a map of refIds indexed by caId.
	 *
	 * @param log
	 * @param user
	 * @param caSvc
	 * @param ca
	 *            Content Assembly for child list
	 * @return the refIdMap
	 * @throws RSuiteException
	 */
	public static Map<String, String> getCaRefIdMap(User user,
			ContentAssemblyService caSvc, ContentAssembly ca)
			throws RSuiteException {
		Map<String, String> refIdMap = new HashMap<String, String>();		
		List<? extends ContentAssemblyItem> childList = ca.getChildrenObjects();
		for (ContentAssemblyItem item: childList) {
			if (item.getObjectType() == ObjectType.CONTENT_ASSEMBLY_REF) {
				String refId = item.getId();
				ContentAssemblyReference caRef = (ContentAssemblyReference)item;
				String caId = caRef.getTargetId();
				refIdMap.put(caId, refId);
			}
		}
		return refIdMap;
		
	}

	public static String getSelfRefId (ExecutionContext context, String containerId) throws RSuiteException {
		int last = 1;
		return getCaRefId(context, containerId, last);
	}

	private static String getCaRefId (ExecutionContext context, String containerId, int position) throws RSuiteException {
		User systemUser = context.getAuthorizationService().getSystemUser();
		ManagedObjectService moServ = context.getManagedObjectService();

		List<ReferenceInfo> refernces = moServ.getDependencyTracker()
				.listAllReferences(systemUser, containerId);

		String ref = null;
		for (ReferenceInfo refernceInfo : refernces) {
			String browserUri = refernceInfo.getBrowseUri();
			String[] uriParts = browserUri.split("/");

			if (uriParts.length > 1) {	
				String parentPart = uriParts[uriParts.length-position];
				ref = parentPart.split(":")[1];
			}
		}

		return ref;
	}

}
