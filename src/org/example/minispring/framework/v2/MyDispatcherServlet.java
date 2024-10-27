package org.example.minispring.framework.v2;

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
	private Map<String, Method> handlerMapping = new HashMap<>();
	
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
	
	private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		String url = req.getRequestURI();
		String contextPath = req.getContextPath();
		url = url.replace(contextPath, "").replaceAll("/+", "/");
		if (!this.handlerMapping.containsKey(url)) {
			resp.getWriter().write("404 Not Found");
			return;
		}
		// 根据url找到对应的方法
		// 例如url(/demo/query)对应的method为
		// org.example.minispring.action.DemoAction.query(HttpServletRequest, HttpServletResponse, String)
		Method method = (Method) this.handlerMapping.get(url);
		// 获取请求参数, 此处为: name = [watermark]
		Map<String, String[]> parameterMap = req.getParameterMap();
		
		// 1. method.getDeclaringClass().getName(): beanName, 此处为org.example.minispring.action.DemoAction
		// 2. IoC.get(beanName): 根据beanName获取到对应的bean实例:org.example.minispring.action.DemoAction@51e3ce14
		// 3. method.invoke调用的就是org.example.minispring.action.DemoAction@51e3ce14.query(req, resp, name)
		//String beanName = method.getDeclaringClass().getName();
		//method.invoke(IoC.get(beanName), new Object[] { req, resp, parameterMap.get("name")[0] });
		
		
		// url参数的动态处理
		// 获取method的形参列表
		Class<?>[] parameterTypes = method.getParameterTypes();
		// 保存赋值参数的位置
		Object[] paramValues = new Object[parameterTypes.length];
		// 根据参数的位置动态赋值
		for (int i = 0; i < parameterTypes.length; i++) {
			Class<?> parameterType = parameterTypes[i];
			if (parameterType == HttpServletRequest.class) {
				paramValues[i] = req;
				continue;
			} else if (parameterType == HttpServletResponse.class) {
				paramValues[i] = resp;
				continue;
			} else if (parameterType == String.class) {
				// 提取方法中加了注解的参数
				Annotation[][] pa = method.getParameterAnnotations();
				for (int j = 0; j < pa.length; j++) {
					for (Annotation a : pa[j]) {
						if (a instanceof MyRequestParam) {
							String paramName = ((MyRequestParam) a).value().trim();
							if (!"".equals(paramName)) {
								String paramValue = Arrays.toString(parameterMap.get(paramName))
										.replaceAll("\\[|\\]", "")
										.replaceAll("\\s", ",");
								paramValues[i] = paramValue;
							}
						}
					}
				}
			}
		}
		String beanName = method.getDeclaringClass().getName();
		// 根据动态获取的参数去invoke method
		method.invoke(IoC.get(beanName), paramValues);
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
				// 什么类需要实例化呢?
				// 加了注解的类需要实例化, 本例中需要实例化@MyController, @MyService注解的类
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
				String url = (baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
				// 保存url -> method的对应关系
				handlerMapping.put(url, method);
				System.out.println("Mapped " + url + " -> " + method);
			}
		}
	}
}
