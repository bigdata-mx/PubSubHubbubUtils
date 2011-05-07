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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.io.ByteStreams;

//The Jetty webserver version used for this project is 7.0.1.v20091125
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class PuSHhandler extends AbstractHandler {
  
  private final CallbackServer webserver;

  public PuSHhandler(CallbackServer webserver) {
    this.webserver = webserver;
  }
	
  public void handle(String target, Request baseRequest, 
                     HttpServletRequest request,
                     HttpServletResponse response)
    throws IOException, ServletException {	
    if (request.getMethod().equals("GET")){
      String hubmode = request.getParameter("hub.mode");
      String hubtopic = request.getParameter("hub.topic");
      String hubchallenge = request.getParameter("hub.challenge");
      String hubverify = request.getParameter("hub.verify_token");
      String hublease = request.getParameter("hub.lease_seconds");
      response.setContentType("application/x-www-form-urlencoded");
      if (((hubmode.equals("subscribe")) || (hubmode.equals("unsubscribe")))) {
        if (webserver.containsAction(hubtopic, hubverify)) {
          response.setStatus(HttpServletResponse.SC_OK);
          response.getWriter().print(hubchallenge);
          webserver.notifySubscriptionHandlers(hubmode, hubtopic);
        } else {
          response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
      }		
      baseRequest.setHandled(true);	
    } else if (request.getMethod().equals("POST")) {
      String provided = request.getHeader("X-Hub-Signature");
      InputStream in= request.getInputStream();
      byte[] data = ByteStreams.toByteArray(in);
      try {
        String expected = Signature.calculateHMAC(data, webserver.getKey());
        if (!expected.equals(provided)) {
          System.err.println(String.format("Ignore message %s!=%s", 
                                           expected, provided));
          return;
        }
      } catch (Exception e) {
        System.err.println("Failed to generate HMAC : " + e.getMessage());
        return;
      }
      
      webserver.notifyContentHandlers(data);

      response.setContentType("application/x-www-form-urlencoded");
      response.setStatus(HttpServletResponse.SC_OK);
      baseRequest.setHandled(true);
    }
  }	
}	