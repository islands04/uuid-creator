package com.github.f4b6a3.uuid.alt;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.junit.Test;

import com.github.f4b6a3.uuid.codec.other.TimeOrderedCodec;
import com.github.f4b6a3.uuid.util.UuidTime;
import com.github.f4b6a3.uuid.util.UuidUtil;

public class GUIDTest {

	private static final int DEFAULT_LOOP_MAX = 100;

	private static final SecureRandom secure = new SecureRandom();

	private static final String REGEX = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
	private static final Pattern PATTERN = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);

	@Test
	public void testConstructorGUID() {
		SplittableRandom random = new SplittableRandom(1);
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			final long msb = random.nextLong();
			final long lsb = random.nextLong();
			GUID guid = new GUID(new GUID(msb, lsb));
			assertEquals(msb, guid.getMostSignificantBits());
			assertEquals(lsb, guid.getLeastSignificantBits());
		}
	}

	@Test
	public void testConstructorLongs() {
		SplittableRandom random = new SplittableRandom(1);
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			final long msb = random.nextLong();
			final long lsb = random.nextLong();
			GUID guid = new GUID(msb, lsb);
			assertEquals(msb, guid.getMostSignificantBits());
			assertEquals(lsb, guid.getLeastSignificantBits());
		}
	}

	@Test
	public void testConstructorJdkUUID() {
		SplittableRandom random = new SplittableRandom(1);
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			final long msb = random.nextLong();
			final long lsb = random.nextLong();
			GUID guid = new GUID(new UUID(msb, lsb));
			assertEquals(msb, guid.getMostSignificantBits());
			assertEquals(lsb, guid.getLeastSignificantBits());
		}
	}

	@Test
	public void testConstructorString() {
		SplittableRandom random = new SplittableRandom(1);
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			final long msb = random.nextLong();
			final long lsb = random.nextLong();
			UUID uuid = new UUID(msb, lsb);
			String str = uuid.toString();
			GUID guid = new GUID(str);
			assertEquals(msb, guid.getMostSignificantBits());
			assertEquals(lsb, guid.getLeastSignificantBits());
		}
	}

	@Test
	public void testConstructorBytes() {
		SplittableRandom seeder = new SplittableRandom(1);
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			byte[] bytes = new byte[GUID.GUID_BYTES];
			(new Random(seeder.nextLong())).nextBytes(bytes);
			GUID guid = new GUID(bytes);
			assertEquals(Arrays.toString(bytes), Arrays.toString(guid.toBytes()));
		}

		try {
			byte[] bytes = null;
			new GUID(bytes);
			fail("Should throw an exception");
		} catch (IllegalArgumentException e) {
			// success
		}

		try {
			byte[] bytes = new byte[GUID.GUID_BYTES + 1];
			new GUID(bytes);
			fail("Should throw an exception");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	@Test
	public void testV1() {
		testV1(() -> GUID.v1());
		testV1(() -> GUID.v1(null, null));
		testV1(() -> GUID.v1(null, secure));
		testV1(() -> GUID.v1(Instant.now(), null));
		testV1(() -> GUID.v1(Instant.now(), secure));
	}

	public void testV1(Supplier<GUID> function) {
		GUID prev = function.get();
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			long t0 = System.currentTimeMillis();
			GUID guid = function.get();
			long t1 = UuidTime.toUnixTimestamp(guid.toUUID().timestamp()) / 10_000L;
			long t2 = System.currentTimeMillis();
			assertNotNull(guid);
			assertNotEquals(prev, guid);
			assertNotEquals(GUID.NIL, guid);
			assertNotEquals(GUID.MAX, guid);
			assertEquals(1, guid.version());
			assertTrue(t0 <= t1 && t1 <= t2);
			prev = guid;
		}
	}

	@Test
	public void testV2() {
		GUID prev = GUID.v2((byte) 0, (int) 0);
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			byte ld = (byte) i;
			int li = (int) i * 31;
			GUID guid = GUID.v2(ld, li);
			byte localDomain = UuidUtil.getLocalDomain(guid.toUUID());
			int localIdentifier = UuidUtil.getLocalIdentifier(guid.toUUID());
			assertNotNull(guid);
			assertNotEquals(prev, guid);
			assertNotEquals(GUID.NIL, guid);
			assertNotEquals(GUID.MAX, guid);
			assertEquals(2, guid.version());
			assertEquals(ld, localDomain);
			assertEquals(li, localIdentifier);
			prev = guid;
		}
	}

	@Test
	public void testV3() {
		GUID prev = GUID.v3(GUID.NIL, "");
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			GUID namespace = new GUID(UUID.randomUUID());
			String name = UUID.randomUUID().toString();
			GUID guid = GUID.v3(namespace, name);
			assertNotNull(guid);
			assertNotEquals(prev, guid);
			assertEquals(3, guid.version());
			assertNotEquals(GUID.NIL, guid);
			assertNotEquals(GUID.MAX, guid);
			assertNotEquals(GUID.v3(null, name), GUID.v3(namespace, name));
			assertNotEquals(GUID.v3(GUID.NIL, name), GUID.v3(namespace, name));
			assertNotEquals(GUID.v3(namespace, name), GUID.v5(namespace, name)); // v5
			assertEquals(GUID.v3(null, name), GUID.v3(null, name));
			assertEquals(GUID.v3(GUID.NIL, name), GUID.v3(GUID.NIL, name));
			assertEquals(GUID.v3(namespace, name), GUID.v3(namespace, name));
			assertEquals(GUID.v3(namespace, name), GUID.v3(namespace, name.getBytes(StandardCharsets.UTF_8)));
			assertEquals(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), GUID.v3(null, name).toUUID());
			prev = guid;
		}
		{
			// Example of a UUIDv3 Value
			// draft-ietf-uuidrev-rfc4122bis-03
			GUID guid = GUID.v3(GUID.NAMESPACE_DNS, "www.example.com");
			assertEquals("5df41881-3aed-3515-88a7-2f4a814cf09e", guid.toString());
		}
	}

	@Test
	public void testV4() {
		testV4(() -> GUID.v4());
		testV4(() -> GUID.v4(secure));
	}

	public void testV4(Supplier<GUID> function) {
		GUID prev = function.get();
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			GUID guid = function.get();
			assertNotNull(guid);
			assertNotEquals(prev, guid);
			assertNotEquals(GUID.NIL, guid);
			assertNotEquals(GUID.MAX, guid);
			assertEquals(4, guid.version());
			prev = guid;
		}
	}

	@Test
	public void testV5() {
		GUID prev = GUID.v5(GUID.NIL, "");
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			GUID namespace = new GUID(UUID.randomUUID());
			String name = UUID.randomUUID().toString();
			GUID guid = GUID.v5(namespace, name);
			assertNotNull(guid);
			assertNotEquals(prev, guid);
			assertEquals(5, guid.version());
			assertNotEquals(GUID.NIL, guid);
			assertNotEquals(GUID.MAX, guid);
			assertNotEquals(GUID.v5(null, name), GUID.v5(namespace, name));
			assertNotEquals(GUID.v5(GUID.NIL, name), GUID.v5(namespace, name));
			assertNotEquals(GUID.v5(namespace, name), GUID.v3(namespace, name)); // v3
			assertEquals(GUID.v5(null, name), GUID.v5(null, name));
			assertEquals(GUID.v5(GUID.NIL, name), GUID.v5(GUID.NIL, name));
			assertEquals(GUID.v5(namespace, name), GUID.v5(namespace, name));
			assertEquals(GUID.v5(namespace, name), GUID.v5(namespace, name.getBytes(StandardCharsets.UTF_8)));
			prev = guid;
		}
		{
			// Example of a UUIDv5 Value
			// draft-ietf-uuidrev-rfc4122bis-03
			GUID guid = GUID.v5(GUID.NAMESPACE_DNS, "www.example.com");
			assertEquals("2ed6657d-e927-568b-95e1-2665a8aea6a2", guid.toString());
		}
	}

	@Test
	public void testV6() {
		testV6(() -> GUID.v6());
		testV6(() -> GUID.v6(null, null));
		testV6(() -> GUID.v6(null, secure));
		testV6(() -> GUID.v6(Instant.now(), null));
		testV6(() -> GUID.v6(Instant.now(), secure));
	}

	public void testV6(Supplier<GUID> function) {
		GUID prev = function.get();
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			long t0 = System.currentTimeMillis();
			GUID guid = function.get();
			GUID temp = new GUID(TimeOrderedCodec.INSTANCE.decode(guid.toUUID()));
			long t1 = UuidTime.toUnixTimestamp(temp.toUUID().timestamp()) / 10_000L;
			long t2 = System.currentTimeMillis();
			assertNotNull(guid);
			assertNotEquals(prev, guid);
			assertNotEquals(GUID.NIL, guid);
			assertNotEquals(GUID.MAX, guid);
			assertEquals(6, guid.version());
			assertTrue(t0 <= t1 && t1 <= t2);
			prev = guid;
		}
	}

	@Test
	public void testV7() {
		testV7(() -> GUID.v7());
		testV7(() -> GUID.v7(null, null));
		testV7(() -> GUID.v7(null, secure));
		testV7(() -> GUID.v7(Instant.now(), null));
		testV7(() -> GUID.v7(Instant.now(), secure));
	}

	public void testV7(Supplier<GUID> function) {
		GUID prev = function.get();
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			long t0 = System.currentTimeMillis();
			GUID guid = function.get();
			long t1 = guid.getMostSignificantBits() >>> 16;
			long t2 = System.currentTimeMillis();
			assertNotNull(guid);
			assertNotEquals(prev, guid);
			assertNotEquals(GUID.NIL, guid);
			assertNotEquals(GUID.MAX, guid);
			assertEquals(7, guid.version());
			assertTrue(t0 <= t1 && t1 <= t2);
			prev = guid;
		}
	}

	@Test
	public void testToUUID() {
		SplittableRandom random = new SplittableRandom(1);
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			final long msb = random.nextLong();
			final long lsb = random.nextLong();
			UUID uuid = (new GUID(msb, lsb)).toUUID();
			assertEquals(msb, uuid.getMostSignificantBits());
			assertEquals(lsb, uuid.getLeastSignificantBits());
		}
	}

	@Test
	public void testToString() {
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			UUID uuid = UUID.randomUUID();
			GUID guid = new GUID(uuid);
			assertEquals(uuid.toString(), guid.toString());
		}
	}

	@Test
	public void testToBytes() {
		SplittableRandom seeder = new SplittableRandom(1);
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {

			byte[] bytes1 = new byte[16];
			(new Random(seeder.nextLong())).nextBytes(bytes1);
			GUID guid0 = new GUID(bytes1);

			byte[] bytes2 = guid0.toBytes();
			for (int j = 0; j < bytes1.length; j++) {
				assertEquals(bytes1[j], bytes2[j]);
			}
		}
	}

	@Test
	public void testHashCode() {

		SplittableRandom seeder = new SplittableRandom(1);
		byte[] bytes = new byte[GUID.GUID_BYTES];

		// invoked on the same object
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			(new Random(seeder.nextLong())).nextBytes(bytes);
			GUID guid1 = new GUID(bytes);
			assertEquals(guid1.hashCode(), guid1.hashCode());
		}

		// invoked on two equal objects
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			(new Random(seeder.nextLong())).nextBytes(bytes);
			GUID guid1 = new GUID(bytes);
			GUID guid2 = new GUID(bytes);
			assertEquals(guid1.hashCode(), guid2.hashCode());
		}
	}

	@Test
	public void testEquals() {

		SplittableRandom seeder = new SplittableRandom(1);
		byte[] bytes = new byte[GUID.GUID_BYTES];

		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {

			(new Random(seeder.nextLong())).nextBytes(bytes);
			GUID guid1 = new GUID(bytes);
			GUID guid2 = new GUID(bytes);
			assertEquals(guid1, guid2);
			assertEquals(guid1.toString(), guid2.toString());
			assertEquals(Arrays.toString(guid1.toBytes()), Arrays.toString(guid2.toBytes()));

			// change all bytes
			for (int j = 0; j < bytes.length; j++) {
				bytes[j]++;
			}
			GUID guid3 = new GUID(bytes);
			assertNotEquals(guid1, guid3);
			assertNotEquals(guid1.toString(), guid3.toString());
			assertNotEquals(Arrays.toString(guid1.toBytes()), Arrays.toString(guid3.toBytes()));
		}
	}

	@Test
	public void testCompareTo() {

		final long zero = 0L;
		SplittableRandom random = new SplittableRandom(1);
		byte[] bytes = new byte[GUID.GUID_BYTES];

		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {

			bytes = ByteBuffer.allocate(16).putLong(random.nextLong()).putLong(random.nextLong()).array();
			GUID guid1 = new GUID(bytes);
			BigInteger number1 = new BigInteger(1, bytes);

			bytes = ByteBuffer.allocate(16).putLong(random.nextLong()).putLong(random.nextLong()).array();
			GUID guid2 = new GUID(bytes);
			GUID guid3 = new GUID(bytes);
			BigInteger number2 = new BigInteger(1, bytes);
			BigInteger number3 = new BigInteger(1, bytes);

			// compare numerically
			assertEquals(number1.compareTo(number2) > 0, guid1.compareTo(guid2) > 0);
			assertEquals(number1.compareTo(number2) < 0, guid1.compareTo(guid2) < 0);
			assertEquals(number2.compareTo(number3) == 0, guid2.compareTo(guid3) == 0);

			// compare lexicographically
			assertEquals(number1.compareTo(number2) > 0, guid1.toString().compareTo(guid2.toString()) > 0);
			assertEquals(number1.compareTo(number2) < 0, guid1.toString().compareTo(guid2.toString()) < 0);
			assertEquals(number2.compareTo(number3) == 0, guid2.toString().compareTo(guid3.toString()) == 0);
		}

		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {

			bytes = ByteBuffer.allocate(16).putLong(zero).putLong(random.nextLong()).array();
			GUID guid1 = new GUID(bytes);
			BigInteger number1 = new BigInteger(1, bytes);

			bytes = ByteBuffer.allocate(16).putLong(zero).putLong(random.nextLong()).array();
			GUID guid2 = new GUID(bytes);
			GUID guid3 = new GUID(bytes);
			BigInteger number2 = new BigInteger(1, bytes);
			BigInteger number3 = new BigInteger(1, bytes);

			// compare numerically
			assertEquals(number1.compareTo(number2) > 0, guid1.compareTo(guid2) > 0);
			assertEquals(number1.compareTo(number2) < 0, guid1.compareTo(guid2) < 0);
			assertEquals(number2.compareTo(number3) == 0, guid2.compareTo(guid3) == 0);

			// compare lexicographically
			assertEquals(number1.compareTo(number2) > 0, guid1.toString().compareTo(guid2.toString()) > 0);
			assertEquals(number1.compareTo(number2) < 0, guid1.toString().compareTo(guid2.toString()) < 0);
			assertEquals(number2.compareTo(number3) == 0, guid2.toString().compareTo(guid3.toString()) == 0);
		}

		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {

			bytes = ByteBuffer.allocate(16).putLong(random.nextLong()).putLong(zero).array();
			GUID guid1 = new GUID(bytes);
			BigInteger number1 = new BigInteger(1, bytes);

			bytes = ByteBuffer.allocate(16).putLong(random.nextLong()).putLong(zero).array();
			GUID guid2 = new GUID(bytes);
			GUID guid3 = new GUID(bytes);
			BigInteger number2 = new BigInteger(1, bytes);
			BigInteger number3 = new BigInteger(1, bytes);

			// compare numerically
			assertEquals(number1.compareTo(number2) > 0, guid1.compareTo(guid2) > 0);
			assertEquals(number1.compareTo(number2) < 0, guid1.compareTo(guid2) < 0);
			assertEquals(number2.compareTo(number3) == 0, guid2.compareTo(guid3) == 0);

			// compare lexicographically
			assertEquals(number1.compareTo(number2) > 0, guid1.toString().compareTo(guid2.toString()) > 0);
			assertEquals(number1.compareTo(number2) < 0, guid1.toString().compareTo(guid2.toString()) < 0);
			assertEquals(number2.compareTo(number3) == 0, guid2.toString().compareTo(guid3.toString()) == 0);
		}
	}

	@Test
	public void testProtectedGregorian() {
		testProtectedGregorianInstant(Instant.parse("1582-10-15T00:00:00.000Z").toEpochMilli());
		testProtectedGregorianInstant(Instant.parse("1970-01-01T00:00:00.000Z").toEpochMilli());
		testProtectedGregorianInstant(Instant.parse("1955-11-12T06:38:00.000Z").toEpochMilli());
		testProtectedGregorianInstant(Instant.parse("1985-10-26T09:00:00.000Z").toEpochMilli());
		testProtectedGregorianInstant(Instant.parse("2015-10-21T07:28:00.000Z").toEpochMilli());
	}

	private static void testProtectedGregorianInstant(final long time) {
		long epoch = 12219292800L; // seconds since "1582-10-15T00:00:00.000Z"
		assertEquals(GUID.gregorian(time) / 10_000_000L, epoch + Instant.ofEpochMilli(time).getEpochSecond());
	}

	@Test
	public void testProtectedHash() {
		String name = "THIS IS A TEST";
		GUID guid = GUID.v3(null, name);
		UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
		assertEquals(guid.toString(), uuid.toString());
	}

	@Test
	public void testProtectedHasher() {
		assertNotNull(GUID.hasher("MD5"));
		assertNotNull(GUID.hasher("SHA-1"));
		try {
			GUID.hasher("AAA");
			fail("Should throw exception");
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	@Test
	public void testProtectedVersion() {
		for (int i = 0; i < 32; i++) {
			GUID guid = GUID.version(GUID.NIL.getMostSignificantBits(), GUID.NIL.getLeastSignificantBits(), i);
			assertEquals(guid.getMostSignificantBits() & 0xffffffffffff0fffL, 0L);
			assertEquals(guid.getLeastSignificantBits() & 0x3fffffffffffffffL, 0L);
			assertEquals(guid.version(), i % 16);
		}
	}

	@Test
	public void testValid() {
		testValidator((String string) -> {
			return GUID.valid(string);
		});
	}

	@Test
	public void testParser() {

		// canonical format with 36 characters
		for (int i = 0; i < DEFAULT_LOOP_MAX; i++) {
			String string = UUID.randomUUID().toString();
			GUID guid = GUID.Parser.parse(string);
			assertEquals(string, guid.toString());
		}

		testValidator((String string) -> {
			try {
				return GUID.Parser.parse(string) != null;
			} catch (IllegalArgumentException e) {
				return false;
			}
		});

		// compare with regular expression
		testValidator((String string) -> {
			boolean expected = (string != null && PATTERN.matcher(string).matches());
			boolean result = GUID.Parser.valid(string);
			assertEquals(expected, result);
			return expected && result;
		});
	}

	public void testValidator(Function<String, Boolean> validator) {

		String guid = null;
		assertFalse("Null GUID string should be invalid.", validator.apply(guid));

		guid = "";
		assertFalse("GUID with empty string should be invalid.", validator.apply(guid));

		guid = "0-0-0-0-0";
		assertFalse("GUID 0-0-0-0-0 should be invalid.", validator.apply(guid));

		guid = "this should be invalid";
		assertFalse("GUID 'this should be invalid' should be invalid.", validator.apply(guid));

		guid = "01234567-89ab-4def-abcd-ef0123456789";
		assertTrue("GUID with length equals to 36 should be valid.", validator.apply(guid));

		guid = "01234567-89ab-4def-abcdef01-23456789";
		assertFalse("GUID with length 36 and hyphen in wrong position should be invalid.", validator.apply(guid));

		guid = "01234567-89ab-4def-abcd-ef01-3456789";
		assertFalse("GUID with length equals to 36 with an extra hyphen should be invalid.", validator.apply(guid));

		guid = "01234567-89ab-4def-abcddef0123456789";
		assertFalse("GUID with length equals to 36 with a missing hyphen should be invalid.", validator.apply(guid));

		guid = "0123456789ab4defabcdef0123456789";
		assertFalse("GUID without hyphens should be invalid.", validator.apply(guid));

		guid = UUID.randomUUID().toString();
		assertTrue("GUID generated by UUID.randomUUID() should be valid.", validator.apply(guid));

		guid = "01234567-89ab-4def-abcd-ef0123456789";
		assertTrue("GUID in lower case should be valid.", validator.apply(guid));

		guid = "01234567-89AB-4DEF-ABCD-EF0123456789";
		assertTrue("GUID in upper case should valid.", validator.apply(guid));

		guid = "01234567-89ab-4DEF-abcd-EF0123456789";
		assertTrue("GUID in upper and lower case should valid.", validator.apply(guid));

		guid = "01234567-89ab-4def-abcd-SOPQRSTUVXYZ";
		assertFalse("GUID string with non hexadecimal chars should be invalid.", validator.apply(guid));

		guid = "01234567-89ab-4def-!@#$-ef0123456789";
		assertFalse("GUID string non alphanumeric chars should be invalid.", validator.apply(guid));
	}
}
