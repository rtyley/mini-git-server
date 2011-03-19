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

package com.google.gerrit.sshd.commands;

import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.git.TransferConfig;
import com.google.gerrit.sshd.AbstractGitCommand;
import com.google.inject.Inject;
import org.eclipse.jgit.errors.UnpackException;
import org.eclipse.jgit.transport.ReceivePack;

import java.io.IOException;
import java.io.InterruptedIOException;

/** Receives change upload over SSH using the Git receive-pack protocol. */
final class Receive extends AbstractGitCommand {

  @Inject
  private IdentifiedUser currentUser;

  @Inject
  private IdentifiedUser.GenericFactory identifiedUserFactory;

  @Inject
  private TransferConfig config;

  @Override
  protected void runImpl() throws IOException, Failure {


    final ReceivePack rp = new ReceivePack(repo);
    rp.setRefLogIdent(currentUser.newRefLogIdent());
    rp.setTimeout(config.getTimeout());
    try {
      rp.receive(in, out, err);
    } catch (InterruptedIOException err) {
      throw new Failure(128, "fatal: client IO read/write timeout", err);

    } catch (UnpackException badStream) {
      // This may have been triggered by branch level access controls.
      // Log what the heck is going on, as detailed as we can.
      //
      StringBuilder msg = new StringBuilder();
      msg.append("Unpack error on project \""
          + projectName + "\":\n");

      msg.append("\n");

      IOException detail = new IOException(msg.toString(), badStream);
      throw new Failure(128, "fatal: Unpack error, check server log", detail);
    }
  }

}
