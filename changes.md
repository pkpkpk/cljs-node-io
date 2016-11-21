### 0.2.0
 + remove wrapper around cljs-node-io.file/File.
  - File now takes a path only..is not polymorphic like java.io.File
  - use cljs-node-io.core/file
 + various type + docstring fixes

### 0.3.0
 + removed andare; started clashing with figwheel
 + add wire-report
 + add async ns, converts nodejs event emitters into core.async message passers