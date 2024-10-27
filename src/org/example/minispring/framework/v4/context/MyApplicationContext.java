package org.example.minispring.framework.v4.context;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.example.minispring.framework.annotation.MyAutowired;
import org.example.minispring.framework.annotation.MyController;
import org.example.minispring.framework.annotation.MyService;
import org.example.minispring.framework.v4.beans.config.MyBeanDefinition;
import org.example.minispring.framework.v4.beans.config.MyBeanPostProcessor;
import org.example.minispring.framework.v4.beans.config.MyBeanWrapper;
import org.example.minispring.framework.v4.context.support.MyBeanDefinitionReader;
import org.example.minispring.framework.v4.context.support.MyDefaultListableBeanFactory;
import org.example.minispring.framework.v4.core.MyBeanFactory;

public class MyApplicationContext extends MyDefaultListableBeanFactory implements MyBeanFactory {

	private String[] configLocations;
	private MyBeanDefinitionReader reader;
	
	// 用来保证注册单例的容器
	private Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<>();
	
	// 用来存储所有被代理过的对象
	private Map<String, MyBeanWrapper> factoryBeanInstanceCache = new ConcurrentHashMap<>();
	
	public MyApplicationContext(String... configLocations) {
		this.configLocations = configLocations;
		try {
			refresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void refresh() throws Exception {
		// 1. 定位配置文件
		reader = new MyBeanDefinitionReader(configLocations);
		
		// 2. 加载配置文件，扫描相关类，把它们封装成BeanDefinition
		List<MyBeanDefinition> beanDefinitions = reader.loadBeanDefinitions();
		
		// 3. 注册，把配置信息放到容器里面（伪IoC容器）
		doRegisterBeanDefinition(beanDefinitions);
		
		// 4. 把不是延迟加载的类提前初始化
		doAutowired();
	}
	
	private void doRegisterBeanDefinition(List<MyBeanDefinition> beanDefinitions) throws Exception {
		for (MyBeanDefinition beanDefinition : beanDefinitions) {
			if (super.beanDefinitionMap.containsKey(beanDefinition.getFactoryBeanName())) {
				throw new Exception("The '" + beanDefinition.getFactoryBeanName() + "' already exists!");
			}
			super.beanDefinitionMap.put(beanDefinition.getFactoryBeanName(), beanDefinition);
		}
	}
	
	private void doAutowired() {
		for (String key : super.beanDefinitionMap.keySet()) {
			String beanName = key;
			if (!super.beanDefinitionMap.get(key).isLazyInit()) {
				try {
					getBean(beanName);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public Object getBean(Class<?> beanClass) throws Exception {
		return getBean(beanClass.getName());
	}

	@Override
	public Object getBean(String beanName) throws Exception {
		MyBeanDefinition beanDefinition = super.beanDefinitionMap.get(beanName);
		try {
			// 生成通知事件
			MyBeanPostProcessor beanPostProcessor = new MyBeanPostProcessor();
			
			Object instance = instantiateBean(beanDefinition);
			if (null == instance) { return null; }
			
			// 在实例初始化以前调用一次
			beanPostProcessor.postProcessBeforeInitialization(instance, beanName);
			
			MyBeanWrapper beanWrapper = new MyBeanWrapper(instance);
			this.factoryBeanInstanceCache.put(beanName, beanWrapper);
			
			// 在实例初始化以后调用一次
			beanPostProcessor.postProcessAfterInitialization(instance, beanName);
			
			populateBean(beanName, instance);
			
			return this.factoryBeanInstanceCache.get(beanName).getWrappedInstance();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 接收一个BeanDefinition，返回一个实例bean
	 * 
	 * @param beanDefinition
	 * @return
	 */
	private Object instantiateBean(MyBeanDefinition beanDefinition) {
		Object instance = null;
		String className = beanDefinition.getBeanClassName();
		if (this.factoryBeanObjectCache.containsKey(className)) {
			instance = this.factoryBeanObjectCache.get(className);
		} else {
			try {
				Class<?> clazz = Class.forName(className);
				instance = clazz.newInstance();
				this.factoryBeanObjectCache.put(beanDefinition.getFactoryBeanName(), instance);
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return instance;
	}
	
	private void populateBean(String beanName, Object instance) {
		Class<?> clazz = instance.getClass();
		if (!(clazz.isAnnotationPresent(MyController.class) || clazz.isAnnotationPresent(MyService.class))) {
			return;
		}
		
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			if (!field.isAnnotationPresent(MyAutowired.class)) { continue; }
			
			MyAutowired autowired = field.getAnnotation(MyAutowired.class);
			String autowiredBeanName = autowired.value().trim();
			if ("".equals(autowiredBeanName)) {
				autowiredBeanName = field.getType().getName();
			}
			
			field.setAccessible(true);
			try {
				MyBeanDefinition autoWiredBeanDefinition = super.beanDefinitionMap.get(autowiredBeanName);
				Object autoWiredBean = instantiateBean(autoWiredBeanDefinition);
				field.set(instance, autoWiredBean);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
	
	public String[] getBeanDefinitionNames() {
		return this.beanDefinitionMap.keySet().toArray(new String[0]);
	}
	
	public int getBeanDefinitionCount() {
		return this.beanDefinitionMap.size();
	}
	
	public Properties getConfig() {
		return this.reader.getConfig();
	}
	
}
