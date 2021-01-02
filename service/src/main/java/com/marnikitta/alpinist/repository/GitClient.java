package com.marnikitta.alpinist.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

public class GitClient {
  private static final String GIT_PATH = "/usr/bin/git";
  private final Runtime runtime = Runtime.getRuntime();
  private final Logger log = LoggerFactory.getLogger(GitClient.class);
  private final Path baseDir;
  private boolean hasRemote = false;

  public GitClient(Path baseDir) {
    this.baseDir = baseDir;
  }

  boolean hasRemote() {
    return hasRemote;
  }

  void configure(String name, String email) {
    git("config", "user.name", name);
    git("config", "user.email", email);
  }

  void init() {
    git("init");
  }

  void addRemote(String remote) {
    git("remote", "add", "origin", remote);
    hasRemote = true;
  }

  void pull() {
    git("fetch", "origin");
    git("merge", "-q", "-s", "recursive", "-X", "theirs", "origin/master");
  }

  void commitAll(String message) {
    git("add", "--all");
    git("commit", "-m", message);
  }
  public void push() {
    git("push", "origin", "master:master");
  }

  @SuppressWarnings("NestedTryStatement")
  private String git(String... args) {
    final String[] command = new String[args.length + 3];
    command[0] = GIT_PATH;
    command[1] = "-C";
    command[2] = baseDir.toString();
    System.arraycopy(args, 0, command, 3, args.length);
    final String com = Arrays.toString(command);
    log.trace(com);

    try {
      //noinspection CallToRuntimeExec
      final Process exec = runtime.exec(command);

      final String error;
      try (BufferedReader br = new BufferedReader(new InputStreamReader(exec.getErrorStream()))) {
        error = br.lines().collect(Collectors.joining("\n"));
      }
      final String result;
      try (BufferedReader br = new BufferedReader(new InputStreamReader(exec.getInputStream()))) {
        result = br.lines().collect(Collectors.joining("\n"));
      }

      if (!error.isEmpty()) {
        log.trace("{}: {}", com, error);
      }
      if (!result.isEmpty()) {
        log.trace("{}: {}", com, result);
      }

      if (exec.waitFor() != 0) {
        throw new RuntimeException(result + " " + error);
      }
      return result;
    } catch (IOException e) {
      log.error("Error during executing command", e);
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
