package com.devpath.rule.application;

public class SkillMatrixNotFoundException extends RuntimeException {
    public SkillMatrixNotFoundException() { super("The skill matrix was not found"); }
}
