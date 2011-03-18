// Copyright (C) 2009 The Android Open Source Project
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

package com.google.gerrit.httpd;

import com.google.gerrit.httpd.raw.SshInfoServlet;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.internal.UniqueAnnotations;
import com.google.inject.servlet.ServletModule;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.inject.Scopes.SINGLETON;

class UrlModule extends ServletModule {
  @Override
  protected void configureServlets() {
    serve("/logout").with(HttpLogoutServlet.class);
    serve("/signout").with(HttpLogoutServlet.class);
    serve("/ssh_info").with(SshInfoServlet.class);

    filter("/p/*").through(ProjectAccessPathFilter.class);
    serve("/p/*").with(ProjectServlet.class);

    serve("/Main.class").with(notFound());
    serve("/com/google/gerrit/launcher/*").with(notFound());
    serve("/servlet/*").with(notFound());
  }

  private Key<HttpServlet> notFound() {
    return key(new HttpServlet() {
      private static final long serialVersionUID = 1L;

      @Override
      protected void doGet(final HttpServletRequest req,
          final HttpServletResponse rsp) throws IOException {
        rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
      }
    });
  }

  private Key<HttpServlet> screen(final String target) {
    return key(new HttpServlet() {
      private static final long serialVersionUID = 1L;

      @Override
      protected void doGet(final HttpServletRequest req,
          final HttpServletResponse rsp) throws IOException {
        toGerrit(target, req, rsp);
      }
    });
  }

  private Key<HttpServlet> legacyGerritScreen() {
    return key(new HttpServlet() {
      private static final long serialVersionUID = 1L;

      @Override
      protected void doGet(final HttpServletRequest req,
          final HttpServletResponse rsp) throws IOException {
        final String token = req.getPathInfo().substring(1);
        toGerrit(token, req, rsp);
      }
    });
  }

  private Key<HttpServlet> key(final HttpServlet servlet) {
    final Key<HttpServlet> srv =
        Key.get(HttpServlet.class, UniqueAnnotations.create());
    bind(srv).toProvider(new Provider<HttpServlet>() {
      @Override
      public HttpServlet get() {
        return servlet;
      }
    }).in(SINGLETON);
    return srv;
  }

  private void toGerrit(final String target, final HttpServletRequest req,
      final HttpServletResponse rsp) throws IOException {
    final StringBuilder url = new StringBuilder();
    url.append(req.getContextPath());
    url.append('/');
    url.append('#');
    url.append(target);
    rsp.sendRedirect(url.toString());
  }
}
