/*
 * Copyright 2013 eXo Platform SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package juzu.impl.request;

import juzu.Response;
import juzu.Scope;
import juzu.impl.bridge.spi.DispatchBridge;
import juzu.impl.bridge.spi.EventBridge;
import juzu.impl.inject.Scoped;
import juzu.impl.inject.ScopingContext;
import juzu.impl.inject.spi.BeanLifeCycle;
import juzu.impl.inject.spi.InjectionContext;
import juzu.impl.bridge.spi.ActionBridge;
import juzu.impl.bridge.spi.RenderBridge;
import juzu.impl.bridge.spi.RequestBridge;
import juzu.impl.bridge.spi.ResourceBridge;
import juzu.impl.plugin.controller.ControllerPlugin;
import juzu.impl.plugin.controller.descriptor.ControllersDescriptor;
import juzu.request.ActionContext;
import juzu.request.Dispatch;
import juzu.request.EventContext;
import juzu.request.Phase;
import juzu.request.RenderContext;
import juzu.request.RequestContext;
import juzu.request.RequestParameter;
import juzu.request.ResourceContext;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class Request implements ScopingContext {

  public static Request getCurrent() {
    return current.get();
  }

  /** . */
  private static final ThreadLocal<Request> current = new ThreadLocal<Request>();

  /** . */
  private final ControllerPlugin plugin;

  /** . */
  private final RequestBridge bridge;

  /** . */
  private final RequestContext context;

  /** . */
  private final Map<String, RequestParameter> parameters;

  /** . */
  private final Map<ControlParameter, Object> arguments;

  /** The response. */
  private Response response;

  public Request(
    ControllerPlugin plugin,
//    Application application,
    Method method,
    Map<String, RequestParameter> parameters,
    RequestBridge bridge) {
    RequestContext context;

    // Make a copy of the original arguments provided by the bridge
    Map<ControlParameter, Object> arguments = new HashMap<ControlParameter, Object>(bridge.getArguments());

    //
    if (bridge instanceof RenderBridge) {
      context = new RenderContext(this, method, (RenderBridge)bridge);
    }
    else if (bridge instanceof ActionBridge) {
      context = new ActionContext(this, method, (ActionBridge)bridge);
    }
    else if (bridge instanceof EventBridge) {
      context = new EventContext(this, method, (EventBridge)bridge);
    }
    else {
      context = new ResourceContext(this, method, (ResourceBridge)bridge);
    }

    //
    this.context = context;
    this.bridge = bridge;
    this.parameters = parameters;
    this.arguments = arguments;
    this.plugin = plugin;
  }

  public RequestBridge getBridge() {
    return bridge;
  }

  public Response getResponse() {
    return response;
  }

  public void setResponse(Response response) {
    this.response = response;
  }

  public Map<String, RequestParameter> getParameters() {
    return parameters;
  }

  public RequestContext getContext() {
    return context;
  }

  public Map<ControlParameter, Object> getArguments() {
    return arguments;
  }

  public void setArguments(Map<ControlParameter, Object> arguments) {
    this.arguments.clear();
    this.arguments.putAll(arguments);
  }

  public void setArgument(ControlParameter parameter, Object value) {
    this.arguments.put(parameter, value);
  }

  public final Scoped getContextualValue(Scope scope, Object key) {
    switch (scope) {
      case FLASH:
        return bridge.getFlashValue(key);
      case REQUEST:
        return bridge.getRequestValue(key);
      case SESSION:
        return bridge.getSessionValue(key);
      case IDENTITY:
        return bridge.getIdentityValue(key);
      default:
        throw new AssertionError();
    }
  }

  public final void setContextualValue(Scope scope, Object key, Scoped value) {
    switch (scope) {
      case FLASH:
        bridge.setFlashValue(key, value);
        break;
      case REQUEST:
        bridge.setRequestValue(key, value);
        break;
      case SESSION:
        bridge.setSessionValue(key, value);
        break;
      case IDENTITY:
        bridge.setIdentityValue(key, value);
        break;
      default:
        throw new AssertionError();
    }
  }

  public boolean isActive(Scope scope) {
    switch (scope) {
      case IDENTITY:
        return false;
      default:
        return true;
    }
  }

  /** . */
  private int index = 0;

  public void invoke() {
    boolean set = current.get() == null;
    try {
      if (set) {
        current.set(this);
      }

      //
      List<RequestFilter> filters = plugin.getFilters();

      //
      if (index >= 0 && index < filters.size()) {
        RequestFilter plugin = filters.get(index);
        try {
          index++;
          plugin.invoke(this);
        }
        finally {
          index--;
        }
      }
      else if (index == filters.size()) {

        // Get arguments
        Method<?> method = context.getMethod();
        Object[] args = new Object[method.getParameters().size()];
        for (int i = 0;i < args.length;i++) {
          ControlParameter parameter = method.getParameters().get(i);
          args[i] = arguments.get(parameter);
        }

        // Invoke
        doInvoke(this, args, plugin.getInjectionContext());
      }
      else {
        throw new AssertionError();
      }
    }
    finally {
      if (set) {
        current.set(null);
      }
    }
  }

  private static <B, I> void doInvoke(Request request, Object[] args, InjectionContext<B, I> manager) {
    RequestContext context = request.getContext();
    Class<?> type = context.getMethod().getType();

    BeanLifeCycle lifeCycle = manager.get(type);

    if (lifeCycle != null) {
      try {

        // Get controller
        Object controller;
        try {
          controller = lifeCycle.get();
        }
        catch (InvocationTargetException e) {
          request.response = Response.error(e.getCause());
          controller = null;
        }

        //
        if (controller != null) {

          // Begin request callback
          if (controller instanceof juzu.request.RequestLifeCycle) {
            try {
              ((juzu.request.RequestLifeCycle)controller).beginRequest(context);
            }
            catch (Exception e) {
              request.response = new Response.Error(e);
            }
          }

          // If we have no response yet
          if (request.getResponse() == null) {
            // We invoke method on controller
            try {
              Object ret = context.getMethod().getMethod().invoke(controller, args);
              if (ret instanceof Response) {
                // We should check that it matches....
                // btw we should try to enforce matching during compilation phase
                // @Action -> Response.Action
                // @View -> Response.Mime
                // as we can do it
                request.response = (Response)ret;
              }
            }
            catch (InvocationTargetException e) {
              request.response = Response.error(e.getCause());
            }
            catch (IllegalAccessException e) {
              throw new UnsupportedOperationException("hanle me gracefully", e);
            }

            // End request callback
            if (controller instanceof juzu.request.RequestLifeCycle) {
              try {
                ((juzu.request.RequestLifeCycle)controller).endRequest(context);
              }
              catch (Exception e) {
                request.response = Response.error(e);
              }
            }
          }
        }
      }
      finally {
        lifeCycle.close();
      }
    }
  }

  public Dispatch createDispatch(Method<?> method, DispatchBridge spi) {
    ControllersDescriptor desc = plugin.getDescriptor();
    Dispatch dispatch;
    if (method.getPhase() == Phase.ACTION) {
      dispatch = new Phase.Action.Dispatch(spi);
    } else if (method.getPhase() == Phase.VIEW) {
      dispatch = new Phase.View.Dispatch(spi);
      dispatch.escapeXML(desc.getEscapeXML());
    } else if (method.getPhase() == Phase.RESOURCE) {
      dispatch = new Phase.Resource.Dispatch(spi);
      dispatch.escapeXML(desc.getEscapeXML());
    } else {
      throw new AssertionError();
    }
    dispatch.escapeXML(desc.getEscapeXML());
    return dispatch;
  }
}
