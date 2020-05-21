package com.yo1000.heagent;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class ParameterLoader {
    private static final String ARGS_ENTRY_SEPARATOR = ",";
    private static final String ARGS_ENTRY_ITEM_SEPARATOR = "=";
    private static final String ARGS_ENTRY_ITEM_FORMAT = "[^=]+=[^=]+";
    private static final int ARGS_ENTRY_ITEM_KEY_INDEX = 0;
    private static final int ARGS_ENTRY_ITEM_VALUE_INDEX = 1;

    private Map<String, String> agentArgs;
    private String prefix = "";

    public ParameterLoader(String agentArgs) {
        this(agentArgs, null);
    }

    public ParameterLoader(String agentArgs, String prefix) {
        this.agentArgs = parseAgentArgs(agentArgs);
        if (prefix != null && !prefix.isEmpty()) this.prefix = prefix;
    }

    <T> T loadByName(String kebakCaseName, T defaultValue, Parser<T> parser) {
        CompatibleNamingResolver namingResolver = new CompatibleNamingResolver(prefix + kebakCaseName);

        T v = loadFromEnvVars(namingResolver.toSnakeUpperCase(), defaultValue, parser);
        v = loadFromSystemProps(namingResolver.toKebabLowerCase(), v, parser);
        return loadFromAgentArgs(namingResolver.toCamelCase(), v, parser);
    }

    private Map<String, String> parseAgentArgs(String agentArgs) {
        if (agentArgs == null || agentArgs.trim().isEmpty()) return Collections.emptyMap();

        String normalizedArgs = agentArgs.replaceAll("\\s*", "");
        return Arrays.stream(normalizedArgs.split(ARGS_ENTRY_SEPARATOR))
                .filter(s -> s.matches(ARGS_ENTRY_ITEM_FORMAT))
                .map(s -> s.split(ARGS_ENTRY_ITEM_SEPARATOR))
                .collect(Collectors.toMap(
                        keyValue -> keyValue[ARGS_ENTRY_ITEM_KEY_INDEX],
                        keyValue -> keyValue[ARGS_ENTRY_ITEM_VALUE_INDEX])
                );
    }

    <T> T loadFromEnvVars(String name, T defaultValue, Parser<T> parser) {
        String v = System.getenv(name);
        if (v == null) return defaultValue;
        return parser.parse(v, defaultValue);
    }

    <T> T loadFromSystemProps(String name, T defaultValue, Parser<T> parser) {
        String v = System.getProperty(name);
        if (v == null) return defaultValue;
        return parser.parse(v, defaultValue);
    }

    <T> T loadFromAgentArgs(String name, T defaultValue, Parser<T> parser) {
        String v = agentArgs.get(name);
        if (v == null) return defaultValue;
        return parser.parse(v, defaultValue);
    }
}
