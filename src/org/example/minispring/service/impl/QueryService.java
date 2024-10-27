package org.example.minispring.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.example.minispring.framework.annotation.MyService;
import org.example.minispring.service.IQueryService;

@MyService
public class QueryService implements IQueryService {

	@Override
	public Map<String, Object> get(String name, int age, String phoneNum, String email) {
		Map<String, Object> result = new HashMap<>();
		result.put("name", name);
		result.put("age", age);
		result.put("phoneNum", phoneNum);
		result.put("email", email);
		return result;
	}

}
