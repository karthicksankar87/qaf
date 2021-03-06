/**
 * 
 */
package com.qmetry.qaf.automation.http;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;

import com.qmetry.qaf.automation.testng.pro.DataProviderUtil;
import com.qmetry.qaf.automation.util.StringUtil;

/**
 * @author chirag.jayswal
 *
 */
public class UriProxySelector extends ProxySelector {
	private static final Log logger = LogFactoryImpl.getLog(UriProxySelector.class);
	private static final String PROXY_SERVER_KEY = "proxy.server";
	private static final String PROXY_PORT_KEY = "proxy.port";
	private static final String PROXY_HOSTS_KEY = "host.to.proxy";

	private static final UriProxySelector INSTANCE = new UriProxySelector();
	private ProxySelector defaultSelector = null;
	private List<String> hostsToProxy = null;
	private List<Proxy> proxies = null;

	private UriProxySelector() {
		defaultSelector = ProxySelector.getDefault();
		logger.info("Registed UriProxySelector");
	}

	public static UriProxySelector getInstance() {
		return INSTANCE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.ProxySelector#select(java.net.URI)
	 */
	@Override
	public List<Proxy> select(URI uri) {
		if (null == proxies) {
			// this is the first time when proxy select request happened!...
			intiProxySettings();
		}
		if (null != proxies && !proxies.isEmpty() && null != hostsToProxy) {
			for (String host : hostsToProxy) {
				if (uri.getHost().toUpperCase().contains(host.toUpperCase())) {
					return proxies;
				}
			}
		}
		return defaultSelector.select(uri);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.net.ProxySelector#connectFailed(java.net.URI,
	 * java.net.SocketAddress, java.io.IOException)
	 */
	@Override
	public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
		defaultSelector.connectFailed(uri, sa, ioe);
	}

	@SuppressWarnings("unchecked")
	private void intiProxySettings() {
		try {
			hostsToProxy = getBundle().getList(PROXY_HOSTS_KEY);
			String proxyServer = getBundle().getString(PROXY_SERVER_KEY, "");
			int proxyPort = getBundle().getInt(PROXY_PORT_KEY, 80);
			if (StringUtil.isNotBlank(proxyServer)) {
				SocketAddress sa = new InetSocketAddress(proxyServer, proxyPort);
				proxies = Arrays.asList(new Proxy(Proxy.Type.HTTP, sa), new Proxy(Proxy.Type.SOCKS, sa));
				logger.info("proxy settings done using proxy server " + proxies + " for " + hostsToProxy);
			} else {
				logger.warn(
						"proxy ignored as missing proxyServer. To enable proxy make sure you have non blank value of property '"
								+ PROXY_SERVER_KEY + "'");
				proxies = new ArrayList<Proxy>();
			}
		} catch (Exception e) {
			logger.error("Unable to init proxy settings", e);
		}
	}
}
