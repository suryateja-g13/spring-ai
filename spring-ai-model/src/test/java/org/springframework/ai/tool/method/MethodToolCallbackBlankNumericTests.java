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

package org.springframework.ai.tool.method;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.util.json.JsonParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodToolCallbackBlankNumericTests {

	static Stream<Arguments> blankInputs() {
		return Stream.of(false, true).flatMap(primitive -> Stream.of("", " ", "\t\n", "\u2003")
			.map(input -> Arguments.of(primitive, input)));
	}

	static Stream<Arguments> validInputs() {
		return Stream.of(false, true).flatMap(primitive -> Stream.of(0L, 42L)
			.map(input -> Arguments.of(primitive, input)));
	}

	@ParameterizedTest
	@MethodSource("blankInputs")
	void blankLongInputIsWrappedBeforeToolExecution(boolean primitive, String input) throws Exception {
		var tool = new IdTool();
		var callback = callback(tool, primitive);
		String jsonInput = JsonParser.toJson(Map.of("id", input));

		assertThatThrownBy(() -> callback.call(jsonInput))
			.isInstanceOf(ToolExecutionException.class)
			.hasCauseExactlyInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Cannot convert blank or empty string to numeric type 'Long'");
		assertThat(tool.executions).isZero();
	}

	@ParameterizedTest
	@MethodSource("validInputs")
	void validLongInputExecutesExactlyOnce(boolean primitive, long input) throws Exception {
		var tool = new IdTool();
		var callback = callback(tool, primitive);

		assertThat(callback.call("{\"id\":" + input + "}")).isEqualTo(Long.toString(input));
		assertThat(tool.executions).isEqualTo(1);
	}

	private MethodToolCallback callback(IdTool tool, boolean primitive) throws Exception {
		var method = IdTool.class.getMethod(primitive ? "primitive" : "boxed", primitive ? long.class : Long.class);
		return MethodToolCallback.builder()
			.toolDefinition(ToolDefinition.builder().name("lookup").description("Look up an id").inputSchema("{}").build())
			.toolObject(tool).toolMethod(method).build();
	}

	public static class IdTool {

		int executions;

		public Long boxed(Long id) {
			this.executions++;
			return id;
		}

		public long primitive(long id) {
			this.executions++;
			return id;
		}

	}

}
