package at.ac.tuwien.dsg.util;

/**
 * This class resolves a given URL to a URL which can be retrieved from a proxy.
 * @author hummer@dsg.infosys.tuwien.ac.at
 */
public interface ProxyResolver {
	String getProxyUrl(String url);
}