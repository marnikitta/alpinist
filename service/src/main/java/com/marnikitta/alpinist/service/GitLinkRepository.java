package com.marnikitta.alpinist.service;

import com.marnikitta.alpinist.model.CommonTags;
import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.model.LinkPayload;
import com.marnikitta.alpinist.model.LinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("LockAcquiredButNotSafelyReleased")
public class GitLinkRepository implements LinkRepository {
  private final Logger log = LoggerFactory.getLogger(GitLinkRepository.class);
  private final Path baseDir;

  private final Map<String, TimeCacheEntry> tsCache = new HashMap<>();

  private final GitClient gitClient;
  private final LinkEncoder encoder = new LinkEncoder();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private GitLinkRepository(GitClient client, Path baseDir) {
    this.baseDir = baseDir;
    this.gitClient = client;
  }

  public static GitLinkRepository createFromDirectory(Path baseDir) {
    if (!Files.exists(baseDir.resolve(".git"))) {
      throw new IllegalArgumentException("Directory %s already exists but doesn't have git repository in it");
    }
    return new GitLinkRepository(new GitClient(baseDir), baseDir);
  }

  public static GitLinkRepository createFromRemote(String remote, Path baseDir) throws IOException {
    if (Files.exists(baseDir)) {
      rmRf(baseDir);
    }
    Files.createDirectory(baseDir);
    final GitClient client = new GitClient(baseDir);
    client.init();
    client.addRemote(remote);
    client.configure("Alpinist", "alpinist@marnikitta.com");
    client.pull();
    return new GitLinkRepository(client, baseDir);
  }

  @Override
  public final void sync() {
      try {
        lock.writeLock().lock();
        if (gitClient.hasRemote()) {
          gitClient.pull();
        }
        normilize();
        if (gitClient.hasRemote()) {
          gitClient.push();
        }
      } finally {
        lock.writeLock().unlock();
    }
  }

  @Override
  public Stream<Link> links() {
    try {
      lock.readLock().lock();
      final List<Link> result = new ArrayList<>();
      Files.walkFileTree(baseDir, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (file.getFileName().toString().endsWith(".md")) {
            try {
              result.add(linkAt(file));
            } catch (IllegalArgumentException ignored) {
              log.warn("Error during parsing of {}", file);
            }
          }
          return FileVisitResult.CONTINUE;
        }
      });
      result.sort(Comparator.naturalOrder());
      return result.stream();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      lock.readLock().unlock();
    }
  }

  private Link linkAt(Path linkPath) throws IOException {
    //noinspection DynamicRegexReplaceableByCompiledPattern
    final String name = linkPath.getFileName().toString().replace(".md", "");
    final String collect = new String(Files.readAllBytes(linkPath));
    final LinkPayload payload = encoder.decode(collect);

    {
      // git log is quite time consuming, so if the fs time hasn't been changed
      // I assume that git ts is the same either
      final Instant lastModifiedTime = Files.readAttributes(linkPath, BasicFileAttributes.class)
        .lastModifiedTime().toInstant();
      if (!tsCache.containsKey(name) || !tsCache.get(name).lastModifiedTime().equals(lastModifiedTime)) {
        tsCache.remove(name);
        final Instant created = gitClient.createTime(baseDir.relativize(linkPath));
        final Instant updated = gitClient.updateTime(baseDir.relativize(linkPath));
        tsCache.put(name, new TimeCacheEntry(lastModifiedTime, created, updated));
      }
    }

    final Instant created = tsCache.get(name).created();
    final Instant updated = tsCache.get(name).updated();
    return new Link(name, created, updated, payload);
  }

  private static final class TimeCacheEntry {
    private final Instant lastModifiedTime;
    private final Instant created;
    private final Instant updated;

    private TimeCacheEntry(Instant lastModifiedTime,
                           Instant created,
                           Instant updated) {
      this.lastModifiedTime = lastModifiedTime;
      this.created = created;
      this.updated = updated;
    }

    public Instant lastModifiedTime() {
      return lastModifiedTime;
    }

    public Instant created() {
      return created;
    }

    public Instant updated() {
      return updated;
    }
  }

  @Override
  public Link create(String name, LinkPayload payload) {
    try {
      lock.writeLock().lock();
      final String raw = encoder.encode(payload);
      final Path path = specialPath(payload.tags().collect(Collectors.toSet()));
      if (!Files.exists(path)) {
        Files.createDirectories(path);
      }
      final Path linkPath = path.resolve(name + ".md");
      Files.write(linkPath, Collections.singleton(raw), StandardOpenOption.CREATE_NEW);
      gitClient.commitAll("Create " + name);
      log.info("Created new link at path '{}'", linkPath);
      return linkAt(linkPath);
    } catch (FileAlreadyExistsException ignored) {
      throw new IllegalArgumentException("Link with name '" + name + "' already exists");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void delete(String name) {
    try {
      lock.writeLock().lock();
      final boolean[] deleted = {false};
      Files.walkFileTree(baseDir, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (file.endsWith(name + ".md")) {
            Files.delete(file);
            gitClient.commitAll("Delete " + name);
            log.info("Deleted link at path '{}'", file);
            deleted[0] = true;
            return FileVisitResult.TERMINATE;
          } else {
            return FileVisitResult.CONTINUE;
          }
        }
      });
      if (!deleted[0]) {
        throw new NoSuchElementException("Link with name '" + name + "' doesn't exists");
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public Link update(String name, LinkPayload payload) {
    try {
      lock.writeLock().lock();
      final Path[] linkPath = {null};
      final String raw = encoder.encode(payload);
      Files.walkFileTree(baseDir, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (file.endsWith(name + ".md")) {
            final String currentData = new String(Files.readAllBytes(file));
            if (raw.equals(currentData)) {
              log.warn("Update of '{}' is equal to the current data", file);
              return FileVisitResult.TERMINATE;
            }

            Files.write(file, raw.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            gitClient.commitAll("Update " + name);
            log.info("Updated link at path '{}'", file);
            linkPath[0] = file;
            return FileVisitResult.TERMINATE;
          } else {
            return FileVisitResult.CONTINUE;
          }
        }
      });
      if (linkPath[0] == null) {
        throw new NoSuchElementException("Link with name '" + name + "' doesn't exists");
      } else {
        return linkAt(linkPath[0]);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @SuppressWarnings("MethodWithMultipleReturnPoints")
  private Path specialPath(Set<String> tags) {
    final Collection<String> rootTopics = Arrays.asList(CommonTags.LOGBOOK, CommonTags.LINK, CommonTags.NOTE, CommonTags.SPACE, CommonTags.CONF);
    for (String rootTopic : rootTopics) {
      if (tags.contains(rootTopic)) {
        return baseDir.resolve(rootTopic);
      }
    }

    return baseDir.resolve("unsorted");
  }

  static void rmRf(Path directory) throws IOException {
    if (Files.exists(directory)) {
      Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }

  private void normilize() {
    try {
      Files.walkFileTree(baseDir, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (file.getFileName().toString().endsWith(".md")) {
            try {
              final Link link = linkAt(file);
              final Path expectedPath = specialPath(link.payload().tags().collect(Collectors.toSet()));
              if (!file.getParent().equals(expectedPath) && link.payload()
                .tags()
                .noneMatch(t -> t.equals(CommonTags.NOTE))) {
                if (!Files.exists(expectedPath)) {
                  Files.createDirectory(expectedPath);
                }
                try {
                  Files.move(file, expectedPath.resolve(link.name() + ".md"));
                } catch (FileAlreadyExistsException ignored) {
                  Files.move(file, expectedPath.resolve(link.name() + "_" + UUID.randomUUID() + ".md"));
                }
                gitClient.commitAll("Move " + link.name());
                tsCache.remove(link.name());
                log.info("Moved link {} to path '{}'", link.name(), expectedPath);
              }
            } catch (IllegalArgumentException ignored) {
              log.warn("Error during parsing of {}", file);
            }
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
