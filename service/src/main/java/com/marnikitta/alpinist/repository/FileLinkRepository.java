package com.marnikitta.alpinist.repository;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("LockAcquiredButNotSafelyReleased")
public class FileLinkRepository implements LinkRepository {
  private final Logger log = LoggerFactory.getLogger(FileLinkRepository.class);
  private final Path baseDir;

  private final LinkEncoder encoder = new LinkEncoder();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  public FileLinkRepository(Path baseDir) {
    this.baseDir = baseDir;
  }

  @Override
  public final void sync() {
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
    return new Link(name, payload);
  }

  @Override
  public Link create(String name, LinkPayload payload) {
    if (name.isEmpty() || name.isBlank()) {
      throw new IllegalArgumentException("Name shouldn't be empty");
    }
    try {
      lock.writeLock().lock();
      final String raw = encoder.encode(payload);
      final Path path = baseDir.resolve("link");
      if (!Files.exists(path)) {
        Files.createDirectories(path);
      }
      final Path linkPath = path.resolve(name + ".md");
      Files.write(linkPath, Collections.singleton(raw), StandardOpenOption.CREATE_NEW);
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
  public boolean delete(String name) {
    try {
      lock.writeLock().lock();
      final boolean[] deleted = {false};
      Files.walkFileTree(baseDir, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (file.endsWith(name + ".md")) {
            Files.delete(file);
            log.info("Deleted link at path '{}'", file);
            deleted[0] = true;
            return FileVisitResult.TERMINATE;
          } else {
            return FileVisitResult.CONTINUE;
          }
        }
      });
      return deleted[0];
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
              linkPath[0] = file;
              return FileVisitResult.TERMINATE;
            }

            Files.write(file, raw.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
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

  public static void rmRf(Path directory) throws IOException {
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

  public void migrate() {
    try {
      Files.walkFileTree(baseDir, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (file.getFileName().toString().endsWith(".md")) {
            try {
              final Link link = linkAt(file);
              //System.out.println(link.name());

              try {
                final LinkPayload linkPayload = link.payload()
                  .withUpdatedDiscussion(updatedLinkBody(link.payload().rawDiscussion()).trim());
                final String raw = encoder.encode(linkPayload);
                Files.write(file, raw.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
              } catch (Exception e) {
                System.out.println(file);
                return FileVisitResult.CONTINUE;
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

  private String updatedLinkBody(String discussion) {
    final List<String> lines = discussion.lines().collect(Collectors.toList());
    if (lines.isEmpty()) {
      return discussion;
    }
    final String firstLine = lines.get(0);
    if (!firstLine.startsWith("[[")) {
      return discussion;
    }
    final List<String> outlink = Arrays.stream(firstLine.split(","))
      .map(t -> {
        final Matcher matcher = LinkPayload.OUTLINK_PATTERN.matcher(t.trim());
        if (!matcher.matches()) {
          throw new IllegalArgumentException();
        }
        return matcher.group("outlink");
      }).collect(Collectors.toList());

    outlink.removeAll(List.of("unread", "link", "note", "shelved"));

    Collections.sort(outlink);
    final String oulinksLine = outlink.stream().map(l -> "[[" + l + "]]").collect(Collectors.joining(", "));
    lines.remove(0);
    if (!outlink.isEmpty()) {
      lines.add("\n" + oulinksLine);
    }

    return String.join("\n", lines);
  }
}
