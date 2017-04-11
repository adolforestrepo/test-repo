package org.astd.rsuite.constants;

public enum CaConstants implements ProjectConstant {

	CA_TYPE_ARTICLE("article"),
	CA_TYPE_PRODUCT("product"),
	CA_TYPE_VOLUME("volume"),
	CA_TYPE_ISSUE("issue"),
	
	CA_NAME_IN_PROGERSS ("Books"),
	CA_NAME_PUBLISHED ("Magazines"),
	CA_NAME_MISCELLANEOUS ("Miscellaneous"),
	LMD_NAME_PRODUCT_CODE ("product_code"),
	CA_NAME_SUPPORT ("Support");
	

	private String constantName;

	private CaConstants (String constantName) {
		this.constantName = constantName;
	}

	
	@Override
	public String getName() {
		return constantName;
	}

	

	
}

