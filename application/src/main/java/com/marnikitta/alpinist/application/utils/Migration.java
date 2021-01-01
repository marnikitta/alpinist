package com.marnikitta.alpinist.application.utils;

import com.marnikitta.alpinist.service.FileLinkRepository;

import java.nio.file.Path;

public class Migration {
  public static void main(String... args) {
    final FileLinkRepository repository = new FileLinkRepository(Path.of(
      "/Users/nikitamarshalkin/wiki_migration_test"));
    repository.migrate();
  }
}
