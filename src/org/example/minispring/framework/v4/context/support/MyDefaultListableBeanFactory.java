package org.example.minispring.framework.v4.context.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.example.minispring.framework.v4.beans.config.MyBeanDefinition;

public class MyDefaultListableBeanFactory extends MyAbstractApplicationContext {

	// 存储注册信息的BeanDefinition
	protected final Map<String, MyBeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
	
}
