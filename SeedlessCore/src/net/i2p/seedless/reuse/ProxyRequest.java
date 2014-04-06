/**
 *            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *                    Version 2, December 2004
 *
 * Copyright (C) sponge
 *   Planet Earth
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this license document, and changing it is allowed as long
 * as the name is changed.
 *
 *            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 *  0. You just DO WHAT THE FUCK YOU WANT TO.
 *
 * See...
 *
 *	http://sam.zoy.org/wtfpl/
 *	and
 *	http://en.wikipedia.org/wiki/WTFPL
 *
 * ...for any additional details and license questions.
 */
package net.i2p.seedless.reuse;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
/*
import java.util.Iterator;
import java.util.List;
import net.i2p.I2PAppContext;
import net.i2p.router.RouterContext;
*/
/**
 *
 * @author sponge
 */
public class ProxyRequest {

    private URLConnection c = null;
    private HttpURLConnection h = null;

    /**
     * Instance
     */
    public ProxyRequest() {
    }

    /**
     * This function makes an HTTP GET request of the specified URL using a proxy if provided.
     * If successful, the HTTPURLConnection is returned.
     *
     * @param strURL A string representing the URL to request, eg, "http://sponge.i2p/"
     * @param header the X-Seedless: header to add to the request.
     * @param strProxy A string representing either the IP address or host name of the proxy server.
     * @param iProxyPort  An integer that indicates the proxy port or -1 to indicate the default port for the protocol.
     * @return HTTPURLConnection or null
     */
    public HttpURLConnection doURLRequest(String strURL, String header, String strProxy, int iProxyPort) throws InterruptedException {
        boolean retry = false;
        int tix = 0;

        do {
            tix++;
            retry = false;
            try {
                URL url = null;
                // System.out.println("HTTP Request: " + strURL);
                URL urlOriginal = new URL(strURL);
                if((null != strProxy) && (0 < strProxy.length())) {
                    URL urlProxy = new URL(urlOriginal.getProtocol(), strProxy, iProxyPort, strURL); // The original URL is passed as "the file on the host".
                    //System.out.println("Using Proxy: " + strProxy);
                    //if(-1 != iProxyPort) {
                    //    System.out.println("Using Proxy Port: " + iProxyPort);
                    //}
                    url = urlProxy;
                } else {
                    return null;
                }
                c = url.openConnection();
                c.setUseCaches(false);
                c.setConnectTimeout(1000 * 50); // Eepproxy will time out in 1 minute for connects, we need to beat this
                c.setReadTimeout(1000 * 300); // Eeepproxy will read timeout after ~5minutes if the router is new enough...
                if(header != null) {
                    c.setRequestProperty("X-Seedless", header);
                }
                if(c instanceof HttpURLConnection) {
                    // instanceof returns true only if the object is not null.
                    h = (HttpURLConnection)c;
                    h.connect();
                    return h;
                }
                return null;
            } catch(MalformedURLException exc) {
                return null;
            } catch(ConnectException exc) {
                try {
                    /* Is this right? or no?
                    List<RouterContext> x = net.i2p.router.RouterContext.listContexts();
                    Iterator<RouterContext> it = x.iterator();
                    while (it.hasNext()) {
                        RouterContext ctx=it.next();
                        if(ctx.isRouterContext()) {
            				if(ctx.router().isFinalShutdownInProgress()) {
                                return null;
                            }
                        }
                    }
                     */
                    System.out.println("Proxy not quite ready yet, will retry in 60 seconds.");
                    Thread.sleep(60000);
                    retry = true;
                } catch(InterruptedException ex) {
                    /*
                     * we hope this works instead of checking the context above.
                     * If this works we do not need the above stuff.
                     */
                    throw new InterruptedException(ex.getMessage());
                }
            } catch(InterruptedIOException ioe) {
                    throw new InterruptedException(ioe.getMessage());
            } catch(IOException exc) {
                System.out.println(exc.toString());
            }
        } while(retry && tix < 10);
        return null;
    }
}

