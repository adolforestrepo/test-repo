package org.astd.rsuite.advisors.mo;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.astd.rsuite.ProjectMessageResource;

import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.control.DefaultManagedObjectAdvisor;
import com.reallysi.rsuite.api.control.ManagedObjectAdvisorContext;
import com.reallysi.rsuite.api.control.MetaDataContainer;
import com.reallysi.rsuite.api.extensions.ExecutionContext;

/**
 * Base class for all of this project's MO advisors that edit metadata. MO advisors need to take
 * care not to lose existing metadata when adding or updating metadata. If any metadata is added to
 * the advisor's metadata container, that metadata COMPLETELY REPLACES the object's existing
 * metadata. By populating the advisor's metadata container with the object's current metadata,
 * changes may be made without losing any. Subclasses should call
 * {@link #prepopulateAdvisorMetadataContainer(ExecutionContext, ManagedObjectAdvisorContext)}
 * before otherwise manipulating the contents of the advisor's metadata container.
 * <p>
 * When subclasses wish to update metadata in the container (as opposed to always add), they should
 * do so with {@link #setMetadataInContainer(MetaDataContainer, String, String)} or
 * {@link #setMetadataInContainer(MetaDataContainer, List)}. If there isn't a piece of metadata
 * already in the container with the same name, it will be added to the container.
 * <p>
 * There is at least partial support of repeating LMD. Whenever metadata is added to an empty
 * container, all metadata presented is added to the container. Whenever the metadata container has
 * some metadata, metadata from the list is excluded when the container metadata is preferred and
 * the metadata names match. This could be problematic when an advisor wants to add another LMD
 * value with the same name.
 */
public class BaseMetadataEditingManagedObjectAdvisor
    extends DefaultManagedObjectAdvisor {

  private static Log log = LogFactory.getLog(BaseMetadataEditingManagedObjectAdvisor.class);

  /**
   * Copy the MO's existing LMD to the advisor's metadata container. When the container already has
   * an LMD value with a matching name, the LMD value is not updated. This prevents a secondary MO
   * advisor from overriding a previous MO advisor's LMD edits while also not dropping any LMD.
   * 
   * @param context
   * @param advisorContext
   * @throws RSuiteException
   */
  public void prepopulateAdvisorMetadataContainer(ExecutionContext context,
      ManagedObjectAdvisorContext advisorContext) throws RSuiteException {
    // Do not blindly add all as there may be multiple advisors (or a change in RSuite's
    // behavior).
    setMetadataInContainer(advisorContext.getMetaDataContainer(), context.getManagedObjectService()
        .getManagedObject(advisorContext.getUser(), advisorContext.getId()).getMetaDataItems(),
        true); // avoid restoring original values by preferring values already in the container.
  }

  /**
   * Set a piece of metadata to the metadata container. Metadata container items are updated if and
   * only if the metadata name matches (case-insensitive).
   * <p>
   * This method may be incapable of <i>adding</i> metadata configured to repeat.
   * 
   * @param mdContainer
   * @param name
   * @param value
   */
  public void setMetadataInContainer(MetaDataContainer mdContainer, String name, String value) {
    setMetadataInContainer(mdContainer, name, value, false); // false = prefer values in list
  }

  /**
   * Set a piece of metadata to the metadata container. Metadata container items are updated if the
   * metadata name matches (case-insensitive), the values differ (case-sensitive), and not
   * instructed to retain the container value.
   * <p>
   * This method may be incapable of <i>adding</i> metadata configured to repeat.
   * 
   * @param mdContainer
   * @param name
   * @param value
   * @param preferContainerValue Submit true when to keep an LMD value in the container as opposed
   *        to the list when the names match. This was added to prevent a subsequent MO advisor from
   *        restoring an original LMD value when making sure no LMD is dropped.
   */
  public void setMetadataInContainer(MetaDataContainer mdContainer, String name, String value,
      boolean preferContainerValue) {
    if (mdContainer != null && StringUtils.isNotBlank(name) && value != null) {
      List<MetaDataItem> mdList = new ArrayList<MetaDataItem>(1);
      mdList.add(new MetaDataItem(name, value));
      setMetadataInContainer(mdContainer, mdList, preferContainerValue);
    }
  }

  /**
   * Set a piece of metadata to the metadata container. Metadata container items are updated if and
   * only if the metadata name matches (case-insensitive).
   * <p>
   * This method may be incapable of <i>adding</i> metadata configured to repeat.
   * 
   * @param mdContainer
   * @param mdList
   */
  public void setMetadataInContainer(MetaDataContainer mdContainer, List<MetaDataItem> mdList) {
    setMetadataInContainer(mdContainer, mdList, false); // false = prefer values in list
  }

  /**
   * Set a piece of metadata to the metadata container. Metadata container items are updated if the
   * metadata name matches (case-insensitive), the values differ (case-sensitive), and not
   * instructed to retain the container value.
   * <p>
   * This method may be incapable of <i>adding</i> metadata configured to repeat but is able to
   * initially accept repeating LMD. The metadata container must be empty and the metadata list must
   * include the repeating metadata (same name but multiple values).
   * 
   * @param mdContainer
   * @param mdList
   * @param preferContainerValue Submit true when to keep an LMD value in the container as opposed
   *        to the list when the names match. This was added to prevent a subsequent MO advisor from
   *        restoring an original LMD value when making sure no LMD is dropped.
   */
  public void setMetadataInContainer(MetaDataContainer mdContainer, List<MetaDataItem> mdList,
      boolean preferContainerValue) {
    if (mdContainer != null && mdList != null) {

      // By blinding accepting all metadata in the list when the container is empty, we accept all
      // repeating LMD, thereby providing partial support of repeating LMD.
      boolean blindlyAccept = mdContainer.getMetaDataItemList().size() == 0;

      for (MetaDataItem incoming : mdList) {
        boolean skip = false;
        boolean updated = false;
        for (MetaDataItem existing : mdContainer.getMetaDataItemList()) {
          if (!blindlyAccept && existing.getName().equalsIgnoreCase(incoming.getName())) {
            if (existing.getValue().equals(incoming.getValue())) {
              skip = true;
              break;
            }

            if (preferContainerValue) {
              log.info(ProjectMessageResource.getMessageText(
                  "update.mo.advisor.info.retained.existing.lmd.value", existing.getName(), existing
                      .getValue(), incoming.getValue()));
              skip = true;
              break;
            }

            log.info(ProjectMessageResource.getMessageText(
                "update.mo.advisor.info.updated.existing.lmd.value", existing.getName(), existing
                    .getValue(), incoming.getValue()));
            existing.setValue(incoming.getValue());
            updated = true;
            break;
          }
        }

        if (blindlyAccept || (!skip && !updated)) {
          log.info(ProjectMessageResource.getMessageText(
              "update.mo.advisor.info.added.new.lmd.value", incoming.getName(), incoming
                  .getValue()));
          mdContainer.addMetaDataItem(incoming.getName(), incoming.getValue());
        }
      }
    }
  }
}
