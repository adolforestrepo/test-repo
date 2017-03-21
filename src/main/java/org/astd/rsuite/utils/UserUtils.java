package org.astd.rsuite.utils;

import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.service.AuthorizationService;

/**
 * A collection of static utility methods related to users.
 */
public class UserUtils {

  /**
   * Get an instance of <code>User</code> from a user's ID.
   * 
   * @param authService
   * @param userId
   * @return User
   * @throws RSuiteException Thrown if the user ID is not defined in either user manager.
   */
  public static User getUser(AuthorizationService authService, String userId)
      throws RSuiteException {
    return authService.findUser(userId);
  }

  /**
   * When all you have is the user's ID and all you want is the user's full name, call this method.
   * 
   * @param authService
   * @param userId
   * @return The user's full name, as best as is known to RSuite. When a user isn't found by the
   *         specified name, the user ID is returned. If the given user ID is blank, "[Unknown]" is
   *         returned.
   * @throws RSuiteException May be thrown by {@link #getUser(AuthorizationService, String)}.
   */
  public static String getUserFullname(AuthorizationService authService, String userId)
      throws RSuiteException {
    User user = getUser(authService, userId);
    if (user == null) {
      if (StringUtils.isBlank(userId)) {
        return "[Unknown]";
      }
      return userId;
    }
    return user.getFullName();
  }

  /**
   * @param fullName
   * @return display name in the format of LastName, FirstName
   */
  public static String convertUserFullName(String fullName) {

    // It is assumed that full name is in the format of fisrtName lastName.
    String cleanFullName = fullName.trim(); // no white space in front of the first name
    int endOfFirstName = cleanFullName.indexOf(" ");
    if (endOfFirstName == -1) {
      return fullName;
    } else {
      return cleanFullName.substring(endOfFirstName + 1).trim() + ", " + cleanFullName.substring(0,
          endOfFirstName);
    }
  }

  /**
   * For sorting users based on Lastname, FisrtName.
   */
  public static class UserComparator
      implements Comparator<User> {

    @Override
    public int compare(User user1, User user2) {

      return convertUserFullName(user1.getFullName()).compareToIgnoreCase(convertUserFullName(user2
          .getFullName()));
    }

  }
}
