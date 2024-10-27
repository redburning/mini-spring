package org.example.minispring.service.impl;

import org.example.minispring.framework.annotation.MyService;
import org.example.minispring.service.IDemoService;

@MyService
public class DemoService implements IDemoService {

	@Override
	public String get(String name) {
		return "Hello " + name + "!";
	}
}
