package com.marnikitta.alpinist.model;

import java.util.stream.Stream;

public interface LinkRepository {

  /**
   * Synchronizes local repository with its replicas
   */
  void sync();

  /**
   * Fetches all links
   *
   * @return the ordered stream of links, naturally ordered
   */
  Stream<Link> links();


  /**
   * Creates new link
   *
   * @param name    name of the link to create
   * @param payload payload of link
   * @throws IllegalArgumentException if the link with such name exists
   */
  Link create(String name, LinkPayload payload);

  /**
   * Deletes link by its name
   *
   * @param name name of the link to delete
   * @throws java.util.NoSuchElementException if link with name doesn't exists
   */
  void delete(String name);

  /**
   * Updates link by its name
   *
   * @param name    name of the link to update
   * @param payload the new payload of the link
   * @throws java.util.NoSuchElementException if link with name doesn't exists
   */
  Link update(String name, LinkPayload payload);
}
