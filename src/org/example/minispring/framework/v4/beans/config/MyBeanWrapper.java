package org.example.minispring.framework.v4.beans.config;

/**
 * 用于封装创建后的对象实例，
 * 代理对象（Proxy Object）或者原生对象（Original Object）都由BeanWrapper保存
 */
public class MyBeanWrapper {
	
	private Object wrappedInstance;
	//private Class<?> wrappedClass;
	
	public MyBeanWrapper(Object wrappedInstance) {
		this.wrappedInstance = wrappedInstance;
	}
	
	public Object getWrappedInstance() {
		return this.wrappedInstance;
	}
	
	public Class<?> getWrappedClass() {
		return this.wrappedInstance.getClass();
	}
	
}
