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

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Enumeration;

import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.io.ByteStreams;

//The Jetty webserver version used for this project is 7.0.1.v20091125
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class PuSHhandler extends AbstractHandler {
  
  private final Logger logger = Logger.getLogger(getClass());

  private final CallbackServer webserver;
  // private final DataOutputStream out;
  private boolean error = false;

  public PuSHhandler(CallbackServer webserver) {
    this.webserver = webserver;
    // DataOutputStream out = null;
    // try {
    //   String fileName = "/var/data/rodrigo/rss-message.log";
    //   File file = new File(fileName);
    //   out = new DataOutputStream
    // 	(new BufferedOutputStream(new FileOutputStream(file, true)));
    // } catch(Exception e) {
    //   e.printStackTrace();
    // }
    // this.out = out;
  }
  
  public void handle(String target, Request baseRequest, 
                     HttpServletRequest request,
                     HttpServletResponse response)
    throws IOException, ServletException {	
    logger.trace("Request Method: " + request.getMethod());
    if (request.getMethod().equals("GET")){
      String hubmode = request.getParameter("hub.mode");
      String hubtopic = request.getParameter("hub.topic");
      String hubchallenge = request.getParameter("hub.challenge");
      String hubverify = request.getParameter("hub.verify_token");
      String hublease = request.getParameter("hub.lease_seconds");
      response.setContentType("application/x-www-form-urlencoded");
      logger.trace("Request hub.mode: " + hubmode);
      logger.trace("Request hub.topic: " + hubtopic);
      logger.trace("Request hub.challenge: " + hubchallenge);
      logger.trace("Request hub.verify: " + hubverify);
      logger.trace("Request hub.lease: " + hublease);
      if (hubmode.equals("subscribe") || hubmode.equals("unsubscribe")) {
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
      //printMessage(request, data);
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

  // private void printMessage(HttpServletRequest req, byte[] data) {
  //   try {
  //     printToLog("#################################################");
  //     Enumeration<String> headers = req.getHeaderNames();
  //     while(headers.hasMoreElements()) {
  // 	String headerName = headers.nextElement();
  // 	printToLog(" > " + headerName + " : " + req.getHeader(headerName));
  //     }
  //     printToLog("-------------------------------------------------");
  //     printToLog(new String(data, "UTF-8"));
  //   } catch (Exception e) {
  //     e.printStackTrace();
  //     error = true;
  //   }
  // }
  
  // private void printToLog(String body) throws Exception {
  //   if (out != null && !error) {
  //     out.writeBytes(body);
  //     out.flush();
  //   }
  //   //System.out.println(body);
  // }
  
  // private void printToLog(byte[] body) throws Exception {
  //   if (out != null && !error) {
  //     out.write(body);
  //     out.flush();
  //   }
  //   //System.out.println(new String(body, "UTF-8"));
  // }
}	
