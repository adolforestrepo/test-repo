package org.astd.rsuite.events;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reallysi.rsuite.api.Alias;
import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.event.Event;
import com.reallysi.rsuite.api.event.EventHandler;
import com.reallysi.rsuite.api.event.EventTypes;
import com.reallysi.rsuite.api.event.events.ObjectCheckedInEventData;
import com.reallysi.rsuite.api.event.events.ObjectInsertedEventData;
import com.reallysi.rsuite.api.extensions.ExecutionContext;

public class SetNonXmlAliasEventHandler
    implements EventHandler {

  private static Log log = LogFactory.getLog(SetNonXmlAliasEventHandler.class);

  @Override
  public void handleEvent(ExecutionContext context, Event event, Object object) throws RSuiteException {

    String type = event.getType();

    Object userData = event.getUserData();
    ObjectCheckedInEventData checkInEventData = null;
    ObjectInsertedEventData insertEventData = null;
    ManagedObject moInsertedOrCheckedIn = null;

    // Only care about the content assembly created event.
    if (EventTypes.OBJECT_CHECKEDIN.equals(event.getType())) {
      checkInEventData = (ObjectCheckedInEventData) userData;
      moInsertedOrCheckedIn = checkInEventData.getManagedObject();

    } else if (EventTypes.OBJECT_INSERTED.equals(type)) {
      insertEventData = (ObjectInsertedEventData) userData;
      moInsertedOrCheckedIn = insertEventData.getManagedObject();
    }


    // for now run regardless to ensure references are correct
    if (moInsertedOrCheckedIn != null && moInsertedOrCheckedIn.isNonXml()) {
      setNonXmlAlias(context, moInsertedOrCheckedIn);
    }
  }

  /**
   * Add the display name of a MO as the alias. *
   * 
   * @param context
   * @param mo
   * @throws RSuiteException
   */
  private void setNonXmlAlias(ExecutionContext context, ManagedObject mo) throws RSuiteException {
    Alias[] aliases = mo.getAliases();
    if (aliases.length > 0) {
      log.info("Managed object [" + mo.getId() + "] already has one or more aliases");
      return;
    }
    String displayName = mo.getDisplayName();
    if (displayName != null && !"".equals(displayName)) {
      log.info("Adding alias \"" + displayName + "\" to MO [" + mo.getId() + "]");
      context.getManagedObjectService().addAlias(context.getAuthorizationService().getSystemUser(), mo.getId(), displayName);
    } else {
      log.info("Managed object [" + mo.getId() + "] has null or empty display name, alias not set.");
    }
  }

}
