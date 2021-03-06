// Copyright 2015 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.trace.samples.gae.hellotrace;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.cloud.trace.sdk.CloudTraceException;
import com.google.cloud.trace.sdk.ThreadTraceContextTraceSpanDataListener;
import com.google.cloud.trace.sdk.TraceSpanData;
import com.google.cloud.trace.sdk.gae.AppEngineTraceSpanDataBuilderFactory;
import com.google.cloud.trace.sdk.gae.AppEngineTraceSpanDataListener;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A sample application that creates and writes a custom span.
 */
public class HelloTrace extends HttpServlet {
  private static final Logger logger = Logger.getLogger(HelloTrace.class.getName());

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws IOException {
    // Creates a custom span "/WhoAmI".
    TraceSpanData.setListener(new AppEngineTraceSpanDataListener(
        new ThreadTraceContextTraceSpanDataListener()));
    TraceSpanData span =
        new TraceSpanData(AppEngineTraceSpanDataBuilderFactory.getBuilder("/WhoAmI"));
    span.start();

    UserService userService = UserServiceFactory.getUserService();
    String who = "Trace";
    if (userService.isUserLoggedIn()) {
      User user = userService.getCurrentUser();
      who = user.getNickname();
    }
    String loginURL = userService.createLoginURL("/");
    String logoutURL = userService.createLogoutURL("/");
    span.end();

    res.setContentType("text/html");
    res.getWriter().println("<html>");
    res.getWriter().println(" <head>");
    res.getWriter().println("  <title>Hello Trace</title>");
    res.getWriter().println(" </head>");
    res.getWriter().println(" <body>");
    res.getWriter().println("  <h1>Hello " + who + "! Your first custom span: </h1>");
    res.getWriter().println("  <p>" + span + "</p>");
    res.getWriter().println("  <p><a href=\"" + loginURL + "\">login</a></p>");
    res.getWriter().println("  <p><a href=\"" + logoutURL + "\">logout</a></p>");
    res.getWriter().println(" </body>");
    res.getWriter().println("</html>");

    // Writes the span through Cloud Trace API.
    try {
      TraceWriterSingleton.getInstance().writeSpan(span);
    } catch (CloudTraceException e) {
      logger.log(Level.SEVERE, "Failed to write span: ", e);
    }
  }
}

