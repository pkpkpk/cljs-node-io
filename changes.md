### 0.2.0
 + remove wrapper around cljs-node-io.file/File.
  - File now takes a path only..is not polymorphic like java.io.File
  - use cljs-node-io.core/file
 + various type + docstring fixes