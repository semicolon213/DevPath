package com.devpath.rule.application;

public class RuleEvaluationNotFoundException extends RuntimeException {
    public RuleEvaluationNotFoundException() { super("The rule evaluation was not found"); }
}
