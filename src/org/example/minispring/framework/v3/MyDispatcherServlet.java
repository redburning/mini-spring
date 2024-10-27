package org.example.minispring.framework.v3;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.example.minispring.framework.annotation.MyAutowired;
import org.example.minispring.framework.annotation.MyController;
import org.example.minispring.framework.annotation.MyRequestMapping;
import org.example.minispring.framework.annotation.MyRequestParam;
import org.example.minispring.framework.annotation.MyService;

public class MyDispatcherServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// 保存application.properties配置文件中的内容
	private Properties contextConfig = new Properties();
	
	// 保存扫描到所有的类名
	private List<String> classNames = new ArrayList<>();
	
	// 传说中的IoC容器，我们来揭开它的神秘面纱
	// 为了简化程序，暂时不考虑ConcurrentHashMap，主要关注原理和设计思想
	private Map<String, Object> IoC = new HashMap<>();
	
	// 保存url -> method的映射关系
	private List<Handler> handlerMapping = new ArrayList<>();
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}
	
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			doDispatch(req, resp);
		} catch (Exception e) {
			e.printStackTrace();
			resp.getWriter().write("500 Exception " + Arrays.toString(e.getStackTrace()));
		}
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		// 1. 加载配置文件
		doLoadConfig(config.getInitParameter("contextConfigLocation"));
		
		// 2. 扫描相关类
		doScanner(contextConfig.getProperty("scanPackage"));
		
		// 3. 初始化扫描到的类，并将它们放到IoC容器中
		doInstance();
		
		// 4. 依赖注入
		doAutowired();
		
		// 5. 初始化handlerMapping
		initHandlerMapping();
	}

	private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		Handler handler = getHandler(req);
		if (handler == null) {
			resp.getWriter().write("404 Not Found!");
			return;
		}
		
		// 获得方法的形参列表
		Class<?>[] paramTypes = handler.method.getParameterTypes();
		Object[] paramValues = new Object[paramTypes.length];
		
		// 从req中接收到的参数是Map类型，需要按照handler的method中的参数顺序传递
		// handler.paramIndexMapping就发挥作用了：按照参数名称映射到正确的index位置
		// 保证了paramValues中参数按顺序正确传递给method
		Map<String, String[]> params = req.getParameterMap();
		for (Map.Entry<String, String[]> entry : params.entrySet()) {
			String value = Arrays.toString(entry.getValue()).replaceAll("\\[|\\]", "").replaceAll("\\s", ",");
			if (!handler.paramIndexMapping.containsKey(entry.getKey()))
				continue;
			
			int index = handler.paramIndexMapping.get(entry.getKey());
			paramValues[index] = convert(paramTypes[index], value);
		}
		
		if (handler.paramIndexMapping.containsKey(HttpServletRequest.class.getName())) {
			int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
			paramValues[reqIndex] = req;
		}
		
		if (handler.paramIndexMapping.containsKey(HttpServletResponse.class.getName())) {
			int respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
			paramValues[respIndex] = resp;
		}
		
		Object returnValue = handler.method.invoke(handler.controller, paramValues);
		if (returnValue == null || returnValue instanceof Void)
			return;
		resp.getWriter().write(returnValue.toString());
	}
	
	/**
	 * 加载配置文件，本例中配置文件configFileName为application.properties
	 */
	private void doLoadConfig(String configFileName) {
		// 找到Spring主配置文件所在路径
		// 读取出来保存到Properties文件中
		// 本例中配置文件中的内容只有一行: scanPackage=org.example.minispring
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(configFileName);
		try {
			contextConfig.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 扫描相关的类，本例中scanPackage=org.example.minispring
	 */
	private void doScanner(String scanPackage) {
		// 转换为文件路径，手机上就是把"."替换为"/"
		URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
		File classDir = new File(url.getFile());
		for (File file : classDir.listFiles()) {
			if (file.isDirectory()) {
				// 递归扫描子文件夹
				doScanner(scanPackage + "." + file.getName());
			} else {
				// 只需要扫描.class文件
				if (!file.getName().endsWith(".class"))
					continue;
				String clazzName = scanPackage + "." + file.getName().replace(".class", "");
				classNames.add(clazzName);
			}
		}
	}
	
	/**
	 * 初始化扫描到的类，并将它们放到IoC容器中
	 * 
	 * 本例中IoC容器中的内容如下：
	 * org.example.minispring.service.IDemoService = org.example.minispring.service.impl.DemoService@c97ae21
	 * org.example.minispring.service.impl.DemoService = org.example.minispring.service.impl.DemoService@c97ae21
	 * org.example.minispring.action.DemoAction = org.example.minispring.action.DemoAction@25051c3
	 */
	private void doInstance() {
		if (classNames.isEmpty())
			return;
		try {
			for (String className : classNames) {
				Class<?> clazz = Class.forName(className);
				if (clazz.isAnnotationPresent(MyController.class)) {
					Object instance = clazz.newInstance();
					String beanName = clazz.getName();
					IoC.put(beanName, instance);
				} else if (clazz.isAnnotationPresent(MyService.class)) {
					// 自定义的beanName
					MyService service = clazz.getAnnotation(MyService.class);
					String beanName = service.value();
					if ("".equals(beanName.trim())) {
						beanName = clazz.getName();
					}

					Object instance = clazz.newInstance();
					IoC.put(beanName, instance);
					// 根据接口类型自动赋值
					for (Class<?> i : clazz.getInterfaces()) {
						if (IoC.containsKey(i.getName())) {
							throw new Exception("The '" + i.getName() + "' already exists!");
						}
						// 把接口的类型直接当做key
						IoC.put(i.getName(), instance);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 完成依赖注入
	 */
	private void doAutowired() {
		if (IoC.isEmpty())
			return;
		for (Entry<String, Object> entry : IoC.entrySet()) {
			// 获取所有的字段，包括public, private, protected, default类型的
			Field[] fields = entry.getValue().getClass().getDeclaredFields();
			for (Field field : fields) {
				if (!field.isAnnotationPresent(MyAutowired.class))
					continue;
				MyAutowired autoWired = field.getAnnotation(MyAutowired.class);

				// 如果用户没有定义beanName，默认就根据类型注入
				String beanName = autoWired.value().trim();
				if ("".equals(beanName)) {
					beanName = field.getType().getName();
				}

				// 如果是public之外的类型，只要加了@MyAutowired注解都要强制赋值
				// 反射中叫做暴力访问
				field.setAccessible(true);

				try {
					// 用反射机制动态给字段赋值
					// 赋值后DemoAction.demoService = org.example.minispring.service.impl.DemoService@c97ae21
					// 也即DemoService实例被注入到了DemoAction对象中，此谓之依赖注入
					field.set(entry.getValue(), IoC.get(beanName));
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 初始化HandlerMapping
	 * 
	 * 本例中handlerMapping中的内容是：
	 * /demo/query = org.example.minispring.action.DemoAction.query(HttpServletRequest, HttpServletResponse, String)
	 */
	private void initHandlerMapping() {
		if (IoC.isEmpty())
			return;

		for (Entry<String, Object> entry : IoC.entrySet()) {
			Class<?> clazz = entry.getValue().getClass();
			if (!clazz.isAnnotationPresent(MyController.class))
				continue;

			String baseUrl = "";
			if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
				MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
				baseUrl = requestMapping.value();
			}

			// 解析@MyController中方法上的@MyRequestMapping注解
			Method[] methods = clazz.getMethods();
			for (Method method : methods) {
				if (!method.isAnnotationPresent(MyRequestMapping.class)) {
					continue;
				}
				MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);				
				// 组合方法签名上的完整url，正则替换是为防止路径中出现多个连续多个"/"的不规范写法
				String regex = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
				Pattern pattern = Pattern.compile(regex);
				// 保存url -> method的对应关系
				handlerMapping.add(new Handler(pattern, entry.getValue(), method));
				System.out.println("Mapped " + regex + " -> " + method);
			}
		}
	}

	/**
	 * 根据request的url找到匹配的handler
	 */
	private Handler getHandler(HttpServletRequest req) {
		if (handlerMapping.isEmpty())
			return null;
		
		String url = req.getRequestURI();
		String contextPath = req.getContextPath();
		url = url.replace(contextPath, "").replaceAll("/+", "/");
		for (Handler handler : handlerMapping) {
			Matcher matcher = handler.pattern.matcher(url);
			// 没有匹配上，继续匹配下一个
			if (!matcher.matches())
				continue;
			return handler;
		}
		return null;
	}
	
	/*
	 * url传过来的参数都是String类型的，只需要把String转换为任意类型
	 */
	private Object convert(Class<?> type, String value) {
		if (Integer.class == type) {
			return Integer.valueOf(value);
		}
		// 如果还有double或者其他类型的参数，继续添加if
		return value;
	}
	
	/**
	 * 内部类，记录Controller中RequestMapping和Method的对应关系
	 */
	private class Handler {
		
		protected Object controller;	// 保存方法对应的实例
		protected Method method;		// 保存映射的方法
		protected Pattern pattern;		// url pattern
		protected Map<String, Integer> paramIndexMapping;	// 保存参数名-参数index的映射
		
		/**
		 * 构造方法
		 * @param pattern
		 * @param controller
		 * @param method
		 */
		protected Handler(Pattern pattern, Object controller, Method method) {
			this.pattern = pattern;
			this.controller = controller;
			this.method = method;
			paramIndexMapping = new HashMap<>();
			putParamIndexMapping(method);
		}
		
		private void putParamIndexMapping(Method method) {
			// 提取方法中加了注解的参数
			Annotation[][] pa = method.getParameterAnnotations();
			for (int i = 0; i < pa.length; i++) {
				for (Annotation a: pa[i]) {
					if (a instanceof MyRequestParam) {
						String paramName = ((MyRequestParam) a).value().trim();
						if (!"".equals(paramName)) {
							paramIndexMapping.put(paramName, i);
						}
					}
				}
			}
			// 提取方法中的request和response参数
			Class<?>[] paramsTypes = method.getParameterTypes();
			for (int i = 0; i < paramsTypes.length; i++) {
				Class<?> type = paramsTypes[i];
				if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
					paramIndexMapping.put(type.getName(), i);
				}
			}
		}
	}
	
}
