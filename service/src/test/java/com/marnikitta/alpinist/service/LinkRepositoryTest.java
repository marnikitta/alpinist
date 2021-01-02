package com.marnikitta.alpinist.service;

import com.marnikitta.alpinist.model.Link;
import com.marnikitta.alpinist.model.LinkGenerator;
import com.marnikitta.alpinist.model.LinkPayload;
import com.marnikitta.alpinist.model.LinkRepository;
import com.marnikitta.alpinist.repository.FileLinkRepository;
import com.marnikitta.alpinist.repository.GitLinkRepository;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class LinkRepositoryTest {
  private final Path writerPath = Paths.get("./writer");
  private final Path readerPath = Paths.get("./reader");
  private final Path templatePath = Paths.get("./src/test/resources/remote.git");
  private final Path remotePath = Paths.get("./test-remote.git");

  private LinkRepository writerLinkRepository;
  private LinkRepository readerLinkRepository;

  @BeforeMethod
  public void setUp() throws IOException {
    FileLinkRepository.rmRf(remotePath);
    FileLinkRepository.rmRf(writerPath);
    FileLinkRepository.rmRf(readerPath);
    Files.createDirectory(readerPath);
    Files.createDirectory(writerPath);
    //noinspection CallToRuntimeExecWithNonConstantString
    Runtime.getRuntime().exec("cp -r " + templatePath + ' ' + remotePath);
    writerLinkRepository = GitLinkRepository.createFromRemote(
      "file://" + remotePath.toAbsolutePath(),
      writerPath
    );
    readerLinkRepository = GitLinkRepository.createFromRemote(
      "file://" + remotePath.toAbsolutePath(),
      readerPath
    );
  }

  @AfterMethod
  public void tearDown() throws IOException {
    FileLinkRepository.rmRf(remotePath);
    FileLinkRepository.rmRf(writerPath);
    FileLinkRepository.rmRf(readerPath);
  }

  @Test
  public void testInit() {
    //do nothing, test the logic is in setUp and tearDown methods
  }

  @Test
  public void testWriteRead() {
    final LinkGenerator generator = new LinkGenerator();
    for (int i = 0; i < 100; ++i) {
      final String name = generator.nextWord();
      writerLinkRepository.create(name, generator.get());
    }
    writerLinkRepository.sync();
    readerLinkRepository.sync();

    final List<Link> rLinks = readerLinkRepository.links().collect(Collectors.toList());
    final List<Link> wLinks = writerLinkRepository.links().collect(Collectors.toList());
    Assert.assertEquals(rLinks.size(), 100);
    Assert.assertEquals(wLinks.size(), 100);
    Assert.assertEquals(rLinks, wLinks);

    writerLinkRepository.sync();
    readerLinkRepository.sync();

    final List<Link> rLinks1 = readerLinkRepository.links().collect(Collectors.toList());
    final List<Link> wLinks1 = writerLinkRepository.links().collect(Collectors.toList());
    Assert.assertEquals(rLinks1.size(), 100);
    Assert.assertEquals(wLinks1.size(), 100);
    Assert.assertEquals(rLinks1, wLinks1);
  }

  @Test
  public void testWriteUpdateRead() {
    final LinkGenerator generator = new LinkGenerator();
    final List<String> names = new ArrayList<>();

    for (int i = 0; i < 100; ++i) {
      final String name = generator.nextWord();
      writerLinkRepository.create(name, generator.get());
      names.add(name);
    }
    writerLinkRepository.sync();
    readerLinkRepository.sync();

    for (String name : names) {
      writerLinkRepository.update(name, generator.get());
    }
    writerLinkRepository.sync();
    readerLinkRepository.sync();

    final List<Link> rLinks = readerLinkRepository.links().collect(Collectors.toList());
    final List<Link> wLinks = writerLinkRepository.links().collect(Collectors.toList());
    Assert.assertEquals(rLinks.size(), 100);
    Assert.assertEquals(wLinks.size(), 100);
    Assert.assertEquals(rLinks, wLinks);
  }

  @Test
  public void testWriteDeleteRead() {
    final LinkGenerator generator = new LinkGenerator();
    final List<String> names = new ArrayList<>();

    for (int i = 0; i < 100; ++i) {
      final String name = generator.nextWord();
      writerLinkRepository.create(name, generator.get());
      names.add(name);
    }
    writerLinkRepository.sync();
    readerLinkRepository.sync();

    for (String name : names) {
      writerLinkRepository.delete(name);
    }
    writerLinkRepository.sync();
    readerLinkRepository.sync();

    Assert.assertEquals(readerLinkRepository.links().count(), 0);
    Assert.assertEquals(writerLinkRepository.links().count(), 0);
  }

  public void testDeleteNonExisting() {
    Assert.assertFalse(writerLinkRepository.delete("random-name"));
  }

  @Test(expectedExceptions = NoSuchElementException.class)
  public void testUpdateNonExisting() {
    final LinkGenerator generator = new LinkGenerator();
    writerLinkRepository.update("random-name", generator.get());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCreteDuplicate() {
    final LinkGenerator generator = new LinkGenerator();
    final LinkPayload payload = generator.get();
    writerLinkRepository.create("random-name", payload);
    writerLinkRepository.create("random-name", payload);
  }
}