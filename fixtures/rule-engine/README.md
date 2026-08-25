# Rule Engine Fixtures

`baseline-v1.json` is the first deterministic golden dataset for the versioned repository baseline rule set.
`baseline-v2.json` covers the approved active rule set, including database, architecture, and DevOps categories.
It contains the exact category/rule weights, formula parameters, normalized facts, and expected scores used for
regression verification. Changes to those values require a new rule-set version and a new fixture; historical
fixtures remain immutable.
