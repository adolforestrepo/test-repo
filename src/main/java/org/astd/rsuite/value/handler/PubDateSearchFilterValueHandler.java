package org.astd.rsuite.value.handler;

import org.astd.rsuite.constants.LayeredMetadataConstants;

import com.reallysi.rsuite.api.search.SearchFieldValueType;
import com.reallysi.rsuite.api.search.faceted.MetadataSearchScope;

/**
 * The date search filter value handler for the planned pub date LMD definition.
 *
 * @since RSuite 4.1.4 (RCS-2976)
 */
public class PubDateSearchFilterValueHandler
    extends LMDDateSearchFilterValueHandler
    implements LayeredMetadataConstants {

  public PubDateSearchFilterValueHandler() {
    super("pubDate", "Pub Date", "TODO", // Kept as a sample
        SearchFieldValueType.DATE, MetadataSearchScope.GLOBAL);
  }

}
