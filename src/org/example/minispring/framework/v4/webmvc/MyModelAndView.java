package org.example.minispring.framework.v4.webmvc;

import java.util.Map;

/*
 * 用于封装页面模板和要往页面传送的参数的对应关系
 */
public class MyModelAndView {

	// 页面模板的名称
	private String viewName;
	// 向页面传递的参数
	private Map<String, ?> model;
	
	public MyModelAndView(String viewName) {
		this(viewName, null);
	}
	
	public MyModelAndView(String viewName, Map<String, ?> model) {
		this.setViewName(viewName);
		this.setModel(model);
	}

	public String getViewName() {
		return viewName;
	}

	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

	public Map<String, ?> getModel() {
		return model;
	}

	public void setModel(Map<String, ?> model) {
		this.model = model;
	}
	
}
