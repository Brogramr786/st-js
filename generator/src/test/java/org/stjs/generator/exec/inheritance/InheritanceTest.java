package org.stjs.generator.exec.inheritance;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.stjs.generator.MultipleFileGenerationException;
import org.stjs.generator.utils.AbstractStjsTest;

public class InheritanceTest extends AbstractStjsTest {

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Test
	public void testReenteringSuper() {
		assertEquals(4.0, executeAndReturnNumber(Inheritance1.class), 0);
	}

	@Test
	public void testCopyPrototype() {
		assertEquals(2.0, executeAndReturnNumber(Inheritance2.class), 0);
	}

	/**
	 * Verifies that .js files generated with ST-JS 1.2 (and therefore using the 1.2 version of stjs.extends) will still
	 * run correctly when executed with the current version.
	 */
	@Test
	public void testExtendCompatibilityWith12() {
		assertEquals("4 2 WXYZ undefined", execute("src/test/resources/javascript/Inheritance-generated-with-1.2.js"));
	}

	@Test
	public void testInstanceofSuperClass() {
		assertEquals(1.0, executeAndReturnNumber(Inheritance3.class), 0);
	}

	@Test
	public void testInstanceofInterface() {
		expectedEx.expect(MultipleFileGenerationException.class);
		expectedEx.expectMessage("You cannot call instanceof on an interface");

		assertEquals(1.0, executeAndReturnNumber(Inheritance4.class), 0);
	}

	@Test
	public void testInstanceofSuperInterface() {
		expectedEx.expect(MultipleFileGenerationException.class);
		expectedEx.expectMessage("You cannot call instanceof on an interface");

		assertEquals(1.0, executeAndReturnNumber(Inheritance5.class), 0);
	}
}
