package org.example.minispring.service;

import java.util.Map;

public interface IQueryService {

	public Map<String, Object> get(String name, int age, String phoneNum, String email);
	
}
