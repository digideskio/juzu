/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package juzu.test.protocol.http;

import juzu.impl.plugin.application.Application;
import juzu.impl.plugin.application.ApplicationLifeCycle;
import juzu.impl.asset.AssetManager;
import juzu.impl.asset.AssetServer;
import juzu.impl.common.MethodHandle;
import juzu.impl.request.Method;
import juzu.request.Phase;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class HttpServletImpl extends HttpServlet {


  /** . */
  private ApplicationLifeCycle<?, ?> application;

  /** . */
  private AssetServer assetServer;

  /** . */
  AssetManager scriptManager;

  /** . */
  AssetManager stylesheetManager;

  private RequestBridgeImpl create(HttpServletRequest req, HttpServletResponse resp) {
    Phase phase = Phase.VIEW;
    Map<String, String[]> parameters = new HashMap<String, String[]>();

    //
    String methodId = null;
    for (Map.Entry<String, String[]> entry : ((Map<String, String[]>)req.getParameterMap()).entrySet()) {
      String name = entry.getKey();
      String[] value = entry.getValue();
      if (name.equals("juzu.phase")) {
        phase = Phase.valueOf(value[0]);
      }
      else if (name.equals("juzu.op")) {
        methodId = value[0];
      }
      else {
        parameters.put(name, value);
      }
    }

    //
    MethodHandle method = methodId != null ? application.getDescriptor().getControllers().getMethodById(methodId).getHandle() : null;

    //
    if (method == null) {
      Method descriptor = application.getApplication().getDescriptor().getControllers().getResolver().resolve(Phase.VIEW, Collections.<String>emptySet());
      method = descriptor != null ? descriptor.getHandle() : null;
    }

    //
    if (phase == Phase.ACTION) {
      return new ActionBridgeImpl(application.getApplication(), req, resp, method, parameters);
    } else if (phase == Phase.VIEW) {
      return new RenderBridgeImpl(this, application.getApplication(), req, resp, method, parameters);
    } else if (phase == Phase.RESOURCE) {
      return new ResourceBridgeImpl(application.getApplication(), req, resp, method, parameters);
    } else {
      throw new AssertionError();
    }
  }

  @Override
  public void init() throws ServletException {
    try {
      ApplicationLifeCycle<?, ?> application = AbstractHttpTestCase.getCurrentApplication();

      //
      application.refresh();

      // Bind the asset managers
      assetServer = new AssetServer();
      scriptManager = application.getScriptManager();
      stylesheetManager = application.getStylesheetManager();

      // Manually register here : a bit ugly
      assetServer.register(application);

      //
      this.application = application;
    }
    catch (Exception e) {
      throw new ServletException(e);
    }
  }

  @Override
  public void destroy() {
    assetServer.unregister(application);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String path = req.getRequestURI().substring(req.getContextPath().length());
    String contentType;
    if (path.endsWith(".js")) {
      contentType = "text/javascript";
    }
    else if (path.endsWith(".css")) {
      contentType = "text/css";
    }
    else {
      contentType = null;
    }
    if (contentType != null) {
      if (!assetServer.doGet(path, getServletContext(), resp)) {
        resp.sendError(404, "Path " + path + " could not be resolved");
      }
    }
    else {
      RequestBridgeImpl requestBridge = create(req, resp);
      try {
        Application context = application.getApplication();
        context.invoke(requestBridge);
      }
      finally {
        requestBridge.close();
      }
    }
  }
}
