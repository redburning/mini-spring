package org.example.minispring.framework.v4.beans.config;

public class MyBeanPostProcessor {

	/**
	 * 为在Bean的初始化之前提供回调入口
	 * 
	 * @param bean
	 * @param beanName
	 * @return
	 * @throws Exception
	 */
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws Exception {
		return bean;
	}
	
	/**
	 * 为在Bean的初始化之后提供回调入口
	 * 
	 * @param bean
	 * @param beanName
	 * @return
	 * @throws Exception
	 */
	public Object postProcessAfterInitialization(Object bean, String beanName) throws Exception {
		return bean;
	}
	
}
