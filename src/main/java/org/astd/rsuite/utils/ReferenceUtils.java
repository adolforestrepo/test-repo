package org.astd.rsuite.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.ReferenceInfo;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;

/**
 * A collection of static reference-related utility methods.
 */
public class ReferenceUtils {

  /**
   * Get a list of object IDs where those objects reference the specified object.
   * <p>
   * Code is too ugly not to put in a corner.
   * 
   * @param context
   * @param user
   * @param id
   * @return A list of zero or more object IDs that reference the specified object.
   * @throws RSuiteException
   */
  public static List<String> listDirectlyReferencingObjectIds(ExecutionContext context, User user,
      String id) throws RSuiteException {
    List<String> ids = new ArrayList<String>();
    List<ReferenceInfo> refs = context.getManagedObjectService().getDependencyTracker()
        .listDirectReferences(user, id);
    if (refs != null) {
      for (ReferenceInfo ref : refs) {
        String browseUri = ref.getBrowseUri();
        if (StringUtils.isNotBlank(browseUri)) {
          String[] pathParts = browseUri.split("/");
          int parentIdx = pathParts.length - 2;
          if (parentIdx > -1) {
            ids.add(pathParts[parentIdx].split(":")[1]);
          }
        }
      }
    }
    return ids;
  }

}
