package org.stjs.generator.writer.typedarrays;

public class Float32ArrayInit2 {
	public void testInit() {
		float[] arr = new float[]{ 1.2f, 2f, 3, .4f, a() };
	}

	public int a() {
		return 42;
	}
}
