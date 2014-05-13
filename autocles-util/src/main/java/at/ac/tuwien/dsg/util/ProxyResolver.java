package at.ac.tuwien.dsg.util;

/**
 * This class resolves a given URL to a URL which can be retrieved from a proxy.
 * @author hummer@dsg.infosys.tuwien.ac.at
 */
public interface ProxyResolver {
	String getProxyUrl(String url);

	public static class EmptyProxyResolver extends DefaultProxyResolver {
		public EmptyProxyResolver() {
			super("<url>");
		}
	}
	public static class DefaultProxyResolver implements ProxyResolver {
		private String proxyURL;
		public DefaultProxyResolver(String proxyURL) {
			this.proxyURL = proxyURL;
		}
		public String getProxyUrl(String url) {
			return proxyURL.replace("<url>", url);
		}
	}
}