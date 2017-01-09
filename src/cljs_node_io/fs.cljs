(ns cljs-node-io.fs "A wrapper around node's fs module."
  (:require-macros [cljs-node-io.macros :refer [try-true with-chan with-bool-chan]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! take! close! promise-chan chan]]
            [cljs.core.async.impl.protocols :as impl :refer [Channel]]))

(def fs (js/require "fs"))
(def path (js/require "path"))
(def ^{:doc "@type {!string}"} sep (.-sep path))

(defn stat
  "Synchronous stat
   @param {!string} pathstring
   @return {!fs.Stats} file stats object"
  [pathstring]
  (.statSync fs pathstring))

(defn astat
  "Asynchronous stat
   @param {!string} pathstr
   @return {!Channel} promise-chan receiving [?err ?fs.Stats]"
  [pathstr]
  (assert (string? pathstr))
  (with-chan (.stat fs pathstr)))

(defn lstat
  "Synchronous lstat identical to stat(), except that if path is a symbolic link,
   then the link itself is stat-ed, not the file that it refers to
   @param {!string} pathstr
   @return {!fs.Stats} file stats object"
  [pathstr]
  (.lstatSync fs pathstr))

(defn alstat
  "Asynchronous lstat
   @param {!string} pathstr
   @return {!Channel} promise-chan receiving [?err ?fs.Stats]"
  [pathstr]
  (assert (string? pathstr))
  (with-chan (.lstat fs pathstr)))

(defn stat->clj
  "Convert a fs.Stats object to edn. Function are swapped out for their return values.
   This is useful at repl but not particularly efficient.
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

(defn- bita->int
  "@param {!Array<!Number>} bita :: an array of 1s an 0s
   @return {!Number} integer"
  [bita]
  (js/parseInt (.join bita "") 2))

(defn- stat->perm-bita
  "@param {!fs.Stats} s :: a fs.Stats object
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
    (amap a i res (if-not (zero? (aget a i)) 1 0))))

(defn permissions
  "@param {!fs.Stats} st
   @return {!Number}"
  [st] (-> st stat->perm-bita bita->int))

(defn gid-uid
  "@return {!IMap}"
  []{:gid (.getgid js/process) :uid (.getuid js/process)})


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
  (assert (string? pathstring))
  (let [stats (try (.statSync fs pathstring) (catch js/Error e false))]
    (if-not stats
      false
      (.isDirectory stats))))

(defn adir?
  "Asynchronous directory predicate.
   @param {!string} pathstr
   @return {!Channel} promise-chan receiving boolean"
  [pathstr]
  (assert (string? pathstr))
  (let [c (promise-chan)
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
  (assert (string? pathstring))
  (let [stats (try (lstat pathstring) (catch js/Error e false))]
    (if-not stats
      false
      (.isFile stats))))

(defn afile?
  "Asynchronous file predicate.
   @param {!string} pathstr
   @return {!Channel} promise-chan receiving boolean"
  [pathstr]
  (assert (string? pathstr))
  (let [c (promise-chan)
        stat-ch (alstat pathstr)]
    (take! stat-ch
      (fn [[err stats]]
        (put! c (if-not err (.isFile stats) false))))
    c))

(defn ^boolean absolute?
  "@param {!string} pathstr :: path to test
   @return {!boolean} is pathstr an absolute path"
  [pathstr]
  (assert (string? pathstr))
  (path.isAbsolute pathstr))

(defn ^boolean fexists?
  "Synchronously test if a file or directory exists
   @param {!string} pathstr :: file path to test
   @return {!boolean}"
  [pathstr]
  (assert (string? pathstr))
  (try-true (.accessSync fs pathstr (.-F_OK fs))))

(defn afexists?
  "Asynchronously test if a file or directory exists
   @param {!string} pathstr
   @return {!Channel} promise-chan receiving boolean"
  [pathstr]
  (assert (string? pathstr))
  (with-bool-chan (.access fs pathstr (.-F_OK fs))))

(defn ^boolean readable?
  "Synchronously test if a file is readable to the process
   @param {!string} pathstr :: path to test for process read permission
   @return {!boolean}"
  [pathstr]
  (assert (string? pathstr))
  (try-true (.accessSync fs pathstr (.-R_OK fs))))

(defn areadable?
  "Asynchronously test if a file is readable to the process
   @param {!string} pathstr
   @return {!Channel} promise-chan receiving boolean"
  [pathstr]
  (assert (string? pathstr))
  (with-bool-chan (.access fs pathstr (.-R_OK fs))))

(defn ^boolean writable?
  "Synchronously test if a file is writable to the process
   @param {!string} pathstr :: path to test for process write permission
   @return {!boolean}"
  [pathstr]
  (assert (string? pathstr))
  (try-true (.accessSync fs pathstr (.-W_OK fs))))

(defn awritable?
  "Asynchronously test if a file is writable to the process
   @param {!string} pathstr :: path to test for process write permission
   @return {!Channel} promise-chan receiving boolean"
  [pathstr]
  (assert (string? pathstr))
  (with-bool-chan (.access fs pathstr (.-W_OK fs))))

(defn ^boolean executable?
  "@param {!string} pathstr :: path to test for process executable permission
   @return {!boolean}"
  [pathstr]
  (assert (string? pathstr))
  (if-not (= "win32" (.-platform js/process))
    (try-true (.accessSync fs pathstr (.-X_OK fs)))
    (throw (js/Error "Testing if a file is executable has no effect on Windows"))))

(defn aexecutable?
  "Asynchronously test if a file is executable to the process
   @param {!string} pathstr :: path to test for process execute permission
   @return {!Channel} promise-chan receiving boolean"
  [pathstr]
  (assert (string? pathstr))
  (if-not (= "win32" (.-platform js/process))
    (with-bool-chan (.access fs pathstr (.-X_OK fs)))
    (throw (js/Error "Testing if a file is executable has no effect on Windows"))))

(defn ^boolean symlink?
  "Synchronous test for symbolic link
   @param {!string} pathstr :: path to test
   @return {!boolean}"
  [pathstr]
  (assert (string? pathstr))
  (let [stats (try (lstat pathstr) (catch js/Error e false))]
    (if-not stats
      false
      (.isSymbolicLink stats))))

(defn asymlink?
  "Asynchronously test if path is a symbolic link
   @param {!string} pathstr :: path to test
   @return {!Channel} promise-chan receiving boolean"
  [pathstr]
  (assert (string? pathstr))
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
  (.dirname path pathstring))

(defn basename
  "@return {!string}"
  ([p] (.basename path p))
  ([p ext] (.basename path p ext)))

(defn resolve-path
  "@return {!string}"
  [& paths] (.apply (.-resolve path) nil (apply array paths )))

(defn normalize-path
  "@param {!string} pathstring :: pathstring to normalize
   @return {!string}"
  [pathstring]
  (.normalize path pathstring))

(defn ext
  "@param {!string} pathstring :: file to get extension from
   @return {!string}"
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
   @return {!Channel} promise-chan recieving [?err ?resolvedPathstr]"
  [pathstr]
  (assert (string? pathstr))
  (with-chan (.realpath fs pathstr)))

(defn readlink
  "Synchronous readlink
   @param {!string} pathstr :: the symbolic link to read
   @return {!string} the symbolic link's string value"
  [pathstr]
  (.readlinkSync fs pathstr))

(defn areadlink
  "Asynchronous readlink
   @param {!string} pathstr :: the symbolic link to read
   @return {!Channel} promise-chan receiving [?err ?linkstring]"
  [pathstr]
  (assert (string? pathstr))
  (with-chan (.readlink fs pathstr)))

(defn readdir ; optional cache arg?
  "Synchronously reads directory content
   @param {!string} dirpath :: directory path to read
   @return {!IVector} Vector<strings> representing directory content"
  [dirpath]
  (assert (string? dirpath))
  (vec (.readdirSync fs dirpath)))

(defn areaddir
  "Asynchronously reads directory content
   @param {!string} dirpath :: directory path to read
   @return {!Channel} promise-chan receiving [?err, ?Vector<string>]
    where strings are representing directory content"
  [dirpath]
  (assert (string? dirpath))
  (with-chan (.readdir fs dirpath) vec))

;; /path utilities + reads
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; writes/

(defn chmod
  "Synchronous chmod
   @param {!string} pathstr
   @param {!Number} mode :: must be an integer
   @return {nil} or throws"
  [pathstr mode]
  (.chmodSync fs pathstr mode))

(defn achmod
  "Asynchronous chmod
   @param {!string} pathstr
   @param {!Number} mode :: must be an integer
   @return {!Channel} promise-chan receiving [?err]"
  [pathstr mode]
  (assert (string? pathstr))
  (with-chan (.chmod fs pathstr mode)))

(defn lchmod
  "Synchronous lchmod
   @param {!string} pathstr
   @param {!Number} mode :: must be an integer
   @return {nil}"
  [pathstr mode]
  (.lchmodSync fs pathstr mode))

(defn alchmod
  "Asynchronous lchmod
   @param {!string} pathstr
   @param {!Number} mode :: must be an integer
   @return {!Channel} promise-chan receiving [?err]"
  [pathstr mode]
  (assert (string? pathstr))
  (with-chan (.lchmod fs pathstr mode)))

(defn chown
  "Synchronous chown
   @param {!string} pathstr
   @param {!Number} uid
   @param {!Number} gid
   @return {nil}"
  [pathstr uid gid]
  (.chownSync fs pathstr uid gid))

(defn achown
  "Asynchronous chown
   @param {!string} pathstr
   @param {!Number} uid
   @param {!Number} gid
   @return {!Channel} promise-chan receiving [?err]"
  [pathstr uid gid]
  (assert (string? pathstr))
  (with-chan (.chown fs pathstr uid gid)))

(defn lchown
  "Synchronous lchown
   @param {!string} pathstr
   @param {!Number} uid
   @param {!Number} gid
   @return {nil} or throws"
  [pathstr uid gid]
  (.lchownSync fs pathstr uid gid))

(defn alchown
  "Asynchronous lchown
   @param {!string} pathstr
   @param {!Number} uid
   @param {!Number} gid
   @return {!Channel} promise-chan receiving [?err]"
  [pathstr uid gid]
  (assert (string? pathstr))
  (with-chan (.lchown fs pathstr uid gid)))

(defn utimes
  "synchronous utimes
   - If the value is NaN or Infinity, the value would get converted to Date.now()
   - numerable strings ie '12235' are converted to numbers
   @param {!string} pathstr
   @param {(string|Number)} atime
   @param {(string|Number)} mtime
   @return {nil}"
  [pathstr atime mtime]
  (.utimesSync fs pathstr atime mtime))

(defn autimes
  "asynchronous utimes
   - If the value is NaN or Infinity, the value would get converted to Date.now()
   - numerable strings ie '12235' are converted to numbers
   @param {!string} pathstr
   @param {(string|Number)} atime
   @param {(string|Number)} mtime
   @return {!Channel} promise-chan receiving [?err]"
  [pathstr atime mtime]
  (assert (string? pathstr))
  (with-chan (.utimes fs pathstr atime mtime)))

(defn mkdir
  "Synchronously create a directory
   @param {!string} pathstr :: path of directory to create
   @return {nil} or throws"
  [pathstr]
  (.mkdirSync fs pathstr))

(defn amkdir
  "Asynchronously create a directory
   @param {!string} pathstr :: path of directory to create
   @return {!Channel} promise-chan receiving [?err]"
  [pathstr]
  (assert (string? pathstr))
  (with-chan (.mkdir fs pathstr)))

(defn rmdir
  "Synchronously remove a directory
   @param {!string} pathstr :: path of directory to remove
   @return {nil} or throws"
  [pathstr]
  (.rmdirSync fs pathstr))

(defn armdir
  "Asynchronously remove a directory
   @param {!string} pathstr :: path of directory to remove
   @return {!Channel} promise-chan receiving [?err]"
  [pathstr]
  (assert (string? pathstr))
  (with-chan (.rmdir fs pathstr)))

(defn link
  "Synchronous link. Will not overwrite newpath if it exists.
   @param {!string} srcpathstr
   @param {!string} dstpathstr
   @return {nil} or throws"
  [srcpathstr dstpathstr]
  {:pre [(string? srcpathstr) (string? dstpathstr)]}
  (.linkSync fs srcpathstr dstpathstr))

(defn alink
  "Synchronous link. Will not overwrite newpath if it exists.
   @param {!string} srcpathstr
   @param {!string} dstpathstr
   @return {!Channel} promise-chan receiving [?err]"
  [srcpathstr dstpathstr]
  {:pre [(string? srcpathstr) (string? dstpathstr)]}
  (with-chan (.link fs srcpathstr dstpathstr)))

(defn symlink
  "Synchronous symlink.
   @param {!string} target :: what gets pointed to
   @param {!string} pathstr :: the new symbolic link that points to target
   @return {nil} or throws"
  [target pathstr]
  (.symlinkSync fs target pathstr))

(defn asymlink
  "Synchronous symlink.
   @param {!string} targetstr :: what gets pointed to
   @param {!string} pathstr :: the new symbolic link that points to target
   @return {!Channel} promise-chan receiving [?err]"
  [targetstr pathstr]
  {:pre [(string? targetstr) (string? pathstr)]}
  (with-chan (.symlink fs targetstr pathstr)))

(defn unlink
  "Synchronously unlink a file.
   @param {!string} pathstr :: path of file to unlink
   @return {nil} or throws"
  [pathstr]
  (.unlinkSync fs pathstr))

(defn aunlink
  "Asynchronously unlink a file
   @param {!string} pathstr :: path of file to unlink
   @return {!Channel} promise-chan receiving [?err]"
  [pathstr]
  (assert (string? pathstr))
  (with-chan (.unlink fs pathstr)))

(defn rm
  "Synchronously delete the file or directory path
   @param {!string} pathstr :: can be file or directory
   @return {nil} or throws"
  [pathstr]
  (assert (string? pathstr))
  (if (dir? pathstr)
    (rmdir pathstr)
    (unlink pathstr)))

(defn arm
  "Asynchronously delete the file or directory path
   @param {!string} pathstr
   @return {!Channel} promise-chan receiving [?err]"
  [pathstr]
  (assert (string? pathstr))
  (let [c (promise-chan)
        dc (adir? pathstr)]
    (take! dc
      (fn [d?]
        (take! (if d? (armdir pathstr) (aunlink pathstr))
          (fn [ev] (put! c ev)))))
    c))

(defn rm-r
  "@param {!string} pathstr :: path to a directory to recursively delete. Deletes a passed file as well.
   @return {nil} or throws"
  [pathstr]
  (assert (string? pathstr))
  (assert (false? (boolean (#{ "/" "\\" "\\\\" "//"} pathstr)))
    (str "you just tried to delete root, " (pr-str pathstr) ", be more careful."))
  (if (dir? pathstr)
    (do
      (doseq [p (mapv (partial resolve-path pathstr) (readdir pathstr))]
        (rm-r p))
      (rmdir pathstr))
    (unlink pathstr)))

(defn arm-r
  "asynchronous recursive delete. Crawls in order provided by readdir and makes unlink/rmdir calls sequentially
   after the previous has completed. Breaks on any err which is returned as [err].
   @param {!string} pathstr
   @return {!Channel} promise-chan receiving [?err]"
  [pathstr]
  (assert (string? pathstr))
  (assert (false? (boolean (#{ "/" "\\" "\\\\" "//"} pathstr)))
    (str "you just tried to delete root, " (pr-str pathstr) ", be more careful."))
  (let [c (promise-chan)]
    (go
     (if (<! (adir? pathstr))
       (let [[rderr names] (<! (areaddir pathstr))]
         (if-not rderr
           (do
             (loop [children (mapv (partial resolve-path pathstr) names)]
               (if (some? children)
                 (let [[arm-r-err] (<! (arm-r (first children)))]
                   (if (instance? js/Error arm-r-err)
                     (>! c arm-r-err)
                     (recur (next children))))))
             (>! c (<! (armdir pathstr))))
           (>! c [rderr])))
       (>! c (<! (aunlink pathstr)))))
    c))

(defn rename
  "Synchronously rename a file.
   @param {!string} oldpathstr :: file to rename
   @param {!string} newpathstr :: what to rename it to
   @return {nil}"
  [oldpathstr newpathstr]
  (.renameSync fs oldpathstr newpathstr))

(defn arename
  "Asynchronously rename a file
   @param {!string} oldpathstr :: file to rename
   @param {!string} newpathstr :: what to rename it to
   @return {!Channel} promise-chan receiving [?err]"
  [oldpathstr newpathstr]
  {:pre [(string? oldpathstr) (string? newpathstr)]}
  (with-chan (.rename fs oldpathstr newpathstr)))

(defn truncate
  "Synchronous truncate
   @param {!string} pathstr
   @param {!number} length
   @return {nil} or throws"
  [pathstr length]
  (.truncateSync fs pathstr length))

(defn atruncate
  "Asynchronous truncate
   @param {!string} pathstr
   @param {!number} len
   @return {!Channel} promise-chan receiving [?err]"
  [pathstr len]
  (assert (string? pathstr))
  (with-chan (.truncate fs pathstr len)))

;; /writes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; read+write Files

(defn readFile
  "@param {!string} pathstr :: the file path to read
   @param {!string} enc :: encoding , if \"\" (an explicit empty string), => raw buffer
   @return {(buffer.Buffer|string)} or throw"
  [pathstr enc] (.readFileSync fs pathstr enc))

(defn areadFile
  "@param {!string} pathstr
   @param {!string} enc :: if \"\" (an explicit empty string) => raw buffer
   @return {!Channel} promise-chan receiving [?err ?(str|Buffer)] on successful read"
  [pathstr enc]
  (with-chan (.readFile fs pathstr enc)))

(defn writeFile
  "synchronously writes content to file represented by pathstring.
   @param {!string} pathstr :: file to write to
   @param {(string|buffer.Buffer)} content :: if buffer, :encoding is ignored
   @param {?IMap} opts :: {:encoding {string}, :append {boolean}, :flags {string}, :mode {int}}
    - flags override append
    - :encoding defaults to utf8
   @return {nil} or throws"
  [pathstr content opts]
  (.writeFileSync fs pathstr content
                  #js{"flag"     (or (:flags opts) (if (:append opts) "a" "w"))
                      "mode"     (or (:mode opts)  438)
                      "encoding" (or (:encoding opts) "utf8")}))

(defn awriteFile
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
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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
   @return {!cljs-node-io.fs/Watcher}"
  ([filename] (watch filename nil))
  ([filename opts]
    (let [defaults {:persistent true
                    :recursive false
                    :encoding "utf8"
                    :buf-or-n 10}
          opts (merge defaults opts)
          out (chan (get opts :buf-or-n) (map #(conj [filename] %)))
          w (fs.watch filename (clj->js opts))]
      (->Watcher w (watcher->ch w out)))))

(defn watchFile
  "Prefer watch. Polls files and returns stat objects. Opts:
     :peristent {boolean} (true) :: whether the process should continue as long as files are being watched.
     :interval {number} (5007) :: polling interval in msecs
     :edn? {boolean} (true) :: converts stats to edn
     :buf-or-n {(impl/Buffer|number)} (10) :: channel buffer
   @return {!Channel} <= [current fs.stat, previous fs.stat]"
  ([filename] (watchFile filename nil))
  ([filename opts]
   (let [defaults {:interval 5007
                   :persistent true
                   :edn? true
                   :buf-or-n 10}
         {:keys [edn? buf-or-n] :as opts} (merge defaults opts)
         out (chan buf-or-n (map #(conj [filename] %)))
         cb (fn [curr prev]
              (put! out
                (if edn?
                  [(stat->clj curr)(stat->clj prev)]
                  [curr prev])))
         w (fs.watchFile filename (clj->js opts) cb)]
     out)))

(defn unwatchFile
  "remove all watchers from a file
   @param {!string} pathstr
   @return {nil}"
  [pathstr]
  (fs.unwatchFile pathstr))

(defn touch
  "creates a file if non-existent, writes blank string to an existing
   @param {!string} pathstr
   @return {nil}"
  [pathstr]
  (writeFile pathstr "" nil))

(defn atouch
  "creates a file if non-existent, writes blank string to an existing
   @param {!string} pathstr
   @return {!Channel} promise-chan recieving [?err]"
  [pathstr]
  (awriteFile pathstr "" nil))