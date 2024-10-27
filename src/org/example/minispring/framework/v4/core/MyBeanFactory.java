package org.example.minispring.framework.v4.core;

public interface MyBeanFactory {

	/**
	 * 根据 beanName 从 IoC 容器中获得一个实例 Bean
	 * 
	 * @param beanName
	 * @return
	 * @throws Exception
	 */
	Object getBean(String beanName) throws Exception;
	
	public Object getBean(Class<?> beanClass) throws Exception;
	
}
