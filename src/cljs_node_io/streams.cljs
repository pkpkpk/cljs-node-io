(ns cljs-node-io.streams
  (:require [cljs.nodejs :as nodejs :refer [require]]
            [cljs-node-io.protocols
              :refer [IGetType get-type
                      Coercions as-url as-file
                      IOFactory make-reader make-writer make-input-stream make-output-stream]])
  )

(def fs (require "fs"))


(def default-options
  {:flags "r"
   :encoding "utf8"
  ;  :fd nil
   :mode "0o666"
   :autoClose true})

(defmulti FileInputStream* get-type) ;if gonna check for isFd need custom dispatcher here

(defmethod FileInputStream* :file-descriptor [& args] nil) ;maybe can ignore those because node checks for this internally

(defmethod FileInputStream* :string [pathstring] ;should never reach this, should be coerced to file or URi
  (let [filestreamobj (.createReadStream fs pathstring)] ;should check path validity, URI too
    (specify! filestreamobj
      IGetType
      (get-type [_] :FileInputStream)
      IOFactory
      (make-reader [this opts] this)
      )
    filestreamobj))

(defmethod FileInputStream* :file [file]
  (let [filestreamobj (.createReadStream fs (.getPath file))] ;handle path, URI too
    (specify! filestreamobj
      IGetType
      (get-type [_] :FileInputStream)
      IOFactory
      (make-reader [this opts] this)
      (make-input-stream [this opts] (println  "tried to make input stream out of existing stream") this)
      )
    filestreamobj))



(defn FileInputStream
  ([file] (FileInputStream* file))
  ([file opts] (FileInputStream* file)))


(deftype FileOutputStream [x y])
