package at.ac.tuwien.dsg.test;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;

import at.ac.tuwien.infosys.aggr.xml.XPathProcessor;
import at.ac.tuwien.infosys.util.Util;

/**
 * This class represents the test execution result of
 * a single test client request, e.g., executed via JMeter.
 * 
 * @author hummer
 */
public class TestRequest {

	private static final Util util = new Util();

	private long timestamp;
	private boolean success;
	private Object request;
	private Object result;

	public static class TestRequests {
		public final List<TestRequest> requests = new LinkedList<TestRequest>();

		public double getSuccessRatio() {
			return ((double)getNumSuccessful()) / ((double)requests.size());
		}
		public int getNumSuccessful() {
			int i = 0;
			for(TestRequest r : requests) {
				if(r.success)
					i ++;
			}
			return i;
		}

		public static TestRequests parse(String url) throws Exception {
			String content = util.io.readURL(url).trim();
			if(content.startsWith("<")) {
				Element contentEl = util.xml.toElement(content);
				return parse(contentEl);
			}
			return new TestRequests();
		}
		public static TestRequests parse(Element e) throws Exception {
			TestRequests result = new TestRequests();
			for(Element c : util.xml.getChildElements(e)) {
				TestRequest exe = TestRequest.parse(c);
				if(exe != null) {
					result.requests.add(exe);
				}
			}
			return result;
		}
	}

	public static TestRequest parse(Element e) throws Exception {
		List<String> urls = XPathProcessor.evaluateAsList("java.net.URL/text()", e);
		List<String> rcs = XPathProcessor.evaluateAsList("@rc", e);
		if(!urls.isEmpty()) {
			TestRequest exe = new TestRequest();
			exe.success = e.getAttribute("s").equals("true");
			exe.request = urls.get(0);
			exe.result = rcs.get(0);
			return exe;
		}
		return null;
	}
	
	public Object getRequest() {
		return request;
	}
	public void setRequest(Object request) {
		this.request = request;
	}
	public Object getResult() {
		return result;
	}
	public void setResult(Object result) {
		this.result = result;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public boolean isSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}
}
