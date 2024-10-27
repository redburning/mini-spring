package org.example.minispring.framework.v4.webmvc;

import java.io.File;
import java.util.Locale;

public class MyViewResolver {

	private static final String DEFAULT_TEMPLATE_SUFFIX = ".html";
	
	private String templateRootPath;
	private String viewName;
	
	public MyViewResolver(String templateRootPath) {
		this.templateRootPath = templateRootPath;
	}
	
	public MyView resolveViewName(String viewName, Locale locale) throws Exception {
		this.viewName = viewName;
		if (null == viewName || "".equals(viewName.trim())) { return null; }
		viewName = viewName.endsWith(DEFAULT_TEMPLATE_SUFFIX) ? viewName : (viewName + DEFAULT_TEMPLATE_SUFFIX);
		File templateFile = new File((this.templateRootPath + "/" + viewName).replaceAll("/+", "/"));
		return new MyView(templateFile);
	}
	
	public String getViewName() {
		return viewName;
	}
	
}
