package com.marnikitta.alpinist.model;

import com.marnikitta.alpinist.repository.LinkEncoder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class LinkPayloadEncoderTest {
  private final LinkEncoder encoder = new LinkEncoder();

  @Test(invocationCount = 1000)
  public void testRandomEncodeDecode() {
    final LinkPayload expected = new LinkGenerator().get();
    final LinkPayload actual = encoder.decode(encoder.encode(expected));

    Assert.assertEquals(actual, expected);
  }

  @Test(invocationCount = 1000)
  public void testRandomDecodeEncode() {
    final String expected = encoder.encode(new LinkGenerator().get());
    final String actual = encoder.encode(encoder.decode(expected));

    Assert.assertEquals(actual, expected);
  }

  @Test
  public void decodeDateTime() {
    final LinkPayload li = encoder.decode("# Title\n__Updated:__ 2020-07-01T12:23\n");
    Assert.assertEquals(li.updated(), Optional.of(LocalDateTime.of(2020, 7, 1, 12, 23)));
  }

  @Test(expectedExceptions = DateTimeParseException.class)
  public void decodeDateOnly() {
    // FIXME: 08.01.2021 
    final LinkPayload li = encoder.decode("# Title\n__Updated:__ 2020-07-01\n");
  }
}