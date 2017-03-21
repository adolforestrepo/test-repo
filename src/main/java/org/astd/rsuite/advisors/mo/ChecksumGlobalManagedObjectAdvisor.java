package org.astd.rsuite.advisors.mo;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;
import org.astd.rsuite.constants.LayeredMetadataConstants;

import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisor;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisorContext;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.rsicms.rsuite.helpers.utils.RSuiteUtils;

/**
 * A managed object advisor responsible for setting a checksum (MD5 hash) on new and update non-XML
 * MOs. Should the need arise, this could be broadened to support XML MOs.
 */
public class ChecksumGlobalManagedObjectAdvisor
    extends BaseMetadataEditingManagedObjectAdvisor
    implements ManagedObjectAdvisor, LayeredMetadataConstants {

  private static Log log = LogFactory.getLog(ChecksumGlobalManagedObjectAdvisor.class);

  public ChecksumGlobalManagedObjectAdvisor() {
    super();
  }

  @Override
  public void adviseDuringInsert(ExecutionContext context,
      ManagedObjectAdvisorContext advisorContext) throws RSuiteException {
    setNonXmlChecksum(context, advisorContext, true);
  }

  @Override
  public void adviseDuringUpdate(ExecutionContext context,
      ManagedObjectAdvisorContext advisorContext) throws RSuiteException {
    setNonXmlChecksum(context, advisorContext, false);
  }

  /**
   * For non-XML MOs, use the incoming bytes to calculate its checksum (MD5 hash) and add it to the
   * metadata container.
   * 
   * @param context
   * @param advisorContext
   * @param isInsert Submit true for adviseDuringInsert() and false for adviseDuringUpdate().
   */
  public void setNonXmlChecksum(ExecutionContext context,
      ManagedObjectAdvisorContext advisorContext, boolean isInsert) {
    String opLabel = isInsert ? "insert" : "update";
    try {
      // Only want non-XML MOs
      if (advisorContext.getNonXmlFile() != null) {

        // The update flow has a couple extra steps.
        if (!isInsert) {
          // Make sure the object is still around.
          if (!context.getManagedObjectService().hasObject(advisorContext.getUser(), advisorContext
              .getId())) {
            log.warn(ProjectMessageResource.getMessageText(
                "checksum.mo.advisor.warn.given.unknown.mo.id", advisorContext.getId(), opLabel));
            return;
          }

          // Make sure we don't lose its existing LMD
          prepopulateAdvisorMetadataContainer(context, advisorContext);
        }

        // Calculate the checksum
        MetaDataItem mdItem = RSuiteUtils.constructMd5HashLmd(context, advisorContext
            .getNonXmlFile());
        log.info(ProjectMessageResource.getMessageText(
            "checksum.mo.advisor.info.calculated.checksum", advisorContext.getId(), opLabel, mdItem
                .getValue()));
        List<MetaDataItem> mdList = new ArrayList<MetaDataItem>(1);
        mdList.add(mdItem);

        // Get the checksum into the metadata container.
        setMetadataInContainer(advisorContext.getMetaDataContainer(), mdList);
      }
    } catch (Exception e) {
      log.warn(ProjectMessageResource.getMessageText("checksum.mo.advisor.warn.processing.mo",
          opLabel, advisorContext.getId(), StringUtils.isBlank(e.getMessage()) ? StringUtils.EMPTY
              : ": ".concat(e.getMessage())), e);
    }
  }

}
