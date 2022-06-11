(ns cljs-node-io.fs "A wrapper around node's fs module."
  (:refer-clojure :exclude [exists?])
  (:require-macros [cljs-node-io.macros :refer [try-true with-chan with-bool-chan with-promise]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! take! close! promise-chan chan]]
            [cljs.core.async.impl.protocols :as impl :refer [Channel]]))

(def fs (js/require "fs"))
(def path (js/require "path"))
(def ^{:doc "@type {!string}"} sep (.-sep path))

(defn- stat->clj
  "Convert a fs.Stats object to edn. Function are swapped out for their return values.
   @param {!fs.Stats} st
   @return {!IMap}"
  [st]
  (let [ks (goog.object.getKeys st)
        vs (goog.object.getValues st)]
    (into {}
      (comp
        (remove #(= (nth % 0) "_checkModeProperty"))
        (map (fn [[k v]] [(keyword k) (if (fn? v) (.apply v st) v)])))
      (map vector ks vs))))

(defn stat
  "Synchronous stat
   @param {!(string|Buffer|URL)} pathstring
   @return {!IMap} file stats object converted to edn"
  [pathstr]
  (stat->clj (.statSync fs pathstr)))

(defn astat
  "Asynchronous stat
   @param {!(string|Buffer|URL)} pathstr
   @return {!Channel} yielding [?err ?edn-stats]"
  [pathstr]
  (with-promise out
    (.stat fs pathstr
       (fn [err stats]
         (put! out (if err [err] [nil (stat->clj stats)]))))))

(defn lstat
  "Synchronous lstat identical to stat(), except that if path is a symbolic link,
   then the link itself is stat-ed, not the file that it refers to
   @param {!(string|Buffer|URL)} pathstr
   @return {!IMap} file stats object converted to edn"
  [pathstr]
  (stat->clj (.lstatSync fs pathstr)))

(defn alstat
  "Asynchronous lstat
   @param {!(string|Buffer|URL)} pathstr
   @return {!Channel} yielding [?err ?edn-stats]"
  [pathstr]
  (with-promise out
    (.lstat fs pathstr
       (fn [err stats]
         (put! out (if err [err] [nil (stat->clj stats)]))))))

(defn- bita->int
  "@param {!Array<!Number>} bita :: an array of 1s an 0s
   @return {!Number} integer"
  [bita]
  (js/parseInt (.join bita "") 2))

(defn- stat->perm-bita
  "@param {!(fs.Stat|IMap)} s :: a fs.Stats object (or as edn)
   @return {!Array<Number>}"
  [s]
  (let [mode (or (get s :mode)
                 (goog.object.get s "mode"))
        ownr (bit-and mode 256)
        ownw (bit-and mode 128)
        ownx (bit-and mode 64)
        grpr (bit-and mode 32)
        grpw (bit-and mode 16)
        grpx (bit-and mode 8)
        othr (bit-and mode 4)
        othw (bit-and mode 2)
        othx (bit-and mode 1)
        a #js [ownr ownw ownx grpr grpw grpx othr othw othx]]
    (amap a i res (if-not (zero? (aget a i)) 1 0))))

(defn permissions
  "@param {!(fs.Stat|IMap)} s :: a fs.Stats object (or as edn)
   @return {!Number}"
  [st] (-> st stat->perm-bita bita->int))

(defn gid-uid
  "@return {!IMap}"
  []{:gid (.getgid js/process) :uid (.getuid js/process)})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; predicates/

(defn ^boolean hidden?
  "@param {!(string|Buffer|URL)} pathstr
   @return {!boolean} is the file hidden (unix only)"
  [pathstr]
  (.test (js/RegExp. "(^|\\/)\\.[^\\/\\.]" ) pathstr))

(defn ^boolean dir?
  "@param {!(string|Buffer|URL)} pathstring
   @return {!boolean} iff abstract pathname exists and is a directory"
  [pathstr]
  (try
    (.isDirectory (.statSync fs pathstr))
    (catch js/Error _ false)))

(defn adir?
  "Asynchronous directory predicate.
   @param {!(string|Buffer|URL)} pathstr
   @return {!Channel} yielding boolean"
  [pathstr]
  (let [out (promise-chan)]
    (.stat fs pathstr
      (fn [err stats]
        (put! out
          (if-not err
            (.isDirectory stats)
            false))))
    out))

(defn ^boolean file?
  "Synchronous file predicate
   @param {!(string|Buffer|URL)} pathstring
   @return {!boolean} iff abstract pathname exists and is a file"
  [pathstr]
  (try
    (.isFile (.lstatSync fs pathstr))
    (catch js/Error _ false)))

(defn afile?
  "Asynchronous file predicate.
   @param {!(string|Buffer|URL)} pathstr
   @return {!Channel} yielding boolean"
  [pathstr]
  (let [out (promise-chan)]
    (.lstat fs pathstr
      (fn [err stats]
        (put! out (if-not err (.isFile stats) false))))
    out))

(defn ^boolean absolute?
  "@param {!string} pathstr :: path to test
   @return {!boolean} is pathstr an absolute path"
  [pathstr]
  (assert (string? pathstr))
  (path.isAbsolute pathstr))

(defn ^boolean exists?
  "Synchronously test if a file or directory exists
   @param {!(string|Buffer|URL)} pathstr :: file path to test
   @return {!boolean}"
  [pathstr]
  (try-true (.accessSync fs pathstr (.-F_OK fs))))

(def ^{:doc "@deprecated"} fexists? exists?)

(defn aexists?
  "Asynchronously test if a file or directory exists
   @param {!(string|Buffer|URL)} pathstr
   @return {!Channel} yielding boolean"
  [pathstr]
  (with-bool-chan (.access fs pathstr (.-F_OK fs))))

(def ^{:doc "@deprecated"} afexists? aexists?)

(defn ^boolean readable?
  "Synchronously test if a file is readable to the process
   @param {!(string|Buffer|URL)} pathstr :: path to test for process read permission
   @return {!boolean}"
  [pathstr]
  (try-true (.accessSync fs pathstr (.-R_OK fs))))

(defn areadable?
  "Asynchronously test if a file is readable to the process
   @param {!(string|Buffer|URL)} pathstr
   @return {!Channel} yielding boolean"
  [pathstr]
  (with-bool-chan (.access fs pathstr (.-R_OK fs))))

(defn ^boolean writable?
  "Synchronously test if a file is writable to the process
   @param {!(string|Buffer|URL)} pathstr :: path to test for process write permission
   @return {!boolean}"
  [pathstr]
  (try-true (.accessSync fs pathstr (.-W_OK fs))))

(defn awritable?
  "Asynchronously test if a file is writable to the process
   @param {!(string|Buffer|URL)} pathstr :: path to test for process write permission
   @return {!Channel} yielding boolean"
  [pathstr]
  (with-bool-chan (.access fs pathstr (.-W_OK fs))))

(defn ^boolean executable?
  "@param {!(string|Buffer|URL)} pathstr :: path to test for process executable permission
   @return {!boolean}"
  [pathstr]
  (if-not (= "win32" (.-platform js/process))
    (try-true (.accessSync fs pathstr (.-X_OK fs)))
    (throw (js/Error "Testing if a file is executable has no effect on Windows"))))

(defn aexecutable?
  "Asynchronously test if a file is executable to the process
   @param {!(string|Buffer|URL)} pathstr :: path to test for process execute permission
   @return {!Channel} yielding boolean"
  [pathstr]
  (if-not (= "win32" (.-platform js/process))
    (with-bool-chan (.access fs pathstr (.-X_OK fs)))
    (throw (js/Error "Testing if a file is executable has no effect on Windows"))))

(defn ^boolean symlink?
  "Synchronous test for symbolic link
   @param {!(string|Buffer|URL)} pathstr :: path to test
   @return {!boolean}"
  [pathstr]
  (let [stats (try (lstat pathstr) (catch js/Error e false))]
    (if-not stats
      false
      (.isSymbolicLink stats))))

(defn asymlink?
  "Asynchronously test if path is a symbolic link
   @param {!(string|Buffer|URL)} pathstr :: path to test
   @return {!Channel} yielding boolean"
  [pathstr]
  (let [c (promise-chan)
        stat-ch (alstat pathstr)]
    (take! stat-ch
      (fn [[err stats]]
        (put! c (if-not err (.isSymbolicLink stats) false))))
    c))

;; /predicates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; path utilities + Reads

(defn dirname
  "@param {!string} pathstring :: path to get parent of
   @return {!string} the parent directory"
  [pathstring]
  (assert (string? pathstring))
  (.dirname path pathstring))

(defn basename
  "@return {!string}"
  ([pathstring]
   (assert (string? pathstring))
   (.basename path pathstring))
  ([pathstring ext]
   (assert (string? pathstring))
   (.basename path pathstring ext)))

(defn resolve-path
  "@return {!string}"
  [& paths]
  (assert (every? string? paths))
  (.apply (.-resolve path) nil (apply array paths)))

(defn normalize-path
  "@param {!string} pathstring :: pathstring to normalize
   @return {!string}"
  [pathstring]
  (assert (string? pathstring))
  (.normalize path pathstring))

(defn ext
  "@param {!string} pathstring :: file to get extension from
   @return {!string}"
  [pathstring]
  (assert (string? pathstring))
  (.extname path pathstring))

(defn realpath
  "Synchronous realpath
   Computes the canonical pathname by resolving ., .., and symbolic links.
   {@link https://nodejs.org/api/fs.html#fsrealpathsyncpath-options}
   @param {!(string|Buffer|URL)} pathstr
   @return {!string} resolved path"
  [pathstr]
  (.realpathSync fs pathstr))

(defn arealpath
  "Asynchronous realpath.
   Computes the canonical pathname by resolving ., .., and symbolic links.
   {@link https://nodejs.org/api/fs.html#fsrealpathpath-options-callback}
   @param {!(string|Buffer|URL)} pathstr
   @return {!Channel} yielding [?err ?resolvedPathstr]"
  [pathstr]
  (with-chan (.realpath fs pathstr)))

(defn readlink
  "Synchronous readlink.
   Reads the contents of the symbolic link referred to by path.
   {@link https://nodejs.org/api/fs.html#fsreadlinksyncpath-options}
   @param {!(string|Buffer|URL)} pathstr :: the symbolic link to read
   @return {!string} the symbolic link's string value"
  [pathstr]
  (.readlinkSync fs pathstr))

(defn areadlink
  "Asynchronous readlink.
   Reads the contents of the symbolic link referred to by path.
   {@link https://nodejs.org/api/fs.html#fsreadlinkpath-options-callback}
   @param {!(string|Buffer|URL)} pathstr :: the symbolic link to read
   @return {!Channel} yielding [?err ?linkstring]"
  [pathstr]
  (with-chan (.readlink fs pathstr)))

(defn readdir
  "Synchronously reads directory content
   {@link https://nodejs.org/api/fs.html#fsreaddirsyncpath-options}
   @param {!(string|Buffer|URL)} dirpath :: directory path to read
   @param {?IMap} opts :: options
     :encoding (string) -> defaults to 'utf8'
     :withFileTypes (bool) -> return fs.Dirent objects instead of strings. see
       {@link https://nodejs.org/api/fs.html#class-fsdirent}
   @return {!IVector} Vector<strings> representing directory content"
  ([dirpath] (vec (.readdirSync fs dirpath)))
  ([dirpath opts] (vec (.readdirSync fs dirpath (clj->js opts)))))

(defn areaddir
  "Asynchronously reads directory content
   {@link https://nodejs.org/api/fs.html#fsreaddirpath-options-callback}
   @param {!(string|Buffer|URL)} dirpath :: directory path to read
   @param {?IMap} opts :: options
     :encoding (string) -> defaults to 'utf8'
     :withFileTypes (bool) -> return fs.Dirent objects instead of strings. see
       {@link https://nodejs.org/api/fs.html#class-fsdirent}
   @return {!Channel} yielding [?err, ?Vector<string|fs.Dirent>]"
  ([dirpath]
   (with-chan (.readdir fs dirpath) vec))
  ([dirpath opts]
   (with-chan (.readdir fs dirpath (clj->js opts)) vec)))

(defn crawl
  "Synchronous depth-first recursive crawl through a directory tree, calling the supplied
   side-effecting function on every node. This function will throw if your supplied
   f throws on any node.
   @param {!string} root :: where to start the crawl. A file simply returns (f root)
   @param {!function<string>} f :: function called on both files & directories
   returns value of (f top-level-root)"
  [root f]
  (assert (fn? f))
  (if (file? root)
    (f root)
    (do
      (doseq [child (mapv (partial resolve-path root) (readdir root))]
        (crawl child f))
      (f root))))

(defn acrawl
  "Asynchronous depth-first recursive crawl through a directory tree, calling the supplied
   potentially side-effecting function on every node. This function will short
   and return on an error.

   The user supplied function must return a 'result-chan'...a Readport/channel
   yielding [?err, ?ok]. There is no easy way to get the compiler to enforce this!

   @param {!string} root :: where to start the crawl. A file simply returns (af root)
   @param {!function<string>} af :: async function called on both files & directories
   @return {!Channel} yields a short circuited [err] or [nil ok].
   This depends on the user following the result chan conventions."
  [root af]
  (assert (fn? af))
  (go
   (if (<! (afile? root))
     (<! (af root))
     (let [[err names :as res] (<! (areaddir root))]
       (if err
         res
         (or (loop [children (mapv (partial resolve-path root) names)]
               (when (seq children)
                 (let [[err :as res] (<! (acrawl (first children) af))]
                   (if err
                     res
                     (recur (next children))))))
             (<! (af root))))))))

;; /path utilities + reads
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; writes/

(defn chmod
  "Synchronous chmod
   {@link https://nodejs.org/api/fs.html#fsfchmodsyncfd-mode}
   @param {!(string|Buffer|URL)} pathstr
   @param {!Number} mode :: must be an integer
   @return {nil}"
  [pathstr mode]
  (.chmodSync fs pathstr mode))

(defn achmod
  "Asynchronous chmod
   {@link https://nodejs.org/api/fs.html#fschmodpath-mode-callback}
   @param {!(string|Buffer|URL)} pathstr
   @param {!Number} mode :: must be an integer
   @return {!Channel} yielding [?err]"
  [pathstr mode]
  (with-chan (.chmod fs pathstr mode)))

(defn chown
  "Synchronous chown
   {@link https://nodejs.org/api/fs.html#fschownsyncpath-uid-gid}
   @param {!(string|Buffer|URL)} pathstr
   @param {!Number} uid
   @param {!Number} gid
   @return {nil}"
  [pathstr uid gid]
  (.chownSync fs pathstr uid gid))

(defn achown
  "Asynchronous chown
   {@link https://nodejs.org/api/fs.html#fschownpath-uid-gid-callback}
   @param {!(string|Buffer|URL)} pathstr
   @param {!Number} uid
   @param {!Number} gid
   @return {!Channel} yielding [?err]"
  [pathstr uid gid]
  (with-chan (.chown fs pathstr uid gid)))

(defn lchown
  "Synchronous lchown
   {@link https://nodejs.org/api/fs.html#fslchownsyncpath-uid-gid}
   @param {!(string|Buffer|URL)} pathstr
   @param {!Number} uid
   @param {!Number} gid
   @return {nil}"
  [pathstr uid gid]
  (.lchownSync fs pathstr uid gid))

(defn alchown
  "Asynchronous lchown
   {@link https://nodejs.org/api/fs.html#fslchownpath-uid-gid-callback}
   @param {!(string|Buffer|URL)} pathstr
   @param {!Number} uid
   @param {!Number} gid
   @return {!Channel} yielding [?err]"
  [pathstr uid gid]
  (with-chan (.lchown fs pathstr uid gid)))

(defn utimes
  "synchronous utimes
   Change the file system timestamps of the object referenced by path.
   - If the value is NaN or Infinity, the value would get converted to Date.now()
   - numerable strings ie '12235' are converted to numbers
   {@link https://nodejs.org/api/fs.html#fsutimessyncpath-atime-mtime}
   @param {!(string|Buffer|URL)} pathstr
   @param {(string|Number)} atime
   @param {(string|Number)} mtime
   @return {nil}"
  [pathstr atime mtime]
  (.utimesSync fs pathstr atime mtime))

(defn autimes
  "asynchronous utimes.
   Change the file system timestamps of the object referenced by path.
   - If the value is NaN or Infinity, the value would get converted to Date.now()
   - numerable strings ie '12235' are converted to numbers
   {@link https://nodejs.org/api/fs.html#fsutimespath-atime-mtime-callback}
   @param {!(string|Buffer|URL)} pathstr
   @param {(string|Number)} atime
   @param {(string|Number)} mtime
   @return {!Channel} yielding [?err]"
  [pathstr atime mtime]
  (assert (string? pathstr))
  (with-chan (.utimes fs pathstr atime mtime)))

(defn lutimes
  "synchronous lutimes
   - same as utimes but symbolic links are not derefed
   - If the value is NaN or Infinity, the value would get converted to Date.now()
   - numerable strings ie '12235' are converted to numbers
   {@link https://nodejs.org/api/fs.html#fslutimessyncpath-atime-mtime}
   @param {!(string|Buffer|URL)} pathstr
   @param {(string|Number)} atime
   @param {(string|Number)} mtime
   @return {nil}"
  [pathstr atime mtime]
  (.lutimesSync fs pathstr atime mtime))

(defn alutimes
  "asynchronous lutimes
   - same as utimes but symbolic links are not derefed
   - If the value is NaN or Infinity, the value would get converted to Date.now()
   - numerable strings ie '12235' are converted to numbers
   {@link https://nodejs.org/api/fs.html#fslutimespath-atime-mtime-callback}
   @param {!(string|Buffer|URL)} pathstr
   @param {(string|Number)} atime
   @param {(string|Number)} mtime
   @return {!Channel} yielding [?err]"
  [pathstr atime mtime]
  (with-chan (.lutimes fs pathstr atime mtime)))

(defn link
  "Synchronously creates a new link from the src to dst
   Will not overwrite newpath if it exists.
   {@link https://nodejs.org/api/fs.html#fslinksyncexistingpath-newpath}
   @param {!(string|Buffer|URL)} src
   @param {!(string|Buffer|URL)} dst
   @return {nil}"
  [src dst]
  (.linkSync fs src dst))

(defn alink
  "Asynchronously creates a new link from the src to dst.
   Will not overwrite newpath if it exists.
   {@link https://nodejs.org/api/fs.html#fslinkexistingpath-newpath-callback}
   @param {!(string|Buffer|URL)} src
   @param {!(string|Buffer|URL)} dst
   @return {!Channel} yielding [?err]"
  [src dst]
  (with-chan (.link fs src dst)))

(defn symlink
  "Synchronous symlink.
   {@link https://nodejs.org/api/fs.html#fssymlinksynctarget-path-type}
   @param {!(string|Buffer|URL)} target :: what gets pointed to
   @param {!(string|Buffer|URL)} pathstr :: the new symbolic link that points to target
   @param {?string} link-type ::'file' or 'dir'
   @return {nil}"
  ([target pathstr] (.symlinkSync fs target pathstr))
  ([target pathstr link-type] (.symlinkSync fs target pathstr link-type)))

(defn asymlink
  "Synchronous symlink.
   {@link https://nodejs.org/api/fs.html#fssymlinktarget-path-type-callback}
   @param {!(string|Buffer|URL)} targetstr :: what gets pointed to
   @param {!(string|Buffer|URL)} pathstr :: the new symbolic link that points to target
   @param {?string} link-type ::'file' or 'dir'
   @return {!Channel} yielding [?err]"
  ([targetstr pathstr]
   (with-chan (.symlink fs
    targetstr pathstr)))
  ([targetstr pathstr link-type]
   (with-chan (.symlink fs targetstr pathstr link-type))))

(defn unlink
  "Synchronously unlink a file.
   {@link https://nodejs.org/api/fs.html#fsunlinksyncpath}
   @param {!(string|Buffer|URL)} pathstr :: path of file to unlink
   @return {nil}"
  [pathstr]
  (.unlinkSync fs pathstr))

(defn aunlink
  "Asynchronously unlink a file
   {@link https://nodejs.org/api/fs.html#fsunlinkpath-callback}
   @param {!(string|Buffer|URL)} pathstr :: path of file to unlink
   @return {!Channel} yielding [?err]"
  [pathstr]
  (assert (string? pathstr))
  (with-chan (.unlink fs pathstr)))

(defn mkdir
  "Synchronously create a directory
   {@link https://nodejs.org/api/fs.html#fsmkdirsyncpath-options}
   @param {!(string|Buffer|URL)} pathstr :: path of directory to create
   @param {?IMap} opts
     :recursive (boolean) -> the first directory path created.
     :mode (string|Number)
   @return {nil}"
  ([pathstr] (.mkdirSync fs pathstr))
  ([pathstr opts] (.mkdirSync fs pathstr (clj->js opts))))

(defn amkdir
  "Asynchronously create a directory
   {@link https://nodejs.org/api/fs.html#fsmkdirpath-options-callback}
   @param {!(string|Buffer|URL)} pathstr :: path of directory to create
   @param {?IMap} opts
    :recursive (boolean) -> the first directory path created.
    :mode (string|number)
   @return {!Channel} yielding [?err]"
  ([pathstr] (with-chan (.mkdir fs pathstr)))
  ([pathstr opts] (with-chan (.mkdir fs pathstr (clj->js opts)))))

(defn rmdir
  "Synchronously remove a directory.
   {@link https://nodejs.org/api/fs.html#fsrmdirsyncpath-options}
   @param {!(string|Buffer|URL)} pathstr :: path of directory to remove
   @param {?IMap} opts
     :maxRetries (number)
     :retryDelay (number)
   @return {nil}"
  ([pathstr](.rmdirSync fs pathstr))
  ([pathstr opts](.rmdirSync fs pathstr (clj->js opts))))

(defn armdir
  "Asynchronously remove a directory
   {@link https://nodejs.org/api/fs.html#fsrmdirpath-options-callback}
   @param {!(string|Buffer|URL)} pathstr :: path of directory to remove
   @param {?IMap} opts
     :maxRetries (number)
     :retryDelay (number)
   @return {!Channel} yielding [?err]"
  ([pathstr](with-chan (.rmdir fs pathstr)))
  ([pathstr opts](with-chan (.rmdir fs pathstr (clj->js opts)))))

(defn rm
  "Synchronously delete the file or directory path
   @param {!string} pathstr :: can be file or directory
   @return {nil}"
  [pathstr]
  (if (dir? pathstr)
    (rmdir pathstr)
    (unlink pathstr)))

(defn rm-f
  "Synchronously delete the file or directory path
   Ignores paths that do not exist.
   @param {!string} pathstr :: can be file or directory
   @return {nil}"
  [pathstr]
  (when (exists? pathstr)
    (rm pathstr)))

(defn rm-r
  "Synchronous recursive delete. Throws when doesnt exist
   @param {!(string|Buffer|URL)} pathstr :: file/dir to recursively delete.
   @return {nil}"
  [pathstr]
  (crawl pathstr rm))

(defn rm-rf
  "Synchronous recursive delete. Ignores paths that do not exist.
   @param {!(string|Buffer|URL)} pathstr :: file/dir to recursively delete.
   @return {nil}"
  [pathstr]
  (when (exists? pathstr)
    (crawl pathstr rm)))

(defn arm
  "Asynchronously delete the file or directory path
   @param {!(string|Buffer|URL)} pathstr
   @return {!Channel} yielding [?err]"
  [pathstr]
  (with-promise out
    (take! (adir? pathstr)
      (fn [d?]
        (take! (if d? (armdir pathstr) (aunlink pathstr))
          (fn [ev] (put! out ev)))))))

(defn arm-f
  "Asynchronously delete the file or directory path
   Ignores paths that do not exist.
   @param {!(string|Buffer|URL)} pathstr
   @return {!Channel} yielding [?err]"
  [pathstr]
  (with-promise out
    (take! (aexists? pathstr)
      (fn [yes?]
        (if ^boolean yes?
          (take! (arm pathstr) #(put! out %))
          (put! out [nil]))))))

(defn arm-r
  "Asynchronous recursive delete. Ignores paths that do not exist.
   Crawls in order provided by readdir and makes unlink/rmdir calls sequentially
   after the previous has completed. Breaks on any err which is returned as [err]
   Returns err on does not exist.
   @param {!(string|Buffer|URL)} pathstr
   @return {!Channel} yielding [?err]"
  [pathstr]
  (acrawl pathstr arm))

(defn arm-rf
  "Asynchronous recursive delete. Ignores paths that do not exist.
   Crawls in order provided by readdir and makes unlink/rmdir calls sequentially
   after the previous has completed. Breaks on any err which is returned as [err]
   Returns err on does not exist.
   @param {!(string|Buffer|URL)} pathstr
   @return {!Channel} yielding [?err]"
  [pathstr]
  (with-promise out
    (take! (aexists? pathstr)
      (fn [yes?]
        (if ^boolean yes?
          (take! (acrawl pathstr arm) #(put! out %))
          (put! out [nil]))))))

(defn rename
  "Synchronously rename a file.
   {@link https://nodejs.org/api/fs.html#fsrenamesyncoldpath-newpath}
   @param {!(string|Buffer|URL)} oldpathstr :: file to rename
   @param {!(string|Buffer|URL)} newpathstr :: what to rename it to
   @return {nil}"
  [oldpathstr newpathstr]
  (.renameSync fs oldpathstr newpathstr))

(defn arename
  "Asynchronously rename a file
   {@link https://nodejs.org/api/fs.html#fsrenameoldpath-newpath-callback}
   @param {!(string|Buffer|URL)} oldpathstr :: file to rename
   @param {!(string|Buffer|URL)} newpathstr :: what to rename it to
   @return {!Channel} yielding [?err]"
  [oldpathstr newpathstr]
  (with-chan (.rename fs oldpathstr newpathstr)))

(defn truncate
  "Synchronous truncate
   {@link https://nodejs.org/api/fs.html#fstruncatesyncpath-len}
   @param {!(string|Buffer|URL)} pathstr
   @param {?number} length
   @return {nil}"
  ([pathstr]
   (.truncateSync fs pathstr))
  ([pathstr length]
   (.truncateSync fs pathstr length)))

(defn atruncate
  "Asynchronous truncate
   {@link https://nodejs.org/api/fs.html#fstruncatepath-len-callback}
   @param {!(string|Buffer|URL)} pathstr
   @param {?number} len
   @return {!Channel} yielding [?err]"
  ([pathstr]
   (with-chan (.truncate fs pathstr)))
  ([pathstr len]
   (with-chan (.truncate fs pathstr len))))

(defn touch
  "Creates a file if non-existent
   @param {!string} pathstr
   @return {nil}"
  [pathstr]
  (try
    (let [t (js/Date.)]
      (fs.utimesSync pathstr t t))
    (catch js/Error _
      (fs.closeSync (fs.openSync pathstr "w")))))

(defn atouch
  "creates a file if non-existent
   @param {!string} pathstr
   @return {!Channel} yielding [?err]"
  [pathstr]
  (with-promise out
    (let [t (js/Date.)]
      (fs.utimes pathstr t t
        (fn [err]
          (if (nil? err)
            (put! out [nil])
            (fs.open pathstr "w"
              (fn [err fd]
                (if err
                  (put! out [err])
                  (fs.close fd (fn [err] (put! out [err]))))))))))))

(defn copy-file
  "Synchronously copy file from src to dst. By default dst is overwritten.
   {@link https://nodejs.org/api/fs.html#fscopyfilesyncsrc-dest-mode}
   @param {(string|buffer.Buffer|URL)} src
   @param {(string|buffer.Buffer|URL)} dst
   @return {nil}"
  ([src dst]
   (copy-file src dst 0))
  ([src dst mode]
   (fs.copyFileSync src dst mode)))

(defn acopy-file
  "Asynchronously copy file from src to dst. By default dst is overwritten.
   {@link https://nodejs.org/api/fs.html#fscopyfilesrc-dest-mode-callback}
   @param {(string|buffer.Buffer|URL)} src
   @param {(string|buffer.Buffer|URL)} dst
   @return {!Channel} yielding [?err]"
  ([src dst]
   (acopy-file src dst 0))
  ([src dst mode]
   (with-chan (fs.copyFile src dst mode))))

;; /writes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; read+write Files

(defn read-file
  "@param {!string} pathstr :: the file path to read
   @param {!string} enc :: encoding , if \"\" (an explicit empty string), => raw buffer
   @return {(buffer.Buffer|string)}"
  [pathstr enc] (.readFileSync fs pathstr enc))

(defn aread-file
  "@param {!string} pathstr
   @param {!string} enc :: if \"\" (an explicit empty string) => raw buffer
   @return {!Channel} yielding [?err (str|Buffer)]"
  [pathstr enc]
  (with-chan (.readFile fs pathstr enc)))

(defn write-file
  "synchronously writes content to file represented by pathstring.
   @param {!string} pathstr :: file to write to
   @param {(string|buffer.Buffer)} content :: if buffer, :encoding is ignored
   @param {?IMap} opts :: {:encoding {string}, :append {boolean}, :flags {string}, :mode {int}}
    - flags override append
    - :encoding defaults to utf8
   @return {nil}"
  [pathstr content opts]
  (.writeFileSync fs pathstr content
                  #js{"flag"     (or (:flags opts) (if (:append opts) "a" "w"))
                      "mode"     (or (:mode opts)  438)
                      "encoding" (or (:encoding opts) "utf8")}))

(defn awrite-file
  "Asynchronously write to a file.
   @param {!string} pathstring : file to write to
   @param {(string|buffer.Buffer)} content : if buffer, :encoding is ignored
   @param {?IMap} opts : :encoding {string}, :append {bool}, :flags {string}, :mode {int}
    - flags override append
   @return {!Channel} promise-chan recieving [?err]"
  [pathstring content opts]
  (with-chan
    (.writeFile fs pathstring content
                #js{"flag"     (or (:flags opts) (if (:append opts) "a" "w"))
                    "mode"     (or (:mode opts) 438)
                    "encoding" (or (:encoding opts) "utf8")})))

;; /read+write Files
;;==============================================================================
;; File Descriptors

(defn ^number open
  "Synchronously open a file-descriptor
   {@link https://nodejs.org/api/fs.html#fsopenpath-flags-mode-callback}
   @param {!string} pathstr
   @return {!Number} integer file-descriptor"
  ([pathstr]
   (fs.openSync pathstr "r"))
  ([pathstr flags]
   (fs.openSync pathstr flags))
  ([pathstr flags mode]
   (fs.openSync pathstr flags mode)))

(defn aopen
  "Asynchronously open a file-descriptor
   {@link https://nodejs.org/api/fs.html#fsopenpath-flags-mode-callback}
   @param {!string} pathstr
   @return {!Channel} yielding [?err ?fd]"
  ([pathstr]
   (with-chan (fs.open pathstr "r")))
  ([pathstr flags]
   (with-chan (fs.open pathstr flags)))
  ([pathstr flags mode]
   (with-chan (fs.open pathstr flags mode))))

(defn close
  "Synchronously close a file-descriptor
   {@link https://nodejs.org/api/fs.html#fsclosesyncfd}
   @param {Number} fd :: the file descriptor to close
   @return {nil}"
  [fd]
  (assert (and (number? fd) (js/Number.isInteger fd)))
  (fs.closeSync fd))

(defn aclose
  "Asynchronously close a file-descriptor
   {@link https://nodejs.org/api/fs.html#fsclosefd-callback}
   @param {Number} fd :: the file descriptor to close
   @return {Channel} yielding [?err]"
  [fd]
  (with-chan (fs.close fd)))

(defn ^number read
  "Synchronously read data from the fd into the specified buffer
   {@link https://nodejs.org/api/fs.html#fsreadsyncfd-buffer-offset-length-position}
   Can pass arguments individually or as a map:
     :buffer (Buffer|ArrayBuffer) - where data will be written
     :offset (Number) - where to start writing in the buffer
     :length (Number) - how many bytes to read from fd
     :pos (Number|Bigint) - Where to begin reading. if -1 reads from current-pos
   @return {!Number} number of bytes read"
  ([fd opts]
   (let [buf (:buffer opts)]
     (assert (js/Buffer.isBuffer buf) ":buffer entry needed to read from a fd")
     (read fd (:buffer opts) opts)))
  ([fd buffer opts]
   (let [{:keys [offset length position]} opts
         offset (or offset 0)
         length (or length (.-byteLength buffer))
         position (or position 0)]
     (read fd buffer offset length position)))
  ([fd buffer offset length position]
   (.readSync fs fd buffer offset length position)))

(defn aread
  "Asynchronously read data from the fd into the specified buffer
   {@link https://nodejs.org/api/fs.html#fsreadfd-buffer-offset-length-position-callback}
   Can pass arguments individually or as a map:
     :buffer (Buffer|ArrayBuffer) - where data will be written
     :offset (Number) - where to start writing in the buffer
     :length (Number) - how many bytes to read from fd
     :pos (Number|Bigint) - Where to begin reading. if -1 reads from current-pos
   @return {!Channel} yielding [?err bytes-read buffer]"
  ([fd opts]
   (let [buf (:buffer opts)]
     (assert (js/Buffer.isBuffer buf) ":buffer entry needed to read from a fd")
     (aread fd (:buffer opts) opts)))
  ([fd buffer opts]
   (let [{:keys [offset length position]} opts
         offset (or offset 0)
         length (or length (.-byteLength buffer))
         position (or position 0)]
     (aread fd buffer offset length position)))
  ([fd buffer offset length position]
   (with-chan (.read fs fd buffer offset length position))))

(defn ^number write
  "Synchronously write data from the buffer to the fd
   {@link https://nodejs.org/api/fs.html#fswritesyncfd-buffer-offset-length-position}
   Can pass arguments individually or as a map:
     :buffer (Buffer|ArrayBuffer) - data to be written
     :offset (Number) - where to start reading in the buffer
     :length (Number) - how many bytes to read
     :pos (Number|Bigint) - where to begin writing in the fd
   @return {!Number} number of bytes written"
  ([fd opts]
   (let [buf (:buffer opts)]
     (assert (js/Buffer.isBuffer buf) ":buffer entry needed to read from a fd")
     (write fd (:buffer opts) opts)))
  ([fd buffer opts]
   (let [{:keys [offset length position]} opts
         offset (or offset 0)
         length (or length (- (.-byteLength buffer) offset))
         position (or position 0)]
     (write fd buffer offset length position)))
  ([fd buffer offset length position]
   (.writeSync fs fd buffer offset length position)))

(defn awrite
  "Asynchronously write data from the buffer to the fd
   {@link https://nodejs.org/api/fs.html#fswritesyncfd-buffer-offset-length-position}
   Can pass arguments individually or as a map:
     :buffer (Buffer|ArrayBuffer) - data to be written
     :offset (Number) - where to start reading in the buffer
     :length (Number) - how many bytes to read
     :pos (Number|Bigint) - where to begin writing in the fd
   @return {!Channel} yielding [?err bytes-written buffer]"
  ([fd opts]
   (let [buf (:buffer opts)]
     (assert (js/Buffer.isBuffer buf) ":buffer entry needed to read from a fd")
     (awrite fd (:buffer opts) opts)))
  ([fd buffer opts]
   (let [{:keys [offset length position]} opts
         offset (or offset 0)
         length (or length (- (.-byteLength buffer) offset))
         position (or position 0)]
     (awrite fd buffer offset length position)))
  ([fd buffer offset length position]
   (with-chan (.write fs fd buffer offset length position))))

(defn fsync
  "Synchronously flush to storage
   {@link https://nodejs.org/api/fs.html#fsfstatsyncfd-options}
   @param {!Number} fd
   @return {nil}"
  [fd]
  (.fsyncSync fs fd))

(defn afsync
  "Asynchronously flush to storage
   {@link https://nodejs.org/api/fs.html#fsfsyncfd-callback}
   @param {!Number} fd
   @return {Channel} yielding [?err]"
  [fd]
  (with-chan (.fsync fs fd)))

(defn fstat
  "Synchronously retrieve a stats map from the file descriptor
   {@link https://nodejs.org/api/fs.html#fsfstatsyncfd-options}
   @param {!Number} fd
   @param {!IMap} edn-stats"
  [fd]
  (stat->clj (.fstatSync fd)))

(defn afstat
  "Asynchronously retrieve a stats map from the file descriptor
   {@link https://nodejs.org/api/fs.html#fsfstatfd-options-callback}
   @param {!Number} fd
   @param {!Channel} yielding [?err edn-stats]"
  [fd]
  (with-promise out
    (.fstat fs fd
      (fn [err stats]
        (put! out (if err [err] [nil (stat->clj stats)]))))))

(defn ftruncate
  "Synchronous ftruncate
   {@link https://nodejs.org/api/fs.html#fsftruncatesyncfd-len}
   @param {!Number} fd
   @param {?Number} len
   @return {nil}"
  ([fd]
   (.ftruncateSync fs fd))
  ([fd len]
   (.ftruncateSync fs fd len)))

(defn aftruncate
  "Asynchronous ftruncate
   {@link https://nodejs.org/api/fs.html#fsftruncatefd-len-callback}
   @param {!Number} fd
   @param {?Number} len
   @return {!Channel} yielding [?err]"
  ([fd]
   (with-chan (.ftruncate fs fd)))
  ([fd len]
   (with-chan (.ftruncate fs fd len))))

;; /fd
;;==============================================================================

(def rl (js/require "readline"))

(defn readline
  "A simple file line reader.
   @param {!string} pathstr
   @return {!Channel} chan receiving [?err ?line] until file is consumed,
   and then the channel closes."
  [pathstr]
  (let [out (chan 10)
        in (fs.createReadStream pathstr)
        _(set! (.-in out) in)
        r (rl.createInterface #js{:input in :crlfDelay js/Infinity})]
    (doto in
          (.on "error" (fn [e] (put! out [e])))
          (.on "close" #(close! out)))
    (doto r
          (.on "line" (fn [line] (put! out [nil line]))))
    out))

;;==============================================================================
;; watch

(defn watcher->ch
  ([FSWatcher out-ch] (watcher->ch FSWatcher out-ch nil))
  ([FSWatcher out-ch {:keys [key buf-or-n] :or {buf-or-n 10}}]
   (let []
    (doto FSWatcher
      (.on "change"
        (fn [eventType filename] ;[string string|Buffer]
          (put! out-ch [(keyword eventType)])))
      (.on "error" (fn [e] (put! out-ch [:error [e]]))))
     out-ch)))

(deftype Watcher [FSWatcher out]
  impl/ReadPort
  (take! [this handler] (impl/take! out handler))
  Object
  (close [this]
    (.close FSWatcher)
    (put! out [:close] #(close! out))))

(defn watch
  "Watch a file or directory.
   Make note of caveats https://nodejs.org/api/fs.html#fs_caveats
   events : 'rename', 'change' , 'error', 'close'
   opts :
    :peristent {boolean} (true) :: whether the process should continue as long as files are being watched.
    :recursive {boolean} (false) :: watch subdirectories
    :buf-or-n {(impl/Buffer|number)} (10) :: channel buffer
    :encoding {string} ('utf8') :: used to interpret passed filename
   @return {!Watcher}"
  ([filename] (watch filename nil))
  ([filename opts]
    (let [defaults {:persistent true
                    :recursive false
                    :encoding "utf8"
                    :buf-or-n 10}
          opts (merge defaults opts)
          key (or (get opts :key) filename)
          out (chan (get opts :buf-or-n) (map #(conj [key] %)))
          w (fs.watch filename (clj->js opts))]
      (->Watcher w (watcher->ch w out)))))

(defn watchFile
  "Prefer watch. Polls files and returns stat objects. Opts:
     :peristent {boolean} (true) :: whether the process should continue as long as files are being watched.
     :interval {number} (5007) :: polling interval in msecs
     :buf-or-n {(impl/Buffer|number)} (10) :: channel buffer
   @return {!Channel} yielding [filename [current fs.stat, previous fs.stat]]"
  ([filename] (watchFile filename nil))
  ([filename opts]
   (let [defaults {:interval 5007
                   :persistent true
                   :buf-or-n 10}
         {:keys [edn? buf-or-n] :as opts} (merge defaults opts)
         out (chan buf-or-n (map #(conj [filename] %)))
         w (fs.watchFile filename (clj->js opts)
             (fn [curr prev]
                  (put! out [(stat->clj curr)(stat->clj prev)])))]
     out)))

(defn unwatchFile
  "remove all watchers from a file
   @param {!string} pathstr
   @return {nil}"
  [pathstr]
  (fs.unwatchFile pathstr))

;; /watch
;;==============================================================================
;; Lock Files

(defrecord LockFile [lock-file-path locked? release-f]
  Object
  (isValid [this] @locked?)
  (release [this] (release-f))
  (close [this] (release-f)))

(defn lock-path
  [pathstr]
  (str pathstr ".LOCK"))

(defn lock-file
  "Attempts to synchronously open a lock-file exclusively. This prevents
   future openings until the holding lock has been released, thereby can
   use to prevent reads and writes within a process.
   If a lock is already held, will throw
   @param {string} pathstr :: the file you want to lock
   @return {LockFile} record with .release() method to unlock the file"
  [pathstr]
  (let [lock-file-path (lock-path pathstr)
        lock-fd (try
                  (open lock-file-path "wx+")
                  (catch js/Error e
                    (throw (js/Error. (str "Failed to acquire lock for path: '" pathstr "':\n" (.-message e))))))
        locked? (atom true)
        release #(when ^boolean @locked?
                   (fs.closeSync lock-fd)
                   (fs.unlinkSync lock-file-path)
                   (reset! locked? false))]
    (LockFile. lock-file-path locked? release)))

(defrecord AsyncLockFile [lock-file-path locked? release-f]
  Object
  (isValid [this] @locked?)
  (release [this] (release-f)) ;; chan<?err>
  (close [this] (release-f))) ;; chan<?err>

(defn alock-file
  "Attempts to asynchronously open a lock-file exclusively. This prevents
   future openings until the holding lock has been released, thereby can
   use to prevent reads and writes within a process.
   A yielded err indicates a failure to obtain a lock, & AsyncLockFile is a
   record with .release() method to unlock the file, returning a promise-chan
   yielding [?err]
   @param {string} pathstr :: the file you want to lock
   @return {Channel} yielding [?err ?AsyncLockFile]"
  [pathstr]
  (with-promise out
    (let [lock-file-path (lock-path pathstr)]
      (fs.open lock-file-path "wx+"
        (fn [?err lock-fd]
          (if (some? ?err)
            (put! out [?err])
            (let [locked? (atom true)
                  release (fn []
                            (with-promise out
                              (if (not @locked?)
                                (put! out [nil])
                                (fs.close lock-fd
                                  (fn [?err]
                                    (if (some? ?err)
                                      (put! out  [?err])
                                      (fs.unlink lock-file-path
                                        (fn [?err]
                                          (if (some? ?err)
                                            (put! out [?err])
                                            (do
                                              (reset! locked? false)
                                              (put! out [nil])))))))))))]
             (put! out [nil (AsyncLockFile. lock-file-path locked? release)]))))))))
