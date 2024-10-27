package org.example.minispring.framework.v4.context.support;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.example.minispring.framework.v4.beans.config.MyBeanDefinition;

/**
 * 对配置文件进行查找、读取、解析
 */
public class MyBeanDefinitionReader {

	private List<String> registryBeanClass = new ArrayList<>();
	private Properties config = new Properties();
	
	// 配置文件中约定的key
	private final String SCAN_PACKAGE = "scanPackage";
	
	public MyBeanDefinitionReader(String... locations) {
		try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(locations[0].replace("classpath", ""))) {
			config.load(is);
		} catch (Exception e) {
			e.printStackTrace();
		}
		doScanner(config.getProperty(SCAN_PACKAGE));
	}
	
	/**
	 * 扫描相关的类，支持逗号分隔的多个类路径。
	 * 本例中scanPackage=org.example.minispring
	 */
	private void doScanner(String scanPackages) {
		for (String scanPackage : scanPackages.split(",")) {
			// 转换为文件路径，实际上就是把"."替换为"/"
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
					registryBeanClass.add(clazzName);
				}
			}
		}
	}
	
	public Properties getConfig() {
		return this.config;
	}
	
	public List<MyBeanDefinition> loadBeanDefinitions() throws ClassNotFoundException {
		List<MyBeanDefinition> beanDefinitionList = new ArrayList<>();
		for (String className : registryBeanClass) {
			Class<?> beanClass = Class.forName(className);
			if (beanClass.isInterface())
				continue;
			beanDefinitionList
					.add(doCreateBeanDefinition(toLowerFirstCase(beanClass.getSimpleName()), beanClass.getName()));
			
			// 找到该bean的所有接口，保存接口类名和实现类对象的映射：
			// org.example.minispring.service.IQueryService -> org.example.minispring.service.impl.QueryService
			// 
			// @MyAutowired IQueryService queryService;
			// 于是这种接口类型的依赖也可以注入成功了
			Class<?>[] interfaces = beanClass.getInterfaces();
			for (Class<?> i : interfaces) {
				beanDefinitionList.add(doCreateBeanDefinition(i.getName(), beanClass.getName()));
			}
		}
		return beanDefinitionList;
	}
	
	/**
	 * 把每一个配置信息解析成一个BeanDefinition,
	 * 例如：{factoryBeanName = demoAction, beanClassName = org.example.minispring.action.DemoAction, lazyInit = false}
	 * 
	 * @param factoryBeanName
	 * @param beanClassName
	 * @return
	 */
	private MyBeanDefinition doCreateBeanDefinition(String factoryBeanName, String beanClassName) {
		MyBeanDefinition beanDefinition = new MyBeanDefinition();
		beanDefinition.setBeanClassName(beanClassName);
		beanDefinition.setFactoryBeanName(factoryBeanName);
		return beanDefinition;
	}
	
	/**
	 * 将类名首字母改为小写
	 * 
	 * @param simpleName
	 * @return
	 */
	private String toLowerFirstCase(String simpleName) {
		char[] chars = simpleName.toCharArray();
		chars[0] += 32;
		return String.valueOf(chars);
	}
	
}
