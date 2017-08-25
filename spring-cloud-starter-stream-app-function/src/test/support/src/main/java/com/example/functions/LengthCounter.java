package com.example.functions;

import java.util.function.Function;

/**
 * @author Eric Bottard
 */
public class LengthCounter implements Function<String, Integer> {

	@Override
	public Integer apply(String string) {
		return string.length();
	}
}
