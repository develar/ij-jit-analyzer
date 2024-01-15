## Run Name Glossary

* `(N)` represents the number of a run, for example, `(1)`. The number generally goes up to 3.
* `ccN` signifies that the VM option `-XX:CICompilerCount=N` was employed. For instance, `cc2` indicates that `-XX:CICompilerCount=2` was used.
* `tc` denotes that the VM option `-XX:-TieredCompilation` was not used. Note that [Tiered Compilation](https://stackoverflow.com/a/38721975) is enabled by default.

For each scenario, three runs are performed to ensure that JIT behavior is consistent.

