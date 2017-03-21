package org.astd.rsuite.utils;

import org.apache.commons.lang3.StringUtils;

import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.api.system.MailMessageBean;

public class EmailUtils {

  /**
   * Send an email notification.
   * 
   * @param context
   * @param user
   * @param mailMessageBean
   * @throws RSuiteException
   */
  public static void sendNotification(ExecutionContext context, User user,
      MailMessageBean mailMessageBean) throws RSuiteException {

    // Automatically include the requesting user
    if (StringUtils.isNotBlank(user.getEmail())) {
      String to = mailMessageBean.getTo();
      if (StringUtils.isBlank(to))
        to = user.getEmail();
      else
        to = to.concat(",").concat(user.getEmail());
      mailMessageBean.setTo(to);
    }

    context.getMailService().send(mailMessageBean);
  }
}
