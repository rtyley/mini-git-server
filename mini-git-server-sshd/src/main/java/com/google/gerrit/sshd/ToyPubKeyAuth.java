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

package com.google.gerrit.sshd;

import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.bouncycastle.openssl.PEMReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;

import static org.apache.commons.io.FileUtils.listFiles;

/**
 * Authenticates by public key really permissively
 */
@Singleton
class ToyPubKeyAuth implements PublickeyAuthenticator {
    private static final Logger log =
      LoggerFactory.getLogger(ToyPubKeyAuth.class);

    private final IdentifiedUser.GenericFactory userFactory;
    private final SitePaths sitePaths;

    @Inject
  ToyPubKeyAuth(IdentifiedUser.GenericFactory uf, final SitePaths sitePaths) {
    userFactory = uf;
        this.sitePaths = sitePaths;
    }
	@Override
	public boolean authenticate(String username, PublicKey publicKey, ServerSession serverSession) {
		final SshSession sd = serverSession.getAttribute(SshSession.KEY);
        File usersDir = new File(sitePaths.etc_dir, "users"); //sitePaths.
        File userDir = new File(usersDir,username);
        if (!userDir.exists() || !userDir.isDirectory()) {
            String error = "User ssh key folder not found " + userDir;
            log.warn(error);
            sd.authenticationError(username, error);
            return false;
        }
        Collection<File> files = listFiles(userDir, new String[]{"pub"}, false);
        for (File publicKeyFile : files) {
            log.info("Trying " + publicKeyFile);
            try {
                PublicKey authorisedKey=SshUtil.parseOpenSSHKey(FileUtils.readFileToString(publicKeyFile));

                if (authorisedKey.equals(publicKey)) {
                    sd.authenticationSuccess(username, userFactory.create(username));
                    return true;  //To change body of implemented methods use File | Settings | File Templates.
                }
            } catch (Exception e) {
                log.error("Problem with " + publicKeyFile, e);
            }
        }
        log.warn("No good keys found from "+files);
        sd.authenticationError(username, "No matching key found");
        return false;
	}
}
