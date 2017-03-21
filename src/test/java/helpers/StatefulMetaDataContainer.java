package test.helpers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.reallysi.rsuite.api.MetaDataItem;
import com.reallysi.rsuite.api.control.MetaDataContainer;

/**
 * A stateful metadata container useful for unit testing.
 */
public class StatefulMetaDataContainer
    implements MetaDataContainer {

  List<MetaDataItem> mdList = new ArrayList<MetaDataItem>();

  @Override
  public void updateMetaDataItem(String id, String name, String value) {

    if (StringUtils.isBlank(id)) {
      return;
    }

    if (mdList != null) {
      for (MetaDataItem item : mdList) {
        if (id.equals(item.getId())) {
          item.setName(name);
          item.setValue(value);
          break;
        }
      }
    }
  }

  @Override
  public boolean isNotEmpty() {
    return mdList.size() > 0;
  }

  @Override
  public List<MetaDataItem> getMetaDataItemList() {
    return mdList;
  }

  @Override
  public void addMetaDataItem(String name, String value) {
    mdList.add(new MetaDataItem(name, value));
  }

}
