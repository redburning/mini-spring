package org.example.minispring.framework.v4.webmvc;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.example.minispring.framework.annotation.MyRequestParam;

/*
 * HandlerAdapter主要完成传递到服务端的参数列表与Method实参列表的对应关系，
 * 完成参数值的类型转换工作。
 * 核心方法是handle()，在handle()方法中用反射来调用被适配的目标方法，
 * 并将转换包装好的参数列表传递过去。
 */
public class MyHandlerAdapter {

	public boolean supports(Object handler) {
		return (handler instanceof MyHandlerMapping);
	}
	
	public MyModelAndView handle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
		MyHandlerMapping handlerMapping = (MyHandlerMapping) handler;
		
		// 每个方法有一个参数列表，这里保存的是形参列表
		Map<String, Integer> paramMapping = new HashMap<>();
		
		Annotation[][] pa = handlerMapping.getMethod().getParameterAnnotations();
		for (int i = 0; i < pa.length; i++) {
			for (Annotation a : pa[i]) {
				if (a instanceof MyRequestParam) {
					String paramName = ((MyRequestParam) a).value();
					// 这里只是保存命名参数
					if (!"".equals(paramName.trim())) {
						paramMapping.put(paramName, i);
					}
				}
			}
		}
		
		Class<?>[] paramTypes = handlerMapping.getMethod().getParameterTypes();
		for (int i = 0; i < paramTypes.length; i++) {
			Class<?> type = paramTypes[i];
			if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
				paramMapping.put(type.getName(), i);
			}
		}
		
		// 用户通过url传递过来的参数列表
		Map<String, String[]> reqParameterMap = req.getParameterMap();
		
		// 构造实参列表
		Object[] paramValues = new Object[paramTypes.length];
		
		// 从req中接收到的参数是Map类型，需要按照handler的method中的参数顺序传递
		// handler.paramIndexMapping就发挥作用了：按照参数名称映射到正确的index位置
		// 保证了paramValues中参数按顺序正确传递给method
		for (String key : reqParameterMap.keySet()) {
			String value = Arrays.toString(reqParameterMap.get(key)).replaceAll("\\[|\\]", "").replaceAll("\\s", "");
			if (!paramMapping.containsKey(key)) { continue; }
			int index = paramMapping.get(key);
			// 因为页面传过来的值都是String类型的，而在方法中定义的类型是千变万化的
			// 所以要针对我们传递过来的参数进行类型转换
			paramValues[index] = caseStringValue(value, paramTypes[index]);
		}
		
		if (paramMapping.containsKey(HttpServletRequest.class.getName())) {
			int reqIndex = paramMapping.get(HttpServletRequest.class.getName());
			paramValues[reqIndex] = req;
		}
		
		if (paramMapping.containsKey(HttpServletResponse.class.getName())) {
			int respIndex = paramMapping.get(HttpServletResponse.class.getName());
			paramValues[respIndex] = resp;
		}
		
		// 从handler中取出controller.method，利用反射机制调用
		Object result = handlerMapping.getMethod().invoke(handlerMapping.getController(), paramValues);
		
		if (result == null) { return null; }
		
		boolean isModelAndView = handlerMapping.getMethod().getReturnType() == MyModelAndView.class;
		if (isModelAndView) {
			return (MyModelAndView) result;
		} else {
			return null;
		}
	}
	
	private Object caseStringValue(String value, Class<?> clazz) {
		if (clazz == String.class) {
			return value;
		} else if (clazz == Integer.class) {
			return Integer.valueOf(value);
		} else if (clazz == int.class) {
			return Integer.valueOf(value).intValue();
		} else {
			return null;
		}
	}
	
}
