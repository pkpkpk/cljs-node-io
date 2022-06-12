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

### 0.5.0
 + add `cljs-node-io.fs/watch` & `cljs-node-io.fs/watchFile`. Both have [platform specific usage details](https://nodejs.org/api/fs.html#fs_caveats)
 + added `cljs-node-io.async/mux`. Instead of having a million infinite go-loops/go-procs everywhere , shrink them down into one extensible readloop.
 + stream->ch functions and IChildProcess' now have channel close semantics. When their underlying resources emit shutdown events, their associated ports will close.

### 1.0.0
 + Breaking Change: Wrapped ChildProcess instances... async spawn + fork, have been move to the `cljs-node-io.spawn` namespace and refactored to be more obvious (though opinionated) in their use.
 + people keep searching for "readline" so I added a primitive `cljs-node-io.fs/readline`.
 + fix watcher to use key
 + misc decrufting

### 2.0.332
+ `IOFactory`, `io/reader`, `io/writer`, `io/input-stream`, `io/output-stream` etc all return native streams whenever they can
+ `io/readable` & `io/writable` defer to IOFactory but are friendlier names for node devs. Additionally, `io/readable` will create a stream an iterables
+ `io/copy` assumes args are files or string file-paths and uses native copyFileSync. It returns nil and throws on error. `io/acopy` uses IOFactory to create and then pipe streams, returns ch<?err>
+ `io/file-seq` now returns files, was string names previously
+ `fs/stat` & variants now return edn automatically, not fs.stat
+ `fs/lchmod` removed, deprecated in native fs
+ notable new stuff in `cljs-node-io.fs`
  - `fs/exists?` instead of `fs/fexists?`
  - `fs/crawl` run a fn on every entry in a directory
  - `fs/lock-file` provides lock-files for simple advisory locking
  - `fs/rm-rf` recursively delete a dir and ignore if missing.
+ externs are packaged in the jar as `cljs_node_io.ext.js`
