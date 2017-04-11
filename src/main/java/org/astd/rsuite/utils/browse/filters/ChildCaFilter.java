package org.astd.rsuite.utils.browse.filters;

import com.reallysi.rsuite.api.*;

public interface ChildCaFilter{
	boolean accept(ContentAssembly ca) throws RSuiteException;
}