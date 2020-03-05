Description: Integration tests for variables

Meta:
    @group variables

Scenario: Verify default variables
Then `1` is = `${key:1}`
Then `2` is = `${list[0]:2}`
Then `3` is = `${map.key:3}`
Then `4` is = `${list[0].key:4}`

Scenario: Verify batch scope has higher priority than global scope
Then `${scope-priority-check}` is equal to `should be batch`

Scenario: Verify step for saving examples table to variable as list of maps
When I initialize SCENARIO variable `testMap` with values:
|key1  |key2  |
|value1|value2|
Then `value1` is = `${testMap[0].key1}`
Then `value2` is = `${testMap[0].key2}`

Scenario: Verify ability to use variables with names containing special characters
When I initialize the scenario variable `vAr` with value `vAl`
When I initialize the scenario variable `v.ar` with value `v.al`
When I initialize the scenario variable `var[0]` with value `val[0]`
When I initialize the scenario variable `v[0].ar` with value `v[0].al`
When I initialize the scenario variable `v.ar[0]` with value `v.al[0]`
When I initialize the scenario variable `v:ar` with value `v:al`
Then `${vAr}` is equal to `vAl`
Then `${v.ar}` is equal to `v.al`
Then `${var[0]}` is equal to `val[0]`
Then `${v[0].ar}` is equal to `v[0].al`
Then `${v.ar[0]}` is equal to `v.al[0]`
Then `${v:ar}` is equal to `v:al`
