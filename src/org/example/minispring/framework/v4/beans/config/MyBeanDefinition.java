package org.example.minispring.framework.v4.beans.config;

public class MyBeanDefinition {

	// 原生bean的全类名, 例如 org.example.minispring.action.DemoAction
	private String beanClassName;
	
	// beanName在IoC容器中存储的key, 例如 demoAction
	private String factoryBeanName;
	
	// 是否延迟加载
	private boolean lazyInit = false;
	
	public String getBeanClassName() {
		return beanClassName;
	}
	
	public void setBeanClassName(String beanClassName) {
		this.beanClassName = beanClassName;
	}
	
	public boolean isLazyInit() {
		return lazyInit;
	}
	
	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}
	
	public String getFactoryBeanName() {
		return factoryBeanName;
	}
	
	public void setFactoryBeanName(String factoryBeanName) {
		this.factoryBeanName = factoryBeanName;
	}
	
}
