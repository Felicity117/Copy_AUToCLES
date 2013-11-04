package at.ac.tuwien.dsg.jsf;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

import at.ac.tuwien.dsg.jopera.JOpera;
import at.ac.tuwien.dsg.jopera.JOpera.Process;
import at.ac.tuwien.infosys.util.Configuration;
import at.ac.tuwien.infosys.util.Util;
import at.ac.tuwien.infosys.util.io.InputOutputStreamBuffer;
import at.ac.tuwien.infosys.util.jsf.DataTableBean;
import at.ac.tuwien.infosys.util.jsf.DataTableBean.DataTableBeanRow;
import at.ac.tuwien.infosys.util.str.StringUtil;
import at.ac.tuwien.infosys.util.str.UrlEncodedQueryString;
import at.ac.tuwien.infosys.ws.EndpointReference;
import at.ac.tuwien.infosys.ws.WebServiceClient;
import at.ac.tuwien.infosys.ws.request.InvocationRequest;
import at.ac.tuwien.infosys.ws.request.InvocationResult;
import at.ac.tuwien.infosys.ws.request.RequestType;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/** 
 * @author Waldemar Hummer
 */
public class UIBean {

	private static String HOST = Configuration.getValue("autocles.webapp.host");

	/* default values and constants */
	private static final Util util = new Util();
	private static final Logger logger = Util.getLogger();
	private static final String JOPERA_HOST = Configuration.getValue("jopera.host");
	private static final String JOPERA_URL = Configuration.getValue("jopera.endpoint.url");
	private static final String DATA_FOLDER = "data/";
	private static final String UPLOAD_FOLDER = DATA_FOLDER + "uploads/";
	private static final String TRACE_URLS_FILE = DATA_FOLDER + "__trace_urls__.txt";
	private static final String DOWNLOAD_URL = "http://" + HOST + "/et/download.jsf?file=<file>";
	private static final String CLOUD_PROXY_URL = Configuration.getValue("cloud.proxy.url");
	private static final String TRACE_GEN_URL = "http://autocles1.us.to:8083/trace?tracespec=<spec>";

	static {
		PropertyConfigurator.configure(UIBean.class.getResource("/log4j.properties"));
	}

	/* JOpera configs */
	private String testName = "test";
	private String joperaUI = "http://" + JOPERA_HOST + "/ui/pages/index.html";
	private String manifestFile = Configuration.getValue("default.jopera.manifestfile");
	private String joperaEndpoint = JOPERA_URL;
	private JOpera jopera = new JOpera(joperaEndpoint, CLOUD_PROXY_URL);
	/* login information */
	private String username = "user1";
	private String password = "pass1";
	private String userLoggedIn = null; /* set to null to enable login! */
	/* test & traces information */
	private String jmeterTestFile = Configuration.getValue("default.jmeter.testfile");
	private String traceURL = Configuration.getValue("default.traces.url");
	private DataTableBean traceSelection;
	private DataTableBean genTraces;
	/* test instance details */
	private Process selectedTest;
	/* Web app related settings */
	private List<String> tabs = Arrays.asList(
			"start", "traces", "tests", "charts", "results", "settings", "cloud", "jopera");
	private Map<String,String> tabNames = util.coll.
			asMap("start", "Start").
			entry("traces", "Traces").
			entry("tests", "Tests").
			entry("charts", "Charts").
			entry("results", "Results").
			entry("settings", "Settings").
			entry("cloud", "Cloud").
			entry("jopera", "JOpera");
	private static String publicIP = Configuration.getPublicIP() != null ? 
			Configuration.getPublicIP() : "localhost";
	private static int webPort = 8888;
	private static String webPath = "/et/";
	/* parameter configurations */
	private String paramValue;
	private String paramKey;
	private Map<String, String> configParameters = util.coll.
			asMap("BASE_EMI", "ami-00000149").
			entry("INSTANCE_TYPE", "m1.medium").
			entry("SECURITY_GROUP", "default");
	/* Cloud configuration */
	private String cloudType;
	private String accessKey;
	private String secretKey;
	private String cloudAdminPage = Configuration.getValue("cloud.admin.url");


	public boolean isAdminLoggedIn() {
		return false; // TODO
	}
	public boolean isLoggedIn() {
		return userLoggedIn != null; // TODO
	}

	public void startTests() throws MalformedURLException, Exception {

		WebServiceClient c = WebServiceClient.getClient(
				new EndpointReference(new URL(JOPERA_URL)));

		String traces = "";
		String separator = " , ";
		for(DataTableBeanRow row : traceSelection.getRows()) {
			if((Boolean)row.getCol2()) {
				traces += row.getCol1() + separator;
			}
		}
		if(traces.endsWith(" , ")) {
			traces = traces.substring(0, traces.length() - separator.length());
		}
		String body = "customer=" + username +
				"&service=" + testName +
				"&testFile=" + util.str.encodeUrl(getDownloadUrl(jmeterTestFile)) +
				"&manifestFile=" + util.str.encodeUrl(getDownloadUrl(manifestFile)) +
				"&traceFile=" + traces +
				"&Action=Run";
		System.out.println("INFO: Sending request to JOpera: " + body);
		InvocationResult res = c.invoke(new InvocationRequest(RequestType.HTTP_POST, body));

		// TODO: parse 
		/* <html>
<body>
The resource can be found at <a href="http://10.99.0.73:8081/memcached/autocles-experiment1327698060878--cb55e97a-a5d9-418d-a9b0-f6621e0cd4fd">http://10.99.0.73:8081/memcached/autocles-experiment1327698060878--cb55e97a-a5d9-418d-a9b0-f6621e0cd4fd</a>
</body>
</html> */

		util.xml.print(res.getResultAsElement());

		/* JOpera response example:
		   <experiment id="exp1368530132416">
			<instance>http://127.0.0.1:8080/rest/ExperimentAutomation/ExeprimentAutomation/1.1/2</instance>
			<client>
			<results>http://127.0.0.1:8181/memcached/exp1368530132416-clientResults</results>
			</client>
			<service>
			<live-feed>http://127.0.0.1:8181/memcached/exp1368530132416-live-feed-service.xml</live-feed>
			<results>http://127.0.0.1:8181/memcached/exp1368530132416-serviceResults</results>
			</service>
			<controller>
			<live-feed>http://127.0.0.1:8181/memcached/exp1368530132416-live-feed-controller.xml</live-feed>
			<results>http://127.0.0.1:8181/memcached/exp1368530132416-controllerResults</results>
			</controller>
			</experiment>
		 */

		// TODO: read IDs of workflows from result

		// TODO: read experiment ID to read data from memcached
		
		// TODO: 

		/* features:
		 * - running 1 trace
		 * - generating traces
		 * - queuing traces
		 * - showing the queue in the GUI
		 * - as soon as results arrive, show results for queue entries
		*/

		addMessage("Test successfully started.");
	}

	public void updateParameter() {
		configParameters.put(paramKey, paramValue.trim());
	}

	public DataTableBean getTestDetails() {
		DataTableBean t = new DataTableBean();
		try {
			for(Process p : jopera.getProcesses()) {
				t.getRows().add(p.getRow());
			}	
		} catch (Exception e) {
			addMessage("Unable to fetch test details from JOpera.", e);
		}
		return t;
	}

	public void addTrace() throws Exception {
		saveNewTrace(traceURL);
		traceSelection = null;
	}

	public DataTableBean getTraces() throws Exception {
		DataTableBean tab = new DataTableBean();
		new File(UPLOAD_FOLDER).mkdirs();
		for(File f : new File(UPLOAD_FOLDER).listFiles()) {
			try {
				String content = util.io.readFile(f);
				tab.getRows().add(new DataTableBeanRow(
						DOWNLOAD_URL.replace("<file>", f.getName()),
						content.split("\n").length));
			} catch (Exception e) {
				System.err.println("Unable to read file: " + f);
				e.printStackTrace();
			}
		}
		if(new File(TRACE_URLS_FILE).exists()) {
			for(String line : util.io.readFile(TRACE_URLS_FILE).split("\n")) {
				try {
					URL u = new URL(line);
					tab.getRows().add(new DataTableBeanRow(u.toString(), "?"));
				} catch (Exception e) {
					/* swallow */
				}
			}
		}
		return tab;
	}

	public void login() throws Exception {
		if(!AuthCheckHandler.checkLogin(username, password)) {
			addMessage("Login unsuccessful. Please try again.");
			FacesContext.getCurrentInstance().getExternalContext()
				.redirect(AuthCheckHandler.redirect);
			return;
		}
		userLoggedIn = username;
		FacesContext.getCurrentInstance().getExternalContext()
			.redirect(AuthCheckHandler.redirect);
	}
	public void logout() throws Exception {
		userLoggedIn = null;
		FacesContext.getCurrentInstance().getExternalContext()
			.redirect("index.jsf");
	}

	public void uploadFile(UploadEvent event) {
		new File(UPLOAD_FOLDER).mkdirs();
		for(UploadItem item : event.getUploadItems()) {
			String file = UPLOAD_FOLDER + item.getFileName();
			try {
				if(item.getData() != null)
					util.io.saveFile(file, item.getData());
				else if(item.getFile() != null)
					util.io.saveFile(file, util.io.readFile(item.getFile()));
				else 
					System.err.println("ERROR: Cannot find upload file content.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		traceSelection = null;
	}

	public String getTracesAsJSON(String fileContents) throws Exception {
		String json = "{\"aaData\":[";
		Map<String,Map<Integer,Double>> dataPoints = new HashMap<String,Map<Integer,Double>>();
		String[] lines = fileContents.split("\n");
		String[] header = lines[0].split(",");
		String[] values = Arrays.asList(lines).subList(1, lines.length).toArray(new String[0]);
		int numClients = header.length - 1;
		for(int j = 1; j <= numClients; j ++) {
			String client = header[j];
			dataPoints.put(client, new HashMap<Integer, Double>());
			for(String entry : values) {
				String[] parts = entry.split(",");
				dataPoints.get(client).put(Integer.parseInt(parts[0]), 
						Double.parseDouble(parts[j]));
			}
		}
		
		for(String client : dataPoints.keySet()) {
			json += "{\"name\":\"" + client + "\", \"data\":[";
			List<Integer> points = new LinkedList<Integer>(dataPoints.get(client).keySet());
			Collections.sort(points);
			for(int i : points) {
				json += "{\"x\":" + i + ",\"y\":" + dataPoints.get(client).get(i) + "},";
			}
			if(json.endsWith(",")) {
				json = json.substring(0, json.length() - 1);
			}
			json += "]},";
		}
		if(json.endsWith(",")) {
			json = json.substring(0, json.length() - 1);
		}
		json += "]}";
		return json;
	}

	public String getFileContents() throws Exception {
		FacesContext fc = FacesContext.getCurrentInstance();
		HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();

		/* Some JSF component library or some Filter might have 
		 * set some headers in the buffer beforehand. We want 
		 * to get rid of them, else it may collide. */
		response.reset(); 

		/* get file parameter from HTTP GET */
		String fileURL = getParam("file");
		final String proxy = getParam("proxy");
		String relativeTo = getParam("relativeTo");
		String toJSON = getParam("toJSON");
		System.out.println("Downloading file " + fileURL + " (relativeTo='" + 
				relativeTo + "', proxy='" + proxy + "')");
		if(util.str.isEmpty(fileURL)) {
			System.out.println("WARN: Empty file URL specified for download.");
			return null;
		}
		if(relativeTo != null) {
			if(util.str.isRelativeURL(fileURL)) {
				fileURL = util.str.concatURLs(relativeTo, fileURL);
			}
		}
		final String originalFileURL = fileURL;
		//System.out.println("Continuing to download file (1) " + fileURL);
		InputStream content = null;
		String localFile = UPLOAD_FOLDER + fileURL;
		boolean typeSet = false;
		if(!util.str.isAbsoluteURL(fileURL) && new File(localFile).exists()) {
			content = new FileInputStream(localFile);
			if(!"true".equals(toJSON)) {
				response.setContentLength((int)new File(localFile).length());
				response.setContentType(((ServletContext)fc.getExternalContext().getContext()).getMimeType(localFile));
				response.setCharacterEncoding("utf-8");
				typeSet = true;
				response.setHeader("Content-Disposition", "attachment;filename=" + fileURL);
			}
		} else {
			if(!util.str.isEmpty(proxy)) {
				fileURL = proxy.replace("<url>", util.str.encodeUrl(fileURL));
			}
			try {
				content = new URL(fileURL).openStream();
			} catch (Exception e) {
				logger.warn("Cannot retrieve contents from URL: " + fileURL);
				return null;
			}
			//response.setHeader("Content-Disposition", "attachment;filename=" + displayName);
		}
		//System.out.println("Continuing to download file (2) " + fileURL);

		if(!util.str.isEmpty(param("attachment"))) {
			int index1 = fileURL.lastIndexOf("/");
			int index2 = fileURL.lastIndexOf("%2F") + 2;
			String displayName = fileURL.substring(Math.max(index1, index2) + 1) + "." + param("attachment");
			response.setHeader("Content-Disposition", "attachment;filename=" + displayName);
			
			/* special case: JTL files */
			// need to inject newlines into the files..
			String contentString = util.io.readFile(content);
			contentString = injectJtlNewlines(contentString);
			content = new ByteArrayInputStream(contentString.getBytes());
		}
		if(!typeSet) {
			response.setCharacterEncoding("utf-8");
		}

		if("true".equals(toJSON)) {
			//System.out.println("Converting trace file to JSON representation: " + fileURL);
			response.setContentType("application/json");
			String contentString = util.io.readFile(content);
			contentString = getTracesAsJSON(contentString);
			response.setContentLength(contentString.length());
			content = new ByteArrayInputStream(contentString.getBytes());
		}

		if("true".equals(param("inject"))) {
			String contentString = util.io.readFile(content);
			for(String key : configParameters.keySet()) {
				String placeholder = "#{{" + key + "}}";
				contentString = contentString.replace(placeholder, configParameters.get(key));
			}
			response.setContentLength(contentString.length());
			content = new ByteArrayInputStream(contentString.getBytes());
		}
		if(!util.str.isEmpty(proxy)) {
			String contentString = util.io.readFile(content);
			System.out.println("Rewriting URLs in downloaded file content...");
			/* do some special content replacements */
			for(final String s : Arrays.asList("src=", "chartsURL=", "ajaxSource: ")) {
				contentString = util.str.replace(contentString, s + "[\"']([^\"']+)[\"']",
					new StringUtil.StringReplacer() {
						public String replace(String toReplace, Map<Integer,String> regexGroups) {
							String url = util.str.concatURLs(originalFileURL, toReplace);
							url = util.str.encodeUrl(url);
							//url = proxy.replace("<url>", url);
							url = getDownloadUrl(url,
									util.coll.asMap(
											"proxy", util.str.encodeUrl(proxy)));
							return s + "\"" + url + "\"";
						}
				}, 1);
			}
			//System.out.println("Setting response length for output: " + contentString.length());
			response.setContentLength(contentString.length());
			content = new ByteArrayInputStream(contentString.getBytes());
		}

		OutputStream output = response.getOutputStream();
		util.io.pipe(content, output);

		/* Important! Otherwise JSF will attempt to render the 
		 * response which obviously will fail since it's 
		 * already written with a file and closed. */
		fc.responseComplete(); 

		return null;
	}

	public void saveSettings() {
		System.out.println("Selected cloud type: " + cloudType);
		try {
			JSch ssh = new JSch();
			Session session = ssh.getSession(getJoperaHost());
			Properties config = new Properties(); 
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();
			Channel channel = session.openChannel("shell");
			InputOutputStreamBuffer buf = new InputOutputStreamBuffer();
			channel.setInputStream(buf.getIS());
			BufferedWriter w = new BufferedWriter(new OutputStreamWriter(buf.getOS()));
			System.out.println("writing command to SSH stream");
			w.write("ls -la");
			System.out.println("getting response from stream");
			String response = util.io.readFile(channel.getInputStream());
			System.out.println("response: " + response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void addMessage(String msg) {
		addMessage(msg, (String)null);
	}
	public static void addMessage(String msg, String details) {
		FacesContext.getCurrentInstance().addMessage("", new FacesMessage(msg, details));
	}
	public static void addMessage(String msg, Exception e) {
		addMessage(msg, util.exc.getStackTrace(e));
	}
	private String getParam(String name) {
		return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(name);
	}
	public String param(String name) {
		return getParam(name);
	}
	public String getDownloadUrl(String file) {
		return getDownloadUrl(file, false);
	}
	public static String getDownloadUrl(String file, boolean proxy) {
		return getDownloadUrl(file, !proxy ? null : 
			util.coll.asMap("proxy", util.str.encodeUrl(CLOUD_PROXY_URL)));
	}
	public static String getDownloadUrl(String file,  
			Map<String,String> additionalParams) {
		String url = "http://" + publicIP + ":" + webPort + 
				webPath + "download.jsf?inject=true";
		if(additionalParams != null) {
			for(String key : additionalParams.keySet()) {
				url += "&" + key + "=" + additionalParams.get(key);
			}
		}
		//if(file.contains("/") || file.contains("?") || file.contains("?")) {
		//	file = util.str.encodeUrl(file);
		//}
		url += "&file=" + file;
		return url;
	}
	public static String getProxyUrl(String url) {
		return getDownloadUrl(url, true);
	}

	public String getTraceFrame(String traceURL) {
		String url = traceURL;
		url = getDownloadUrl(url, util.coll.asMap("toJSON", "true"));
		url = util.str.encodeUrl(url);
		url = "http://" + publicIP + ":" + webPort + webPath + 
				"chart.jsf?title=Trace_Details&json=" + url;
		return url;
	}

	private String injectJtlNewlines(String content) {
		return util.str.replace(content, "[0-9]{4}/[0-9]{2}/[0-9]{2} ", 
			new StringUtil.StringReplacer() {
				public String replace(String toReplace, Map<Integer,String> g) {
					return "\n" + toReplace;
				}
		});
	}
	public DataTableBean getTestResults() {
		String type = param("results");
		String url = "clients".equals(type) ? getSelectedTest().instance(param("inst")).clientResults : null;
		if(util.str.isEmpty(url)) {
			return new DataTableBean();
		}
		DataTableBean result = new DataTableBean();
		String content;
		try {
			System.out.println("Downloading results from: " + url);
			content = util.io.readURL(url);
			// need to inject newlines into the files..
			content = injectJtlNewlines(content);
			int count = 0;
			for(String line : content.split("\n")) {
				if(++count > 1) {
					//System.out.println(line);
					String[] entries = line.split(",");
					if(entries.length > 1) {
						result.addRow(new DataTableBeanRow((Object[])entries));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			addMessage("Unable to download result file from '" + url + "': " + e);
		}
		return result;
	}

	/* GETTERS/SETTERS WITH HELPER FUNCTIONALITY */

	public String getJoperaHost() {
		try {
			return new URL(joperaEndpoint).getHost();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	public DataTableBean getTraceSelection() throws Exception {
		if(traceSelection == null) {
			traceSelection = new DataTableBean();
			for(DataTableBeanRow row : getTraces().getRows()) {
				traceSelection.getRows().add(
						new DataTableBeanRow(row.getCol1(), false));
			}
		}
		return traceSelection;
	}
	public Process getSelectedTest() {
		String pTests = getParam("test");
		if(util.str.isEmpty(pTests)) {
			selectedTest = null;
		} else {
			selectedTest = jopera.getProcess(pTests);
		}
		return selectedTest;
	}
	public String getUrlWithPageParam() {
		String path = FacesContext.getCurrentInstance().getExternalContext().getRequestServletPath();
		path = path.replace(".jsf", "").replace("/", "");
		return getUrl() + path;
	}
	public String getQueryString() {
		String query = ((org.mortbay.jetty.Request)FacesContext.getCurrentInstance().
				getExternalContext().getRequest()).getQueryString();
		if(query == null) {
			query = "";
		}
		if(!query.endsWith("&")) {
			query += "&";
		}
		return query;
	}

	public DataTableBean getConfigParameters() {
		DataTableBean result = new DataTableBean();
		for(String key : configParameters.keySet()) {
			result.getRows().add(new DataTableBeanRow(key, configParameters.get(key)));
		}
		return result;
	}

	public void genTraceAddClient() {
		if(genTraces == null) {
			genTraces = new DataTableBean();
		}
		genTraces.addRow(new DataTableBeanRow("Client " + (genTraces.getRows().size() + 1),
				"sine", "10,0.01,10,0"));
	}
	public void genTraceReset() {
		genTraces = new DataTableBean();
	}
	public void genTraceSave() {
		 String spec = "120:"; // TODO user-input
		 for(int i = 0; i < genTraces.getRows().size(); i ++) {
			 DataTableBeanRow row = genTraces.getRows().get(i);
			 spec += "client" + (i+1) + "," +
			 		row.getCol2() + "," + row.getCol3() + ";";
		 }
		 String url = TRACE_GEN_URL.replace("<spec>", spec);
		 try {
			saveNewTrace(url);
		} catch (Exception e) {
			addMessage("Unable to save trace URLs.", e);
		}
	}
	
	/* UTILITY METHODS */

	private void saveNewTrace(String url) throws Exception {
		String content = "";
		if(new File(TRACE_URLS_FILE).exists()) {
			content = util.io.readFile(TRACE_URLS_FILE);
		}
		boolean exists = false;
		for(DataTableBeanRow r : getTraces().getRows()) {
			if(r.getCol1().toString().trim().equals(url)) {
				addMessage("Trace file URL already exists: " + url);
				exists = true;
			}
		}
		if(!exists) {
			content += "\n" + url;
		}
		util.io.saveFile(TRACE_URLS_FILE, content);
	}

	public boolean isEmpty(String s) {
		return util.str.isEmpty(s);
	}

	public String gsub(String haystack, String needle, String replacement) {
		return haystack.replace(needle, replacement);
	}

	public String url(String p1) {
		return getUrl(new String[]{p1});
	}
	public String url(String p1, String p2) {
		return getUrl(new String[]{p1, p2});
	}
	public String url(String p1, String p2, String p3) {
		return getUrl(new String[]{p1, p2, p3});
	}
	public String getUrl(String ... paramOverrides) {
		String url = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() +
				"/index.jsf";
		if(!url.contains("?")) {
			url += "?";
		}
		String query = getQueryString();
		UrlEncodedQueryString s = UrlEncodedQueryString.parse(query);
		s.remove("t");

		for(String o : paramOverrides) {
			if(o.split("=").length >= 2) {
				int index = o.indexOf("=");
				String key = o.substring(0, index).trim();
				String value = o.substring(index + 1).trim();
				s.set(key, value);
			} else {
				System.out.println("WARN: Unable to parse parameter override: " + o);
			}
		}

		query = s.toString();
		url += query;
		if(!url.endsWith("&")) {
			url += "&";
		}
		return url;
	}
	public String getTabIndex() {
		return "" + Math.max(tabs.indexOf(getParam("t")), 0);
	}
	public DataTableBean getTabs() {
		DataTableBean b = new DataTableBean();
		for(String t : tabs) {
			b.addRow(new DataTableBeanRow(t, tabNames.get(t)));
		}
		return b;
	}

	public String cat(String s1, String s2) {
		return concat(new String[]{s1, s2});
	}
	public String cat(String s1, String s2, String s3) {
		return concat(new String[]{s1, s2, s3});
	}
	public String concat(String ... strings) {
		StringBuilder result = new StringBuilder();
		for(String s : strings) {
			result.append(s);
		}
		return result.toString();
	}
	public String catURLs(String url1, String url2) {
		try {
			return util.str.concatURLs(url1, url2);
		} catch (Exception e) {
			addMessage("WARN: Invalid URL data, please return to the overview page.");
			return null;
		}
	}

	/* GETTERS/SETTERS */

	public DataTableBean getGenTraces() {
		return genTraces;
	}
	public String getCloudType() {
		return cloudType;
	}
	public void setCloudType(String cloudType) {
		this.cloudType = cloudType;
	}
	public String getPublicIP() {
		return publicIP;
	}
	public void setPublicIP(String publicIP) {
		UIBean.publicIP = publicIP;
	}
	public String getAccessKey() {
		return accessKey;
	}
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}
	public String getSecretKey() {
		return secretKey;
	}
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
	public String getParamKey() {
		return paramKey;
	}
	public void setParamKey(String paramKey) {
		this.paramKey = paramKey;
	}
	public String getParamValue() {
		return paramValue;
	}
	public void setParamValue(String paramValue) {
		this.paramValue = paramValue;
	}
	public String getTraceURL() {
		return traceURL;
	}
	public void setTraceURL(String traceURL) {
		this.traceURL = traceURL;
	}
	public String getUserLoggedIn() {
		return userLoggedIn;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getCustomerName() {
		return username;
	}
	public void setCustomerName(String customerName) {
		this.username = customerName;
	}
	public String getJoperaEndpoint() {
		return joperaEndpoint;
	}
	public void setJoperaEndpoint(String joperaEndpoint) {
		this.joperaEndpoint = joperaEndpoint;
		jopera = new JOpera(joperaEndpoint);
	}
	public String getAppName() {
		return testName;
	}
	public void setAppName(String appName) {
		this.testName = appName;
	}
	public String getJmeterTestFile() {
		return jmeterTestFile;
	}
	public void setJmeterTestFile(String jmeterTestFile) {
		this.jmeterTestFile = jmeterTestFile;
	}
	public String getManifestFile() {
		return manifestFile;
	}
	public void setManifestFile(String manifestFile) {
		this.manifestFile = manifestFile;
	}
	public String getCloudAdminPage() {
		return cloudAdminPage;
	}
	public String getJoperaUI() {
		return joperaUI;
	}

}
