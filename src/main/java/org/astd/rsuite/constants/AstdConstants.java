package org.astd.rsuite.constants;

public enum AstdConstants implements ProjectConstant {

	RSUITE_SESSION_KEY ("RSUITE-SESSION-KEY"),

	REST_V1_URL_ROOT ("/rsuite/rest/v1"),

	PARAM_RSUITE_ID ("rsuiteId"),

	UI_PARAM_RSUITE_REFRESH_MANAGED_OBJECTS ("rsuite:refreshManagedObjects"),

	UI_PROPERTY_OBJECTS ("objects"),

	UI_PROPERTY_CHILDREN ("children");
	
	private String constantName;

	private AstdConstants(String constantName) {
		this.constantName = constantName;
	}

	@Override
	public String getName() {
		return constantName;
	}

}
