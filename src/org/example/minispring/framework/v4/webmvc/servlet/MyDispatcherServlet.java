package org.example.minispring.framework.v4.webmvc.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.example.minispring.framework.annotation.MyController;
import org.example.minispring.framework.annotation.MyRequestMapping;
import org.example.minispring.framework.v4.context.MyApplicationContext;
import org.example.minispring.framework.v4.webmvc.MyHandlerAdapter;
import org.example.minispring.framework.v4.webmvc.MyHandlerMapping;
import org.example.minispring.framework.v4.webmvc.MyModelAndView;
import org.example.minispring.framework.v4.webmvc.MyView;
import org.example.minispring.framework.v4.webmvc.MyViewResolver;

public class MyDispatcherServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;

	private final String LOCATION = "contextConfigLocation";
	
	private List<MyHandlerMapping> handlerMappings = new ArrayList<>();
	
	private Map<MyHandlerMapping, MyHandlerAdapter> handlerAdapters = new HashMap<>();
	
	private List<MyViewResolver> viewResolvers = new ArrayList<>();
	
	private MyApplicationContext context;
	
	private ServletContext servletContext;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		this.servletContext = config.getServletContext();
		context = new MyApplicationContext(config.getInitParameter(LOCATION));
		initStrategies(context);
	}
	
	protected void initStrategies(MyApplicationContext context) {
		// 有9种策略
		// 针对每个用户请求，都会经过一些处理策略处理，最终才能有结果输出
		// 每种策略可以自定义干预，但是最终的结果都一致
		
		// ======================传说中的9大组件=========================
		// 文件上传解析
		initMultipartResolver(context);
		
		// 本地化解析
		initLocaleResolver(context);
		
		// 主题解析
		initThemeResolver(context);
		
		// HandlerMapping用来保存url和Controller.Method的对应关系
		initHandlerMappings(context);
		
		// HandlerAdapter用来动态匹配Method参数, 包括类型转换、动态赋值
		initHandlerAdapters(context);
		
		// 异常解析
		initHandlerExceptionResolvers(context);
		
		// 直接将请求解析到视图名
		initRequestToViewNameTranslator(context);
		
		// 通过ViewResolver实现动态模板的解析
		initViewResolvers(context);
		
		// Flash映射管理器
		initFlashMapperManager(context);
	}
	
	/*
	 * 将Controller中配置的RequestMapping和Method进行一一对应
	 */
	private void initHandlerMappings(MyApplicationContext context) {
		String[] beanNames = context.getBeanDefinitionNames();
		try {
			for (String beanName : beanNames) {
				Object bean = context.getBean(beanName);
				Class<?> clazz = bean.getClass();
				if (!clazz.isAnnotationPresent(MyController.class)) { continue; }
				
				String baseUrl = "";
				
				// 扫描Controller类的RequestMapping注解，组装url
				if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
					MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
					baseUrl = requestMapping.value();
				}
				
				// 扫描Controller类的所有public方法的RequestMapping注解，继续组装url
				Method[] methods = clazz.getMethods();
				for (Method method : methods) {
					if (!method.isAnnotationPresent(MyRequestMapping.class)) { continue; }
					
					MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
					String regex = ("/" + baseUrl + requestMapping.value())
							.replaceAll("\\*", ".*").replaceAll("/+", "/");
					Pattern pattern = Pattern.compile(regex);
					this.handlerMappings.add(new MyHandlerMapping(pattern, bean, method));
					System.out.println("Mapping: " + regex + " -> " + method);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void initHandlerAdapters(MyApplicationContext context) {
		for (MyHandlerMapping handlerMapping : this.handlerMappings) {
			this.handlerAdapters.put(handlerMapping, new MyHandlerAdapter());
		}
	}
	
	private void initViewResolvers(MyApplicationContext context) {
		String templateRoot = context.getConfig().getProperty("templateRoot");
		String templateRootPath = this.servletContext.getRealPath(templateRoot);
		File templateRootDir = new File(templateRootPath);
		for (int i = 0; i < templateRootDir.listFiles().length; i++) {
			this.viewResolvers.add(new MyViewResolver(templateRootPath));
		}
	}
	
	/*
	 * 文件上传解析，如果请求类型是multipart，将通过MultipartResolver进行文件上传解析
	 */
	private void initMultipartResolver(MyApplicationContext context) {};
	
	/*
	 * 本地化解析
	 */
	private void initLocaleResolver(MyApplicationContext context) {};
	
	/*
	 * 主题解析
	 */
	private void initThemeResolver(MyApplicationContext context) {};
	
	/*
	 * 异常解析：如果执行过程中遇到异常，将交给HandlerExceptionResolver解析
	 */
	private void initHandlerExceptionResolvers(MyApplicationContext context) {};
	
	/*
	 * 直接将请求解析到视图名
	 */
	private void initRequestToViewNameTranslator(MyApplicationContext context) {};
	
	/*
	 * Flash映射管理器
	 */
	private void initFlashMapperManager(MyApplicationContext context) {};
	
	
	
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
		// 根据用户请求的URL来获得一个Handler
		MyHandlerMapping handler = getHandler(req);
		if (handler == null) {
			processDispatchResult(req, resp, new MyModelAndView("404"));
			return;
		}
		
		MyHandlerAdapter handlerAdapter = getHandlerAdapter(handler);
		
		// 这一步只是调用方法，得到返回值
		MyModelAndView mv = handlerAdapter.handle(req, resp, handler);
		
		// 这一步才是真的输出
		processDispatchResult(req, resp, mv);
	}
	
	/**
	 * 根据请求的url获取到匹配的Handler
	 * 
	 * @param req
	 * @return
	 */
	private MyHandlerMapping getHandler(HttpServletRequest req) {
		if (this.handlerMappings.isEmpty()) { return null; }
		String url = req.getRequestURI();
		String contextPath = req.getContextPath();
		url = url.replace(contextPath, "").replaceAll("/+", "/");
		for (MyHandlerMapping handler : this.handlerMappings) {
			Matcher matcher = handler.getPattern().matcher(url);
			if (matcher.matches()) { 
				return handler; 
			}
		}
		return null;
	}
	
	/**
	 * 根据Handler获取到HandlerAdapter
	 * 
	 * @param handler
	 * @return
	 */
	private MyHandlerAdapter getHandlerAdapter(MyHandlerMapping handler) {
		if (this.handlerAdapters.isEmpty()) { return null; }
		MyHandlerAdapter handlerAdapter = this.handlerAdapters.get(handler);
		if (handlerAdapter.supports(handler)) {
			return handlerAdapter;
		}
		return null;
	}
	
	private void processDispatchResult(HttpServletRequest request, HttpServletResponse response, MyModelAndView mv) throws Exception {
		if (null == mv) { return; }
		
		if (this.viewResolvers == null || this.viewResolvers.isEmpty()) { return; }
		
		for (MyViewResolver viewResolver : this.viewResolvers) {
			MyView view = viewResolver.resolveViewName(mv.getViewName(), null);
			if (view != null) {
				view.render(mv.getModel(), request, response);
				return;
			}
		}
	}
	
}
