package org.example.minispring.framework.v4.webmvc;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

/*
 * 保存URL和Method的映射关系
 */
public class MyHandlerMapping {

	// 目标方法所在的controller对象
	private Object controller;
	// URL对应的目标方法
	private Method method;
	// URL的封装
	private Pattern pattern;
	
	public MyHandlerMapping(Pattern pattern, Object controller, Method method) {
		this.setPattern(pattern);
		this.setController(controller);
		this.setMethod(method);
	}

	public Object getController() {
		return controller;
	}

	public void setController(Object controller) {
		this.controller = controller;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}
	
}
