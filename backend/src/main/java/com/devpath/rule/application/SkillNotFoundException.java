package com.devpath.rule.application;

public class SkillNotFoundException extends RuntimeException {
    public SkillNotFoundException() { super("The current skill assessment was not found"); }
}
