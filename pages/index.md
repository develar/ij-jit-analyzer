# Run Name Explanation

* `(N)` is a placeholder for a run number, like `(1)`. It typically ranges up to 3.
* `ccN` means the VM option `-XX:CICompilerCount=N` was used. So `cc2` means `-XX:CICompilerCount=2` was used.
* `tc` indicates the absence of the VM option `-XX:-TieredCompilation`. Note that [Tiered Compilation](https://stackoverflow.com/a/38721975) is enabled by default.
* `rcsN` means the VM option `-XX:ReservedCodeCacheSize=N` was used. So `rcs512` means `-XX:CICompilerCount=512` was used.

In each case, we conduct three runs to verify the JIT behavior's uniformity.

In certain sections, we have multiple charts, for example, illustrating the difference between code cache sizes of 512 and 240 for Java version 21. These charts aim to reduce the number of data series.