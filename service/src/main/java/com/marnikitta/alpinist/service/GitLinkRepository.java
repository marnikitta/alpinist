package com.marnikitta.alpinist.service;

import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.model.LinkPayload;
import com.marnikitta.alpinist.model.LinkRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@SuppressWarnings("LockAcquiredButNotSafelyReleased")
public class GitLinkRepository implements LinkRepository {
  private final GitClient gitClient;
  private final FileLinkRepository linkRepository;

  public GitLinkRepository(GitClient client, FileLinkRepository linkRepository) {
    this.gitClient = client;
    this.linkRepository = linkRepository;
  }

  public static GitLinkRepository createFromDirectory(Path baseDir) {
    if (!Files.exists(baseDir.resolve(".git"))) {
      throw new IllegalArgumentException("Directory %s already exists but doesn't have git repository in it");
    }
    return new GitLinkRepository(new GitClient(baseDir), new FileLinkRepository(baseDir));
  }

  public static GitLinkRepository createFromRemote(String remote, Path baseDir) throws IOException {
    if (Files.exists(baseDir)) {
      FileLinkRepository.rmRf(baseDir);
    }
    Files.createDirectory(baseDir);
    final GitClient client = new GitClient(baseDir);
    client.init();
    client.addRemote(remote);
    client.configure("Alpinist", "alpinist@marnikitta.com");
    client.pull();
    return new GitLinkRepository(client, new FileLinkRepository(baseDir));
  }

  @Override
  public synchronized final void sync() {
    if (gitClient.hasRemote()) {
      gitClient.pull();
    }
    if (gitClient.hasRemote()) {
      gitClient.push();
    }
  }

  @Override
  public synchronized Stream<Link> links() {
    return linkRepository.links();
  }

  @Override
  public synchronized Link create(String name, LinkPayload payload) {
    final Link link = linkRepository.create(name, payload);
    gitClient.commitAll("Create " + name);
    return link;
  }

  @Override
  public boolean delete(String name) {
    final boolean result = linkRepository.delete(name);
    if (result) {
      gitClient.commitAll("Delete " + name);
    }
    return result;
  }

  @Override
  public Link update(String name, LinkPayload payload) {
    final Link link = linkRepository.update(name, payload);
    gitClient.commitAll("Update " + name);
    return link;
  }
}
