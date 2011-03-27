// Copyright (C) 2008 The Android Open Source Project
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

package com.google.gerrit.common.data;

import java.util.List;
import java.util.Set;

public class GerritConfig implements Cloneable {
  protected String registerUrl;
  protected boolean useContributorAgreements;
  protected boolean useContactInfo;
  protected boolean allowRegisterNewEmail;
  protected String gitDaemonUrl;
  protected String sshdAddress;
  // protected String wildProject;
  protected boolean documentationAvailable;

  protected String backgroundColor;
  protected String topMenuColor;
  protected String textColor;
  protected String trimColor;
  protected String selectionColor;

  public String getRegisterUrl() {
    return registerUrl;
  }

  public void setRegisterUrl(final String u) {
    registerUrl = u;
  }


  public boolean isUseContributorAgreements() {
    return useContributorAgreements;
  }

  public void setUseContributorAgreements(final boolean r) {
    useContributorAgreements = r;
  }

  public boolean isUseContactInfo() {
    return useContactInfo;
  }

  public void setUseContactInfo(final boolean r) {
    useContactInfo = r;
  }

  public String getGitDaemonUrl() {
    return gitDaemonUrl;
  }

  public void setGitDaemonUrl(String url) {
    if (url != null && !url.endsWith("/")) {
      url += "/";
    }
    gitDaemonUrl = url;
  }

  public String getSshdAddress() {
    return sshdAddress;
  }

  public void setSshdAddress(final String addr) {
    sshdAddress = addr;
  }

//  public String getWildProject() {
//    return wildProject;
//  }
//
//  public void setWildProject(final String wp) {
//    wildProject = wp;
//  }

  public boolean isDocumentationAvailable() {
    return documentationAvailable;
  }

  public void setDocumentationAvailable(final boolean available) {
    documentationAvailable = available;
  }

  public String getBackgroundColor() {
    return backgroundColor;
  }

  public void setBackgroundColor(String color) {
    backgroundColor = color;
  }

  public String getTopMenuColor() {
    return topMenuColor;
  }

  public void setTopMenuColor(String color) {
    topMenuColor = color;
  }

  public String getTextColor() {
    return textColor;
  }

  public void setTextColor(String color) {
    textColor = color;
  }

  public String getTrimColor() {
    return trimColor;
  }

  public void setTrimColor(String color) {
    trimColor = color;
  }

  public String getSelectionColor() {
    return selectionColor;
  }

  public void setSelectionColor(String color) {
    selectionColor = color;
  }
}
