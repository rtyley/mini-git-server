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

package com.google.gerrit.server.git;

import com.google.gerrit.lifecycle.LifecycleListener;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.madgag.compress.CompressUtil;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.FilenameUtils;
import org.apache.velocity.texen.util.FileUtil;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.storage.file.LockFile;
import org.eclipse.jgit.storage.file.WindowCache;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.RawParseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import static com.madgag.compress.CompressUtil.unzip;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.FileUtils.openInputStream;
import static org.apache.commons.io.FilenameUtils.removeExtension;

/** Manages Git repositories stored on the local filesystem. */
@Singleton
public class LocalDiskRepositoryManager implements GitRepositoryManager {
  private static final Logger log =
      LoggerFactory.getLogger(LocalDiskRepositoryManager.class);

  private static final String UNNAMED =
      "Unnamed repository; edit this file to name it for gitweb.";

  public static class Lifecycle implements LifecycleListener {
    private final Config cfg;

    @Inject
    Lifecycle(@GerritServerConfig final Config cfg) {
      this.cfg = cfg;
    }

    @Override
    public void start() {
      final WindowCacheConfig c = new WindowCacheConfig();
      c.fromConfig(cfg);
      WindowCache.reconfigure(c);
    }

    @Override
    public void stop() {
    }
  }

  private final File basePath;

  @Inject
  LocalDiskRepositoryManager(final SitePaths site,
      @GerritServerConfig final Config cfg) {
    basePath = site.resolve(cfg.getString("gerrit", null, "basePath"));
    log.info("basePath="+basePath);
    if (basePath == null) {
      throw new IllegalStateException("gerrit.basePath must be configured");
    }

    for (File zipFile : listFiles(basePath, new String[]{"zip"}, true)) {
        File unzippedFolder = new File(removeExtension(zipFile.toString()));
        log.info("Unzipping "+zipFile+" to "+unzippedFolder);
        deleteQuietly(unzippedFolder);
        try {
            unzip(openInputStream(zipFile), unzippedFolder);
        } catch (Exception e) { throw new RuntimeException((e)); }
    }
  }

  /** @return base directory under which all projects are stored. */
  public File getBasePath() {
    return basePath;
  }

  private File gitDirOf(String name) {
    return new File(getBasePath(), name);
  }

  public Repository openRepository(String name)
      throws RepositoryNotFoundException {
    if (isUnreasonableName(name)) {
      throw new RepositoryNotFoundException("Invalid name: " + name);
    }

    try {
      final FileKey loc = FileKey.lenient(gitDirOf(name), FS.DETECTED);
      return RepositoryCache.open(loc);
    } catch (IOException e1) {
      final RepositoryNotFoundException e2;
      e2 = new RepositoryNotFoundException("Cannot open repository " + name);
      e2.initCause(e1);
      throw e2;
    }
  }

  public Repository createRepository(String name)
      throws RepositoryNotFoundException {
    if (isUnreasonableName(name)) {
      throw new RepositoryNotFoundException("Invalid name: " + name);
    }

    try {
      File dir = FileKey.resolve(gitDirOf(name), FS.DETECTED);
      FileKey loc;
      if (dir != null) {
        // Already exists on disk, use the repository we found.
        //
        loc = FileKey.exact(dir, FS.DETECTED);
      } else {
        // It doesn't exist under any of the standard permutations
        // of the repository name, so prefer the standard bare name.
        //
        if (!name.endsWith(Constants.DOT_GIT_EXT)) {
          name = name + Constants.DOT_GIT_EXT;
        }
        loc = FileKey.exact(new File(basePath, name), FS.DETECTED);
      }
      return RepositoryCache.open(loc, false);
    } catch (IOException e1) {
      final RepositoryNotFoundException e2;
      e2 = new RepositoryNotFoundException("Cannot open repository " + name);
      e2.initCause(e1);
      throw e2;
    }
  }

  private boolean isUnreasonableName(String name) {
    if (name.length() == 0) return true; // no empty paths

    if (name.indexOf('\\') >= 0) return true; // no windows/dos stlye paths
    if (name.charAt(0) == '/') return true; // no absolute paths
    if (new File(name).isAbsolute()) return true; // no absolute paths

    if (name.startsWith("../")) return true; // no "l../etc/passwd"
    if (name.contains("/../")) return true; // no "foo/../etc/passwd"
    if (name.contains("/./")) return true; // "foo/./foo" is insane to ask
    if (name.contains("//")) return true; // windows UNC path can be "//..."

    return false; // is a reasonable name
  }
}
