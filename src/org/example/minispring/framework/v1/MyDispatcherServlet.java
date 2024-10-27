package org.example.minispring.framework.v1;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.example.minispring.framework.annotation.MyAutowired;
import org.example.minispring.framework.annotation.MyController;
import org.example.minispring.framework.annotation.MyRequestMapping;
import org.example.minispring.framework.annotation.MyService;

public class MyDispatcherServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// 保存beanName -> bean的映射关系
	private Map<String, Object> beanMapping = new HashMap<>();
	// 保存url -> method的映射关系
	private Map<String, Object> handlerMapping = new HashMap<>();

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
		// 此处method为DemoAction.query(HttpServletRequest, HttpServletResponse, String)
		Method method = (Method) this.handlerMapping.get(url);
		// 获取请求参数, 此处为: name = [浏览器传来的值]
		Map<String, String[]> params = req.getParameterMap();
		
		// 1. method.getDeclaringClass().getName()
        //	   本例为org.example.minispring.action.DemoAction
		// 2. beanMapping.get(beanName): 根据beanName获取到对应的bean实例，例如：
        //	  org.example.minispring.action.DemoAction@51e3ce14
		// 3. method.invoke调用的就是
        //	  org.example.minispring.action.DemoAction@51e3ce14.query(req, resp, name)
		String beanName = method.getDeclaringClass().getName();
		method.invoke(this.beanMapping.get(beanName), new Object[] { req, resp, params.get("name")[0] });
	}
    
	@Override
	public void init(ServletConfig config) throws ServletException {
		InputStream is = this.getClass().getClassLoader()
				.getResourceAsStream(config.getInitParameter("contextConfigLocation"));
		Properties configContext = new Properties();
		try {
			configContext.load(is);
			String scanPackage = configContext.getProperty("scanPackage");
            // 扫描相关的类，本例中scanPackage=org.example.minispring
			doScanner(scanPackage);
            
			for (String className : beanMapping.keySet()) {
				Class<?> clazz = Class.forName(className);
				// 解析@MyController注解
				if (clazz.isAnnotationPresent(MyController.class)) {
					// 保存className和@MyController实例的对应关系
					beanMapping.put(className, clazz.newInstance());
					String baseUrl = "";
					// 解析@MyController上的@MyRequestMapping注解，作为当前Controller的baseUrl
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
				// 解析@MyService注解
				else if (clazz.isAnnotationPresent(MyService.class)) {
					MyService service = clazz.getAnnotation(MyService.class);
					String beanName = service.value();
					if ("".equals(beanName)) {
						beanName = clazz.getName();
					}
					Object instance = clazz.newInstance();					
					// 保存className和@MyService实例的对应关系
					beanMapping.put(beanName, instance);
					for (Class<?> i : clazz.getInterfaces()) {
						beanMapping.put(i.getName(), instance);
					}
				}
			}
			
			// 解析对象之间的依赖关系，依赖注入
			for (Object object : beanMapping.values()) {
				if (object == null) {
					continue;
				}
				Class<?> clazz = object.getClass();
				// 向MyController中注入MyService
				if (clazz.isAnnotationPresent(MyController.class)) {
					Field[] fields = clazz.getDeclaredFields();
					for (Field field : fields) {
						if (!field.isAnnotationPresent(MyAutowired.class)) {
							continue;
						}
						MyAutowired autowired = field.getAnnotation(MyAutowired.class);
						String beanName = autowired.value();
						if ("".equals(beanName)) {
							beanName = field.getType().getName();
						}
                        // 只要加了@MyAutowired注解都要强制赋值
						// 反射中叫做暴力访问
						field.setAccessible(true);
                        // 用反射机制动态给字段赋值
						// 赋值后DemoAction.demoService = DemoService@c97ae21
						// 也即DemoService实例被注入到了DemoAction对象中，这就是依赖注入
						field.set(beanMapping.get(clazz.getName()), beanMapping.get(beanName));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 扫描相关的类，本例中scanPackage=org.example.minispring
	 */
	private void doScanner(String scanPackage) {
		URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
		File classDir = new File(url.getFile());
		for (File file : classDir.listFiles()) {
			if (file.isDirectory()) {
				// 递归扫描子文件夹
				doScanner(scanPackage + "." + file.getName());
			} else {
				String clazzName = scanPackage + "." + file.getName().replace(".class", "");
				beanMapping.put(clazzName, null);
			}
		}
	}
}