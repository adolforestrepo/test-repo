package org.astd.rsuite.advisors.content;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.constants.LayeredMetadataConstants;
import org.astd.rsuite.utils.ContentDisplayLabelAdjuster;
import org.astd.rsuite.utils.ContentDisplayObjectUtils;

import com.reallysi.rsuite.api.ManagedObject;
import com.reallysi.rsuite.api.ObjectType;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.content.ContentAdvisorContext;
import com.reallysi.rsuite.api.content.ContentDisplayObject;
import com.reallysi.rsuite.api.content.DefaultContentDisplayAdvisor;

public class ProjectContentDisplayAdvisor
    extends DefaultContentDisplayAdvisor
    implements LayeredMetadataConstants {

  private static Log log = LogFactory.getLog(ProjectContentDisplayAdvisor.class);
  
  @Override
  public void adjustNodeCollectionList(ContentAdvisorContext arg0, List<ContentDisplayObject> arg1)
      throws RSuiteException {
    // Obsolete implementation method; do nothing
  }

  @Override
  public void adjustContentItem(ContentAdvisorContext context, ContentDisplayObject item)
      throws RSuiteException {

    // At least through RSuite 4.0.14, RSuite can pass in null for the ContentDisplayObject
    // parameter.
    if (item == null)
      return;

    try {
      ManagedObject mo = ContentDisplayObjectUtils.getRealMo(context, item.getManagedObject());

      ObjectType objType = mo.getObjectType();
      StringBuffer label = ContentDisplayObjectUtils.createLabel(mo);
      StringBuffer title = ContentDisplayObjectUtils.createTitle(mo);
      StringBuffer ancilliaryLabel = ContentDisplayObjectUtils.createAncilliaryLabel(item);

      ContentDisplayLabelAdjuster cdAdjuster = new ContentDisplayLabelAdjuster(mo, label,
          ancilliaryLabel, title);

      if (objType == ObjectType.CONTENT_ASSEMBLY) {
        updateCALabel(context, mo, cdAdjuster);
      }

      if (objType == ObjectType.MANAGED_OBJECT || objType == ObjectType.MANAGED_OBJECT_NONXML) {
        updateMOLabel(context, mo, cdAdjuster);
      }

      if (cdAdjuster.isAncilliaryLabelAdjusted()) {
        item.setAncillaryLabel(cdAdjuster.getNewAncilliaryLabel());
      }

      if (cdAdjuster.isLabelAdjusted()) {
        item.setLabel(cdAdjuster.getNewLabel());
      }
    } catch (Exception e) {
      log.error("Unexpected " + e.getClass().getSimpleName() + " handling item [" + item.getId()
          + "] " + item.getDisplayName(), e);
    }
  }

  /**
   * Update the label displayed for Containers.
   * 
   * @param context
   * @param mo
   * @param cdAdjuster
   * @throws RSuiteException
   */
  private void updateCALabel(ContentAdvisorContext context, ManagedObject mo,
      ContentDisplayLabelAdjuster cdAdjuster) throws RSuiteException {
    commonLabelUpdate(context, mo, cdAdjuster);  

  }

  private void updateMOLabel(ContentAdvisorContext context, ManagedObject mo,
      ContentDisplayLabelAdjuster cdAdjuster) throws RSuiteException {

  }

  private void commonLabelUpdate(ContentAdvisorContext context, ManagedObject mo,
      ContentDisplayLabelAdjuster cdAdjuster) throws RSuiteException {

  }
}
