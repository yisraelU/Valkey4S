package io.github.yisraelu.valkey4s.model

import munit.FunSuite

class ValkeyClientConfigSuite extends FunSuite {

  test("fromUriString should parse simple redis URI") {
    val result = ValkeyClientConfig.fromUriString("redis://localhost:6379")

    assert(result.isRight)
    val config = result.toOption.get

    assertEquals(config.addresses.size, 1)
    assertEquals(config.addresses.head.host, "localhost")
    assertEquals(config.addresses.head.port, 6379)
    assertEquals(config.useTls, false)
    assertEquals(config.credentials, None)
    assertEquals(config.databaseId, None)
  }

  test("fromUriString should parse simple valkey URI") {
    val result = ValkeyClientConfig.fromUriString("valkey://localhost:6379")

    assert(result.isRight)
    val config = result.toOption.get

    assertEquals(config.addresses.size, 1)
    assertEquals(config.addresses.head.host, "localhost")
    assertEquals(config.addresses.head.port, 6379)
    assertEquals(config.useTls, false)
    assertEquals(config.credentials, None)
    assertEquals(config.databaseId, None)
  }

  test("fromUriString should parse rediss URI with TLS") {
    val result = ValkeyClientConfig.fromUriString("rediss://secure-server:6380")

    assert(result.isRight)
    val config = result.toOption.get

    assertEquals(config.addresses.head.host, "secure-server")
    assertEquals(config.addresses.head.port, 6380)
    assertEquals(config.useTls, true)
  }

  test("fromUriString should parse valkeys URI with TLS") {
    val result =
      ValkeyClientConfig.fromUriString("valkeys://secure-server:6380")

    assert(result.isRight)
    val config = result.toOption.get

    assertEquals(config.addresses.head.host, "secure-server")
    assertEquals(config.addresses.head.port, 6380)
    assertEquals(config.useTls, true)
  }

  test("fromUriString should parse URI with password") {
    val result =
      ValkeyClientConfig.fromUriString("redis://:mypassword@localhost:6379")

    assert(result.isRight)
    val config = result.toOption.get

    assert(config.credentials.isDefined)
    config.credentials.get match {
      case ServerCredentials.Password(pwd) => assertEquals(pwd, "mypassword")
      case _ => fail("Expected Password credentials")
    }
  }

  test("fromUriString should parse valkey URI with password") {
    val result =
      ValkeyClientConfig.fromUriString("valkey://:mypassword@localhost:6379")

    assert(result.isRight)
    val config = result.toOption.get

    assert(config.credentials.isDefined)
    config.credentials.get match {
      case ServerCredentials.Password(pwd) => assertEquals(pwd, "mypassword")
      case _ => fail("Expected Password credentials")
    }
  }

  test("fromUriString should parse URI with username and password") {
    val result =
      ValkeyClientConfig.fromUriString("redis://alice:secret@localhost:6379")

    assert(result.isRight)
    val config = result.toOption.get

    assert(config.credentials.isDefined)
    config.credentials.get match {
      case ServerCredentials.UsernamePassword(user, pwd) =>
        assertEquals(user, "alice")
        assertEquals(pwd, "secret")
      case _ => fail("Expected UsernamePassword credentials")
    }
  }

  test("fromUriString should parse URI with database number") {
    val result = ValkeyClientConfig.fromUriString("redis://localhost:6379/2")

    assert(result.isRight)
    val config = result.toOption.get

    assertEquals(config.databaseId, Some(2))
  }

  test("fromUriString should parse valkey URI with database number") {
    val result = ValkeyClientConfig.fromUriString("valkey://localhost:6379/3")

    assert(result.isRight)
    val config = result.toOption.get

    assertEquals(config.databaseId, Some(3))
  }

  test("fromUriString should use default port when not specified") {
    val result = ValkeyClientConfig.fromUriString("redis://localhost")

    assert(result.isRight)
    val config = result.toOption.get

    assertEquals(config.addresses.head.port, 6379)
  }

  test("fromUriString should reject invalid scheme") {
    val result = ValkeyClientConfig.fromUriString("http://localhost:6379")

    assert(result.isLeft)
  }

  test("builder should allow fluent configuration") {
    val config = ValkeyClientConfig.builder
      .addAddress("myhost", 7000)
      .withTls(true)
      .withPassword("secret")
      .withDatabase(3)
      .withClientName("my-app")

    assertEquals(config.addresses.size, 2) // localhost + myhost
    assert(config.addresses.exists(_.host == "myhost"))
    assertEquals(config.useTls, true)
    assert(config.credentials.isDefined)
    assertEquals(config.databaseId, Some(3))
    assertEquals(config.clientName, Some("my-app"))
  }

  test("localhost config should have sensible defaults") {
    val config = ValkeyClientConfig.localhost

    assertEquals(config.addresses.size, 1)
    assertEquals(config.addresses.head.host, "localhost")
    assertEquals(config.addresses.head.port, 6379)
    assertEquals(config.useTls, false)
  }
}
