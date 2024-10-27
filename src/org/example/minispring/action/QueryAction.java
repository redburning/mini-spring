package org.example.minispring.action;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.example.minispring.framework.annotation.MyAutowired;
import org.example.minispring.framework.annotation.MyController;
import org.example.minispring.framework.annotation.MyRequestMapping;
import org.example.minispring.framework.annotation.MyRequestParam;
import org.example.minispring.framework.v4.webmvc.MyModelAndView;
import org.example.minispring.service.IQueryService;

@MyController
@MyRequestMapping("/query")
public class QueryAction {

	@MyAutowired
	private IQueryService queryService;
	
	@MyRequestMapping("/index.html")
	public MyModelAndView query(HttpServletRequest req, HttpServletResponse resp, @MyRequestParam("name") String name,
			@MyRequestParam("age") int age, @MyRequestParam("phoneNum") String phoneNum,
			@MyRequestParam("email") String email) {
		Map<String, Object> model = queryService.get(name, age, phoneNum, email);
		return new MyModelAndView("index.html", model);
	}

}
