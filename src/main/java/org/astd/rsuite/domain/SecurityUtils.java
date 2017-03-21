package org.astd.rsuite.domain;

import java.util.ArrayList;
import java.util.List;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.security.ContentPermission;
import com.reallysi.rsuite.api.security.RoleDescriptor;
import com.reallysi.rsuite.api.security.RoleManager;
import com.reallysi.rsuite.service.AuthorizationService;
import com.reallysi.rsuite.service.SecurityService;

/**
 * A collection of static security methods specific to this client.
 * <p>
 * A couple static methods are declared as synchronized, meaning only one can run at a time. This is
 * the behavior we want for methods that are able to add and remove roles using RSuite's role
 * manager. Should there be a different synchronization need in the same class, the role-related
 * methods can be changed to synchronize against a static member variable.
 */
public class SecurityUtils {

  /**
   * Read-only content permissions.
   */
  protected final static ContentPermission[] readOnlyPermissions = new ContentPermission[] {
      ContentPermission.LIST, ContentPermission.VIEW};

  /**
   * Edit-only content permissions (i.e., no delete permissions).
   */
  protected final static ContentPermission[] editOnlyPermissions = new ContentPermission[] {
      ContentPermission.LIST, ContentPermission.VIEW, ContentPermission.EDIT,
      ContentPermission.REUSE};

  /**
   * Get the list of role names that are in the provided list and the specified user has.
   * 
   * @param roleManager
   * @param user
   * @param roleNames
   * @return List of matching role names.
   * @throws RSuiteException
   */
  public static List<String> getMatchingRolesNames(RoleManager roleManager, User user,
      List<String> roleNames) throws RSuiteException {
    List<String> matchingRoleNames = new ArrayList<String>();
    if (roleNames != null) {
      for (String roleName : roleNames) {
        if (userHasRole(roleManager, user, roleName)) {
          matchingRoleNames.add(roleName);
        }
      }
    }
    return matchingRoleNames;
  }

  /**
   * Determine if the user has the specified role.
   * 
   * @param roleManager
   * @param user
   * @param roleName
   * @return True if user has the role; otherwise, false.
   * @throws RSuiteException
   */
  public static boolean userHasRole(RoleManager roleManager, User user, String roleName)
      throws RSuiteException {

    // Check the user object
    if (user.hasRole(roleName)) {
      return true;
    }

    // Now see if the roled manager has a more favorable answer.
    for (RoleDescriptor desc : roleManager.getRolesForUser(user.getUserId().toLowerCase())) {
      if (desc.getName().equalsIgnoreCase(roleName)) {
        return true;
      }
    }

    // User loses.
    return false;
  }

  /**
   * Get a potentially amplified user --one that is allowed to edit the specified object.
   * <p>
   * This method should be used with caution as it has the potential to enable the requesting user
   * to do more than they'd otherwise be able to do. This should only be used for authorized
   * scenarios, such as immediately after making a submitted review read-only but needing to set
   * metadata on it in order to trigger the update of metadata on the associated master sections.
   * 
   * @param authService
   * @param requestingUser The user that needs to edit the object, even if they don't have edit
   *        permissions to the object.
   * @param mo
   * @return If the provided user has the required capability, that user is returned. Else, the
   *         system user is returned.
   * @throws RSuiteException
   */
  public User getAmpEditUser(SecurityService securityService, AuthorizationService authService,
      User requestingUser, ManagedObject mo) throws RSuiteException {

    return getAmpEditUser(securityService, authService, requestingUser, mo.getId());

  }

  /**
   * Get a potentially amplified user --one that is allowed to edit the specified object.
   * <p>
   * This method should be used with caution as it has the potential to enable the requesting user
   * to do more than they'd otherwise be able to do. This should only be used for authorized
   * scenarios, such as immediately after making a submitted review read-only but needing to set
   * metadata on it in order to trigger the update of metadata on the associated master sections.
   * 
   * @param authService
   * @param requestingUser The user that needs to edit the object, even if they don't have edit
   *        permissions to the object.
   * @param id
   * @return If the provided user has the required capability, that user is returned. Else, the
   *         system user is returned.
   * @throws RSuiteException
   */
  public User getAmpEditUser(SecurityService securityService, AuthorizationService authService,
      User requestingUser, String id) throws RSuiteException {

    return getAmpUser(securityService, authService, requestingUser, id, ContentPermission.EDIT);

  }

  /**
   * Get a potentially amplified user --one that is allowed to view the specified object.
   * <p>
   * This method should be used with caution as it has the potential to enable the requesting user
   * to do more than they'd otherwise be able to do. This should only be used for authorized
   * scenarios, such as allowing the delivery system to access metadata.
   * 
   * @param authService
   * @param requestingUser The user that needs the view permission of the object.
   * @param mo
   * @return If the provided user has the required capability, that user is returned. Else, the
   *         system user is returned.
   * @throws RSuiteException
   */
  public User getAmpViewUser(SecurityService securityService, AuthorizationService authService,
      User requestingUser, ManagedObject mo) throws RSuiteException {

    return getAmpViewUser(securityService, authService, requestingUser, mo.getId());

  }

  /**
   * Get a potentially amplified user --one that is allowed to view the specified object.
   * <p>
   * This method should be used with caution as it has the potential to enable the requesting user
   * to do more than they'd otherwise be able to do. This should only be used for authorized
   * scenarios, such as allowing the delivery system to access metadata.
   * 
   * @param authService
   * @param requestingUser The user that needs the view permission of the object.
   * @param id
   * @return If the provided user has the required capability, that user is returned. Else, the
   *         system user is returned.
   * @throws RSuiteException
   */
  public User getAmpViewUser(SecurityService securityService, AuthorizationService authService,
      User requestingUser, String id) throws RSuiteException {

    return getAmpUser(securityService, authService, requestingUser, id, ContentPermission.VIEW);

  }

  /**
   * Get a potentially amplified user --one that has the specified content permission for the given
   * object.
   * <p>
   * This method should be used with caution as it has the potential to enable the requesting user
   * to do more than they'd otherwise be able to do. This should only be used for authorized
   * scenarios, such as immediately after making a submitted review read-only but needing to set
   * metadata on it in order to trigger the update of metadata on the associated master sections.
   * 
   * @param authService
   * @param requestingUser The user that at least temporarily has a legitimate need for a content
   *        permission on an object, but may not have it.
   * @param id
   * @param contentPermission
   * @return If the provided user has the required capability, that user is returned. Else, the
   *         system user is returned.
   * @throws RSuiteException
   */
  public User getAmpUser(SecurityService securityService, AuthorizationService authService,
      User requestingUser, String id, ContentPermission contentPermission) throws RSuiteException {

    if (authService.isAdministrator(requestingUser) || securityService.hasPermission(requestingUser,
        id, contentPermission)) {
      return requestingUser;
    }
    return authService.getSystemUser();

  }

}
