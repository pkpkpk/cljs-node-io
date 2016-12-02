### 0.2.0
 + remove wrapper around cljs-node-io.file/File.
  - File now takes a path only..is not polymorphic like java.io.File
  - use cljs-node-io.core/file
 + various type + docstring fixes

### 0.3.0
 + removed andare; started clashing with figwheel
 + add wire-report
 + add async ns, converts nodejs event emitters into core.async message passers

### 0.4.0
 + `cljs-node-io.async/go-proc` will read from anything with a core.async readport implementation
 + converted all async functions in `cljs-node-io.fs` to return promise-chans, like they should have been to begin with
 + `cljs-node-io.core/copy` now returns a promise-chan that closes when it finishes writing.
   - You can ignore this at the repl or use it to chain a operations.
   - Copy will still throw; if you need fine grained control use the underlying streams manually.
 + added `cljs-node-io.proc` ns. The ChildProcess object itself is subject to change but this is a __super useful__ bunch of functions