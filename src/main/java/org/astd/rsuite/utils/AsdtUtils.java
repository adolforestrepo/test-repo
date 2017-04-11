package org.astd.rsuite.utils;



import java.util.List;

import com.reallysi.rsuite.api.ContentAssembly;
import com.reallysi.rsuite.api.ContentAssemblyItem;
import com.reallysi.rsuite.api.RSuiteException;
import com.reallysi.rsuite.api.User;
import com.reallysi.rsuite.api.extensions.ExecutionContext;
import com.reallysi.rsuite.service.ContentAssemblyService;
import static org.astd.rsuite.constants.CaConstants.CA_NAME_PUBLISHED;
import static org.astd.rsuite.constants.LmdConstants.LMD_NAME_PRODUCT_CODE;

/*import static com.jbjs.rsuite.contants.LmdConstants.LMD_NAME_PRODUCT_CODE;
import static com.jbjs.rsuite.contants.CaConstants.CA_NAME_IN_PROGERSS;
import static com.jbjs.rsuite.contants.CaConstants.CA_NAME_PUBLISHED;
import static com.jbjs.rsuite.contants.CaConstants.CA_TYPE_PRODUCT;
*/
public class AsdtUtils {
	
	public enum ProductParentDisplayName {

		//PRODUCT_PARENT_DISPLAY_NAME_IN_PROGERSS (CA_NAME_IN_PROGERSS.getName()),
		//PRODUCT_PARENT_DISPLAY_NAME_PUBLISHED (CA_NAME_PUBLISHED.getName());
		PRODUCT_PARENT_DISPLAY_NAME_PUBLISHED(CA_NAME_PUBLISHED.getName());

		private String displayName;
	
		private ProductParentDisplayName (String displayName) {
			this.displayName = displayName;
		}
		
		public String getDisplayName() {
			return displayName;
		}
				
	}

	public static ContentAssembly getProduct(ExecutionContext context,
			User user, ProductParentDisplayName productParentDisplayName, String product_code) throws RSuiteException {
		ContentAssemblyService caSrv = context.getContentAssemblyService();
		String rsuiteObjPath = "/".concat(productParentDisplayName.getDisplayName());
		ContentAssemblyItem publishedItem = caSrv.findObjectForPath(user, rsuiteObjPath);
		List<ContentAssembly> products = CAUtils.getChildrenCaByType(context, publishedItem, CA_NAME_PUBLISHED.getName());
		
		return CAUtils.extractCaByLmd(products, LMD_NAME_PRODUCT_CODE.getName(), product_code);
	}

}
