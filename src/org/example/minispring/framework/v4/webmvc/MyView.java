package org.example.minispring.framework.v4.webmvc;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * 模板解析引擎，核心方法是render()。
 * 在render()方法中完成对模板的渲染，最终返回浏览器能识别的字符串，通过response输出。
 */
public class MyView {

	public static final String DEFAULT_CONTENT_TYPE = "text/html;charset=utf-8";
	
	private File viewFile;
	
	public MyView(File viewFile) {
		this.viewFile = viewFile;
	}
	
	public String getContentType() {
		return DEFAULT_CONTENT_TYPE;
	}
	
	public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		StringBuffer sb = new StringBuffer();
		try (RandomAccessFile ra = new RandomAccessFile(this.viewFile, "r")) {
			String line = null;
			while (null != (line = ra.readLine())) {
				line = new String(line.getBytes("ISO-8859-1"), "utf-8");
				Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}", Pattern.CASE_INSENSITIVE);
				Matcher matcher = pattern.matcher(line);
				while (matcher.find()) {
					String paramName = matcher.group(1);
					paramName = paramName.replaceAll("$\\{|\\}", "");
					Object paramValue = model.get(paramName);
					if (null == paramValue) { continue; }
					line = line.replace(matcher.group(0), paramValue.toString());
					matcher = pattern.matcher(line);
				}
				sb.append(line);
			}
			response.setCharacterEncoding("utf-8");
			response.getWriter().write(sb.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
