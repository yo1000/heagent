package com.yo1000.heagent;

public interface Parser<T> {
    T parse(String s, T defaultValue);
}
