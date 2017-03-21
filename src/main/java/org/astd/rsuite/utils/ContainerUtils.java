package org.astd.rsuite.utils;


import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.container.visitor.ListReferencedContentContainerVisitor;
import org.astd.rsuite.operation.result.OperationResult;
import org.astd.rsuite.utils.mo.ManagedObjectQualifier;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ContentAssemblyNodeContainer;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ManagedObjectReference;
import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.control.ContentAssemblyCreateOptions;
import com.reallysi.rsuite.api.control.ObjectDestroyOptions;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.search.SortOrder;
import com.reallysi.rsuite.service.ContentAssemblyService;
import com.reallysi.rsuite.service.ManagedObjectService;
import com.reallysi.rsuite.service.SearchService;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;
import com.rsicms.rsuite.utils.search.SearchUtils;

/**
 * A collection of container utility methods.
 */
public class ContainerUtils {

  private final static Log log = LogFactory.getLog(ContainerUtils.class);

  /**
   * Instance method wrapping RSuiteUtils.getContentAssemblyNodeContainer() whose advantage over
   * ContentAssemblyService#getContentAssemblyNodeContainer() is that RSuiteUtils will automatically
   * resolve the given ID when a reference (common in the web service context).
   * <p>
   * Instance methods help make some unit tests possible.
   * 
   * @param context
   * @param user
   * @param id The ID of a container or reference to a container.
   * @return The content assembly node container identified by the specified ID.
   * @throws RSuiteException
   */
  public ContentAssemblyNodeContainer getContentAssemblyNodeContainer(ExecutionContext context,
      User user, String id) throws RSuiteException {
    return RSuiteUtils.getContentAssemblyNodeContainer(context, user, id);
  }

  /**
   * Get an LMD value by LMD name from the specified container. Should there be multiple LMD values
   * with the same LMD name, the first one, as decided by the RSuite API, will be returned.
   * 
   * @param container
   * @param lmdName
   * @return LMD value or null when the requested LMD isn't set on the specified container.
   * @throws RSuiteException
   */
  public static String getLayeredMetadataValue(ContentAssemblyNodeContainer container,
      String lmdName) throws RSuiteException {
    if (container != null && container.getMetaDataItems() != null && StringUtils.isNotBlank(
        lmdName)) {
      for (MetaDataItem item : container.getMetaDataItems()) {
        if (lmdName.equalsIgnoreCase(item.getName())) {
          return item.getValue();
        }
      }
    }
    return null;
  }

  /**
   * DANGER: This method permanently deletes the given container and EVERYTHING it references, even
   * if also referenced by other containers and or checked out by other users.
   * <p>
   * This implementation minimizes the number of database writes by deleting containers before MOs.
   * <p>
   * There is no code within that explicitly deals with CANodes as those are deleted with the CAs.
   * <p>
   * Error handling could be added in an attempt to continue despite having an issue with one
   * object.
   * 
   * @param context
   * @param user The user to operate as. User must be an administrator.
   * @param container The container to permanently deleted, as well as all content it references.
   * @throws RSuiteException Thrown if unable to complete the operation successfully. A possible
   *         outcome is that some of the objects were destroyed, but not all.
   */
  public static OperationResult deleteContainerAndReferencedContent(ExecutionContext context,
      User user, ContentAssemblyNodeContainer container, Log log) throws RSuiteException {

    OperationResult result = new OperationResult(context.getIDGenerator().allocateId(), "delete",
        log == null ? ContainerUtils.log : log);
    result.markStartOfOperation();

    result.addInfoMessage(ProjectMessageResource.getMessageText(
        "info.received.request.to.delete.specific.container", user.getUserId(), container
            .getDisplayName(), container.getId()));

    // Require user is an administrator.
    if (!context.getAuthorizationService().isAdministrator(user)) {
      result.addFailure(new RSuiteException(RSuiteException.ERROR_PERMISSION_DENIED,
          ProjectMessageResource.getMessageText("security.error.must.be.admin")));
      return result;
    }

    // Visit the container
    ListReferencedContentContainerVisitor visitor = new ListReferencedContentContainerVisitor(
        context, user);
    visitor.visitContentAssemblyNodeContainer(container);

    ManagedObjectService moService = context.getManagedObjectService();
    ObjectDestroyOptions options = new ObjectDestroyOptions();

    // 1st: delete the starting container, so long as it isn't the root container.
    if (visitor.getStartingContainer() != null && context.getContentAssemblyService().getRootFolder(
        user).getId() != visitor.getStartingContainer().getId()) {
      result.addInfoMessage(ProjectMessageResource.getMessageText("info.deleting.object", visitor
          .getStartingContainer().getDisplayName(), visitor.getStartingContainer().getId()));
      deleteContainer(context, user, visitor.getStartingContainer(), log);
    }

    /*
     * 2nd: delete the CAs, in the list's order. Those that were directly referenced by the starting
     * container are no longer referenced by it. The idea is that those sooner in the list may
     * reference those later in the list (and not the other way around).
     */
    if (visitor.getReferencedContentAssemblies() != null) {
      for (ContentAssembly ca : visitor.getReferencedContentAssemblies()) {
        result.addInfoMessage(ProjectMessageResource.getMessageText("info.deleting.object", ca
            .getDisplayName(), ca.getId()));
        deleteContainer(context, user, ca, log);
      }
    }

    /*
     * 3rd: delete the MOs, which are no longer referenced by the containers deleted above.
     */
    if (visitor.getReferencedManagedObjects() != null) {
      for (ManagedObject mo : visitor.getReferencedManagedObjects()) {
        result.addInfoMessage(ProjectMessageResource.getMessageText("info.deleting.object", mo
            .getDisplayName(), mo.getId()));
        try {
          if (moService.isCheckedOutButNotByUser(user, mo.getId())) {
            result.addInfoMessage(ProjectMessageResource.getMessageText("info.canceling.checkout",
                mo.getDisplayName(), mo.getId(), mo.getCheckOutOwner()));
            MOUtils.cancelCheckout(context, user, mo.getId());
          }
          MOUtils.checkout(context, user, mo.getId());
          moService.destroy(user, mo.getId(), options);
        } catch (RSuiteException e) {
          result.addWarning(e);
        }
      }
    }

    result.setDestroyedContentAssemblies(visitor.getReferencedContentAssemblies());
    result.setDestroyedManagedObjects(visitor.getReferencedManagedObjects());
    result.markEndOfOperation();
    return result;
  }

  /**
   * Delete a content assembly node container, regardless of it being a CA or CANode.
   * <p>
   * This method does not delete any content the container references.
   * 
   * @param context
   * @param user
   * @param container
   * @param log
   * @throws RSuiteException
   */
  public static void deleteContainer(ExecutionContext context, User user,
      ContentAssemblyNodeContainer container, Log log) throws RSuiteException {
    if (container instanceof ContentAssembly) {
      context.getContentAssemblyService().removeContentAssembly(user, container.getId());
    } else {
      context.getContentAssemblyService().deleteCANode(user, container.getId());
    }
  }

  /**
   * Rename a CA or CANode.
   * 
   * @param context
   * @param user
   * @param container
   * @param name New name for the container.
   * @return True if it was necessary to rename the container and the rename was performed. When the
   *         rename is not necessary, the method doesn't bother trying and returns false.
   * @throws RSuiteException
   */
  public static boolean renameContainer(ExecutionContext context, User user,
      ContentAssemblyNodeContainer container, String name) throws RSuiteException {
    // Only proceed if we were given a container, the name isn't blank, and the name is different.
    if (container != null && StringUtils.isNotBlank(name) && !name.equals(container
        .getDisplayName())) {
      if (container instanceof ContentAssembly) {
        context.getContentAssemblyService().renameContentAssembly(user, container.getId(), name);
      } else {
        context.getContentAssemblyService().renameCANode(user, container.getId(), name);
      }
      return true;
    }
    return false;
  }

  /**
   * Get the first qualifying MO directly or indirectly referenced by the provided container. The
   * provided MO qualifier provides the logic of which MOs qualify.
   * <p>
   * The implementation defines "first".
   * <p>
   * If the caller needs a list of all qualifying MOs, more work would be required.
   * 
   * @param context
   * @param user
   * @param container
   * @param moQualifier
   * @return The first qualifying MO, or null when there isn't a qualifying MO.
   * @throws RSuiteException
   */
  public static ManagedObject getFirstQualifyingReferencedManagedObject(ExecutionContext context,
      User user, ContentAssemblyNodeContainer container, ManagedObjectQualifier moQualifier)
      throws RSuiteException {

    if (container != null && moQualifier != null) {
      /*
       * IDEA: Stop processing the container as soon as the first qualifying MO is found. Elected
       * not to do this initially as this visitor already existed and the project containers are not
       * expected to reference many objects.
       */
      ListReferencedContentContainerVisitor visitor = new ListReferencedContentContainerVisitor(
          context, user);
      visitor.visitContentAssemblyNodeContainer(container);

      List<ManagedObject> mos = visitor.getReferencedManagedObjects();
      if (mos != null) {
        for (ManagedObject mo : mos) {
          if (moQualifier.accept(mo)) {
            return mo;
          }
        }
      }
    }

    return null;
  }

  /**
   * Get a container's child container, optionally creating it when it doesn't exist.
   * 
   * @param caService
   * @param user
   * @param parentContainer
   * @param childContainerName Used to qualify existing containers or when creating one.
   * @param childContainerType Used when creating one. Not part of qualifying existing ones.
   * @param mayCreate Submit true if this method is allowed to create it, when it doesn't exist.
   * @return The requested container's sub container, or null if it doesn't exist and this method is
   *         not allowed to create it.
   * @throws RSuiteException Thrown if unable to access the container, or when allowed, create it.
   */
  public static ContentAssemblyNodeContainer getChildContainer(ContentAssemblyService caService,
      User user, ContentAssemblyNodeContainer parentContainer, String childContainerName,
      String childContainerType, boolean mayCreate) throws RSuiteException {

    // See if it exists
    List<ContentAssembly> containers = caService.listContentAssembliesByName(user, parentContainer
        .getId(), childContainerName);
    if (containers != null && containers.size() > 0) {
      // Warn if more than one.
      if (containers.size() > 1) {
        log.warn(ProjectMessageResource.getMessageText(
            "container.utils.warn.found.multiple.with.same.name", containers.size(),
            childContainerName, parentContainer.getDisplayName(), parentContainer.getId()));
      }
      // Always return the first, as determined by RSuite.
      return containers.get(0);
    }

    // Create, if allowed.
    if (mayCreate) {
      ContentAssemblyCreateOptions options = new ContentAssemblyCreateOptions();
      options.setCreateHomeless(false);
      options.setSilentIfExists(false);
      if (StringUtils.isNotBlank(childContainerType)) {
        options.setType(childContainerType);
      }
      return caService.createContentAssembly(user, parentContainer.getId(), childContainerName,
          options);
    }

    return null;
  }
  

  /**
   * Get a MO file with a given localname from a Container.
   * 
   * @param user
   * @param moService
   * @param container
   * @return ManagedObject MO matching the given localname.
   * @throws RSuiteException
   */
  public static ManagedObject getMoFromContainerByLocalName(
          User user,
          ManagedObjectService moService,
          ContentAssemblyNodeContainer container,
          String localName)
  throws RSuiteException{     
      List<?> caChildren = container.getChildrenObjects();
      for (int i = 0; i < caChildren.size(); i++) {
          if (caChildren.get(i) instanceof ManagedObjectReference) {
              ManagedObjectReference moChildref = (ManagedObjectReference) caChildren
                      .get(i);

              ManagedObject childMo = moService.getManagedObject(user,
                      moChildref.getTargetId());
              if(childMo.getLocalName().equals(localName))
                return childMo;
          }
      }
      
      return null;
  }
  
  /**
   * Wrapper for the <code>SearchUtils.searchForContentAssemblies</code> function.
   * Returns a list of assemblies matching a given CAType.
   * 
   * @param user
   * @param searchService
   * @param caType
   * @param order
   * @return List<ManagedObject>
   * @throws RSuiteException
   */
  public static List<ManagedObject> getAssembliesByType(
      User user,
      SearchService searchService,
      String caType,
      List<SortOrder> order)
   throws RSuiteException {
      
    List<ManagedObject> assemblies = SearchUtils.searchForContentAssemblies(
        user, 
        searchService, 
        caType, 
        order);
    
    return assemblies;
  }

}
