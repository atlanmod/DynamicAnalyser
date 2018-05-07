package com.tblf.SimpleProject;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {
    
	@Test
	public void testApp() {
		new App().method();
	}

	@Test
	public void testApp2() {
		new App("String").method();
	}
}
