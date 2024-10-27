package org.example.minispring.action;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.example.minispring.framework.annotation.MyAutowired;
import org.example.minispring.framework.annotation.MyController;
import org.example.minispring.framework.annotation.MyRequestMapping;
import org.example.minispring.framework.annotation.MyRequestParam;
import org.example.minispring.service.IDemoService;

@MyController
@MyRequestMapping("/demo")
public class DemoAction {

	@MyAutowired
	private IDemoService demoService;
	
	@MyRequestMapping("/query")
	public void query(HttpServletRequest req, HttpServletResponse resp,
			@MyRequestParam("name") String name) {
		String result = demoService.get(name);
		try {
			resp.getWriter().write("<html><h2>" + result + "</h2></html>");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
