package at.ac.tuwien.dsg.jopera;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.jopera.kernel.State;
import org.w3c.dom.Element;

import at.ac.tuwien.dsg.test.TestRequest.TestRequests;
import at.ac.tuwien.dsg.util.ProxyResolver;
import at.ac.tuwien.infosys.aggr.xml.XPathProcessor;
import at.ac.tuwien.infosys.util.Configuration;
import at.ac.tuwien.infosys.util.Util;
import at.ac.tuwien.infosys.util.jsf.DataTableBean.DataTableBeanRow;
import at.ac.tuwien.infosys.ws.EndpointReference;
import at.ac.tuwien.infosys.ws.WebServiceClient;
import at.ac.tuwien.infosys.ws.request.InvocationRequest;
import at.ac.tuwien.infosys.ws.request.RequestType;

public class JOpera {

	private static final Util util = new Util();

	private String endpointURL = Configuration.getValue("jopera.endpoint.url");
	private final AtomicLong lastUpdateTime = new AtomicLong();
	private static final long MAX_UPDATE_INTERVAL_MS = 10*1000;
	private LinkedList<Process> cachedProcesses;
	private ProxyResolver proxyResolver = new ProxyResolver.EmptyProxyResolver();
	private static final Logger logger = Util.getLogger();

	public static class Process {
		public String id;
		public String name;
		public final List<Instance> instances = new LinkedList<Instance>();
		public List<DataTableBeanRow> getRows() {
			List<DataTableBeanRow> rows = new LinkedList<DataTableBeanRow>();
			for(Instance i : instances) {
				rows.add(i.getRow());
			}
			return rows;
		}
		public DataTableBeanRow getRow() {
			return new DataTableBeanRow(id, name, getNumInstances());
		}
		public Instance instance(String name) {
			for(Instance i : instances) {
				if(i.name.equals(name))
					return i;
			}
			return null;
		}
		public int getNumInstances() {
			return instances.size();
		}
		public String getLiveFeed() {
			return instances.get(instances.size() - 1).getLiveFeed();
		}
	}
	public static class Instance {
		public String name;
		public final List<Task> tasks = new LinkedList<Task>();
		public String serviceFeed;
		public String controllerFeed;
		public String clientResults;
		public String serviceResults;
		public String controllerResults;
		public TestRequests requests;

		public List<DataTableBeanRow> getRows() {
			List<DataTableBeanRow> rows = new LinkedList<DataTableBeanRow>();
			for(Task t : tasks) {
				rows.add(t.getRow());
			}
			return rows;
		}
		public String getLiveFeed() {
			try {
				String pl = task("RespondToClient").
						box("SystemInput").param("Payload");
				return XPathProcessor.evaluate("//live-feed/text()", util.xml.toElement(pl));
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		public DataTableBeanRow getRow() {
			return new DataTableBeanRow(name, 
					getNumTasks(), serviceFeed, controllerFeed,
					clientResults, serviceResults, controllerResults,
					requests == null ? "-" : requests.requests.size(),
					requests == null ? "-" : util.str.format(requests.getSuccessRatio() * 100.0, 2) + "%");
		}
		public Task task(String name) {
			for(Task t : tasks) {
				if(t.name.equals(name))
					return t;
			}
			return null;
		}
		public int getNumTasks() {
			return tasks.size();
		}
	}
	public static class Task {
		public String name;
		public final List<Box> boxes = new LinkedList<Box>();
		public List<DataTableBeanRow> getRows() {
			List<DataTableBeanRow> rows = new LinkedList<DataTableBeanRow>();
			for(Box b : boxes) {
				rows.add(b.getRow());
			}
			return rows;
		}
		public DataTableBeanRow getRow() {
			String state = box("System").param("STATE");
			for(Field f : State.class.getFields()) {
				if(Modifier.isStatic(f.getModifiers())) {
					try {
						Object o = f.get(State.class);
						if(o instanceof State) {
							if(o.toString().equals(state)) {
								state = ((State)o).toUserString();
								break;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			return new DataTableBeanRow(name, getNumBoxes(), state);
		}
		public Box box(String name) {
			for(Box b : boxes) {
				if(b.name.equals(name))
					return b;
			}
			return null;
		}
		public int getNumBoxes() {
			return boxes.size();
		}
	}
	public static class Box {
		public String name;
		public final Map<String,String> params = new HashMap<String, String>();
		public DataTableBeanRow getRow() {
			return new DataTableBeanRow(name, getNumParams());
		}
		public String param(String key) {
			return params.get(key);
		}
		public int getNumParams() {
			return params.size();
		}
	}

	public JOpera(String endpointURL) {
		this(endpointURL, (String)null);
	}
	public JOpera(String endpointURL, String proxyUrl) {
		this(endpointURL, proxyUrl == null ? 
				new ProxyResolver.EmptyProxyResolver() :
				new ProxyResolver.DefaultProxyResolver(proxyUrl));
	}
	public JOpera(String endpointURL, ProxyResolver proxy) {
		while(endpointURL.endsWith("/")) {
			endpointURL = endpointURL.substring(0, endpointURL.length() - 1);
		}
		this.endpointURL = endpointURL;
		this.proxyResolver = proxy;
	}

	public synchronized List<Process> getProcesses() {
		if((System.currentTimeMillis() - lastUpdateTime.get()) <= MAX_UPDATE_INTERVAL_MS) {
			return cachedProcesses;
		}
		String path = endpointURL + "/*/?_Accept=application/xml";
		try {
			//System.out.println("Getting JOpera processes...");
			Element xml = getClient(path).invoke(
					new InvocationRequest(RequestType.HTTP_GET, null))
					.getResultAsElement();
			cachedProcesses = new LinkedList<Process>();
			int index = 0;
			for(Object o : xpath("//process", xml)) {
				Process p = new Process();
				p.id = "" + ++index;
				p.name = ((Element)o).getAttribute("name");
				for(Object o1 : xpath("instance", (Element)o)) {
					Instance i = new Instance();
					i.name = ((Element)o1).getAttribute("name");
					p.instances.add(i);
					for(Object o2 : xpath("task", (Element)o1)) {
						Task t = new Task();
						t.name = ((Element)o2).getAttribute("name");
						i.tasks.add(t);
						for(Object o3 : xpath("box", (Element)o2)) {
							Box b = new Box();
							b.name = ((Element)o3).getAttribute("name");
							t.boxes.add(b);
							for(Object o4 : xpath("param", (Element)o3)) {
								String key = ((Element)o4).getAttribute("name");
								String value = ((Element)o4).getTextContent();
								b.params.put(key, value);
							}
						}
					}
					try {

						Task t = i.task("StoreUserAnwserInMemcached");
						if(t == null) continue;
						Box b = t.box("SystemInput");
						if(b == null || b.param("body") == null) continue;

						Element statusPayload = util.xml.toElement(b.param("body"));
						i.serviceFeed = XPathProcessor.evaluate("descendant::service/live-feed/text()", statusPayload);
						i.controllerFeed = XPathProcessor.evaluate("descendant::controller/live-feed/text()", statusPayload);
						i.clientResults = XPathProcessor.evaluate("descendant::client/results/text()", statusPayload);
						i.serviceResults = XPathProcessor.evaluate("descendant::service/results/text()", statusPayload);
						i.controllerResults = XPathProcessor.evaluate("descendant::controller/results/text()", statusPayload);
						// convert to proxy URLs
						i.serviceFeed = proxyResolver.getProxyUrl(i.serviceFeed);
						i.controllerFeed = proxyResolver.getProxyUrl(i.controllerFeed);
						i.clientResults = proxyResolver.getProxyUrl(i.clientResults);
						i.serviceResults = proxyResolver.getProxyUrl(i.serviceResults);
						i.controllerResults = proxyResolver.getProxyUrl(i.controllerResults);
						System.out.println(" > " + i.serviceFeed);
						System.out.println(" > " + i.controllerFeed);
						// retrieve XML documents
						i.serviceFeed = util.io.readFile(i.serviceFeed);
						i.controllerFeed = util.io.readFile(i.controllerFeed);
						//System.out.println("i.clientResults size: " + util.net.getDownloadSize(UIBean.getProxyUrl(i.clientResults)));
						//System.out.println("i.serviceResults size: " + util.net.getDownloadSize(UIBean.getProxyUrl(i.serviceResults)));
						//System.out.println("i.controllerResults size: " + util.net.getDownloadSize(UIBean.getProxyUrl(i.controllerResults)));
						//System.out.println(" -> " + i.serviceFeed);
						//System.out.println(" -> " + i.controllerFeed);
						/* parse host and port from XML */
						if(!util.str.isEmpty(i.serviceFeed)) {
							i.serviceFeed = String.format("http://%s:%s/chartForm", 
								XPathProcessor.evaluate("//ip/text()", util.xml.toElement(i.serviceFeed)),
								XPathProcessor.evaluate("//port/text()", util.xml.toElement(i.serviceFeed)));
						}
						/* parse host and port from XML */
						if(!util.str.isEmpty(i.controllerFeed)) {
							i.controllerFeed = String.format("http://%s:%s/chartForm", 
								XPathProcessor.evaluate("//ip/text()", util.xml.toElement(i.controllerFeed)),
								XPathProcessor.evaluate("//port/text()", util.xml.toElement(i.controllerFeed)));
						}
						/* parse client request results */
						if(!util.str.isEmpty(i.clientResults)) {
							i.requests = TestRequests.parse(i.clientResults);
						}
					} catch (Exception e) {
						logger.warn("Cannot get details of test instance '" + i.name + "'");
					}
				}
				cachedProcesses.add(p);
			}
			lastUpdateTime.set(System.currentTimeMillis());
			if(cachedProcesses.isEmpty()) {
				logger.warn("Found 0 processes in file: " + path);
			}
			return cachedProcesses;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Process getProcess(String id) {
		if(id != null) {
			for(Process p : getProcesses()) {
				if(id.equals(p.id))
					return p;
			}
		}
		return null;
	}
	
	/* HELPER METHODS */

	public static JOpera get(String url) {
		return new JOpera(url);
	}

	private static <T> List<T> xpath(String path, Object o) throws Exception {
		return XPathProcessor.evaluateAsList(path, (Element)o);
	}

	private WebServiceClient getClient(String url) {
		try {
			return WebServiceClient.getClient(new EndpointReference(new URL(url)));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/* getters/setters */

	public ProxyResolver getProxyResolver() {
		return proxyResolver;
	}
	public void setProxyResolver(ProxyResolver proxyResolver) {
		this.proxyResolver = proxyResolver;
	}
}
