/*
 *  Copyright 2010 royans@gmail.com, oscnovo@gmail.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package  mx.bigdata.utils.pubsubhubbub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.BasicHttpContext;

public class Subscriber {

  private final Logger logger = Logger.getLogger(getClass());

  private final DefaultHttpClient httpClient;

  private final CallbackServer webserver;

  public Subscriber(CallbackServer webserver) {		
    this.webserver = webserver;
    SchemeRegistry schemeRegistry = new SchemeRegistry();
    schemeRegistry
      .register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
    schemeRegistry
      .register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));
    //ConnManagerParams.setMaxTotalConnections(params, 200);
    //ConnPerRouteBean connPerRoute = new ConnPerRouteBean(20);
    //connPerRoute.setDefaultMaxPerRoute(50);
    //ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);
    ThreadSafeClientConnManager cm = 
      new ThreadSafeClientConnManager(schemeRegistry);
    cm.setMaxTotal(200);
    cm.setDefaultMaxPerRoute(50);
    httpClient = new DefaultHttpClient(cm, new BasicHttpParams());
    httpClient.setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {
        public long getKeepAliveDuration(HttpResponse response,
                                         HttpContext context) {
          HeaderElementIterator it = new BasicHeaderElementIterator
	    (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
          while (it.hasNext()) {
            HeaderElement he = it.nextElement();
            String param = he.getName();
            String value = he.getValue();
            if (value != null && param.equalsIgnoreCase("timeout")) {
              try {
                return Long.parseLong(value) * 1000;
              } catch (NumberFormatException ignore) {
              }
            }
          }
          return 30 * 1000;
        }
      });
  }

  /*
   * @throws IOException If an input or output exception occurred
   * 
   * @param The Hub address you want to publish it to
   * 
   * @param The topic_url you want to publish
   * 
   * @return HTTP Response code. 200 is ok. Anything else smells like trouble
   */
  public int subscribe(String hub, String topic_url,
		       String uname, String passwd) throws Exception {
    return executeAction("subscribe", hub, topic_url, uname, passwd);
  }

  /*
   * @throws IOException If an input or output exception occurred
   * 
   * @param The Hub address you want to unpublish it to
   * 
   * @param The topic_url you want to unpublish
   * 
   * @return HTTP Response code. 200 is ok. Anything else smells like trouble
   */
  public int unsubscribe(String hub, String topic_url,
                         String uname, String passwd) throws Exception {
    return executeAction("unsubscribe", hub, topic_url, uname, passwd);
  }

  public int executeAction(String action, String hub, String topic_url,
			   String uname, String passwd) throws Exception {
    String callback = webserver.getCallbackUrl();
    String vtoken = UUID.randomUUID().toString();
    logger.debug("hub.callback: " + callback);
    logger.debug("hub.mode: " + action);
    logger.debug("hub.topic: " + topic_url);
    logger.trace("hub.secret: " + webserver.getKey());
    logger.trace("hub.verify: " + "sync");
    logger.trace("hub.verify_token: " + vtoken);
    List<NameValuePair> nvps = new ArrayList<NameValuePair>();
    nvps.add(new BasicNameValuePair("hub.callback", callback));
    nvps.add(new BasicNameValuePair("hub.mode", action));
    nvps.add(new BasicNameValuePair("hub.topic", topic_url));
    nvps.add(new BasicNameValuePair("hub.secret", webserver.getKey()));
    nvps.add(new BasicNameValuePair("hub.verify", "sync"));
    // nvps.add(new BasicNameValuePair("hub.verify", "async"));
    nvps.add(new BasicNameValuePair("hub.verify_token", vtoken));
    webserver.addAction(action, topic_url, vtoken);
    if (logger.isTraceEnabled()) {
      logger.trace("postBody: " + URLEncodedUtils.format(nvps, "UTF-8"));
    }
    HttpPost httppost = new HttpPost(hub);
    httppost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
    httppost.setHeader("Content-type", 
		       "application/x-www-form-urlencoded; charset=UTF-8");
    httppost.setHeader("User-agent", "RSS pubsubhubbub 0.3");
    httppost.setHeader("Accept", "application/json");
    httppost.setHeader("Accept-Charset", "utf-8");

    UsernamePasswordCredentials credentials = 
      new UsernamePasswordCredentials(uname, passwd);
    BasicScheme scheme = new BasicScheme();
    Header authorizationHeader = scheme.authenticate(credentials,httppost);
    httppost.addHeader(authorizationHeader);
    HttpResponse httpresponse = execute(httpClient, httppost);
    return httpresponse.getStatusLine().getStatusCode();
  }

  private HttpResponse execute(HttpClient httpClient, HttpPost httppost) 
    throws IOException {
    HttpResponse httpresponse = 
      httpClient.execute(httppost, new BasicHttpContext());
    HttpEntity entity = httpresponse.getEntity();
    if (entity != null) {
      entity.consumeContent();
    }
    return httpresponse;
  }
}
