(ns cljs-node-io.streams
  (:require [cljs.nodejs :as nodejs :refer [require]]
            [cljs-node-io.protocols
              :refer [Coercions as-url as-file
                      IOFactory make-reader make-writer make-input-stream make-output-stream]]))

(def fs (require "fs"))

(defn ^Boolean isFd? "is File-descriptor?"
  [path]
  (= path (unsigned-bit-shift-right path 0)))



(defn file-stream-dispatch [f {:keys [fd]}]
  (if (or (and (integer? f)  (isFd? f)) ;redundant?
          (and (integer? fd) (isFd? fd)))
    :file-descriptor
    (type f)))




; Note that fd should be blocking; non-blocking fds should be passed to net.Socket.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def default-input-options
  {
  ;  :flags "r" ;-> under the hood default
  ;  :encoding "utf8" ;-> under the hood default string literal
  ;  :mode "0o666" ;-> under the hood default
   :fd nil ;if specified, will ignore path argument, therefore no 'open' event
   :autoClose true})

(defn stream-reader [st opts] st) ; good place to setup async handlers?


(defn attach-input-impls! [filestreamobj]
  (let [filedesc      (atom nil)
        _             (set! (.-constructor filestreamobj) :FileInputStream)
        _             (.on filestreamobj "open" (fn [fd] (reset! filedesc fd )))]
    (specify! filestreamobj
      IOFactory
      (make-reader [this opts] (stream-reader this opts))
      (make-input-stream [this _] this)
      (make-writer [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an OutputStream."))))
      (make-output-stream [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an OutputStream."))))
      Object
      (getFd [_] @filedesc))))

(defmulti FileInputStream* file-stream-dispatch)

(defmethod FileInputStream* :file-descriptor [fd opts]
  (let [filestreamobj (.createReadStream fs nil (clj->js opts))]
    (attach-input-impls! filestreamobj)))

(defmethod FileInputStream* :string [pathstring opts] ;should never reach this via IOFactor, should be coerced to file or Uri
  (let [filestreamobj (.createReadStream fs pathstring (clj->js opts)) ] ;should check path validity, URI too
    (attach-input-impls! filestreamobj)))

(defmethod FileInputStream* :file [file opts]
  (let [filestreamobj (.createReadStream fs (.getPath file) (clj->js opts))] ;handle path, URI too
    (attach-input-impls! filestreamobj)))

(defn FileInputStream ; file|string|file-descriptor => stream-object => Buffer
  ([file] (FileInputStream* file default-input-options))
  ([file opts] (FileInputStream* file (merge default-input-options opts))))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def default-output-options
  { :flags "w" ;r+ = append?
    :defaultEncoding "utf8" ; can be any encoding accepted by Buffer
    :fd nil ;if specified, will ignore path argument, therefore no 'open' event
    ; :start -> used to write at some postion past the beginning of the file
    ; :mode "0o666" ;needs to be parsed
   })

(defn attach-output-impls! [filestreamobj]
  (let [filedesc      (atom nil)
        _             (set! (.-constructor filestreamobj) :FileOutputStream)
        _             (.on filestreamobj "open" (fn [fd] (reset! filedesc fd )))]
    (specify! filestreamobj
      IOFactory
      (make-reader [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an InputStream."))))
      (make-input-stream [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an InputStream."))))
      (make-writer [this _] this)
      (make-output-stream [this _] this)
      Object
      (getFd [_] @filedesc))))


(defmulti FileOutputStream* (fn [f opts] (type f)))

(defmethod FileOutputStream* :string [pathstring opts] ;should never reach this via IOFactor, should be coerced to file or Uri
  (let [filestreamobj (.createWriteStream fs pathstring (clj->js opts)) ] ;should check path validity, URI too
    (attach-output-impls! filestreamobj)))

(defn FileOutputStream
  ([file] (FileOutputStream* file default-output-options))
  ([file opts] (FileOutputStream* file (merge default-output-options opts))))



    ;
    ; writeStream.bytesWritten#
    ; The number of bytes written so far. Does not include data that is still queued for writing.
