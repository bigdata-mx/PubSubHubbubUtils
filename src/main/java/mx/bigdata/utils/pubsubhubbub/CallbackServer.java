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

package mx.bigdata.utils.pubsubhubbub;

import java.net.BindException;
import java.util.Set;

import com.google.common.collect.Sets;

//The Jetty webserver version used for this project is 7.0.1.v20091125
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;

public final class CallbackServer extends Thread {
  
  private static final String CONTEXT_PATH = "/push";
	
  private final Set<String> approvedActions = Sets.newHashSet();

  private ContentHandler contentHandler;

  private SubscriptionHandler subscriptionHandler;

  private final String key;

  private final String serverUrl;

  public CallbackServer(int port, String key, String hostname) 
    throws Exception {
    this(port, key, hostname, CONTEXT_PATH);
  }

  public CallbackServer(int port, String key, String hostname, 
			String path) throws Exception {
    this.key = key;
    this.serverUrl = hostname + ":" + port + path;
    init(port, path);
  }

  private void init(int port, String path) throws Exception {
    Server server = new Server(port);		
    ContextHandler context = new ContextHandler();
    context.setContextPath(path);
    context.setResourceBase(".");
    context.setClassLoader(Thread.currentThread().getContextClassLoader());
    server.setHandler(context); 
    context.setHandler(new PuSHhandler(this));
    server.start();
  }

  public void addContentHandler(ContentHandler contentHandler) {
    this.contentHandler = contentHandler;
  }

  public void addSubscriptionHandler(SubscriptionHandler subscriptionHandler) {
    this.subscriptionHandler = subscriptionHandler;
  }

  public String getKey() {
    return key;
  }

  public boolean containsAction(String hubtopic, String hubverify) {
    String action = hubtopic + ":" + hubverify;
    return approvedActions.remove(action);
  }

  public void addAction(String hubmode, String hubtopic, String hubverify) {
    String action = hubtopic + ":" + hubverify;
    approvedActions.add(action);
  }

  public String getCallbackUrl() {
    return serverUrl;
  }
    
  void notifyContentHandlers(byte[] bytes) {
    contentHandler.handleContent(bytes);   
  }
    
  void notifySubscriptionHandlers(String action, String hubtopic) {
    subscriptionHandler.handleSubscription(action, hubtopic); 
  }
}
