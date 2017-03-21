package org.astd.rsuite.webservices.utils;

import java.util.ArrayList;
import java.util.List;

import com.reallysi.rsuite.api.remoteapi.CallArgument;
import com.rsicms.rsuite.utils.search.NameValuesPair;

public class WebservicesUtils {

  /**
   * @param roleNames (comma separated role names)
   * @return (list of role names)
   */
  public List<String> getRoleNameList(String roleNames) {
    List<String> trimmedRoleNameList = new ArrayList<String>();
    for (String roleName : roleNames.split(",")) {
      trimmedRoleNameList.add(roleName.trim());
    }
    return trimmedRoleNameList;
  }

  /**
   * @param args
   * @return a name value pair array representation of a call argument list.
   */
  public NameValuesPair[] getLmdNameValuePairs(List<CallArgument> args) {
    List<NameValuesPair> pairList = new ArrayList<NameValuesPair>();
    for (CallArgument arg : args) {
      NameValuesPair pair = new NameValuesPair(arg.getName(), arg.getValue());
      pairList.add(pair);
    }
    return pairList.toArray(new NameValuesPair[pairList.size()]);
  }

}
