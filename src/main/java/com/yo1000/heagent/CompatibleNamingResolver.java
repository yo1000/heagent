package com.yo1000.heagent;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CompatibleNamingResolver {
    private String kebabLowerCase;

    public CompatibleNamingResolver(String kebabLowerCase) {
        if (kebabLowerCase == null) throw new NullPointerException("kebabLowerCase is null");
        if (kebabLowerCase.isEmpty()) throw new IllegalArgumentException("kebabLowerCase is empty");

        this.kebabLowerCase = kebabLowerCase;
    }

    public String toKebabLowerCase() {
        return kebabLowerCase;
    }

    public String toSnakeUpperCase() {
        return kebabLowerCase.replaceAll("-", "_").toUpperCase();
    }

    public String toCamelCase() {
        String[] tokens = kebabLowerCase.split("-");
        if (tokens.length == 1) return kebabLowerCase;

        return tokens[0] + Arrays.stream(Arrays.copyOfRange(tokens, 1, tokens.length))
                .map(s -> {
                    Matcher matcher = Pattern.compile("^(.)(.*)$").matcher(s);
                    if(!matcher.find()) return "";

                    String result = matcher.group(1).toUpperCase();
                    if (matcher.groupCount() >= 2) {
                        result += matcher.group(2);
                    }
                    return result;
                })
                .collect(Collectors.joining());
    }
}
