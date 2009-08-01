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

package com.google.gerrit.server.http;

import com.google.gwt.user.server.rpc.RPCServletUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Redirects from {@code /Gerrit#foo} to {@code /#foo} in JavaScript.
 * <p>
 * This redirect exists to convert the older /Gerrit URL into the more modern
 * URL format which does not use a servlet name for the host page. We cannot do
 * the direct here in the server side, as it would lose any history token that
 * appears in the URL. Instead we send an HTML page which instructs the browser
 * to replace the URL, but preserve the history token.
 */
@SuppressWarnings("serial")
@Singleton
public class LegacyGerritServlet extends HttpServlet {
  private final byte[] raw;
  private final byte[] compressed;

  @Inject
  LegacyGerritServlet(final ServletContext servletContext) throws IOException {
    final String hostPageName = "WEB-INF/LegacyGerrit.html";
    final String doc = HtmlDomUtil.readFile(servletContext, "/" + hostPageName);
    if (doc == null) {
      throw new FileNotFoundException("No " + hostPageName + " in webapp");
    }

    raw = doc.getBytes(HtmlDomUtil.ENC);
    compressed = HtmlDomUtil.compress(raw);
  }

  @Override
  protected void doGet(final HttpServletRequest req,
      final HttpServletResponse rsp) throws IOException {
    final byte[] tosend;
    if (RPCServletUtils.acceptsGzipEncoding(req)) {
      rsp.setHeader("Content-Encoding", "gzip");
      tosend = compressed;
    } else {
      tosend = raw;
    }

    rsp.setHeader("Expires", "Fri, 01 Jan 1980 00:00:00 GMT");
    rsp.setHeader("Pragma", "no-cache");
    rsp.setHeader("Cache-Control", "no-cache, must-revalidate");
    rsp.setContentType("text/html");
    rsp.setCharacterEncoding(HtmlDomUtil.ENC);
    rsp.setContentLength(tosend.length);
    final OutputStream out = rsp.getOutputStream();
    try {
      out.write(tosend);
    } finally {
      out.close();
    }
  }
}
