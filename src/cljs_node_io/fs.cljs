(ns cljs-node-io.fs
  (:require-macros [cljs-node-io.macros :refer [try-true with-chan with-bool-chan]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! take! chan <! pipe  alts!]]
            [cljs.core.async.impl.protocols :refer [Channel]]))

(def fs (js/require "fs"))
(def path (js/require "path"))
(def os (js/require "os"))

(def tmpdir (.tmpdir os))
(def sep (.-sep path))

(defn stat
  "Synchronous stat
   @param {!string} pathstring
   @return {!fs.Stats} file stats object"
  [pathstring]
  (.statSync fs pathstring))

(defn astat
  "Asynchronous stat
   @param {!string} pathstr
   @return {!Channel} receives [err fs.Stats]"
  [pathstr]
  (with-chan (.stat fs pathstr)))

(defn lstat
  "Synchronous lstat Identical to stat(), except that if path is a symbolic link,
   then the link itself is stat-ed, not the file that it refers to
   @param {!string} pathstring
   @return {!fs.Stats} file stats object"
  [pathstr]
  (.lstatSync fs pathstr))

(defn alstat
  "Asynchronous lstat
   @param {!string} pathstr
   @return {!Channel} receives [err fs.Stats]"
  [pathstr]
  (with-chan (.lstat fs pathstr)))

(defn to-bit [number] (if-not (zero? number) 1 0))

(defn bita->int
  "@param {!Array<!Number>} bita : an array of 1s an 0s
   @return {!Number} integer"
  [bita]
  (js/parseInt (.join bita "") 2))

(defn stat->perm-bita
  "@param {!fs.Stats} s : a fs.Stats object
   @return {!Array<Number>}"
  [s]
  (let [mode (aget s "mode")
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
    (amap a i res (to-bit (aget a i)))))

(defn permissions
  "@param {!fs.stat} filepath
   @return {!Number}"
  [st]
  (-> st stat->perm-bita bita->int))

(defn gid-uid
  "@return {!IMap}"
  []
  {:gid (.getgid js/process) :uid (.getuid js/process)})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; predicates/

(defn ^boolean hidden?
  "@param {!string} pathstr
   @return {!boolean} is the file hidden (unix only)"
  [pathstr]
  (.test (js/RegExp. "(^|\\/)\\.[^\\/\\.]" ) pathstr))

(defn ^boolean dir?
  "@param {!string} pathstring
   @return {!boolean} iff abstract pathname exists and is a directory"
  [pathstring]
  (let [stats (try (.statSync fs pathstring) (catch js/Error e false))]
    (if stats
      (.isDirectory stats)
      false)))

(defn adir?
  "Asynchronous directory predicate.
   @param {!string} pathstr
   @return {!Channel} receives boolean"
  [pathstr]
  (let [c  (chan)
        stat-ch (astat pathstr)]
    (take! stat-ch
      (fn [[err stats]]
        (put! c
          (if-not err
            (.isDirectory stats)
            false))))
    c))

(defn ^boolean file?
  "Synchronous file predicate
   @param {!string} pathstring
   @return {!boolean} iff abstract pathname exists and is a file"
  [pathstring]
  (let [stats (try (lstat pathstring) (catch js/Error e false))]
    (if stats
      (.isFile stats)
      false)))

(defn afile?
  "Asynchronous file predicate.
   @param {!string} pathstr
   @return {!Channel} receives boolean"
  [pathstr]
  (let [c  (chan)
        stat-ch (alstat pathstr)]
    (take! stat-ch
      (fn [[err stats]]
        (put! c (if-not err (.isFile stats) false))))
    c))

(defn ^boolean absolute?
  "@param {!string} p : path to test
   @return {!boolean} is p an absolute path"
  [p]
  (.isAbsolute path p))

(defn ^boolean fexists?
  "Synchronously test if a file or directory exists
   @param {!string} p : file path to test
   @return {!boolean}"
  [pathstr]
  (try-true (.accessSync fs pathstr (.-F_OK fs))))

(defn afexists?
  "Asynchronously test if a file or directory exists
   @param {!string} pathstr
   @return {!Channel} receives boolean"
  [pathstr]
  (with-bool-chan (.access fs pathstr (.-F_OK fs))))

(defn ^boolean readable?
  "Synchronously test if a file is readable to the process
   @param {!string} p path to test for process read permission
   @return {!boolean}"
  [p]
  (try-true (.accessSync fs p (.-R_OK fs))))

(defn areadable?
  "Asynchronously test if a file is readable to the process
   @param {!string} pathstr
   @return {!Channel} receives boolean"
  [pathstr]
  (with-bool-chan (.access fs pathstr (.-R_OK fs))))

(defn ^boolean writable?
  "Synchronously test if a file is writable to the process
   @param {!string} p path to test for process write permission
   @return {!boolean}"
  [p]
  (try-true (.accessSync fs p (.-W_OK fs))))

(defn awritable?
  "Asynchronously test if a file is writable to the process
   @param {!string} p path to test for process write permission
   @return {!Channel} receives boolean"
  [pathstr]
  (with-bool-chan (.access fs pathstr (.-W_OK fs))))

(defn ^boolean executable?
  "@param {!string} p path to test for process executable permission
   @return {!boolean}"
  [p]
  (if-not (= "win32" (.-platform js/process))
    (try-true (.accessSync fs p (.-X_OK fs)))
    (throw (js/Error "Testing if a file is executable has no effect on Windows "))))

(defn aexecutable?
  "Asynchronously test if a file is executable to the process
   @param {!string} p path to test for process execute permission
   @return {!Channel} receives boolean"
  [pathstr]
  (with-bool-chan (.access fs pathstr (.-X_OK fs))))

(defn ^boolean slink?
  "Synchronous test for symbolic link"
  [pathstr]
  (let [stats (try (lstat pathstr) (catch js/Error e false))]
    (if-not stats
      false
      (.isSymbolicLink stats))))

(defn aslink?
  "Asynchronously test if path is a symbolic link
   @param {!string} pathstr
   @return {!Channel} receives boolean"
  [pathstr]
  (let [c  (chan)
        stat-ch (alstat pathstr)]
    (take! stat-ch
      (fn [[err stats]]
        (put! c (if-not err (.isSymbolicLink stats) false))))
    c))

;; /predicates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; path utilities/

(defn dirname
  "@param {!string} pathstring : path to get parent of
   @return {!string} the parent directory"
  [pathstring]
  (.dirname path pathstring))

(defn filename
  "@return {!string}"
  ([p] (.basename path p))
  ([p ext] (.basename path p ext)))

(defn resolve-path
  "@param {!...string} paths : pathstring(s) to resolve to absolute path
   @return {!string}"
  [& paths] (.apply (.-resolve path) nil (apply array paths )))

(defn normalize-path
  "@param {!string} pathstring : pathstring to normalize
   @return {!string}"
  [pathstring]
  (.normalize path pathstring))

(defn ext
  "@param {string} pathstring : file to get extension from
   @return {string}"
  [pathstring]  (.extname path pathstring))

(defn realpath
  "Synchronous realpath
   @param {!string} pathstr
   @return {!string} resolved path"
  [pathstr]
  (.realpathSync fs pathstr))

(defn arealpath
  "Asynchronous realpath
   @param {!string} pathstr
   @return {!Channel} [err resolvedPathstr]"
  [pathstr]
  (with-chan (.realpath fs pathstr)))

(defn readlink
  "Synchronous readlink
   @param {!string} pathstr : the symbolic link to read
   @return {!string} the symbolic link's string value"
  [pathstr]
  (.readlinkSync fs pathstr))

(defn areadlink
  "Asynchronous readlink
   @param {!string} pathstr : the symbolic link to read
   @return {!Channel} receives [err linkstring]"
  [pathstr]
  (with-chan (.readlink fs pathstr)))

;; /path utilities
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; side-effecting IO ops/

(defn chmod
  "Synchronous chmod
   @param {!string} path
   @param {!Number} mode must be an integer"
  [pathstr mode]
  (.chmodSync fs pathstr mode))

(defn achmod
  "Asynchronous chmod
   @param {!string} pathstr
   @param {!Number} mode
   @return {!Channel} receives [err]"
  [pathstr mode]
  (with-chan (.chmod fs pathstr mode)))

(defn lchmod
  "Synchronous lchmod
   @param {!string} path
   @param {!Number} mode must be an integer"
  [pathstr mode]
  (.lchmodSync fs pathstr mode))

(defn alchmod
  "Asynchronous lchmod
   @param {!string} pathstr
   @param {!Number} mode
   @return {!Channel} receives [err]"
  [pathstr mode]
  (with-chan (.lchmod fs pathstr mode)))

(defn chown
  "Synchronous chown
   @param {!string} pathstr
   @param {!Number} uid
   @param {!Number} gid"
  [pathstr uid gid]
  (.chownSync fs pathstr uid gid))

(defn achown
  "Asynchronous chown
   @param {!string} pathstr
   @param {!Number} uid
   @param {!Number} gid
   @return {!Channel} receives [err]"
  [pathstr uid gid]
  (with-chan (.chown fs pathstr uid gid)))

(defn lchown
  "Synchronous lchown
   @param {!string} pathstr
   @param {!Number} uid
   @param {!Number} gid"
  [pathstr uid gid]
  (.lchownSync fs pathstr uid gid))

(defn alchown
  "Asynchronous lchown
   @param {!string} pathstr
   @param {!Number} uid
   @param {!Number} gid
   @return {!Channel} receives [err]"
  [pathstr uid gid]
  (with-chan (.lchown fs pathstr uid gid)))

(defn utimes
  "synchronous utimes
   - If the value is NaN or Infinity, the value would get converted to Date.now()
   - numerable strings ie '12235' are converted to numbers  "
  [pathstr atime mtime]
  (.utimesSync fs pathstr atime mtime))

(defn autimes
  "asynchronous utimes
   - If the value is NaN or Infinity, the value would get converted to Date.now()
   - numerable strings ie '12235' are converted to numbers
   @return {!Channel} receives [err]"
  [pathstr atime mtime]
  (with-chan (.utimes fs pathstr atime mtime)))

(defn mkdir
  "Synchronously create a directory
   @param {!string} pathstring : path of directory to create
   @return {!boolean}"
  [pathstring]
  (.mkdirSync fs pathstring))

(defn amkdir
  "Asynchronously create a directory
   @param {!string} pathstr
   @return {!Channel} receives [err]"
  [pathstr]
  (with-chan (.mkdir fs pathstr)))

(defn rmdir
  "Synchronously remove a directory
   @param {!string} pathstring : path of directory to remove
   @param {!boolean}
   @return {nil} or throws"
  [pathstring]
  (.rmdirSync fs pathstring))

(defn armdir
  "Asynchronously remove a directory
   @param {!string} pathstr
   @return {!Channel} receives [err]"
  [pathstr]
  (with-chan (.rmdir fs pathstr)))

(defn link
  "Synchronous link. Will not overwrite newpath if it exists.
   @param {!string} srcpath
   @param {!string} dstpath
   @return {nil} or throws"
  [srcpath dstpath]
  (.linkSync fs srcpath dstpath))

(defn alink
  "Synchronous link. Will not overwrite newpath if it exists.
   @param {!string} srcpath
   @param {!string} dstpath
   @return {!Channel} receives [err]"
  [srcpath dstpath]
  (with-chan (.link fs srcpath dstpath)))

(defn symlink
  "Synchronous symlink.
   @param {!string} target, what gets pointed to
   @param {!string} pathstr, the new symbolic link that points to target
   @return {nil} or throws"
  [target pathstr]
  (.symlinkSync fs target pathstr))

(defn asymlink
  "Synchronous symlink.
   @param {!string} target, what gets pointed to
   @param {!string} pathstr, the new symbolic link that points to target
   @return {!Channel} receives [err]"
  [target pathstr]
  (with-chan (.symlink fs target pathstr)))

(defn unlink
  "Synchronously unlink a file.
   @param {!string} pathstring : path of file to unlink
   @return {nil} or throws"
  [pathstring]
  (.unlinkSync fs pathstring))

(defn aunlink
  "Asynchronously unlink a file
   @param {!string} pathstr
   @return {!Channel} receives [err]"
  [pathstr]
  (with-chan (.unlink fs pathstr)))

(defn delete
  "Synchronously delete the file or directory path
   @param {!string} pathstring : can be file or directory
   @return {nil} or throws"
  [pathstring]
  (if (dir? pathstring)
    (rmdir pathstring)
    (unlink pathstring)))

(defn adelete
  "Asynchronously delete the file or directory path
   @param {!string} pathstr
   @return {!Channel} receives [err]"
  [pathstr]
  (let [c (chan)
        dc (adir? pathstr)]
    (take! dc
      (fn [d?]
        (take! (if d? (armdir pathstr) (aunlink pathstr))
          (fn [ev] (put! c ev)))))
    c))

(defn rename
  "Synchronously rename a file.
   @param {!string} oldpath : file to rename
   @param {!string} newpath : what to rename it to
   @return {!boolean}"
  [oldpath newpath]
  (.renameSync fs oldpath newpath))

(defn arename
  "Asynchronously rename a file
   @param {!string} oldpath : file to rename
   @param {!string} newpath : what to rename it to
   @return {!Channel} receives err|true"
  [oldpath newpath]
  (with-chan (.rename fs oldpath newpath)))

(defn truncate
  "Synchronous truncate
   @param {!string} pathstr
   @param {!number} len"
  [pathstr len]
  (.truncateSync fs pathstr len))

(defn atruncate
  "Asynchronous truncate
   @param {!string} pathstr
   @param {!number} len
   @return {!Channel} receives [err]"
  [pathstr len]
  (with-chan (.truncate fs pathstr len)))

(defn readdir ; optional cache arg?
  "Synchronously reads directory content
   @param {!string} dirpath : directory path to read
   @return {!IVector} Vector<strings> representing directory content"
  [dirpath]
  (vec (.readdirSync fs dirpath)))

(defn areaddir
  "Asynchronously reads directory content
   @param {!string} dirpath
   @return {!Channel} recives [err, Vector<strings>]
    where strings are representing directory content"
  [dirpath]
  (with-chan (.readdir fs dirpath) vec))

(defn readFile
  "if enc is \"\" (an explicit empty string) => raw buffer"
  [pathstring enc] (.readFileSync fs pathstring enc))

(defn areadFile
  "@param {!string} pathstr
   @param {!string} enc : if \"\" (an explicit empty string) => raw buffer
   @return {!Channel} receives err|(str|Buffer) on successful read"
  [pathstr enc]
  (with-chan (.readFile fs pathstr enc)))

(defn writeFile
  "synchronously writes content to file represented by pathstring.
   @param {!string} pathstring : file to write to
   @param {(string|buffer.Buffer)} content : if buffer, :encoding is ignored
   @param {?IMap} opts : :encoding {string}, :append {bool}, :flags {string}, :mode {int}
    - flags override append
   Returns nil or throws"
  [pathstring content opts]
  (.writeFileSync fs pathstring content
                  #js{"flag"     (or (:flags opts) (if (:append opts) "a" "w"))
                      "mode"     (or (:mode opts)  438)
                      "encoding" (or (:encoding opts) "utf8")}))

(defn awriteFile
  "Asynchronously write to a file.
   @param {!string} pathstring : file to write to
   @param {(string|buffer.Buffer)} content : if buffer, :encoding is ignored
   @param {?IMap} opts : :encoding {string}, :append {bool}, :flags {string}, :mode {int}
    - flags override append
   @return {!Channel} recieves [err]"
  [pathstring content opts]
  (with-chan
    (.writeFile fs pathstring content
                #js{"flag"     (or (:flags opts) (if (:append opts) "a" "w"))
                    "mode"     (or (:mode opts) 438)
                    "encoding" (or (:encoding opts) "utf8")})))

