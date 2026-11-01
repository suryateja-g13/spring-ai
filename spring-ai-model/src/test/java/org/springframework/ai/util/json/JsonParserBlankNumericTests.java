/*
 * Copyright 2023-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.util.json;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonParserBlankNumericTests {

	static Stream<Class<?>> numericTypes() {
		return Stream.of(Byte.class, byte.class, Short.class, short.class, Integer.class, int.class,
				Long.class, long.class, Float.class, float.class, Double.class, double.class);
	}

	static Stream<Arguments> blankNumericInputs() {
		return numericTypes().flatMap(type -> Stream.of("", " ", "\t\n", "\u2003")
			.map(blank -> Arguments.of(type, blank)));
	}

	@ParameterizedTest
	@MethodSource("blankNumericInputs")
	void blankNumericInputHasActionableDiagnostic(Class<?> type, String input) {
		assertThatThrownBy(() -> JsonParser.toTypedObject(input, type))
			.isExactlyInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Cannot convert blank or empty string to numeric type");
	}

	@ParameterizedTest
	@MethodSource("numericTypes")
	void validNumericStringsRemainAccepted(Class<?> type) {
		Object result = JsonParser.toTypedObject("42", type);
		assertThat(result).isInstanceOf(Number.class);
		assertThat(((Number) result).doubleValue()).isEqualTo(42);
	}

	static Stream<Arguments> nonNumericBlankInputs() {
		return Stream.of(Arguments.of("", String.class, ""), Arguments.of(" ", String.class, " "),
				Arguments.of("", Boolean.class, false), Arguments.of("", boolean.class, false));
	}

	@ParameterizedTest
	@MethodSource("nonNumericBlankInputs")
	void blankGuardDoesNotChangeNonNumericTypes(String input, Class<?> type, Object expected) {
		assertThat(JsonParser.toTypedObject(input, type)).isEqualTo(expected);
	}

	@Test
	void exactDecimalIntegerRemainsAccepted() {
		assertThat(JsonParser.toTypedObject("42.0", int.class)).isEqualTo(42);
	}

	@Test
	void exactDecimalLongRemainsAccepted() {
		assertThat(JsonParser.toTypedObject("42.0", Long.class)).isEqualTo(42L);
	}

	@Test
	void validBooleanRemainsAccepted() {
		assertThat(JsonParser.toTypedObject("true", Boolean.class)).isEqualTo(true);
	}

	@Test
	void validEnumRemainsAccepted() {
		assertThat(JsonParser.toTypedObject("ACTIVE", Mode.class)).isEqualTo(Mode.ACTIVE);
	}

	@Test
	void blankEnumKeepsItsOwnDiagnostic() {
		assertThatThrownBy(() -> JsonParser.toTypedObject("", Mode.class))
			.isExactlyInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("No enum constant");
	}

	@Test
	void byteOverflowIsNotMisclassifiedAsBlankInput() {
		assertThatThrownBy(() -> JsonParser.toTypedObject("128", Byte.class))
			.isExactlyInstanceOf(NumberFormatException.class);
	}

	@Test
	void fractionalIntegerIsNotSilentlyTruncated() {
		assertThatThrownBy(() -> JsonParser.toTypedObject("1.5", int.class))
			.isExactlyInstanceOf(ArithmeticException.class);
	}

	enum Mode {

		ACTIVE

	}

}
