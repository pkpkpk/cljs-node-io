
## clojure.java.io -> 'cljs-node-io.core
  - ##### https://clojure.github.io/clojure/clojure.java.io-api.html

  * [ ]__copy__
    - [ ] Test
    - ```(copy input output & opts)```
    - Copies input to output.  Returns nil or throws IOException.
    - __Input__ can be:
      - [ ] InputStream
      - [ ] Reader
      - [ ] File
      - [ ] byte[]
      - [ ] String
    - __Output__ may be:
      - [ ] OutputStream
      - [ ] Writer
      - [ ] File
      * [ ] string (coerced to file, not in clj api)
    - Options are key/value pairs and may be one of
      - __:buffer-size__  buffer size to use, default is 1024.
      - __:encoding__     encoding to use if converting between byte and char streams.      
    * __COMBINATIONS__:
      * [x] file file sync
      * [ ] file file stream
      * [x] file string sync
      * [ ] file string stream      
      * [x] string string sync
      * [ ] string string stream                        







  * [x]__delete-file__
    - ```(delete-file f & [silently])```
    - Delete file f. Raise an exception if it fails unless silently is true.
  * [x]__file__
    - ```(file arg)```
    - ```(file parent child)```
    - ```(file parent child & more)```
    - Returns a java.io.File, passing each arg to as-file.  Multiple-arg versions treat the first argument as parent and subsequent args as children relative to the parent.      
  * [x]__make-parents__
    - ```(make-parents f & more)```
    - Given the same arg(s) as for file, creates all parent directories of the file they represent.
  * [ ]__reader__
  * [ ]__writer__
  * [ ]__resource__ (??)
  * __input-stream__
    * [ ] inputstream
    * [ ] File
    * [ ] URI, URL
    * [ ] Socket
    * [ ] byteArray
    * [ ] string
  * __output-stream__
    * [ ] outputstream
    * [ ] File
    * [ ] URI, URL
    * [ ] Socket
    * [ ] byteArray
    * [ ] string
  * [x]__as-relative-path__
    - ```(as-relative-path x) ```
    - Take an as-file-able thing and return a string if it is a relative path, else IllegalArgumentException.
## extras
  *  ### aspit, aslurp, asslurp?
  * __spit__
    - [x] sync
    - [x] async   
  * __slurp__
    - [x] sync
     -  NOT bufferedFileReader+FileStream as in clojure. Nodejs's streams are created
      asynchronously and would require slurp to return a channel. This uses
      FS.readFileSync, fine for small files. Use FileInputStream for more flexibility
    - [x] async
  * [x]__sslurp__
    - *super* slurp, convenience over slurp
    - automatically reads edn+json file into clj data-structures
  * [ ]__file-seq + test__  
  * [ ]__line-seq  + test __
  * [ ]__xml-seq  + test __

## File API
  * ##### https://docs.oracle.com/javase/8/docs/api/java/io/File.html


  * [x] __canExecute()__ -> boolean
    - [ ] test
    - Return: true iff file specified by this abstract pathname exists and the application is allowed to execute the file
    - Tests whether the application can execute the file denoted by this abstract pathname.
    - has no effect on Windows
    - SYNCHRONOUS
  * [x] __canRead()__ -> boolean
    - [ ] test
    - Tests whether the application can read the file denoted by this abstract pathname.
    - Return: true iff file specified by this abstract pathname exists and can be read by the application; false otherwise
    - SYNCHRONOUS
  * [x] __canWrite()__ -> boolean
    - [ ] Test
    - Tests whether the application can modify the file denoted by this abstract pathname.
    - Return: true iff file specified by this abstract pathname exists and the application is allowed to execute the file    
    - SYNCHRONOUS
  <hr>
  * [x] __createNewFile()__ -> boolean
    - [x] Test
    - Atomically creates a new, empty file named by this abstract pathname iff a file with this name does not yet exist.
    - Returns true if the named file does not exist and was successfully created; false if the named file already exists
    - SYNCHRONOUS
  * [x] __delete()__ -> boolean
    - [x] test for files
    - [ ] test for directories
    - Deletes the file or directory denoted by this abstract pathname.
    - If this pathname denotes a directory, then the directory must be empty in order to be deleted.
    - SYNCHRONOUS
  * [x] __deleteOnExit()__ -> void
    - Requests that the file or directory denoted by this abstract pathname be deleted when the virtual machine terminates.
    - beware memory leak when using figwheel
  * [x] __equals(Object obj)__ -> boolean
    - Tests this abstract pathname for equality with the given object.
  * [x] __exists()__ -> boolean
    - Tests whether the file or directory denoted by this abstract pathname exists.
  * [x] __getAbsoluteFile()__ -> :File
    - Returns the absolute form of this abstract pathname.
  * [x] __getAbsolutePath()__ -> String
    - Returns the absolute pathname string of this abstract pathname.
  * [x] __getCanonicalFile()__ -> :File
    - Returns the canonical form of this abstract pathname.
  * [x] __getCanonicalPath()__ -> String
    - Returns the canonical pathname string of this abstract pathname.
  * [x] __getName()__ -> string
    - Returns the name of the file or directory denoted by this abstract pathname.
  * [x] __getParent()__ -> string
    - Returns the pathname string of this abstract pathname's parent, or null if this pathname does not name a parent directory.
  * [x] __getParentFile()__ -> :File
    - Returns the abstract pathname of this abstract pathname's parent, or null if this pathname does not name a parent directory.
  * [x] __getPath()__ -> String
    - Converts this abstract pathname into a pathname string.
  * [x] __hashCode()__ -> int
    - just uses cljs's hash function on the path string
    - Computes a hash code for this abstract pathname.    
  * [x] __isAbsolute()__ -> boolean
    - Tests whether this abstract pathname is absolute.
  * [x] __isDirectory()__ -> boolean
    - Tests whether the file denoted by this abstract pathname is a directory.
  * [x] __isFile()__ -> boolean
    - Tests whether the file denoted by this abstract pathname is a normal file.
  * [x] __lastModified()__ -> ~~long~~ Int
    - Returns the time that the file denoted by this abstract pathname was last modified.
    - A value representing the time the file was last modified, measured in milliseconds since the epoch (00:00:00 GMT, January 1, 1970), or 0 if the file does not exist or if an I/O error occurs
  * [x] __length()__ -> ~~long~~ Int | nil
    - If this pathname denotes a directory -> nil
    - The length, in bytes, of the file denoted by this abstract pathname, or 0 if the file does not exist.
  * [x] __list()__ -> ~~String[]~~ Vector | nil
    - If this abstract pathname does not denote a directory, or if an I/O error occurs. -> nil
    - Returns an array of strings naming the files and directories in the directory denoted by this abstract pathname.
    - The array will be empty if the directory is empty.
  * [x] __list(FilenameFilter filterfn)__ -> ~~String[]~~ Vector | nil
    - Returns an array of strings naming the files and directories in the directory denoted by this abstract pathname that satisfy the specified filter.
    - filterfn(^File dir ^string name) -> boolean, true for files/dirs you wish to keep
  * [x] __listFiles()__ -> ~~File[]~~ Vector
    - Returns an array of abstract pathnames denoting the files in the directory denoted by this abstract pathname.
    - sugar for (.list f (fn [d name] (file? name)))
  * [x] __listFiles(FilenameFilter filterfn)__ -> ~~File[]~~ Vector
    - Returns an array of abstract pathnames denoting the Files in the directory denoted by this abstract pathname that satisfy the specified filter.
    - filterfn(^File dir ^string filename) -> boolean, true for files you wish to keep
    - use (as-file name-str) to get file-filtering behavior
  * [x] __mkdir()__ -> boolean
    - Creates the directory named by this abstract pathname.
    - true if and only if the directory was created; false otherwise
  * [x] __mkdirs()__ -> boolean
    - Creates the directory named by this abstract pathname, including any necessary but nonexistent parent directories.
    - true if and only if the directory was created, along with all necessary parent directories; false otherwise
    - Note that if this operation fails it may have succeeded in creating some of the necessary parent directories.
  * [x] __renameTo(File dest)__ -> boolean
    - Renames the file denoted by this abstract pathname.
    - true if and only if the renaming succeeded; false otherwise
  * [x] __toString()__ -> String
    - Returns the pathname string of this abstract pathname.
  * [x] __toURI()__ -> Uri
    - Constructs a file: URI that represents this abstract pathname.

<hr>
  ### reified file methods not implemented yet:

  * [ ] __compareTo(File pathname)__ -> int
    - Compares two abstract pathnames lexicographically.    
  * [ ] __getFreeSpace()__ -> Long
    - Returns the number of unallocated bytes in the partition named by this abstract path name.    
  * [ ] __getTotalSpace()__ -> long
    - Returns the size of the partition named by this abstract pathname.
  * [ ] __getUsableSpace()__ -> long
    - Returns the number of bytes available to this virtual machine on the partition named by this abstract pathname.
  * [ ] __isHidden()__ -> boolean
    - Tests whether the file named by this abstract pathname is a hidden file.    
  * [ ] __setExecutable(boolean executable)__ -> boolean
    - A convenience method to set the owner's execute permission for this abstract pathname.
  * [ ] __setExecutable(boolean executable, boolean ownerOnly)__ -> boolean
    - Sets the owner's or everybody's execute permission for this abstract pathname.
  * [ ] __setLastModified(long time)__ -> boolean
    - Sets the last-modified time of the file or directory named by this abstract pathname.
  * [ ] __setReadable(boolean readable)__ -> boolean
    - A convenience method to set the owner's read permission for this abstract pathname.
  * [ ] __setReadable(boolean readable, boolean ownerOnly)__ -> boolean
    - Sets the owner's or everybody's read permission for this abstract pathname.
  * [ ] __setReadOnly()__ -> boolean
    - Marks the file or directory named by this abstract pathname so that only read operations are allowed.
  * [ ] __setWritable(boolean writable)__ -> boolean
    - A convenience method to set the owner's write permission for this abstract pathname.
  * [ ] __setWritable(boolean writable, boolean ownerOnly)__ -> boolean
    - Sets the owner's or everybody's write permission for this abstract pathname.
  * ~~[ ] __listFiles(FileFilter filterfn)__ -> File[]~~
      - ~~Returns an array of abstract pathnames denoting the Files in the directory denoted by this abstract pathname that satisfy the specified filter.~~    
  * ~~[ ] __toPath()__ -> Path~~
    - ~~Returns a java.nio.file.Path object constructed from the this abstract path.~~

<hr>

## cljs-node-io.file functions
  * File constructors

  * [ ] __static File	createTempFile(String prefix, String suffix)__
    - Creates an empty file in the default temporary-file directory, using the given prefix and suffix to generate its name.
  * [ ] __static File	createTempFile(String prefix, String suffix, File directory)__
    - Creates a new empty file in the specified directory, using the given prefix and suffix strings to generate its name.
  * [ ] __static File[]	listRoots()__
    - List the available filesystem roots.


<hr>
# todo
  + add file-global toggle for async or separate named methods?
  - add mode to supported opts for reader & writer,  (js/parseInt "0666" 8) , === 0o666 ES6 octal literal
  * verify opts keys through all paths. :append? :async? :stream?
    - should be :append like clojure semantics
  * clj tests rely on correctness of java classes, so mock java
     stuff all needs appropriate tests and full api
  * verify degenerate cases, type returns
  * refactor streams, ditch specify! pattern, just manage path better?
  * path type for normalizing across platforms?
  * error handling can be better, move away from generic js/Error.
  * gcl node-object-stream support
  * object streams, duplex streams, transform streams
  * support other runtimes? JSC, nashorn
  * sugar for native streams, duplex + transform too
  * https://github.com/Raynes/fs
  * Iequiv should check path AND get-type impl
  * ###### java URL has unique set of methods that should be extended to goog.Uri
    * openStream -> opens connection to this URL and returns an input stream for reading
      https://docs.oracle.com/javase/7/docs/api/java/net/URL.html#openStream()
  * test isFd?
  * slurp + spit encodings are broken
  * delete-file should handle absolute paths, not just file objects
  * file reader needs a read, readline methods.
  * line-seq needs stream, probably must be async (breaking from clj)
  * defrecord SomeError [cause context ....]



## examples to do
* streaming mp3 through websocket
* clojure.data.csv port, m3u example?
* cookbook io examples
* large csv file reading via streamsk
* transit
* encodings
* webcrawler?




### Notes
  * biased towards sync calls, async makes for poor repl experience
  * consolidated URL & URI, goog.net.Uri is great
  * node streams close themselves, cleanup automatically?
  * polymorphic java constructors (ie java.io.File)
    necessitate alot of indirection
  * default-impl-obj + specify! is a cool pattern

; read [] -> int , reads a byte of data from this inputstream
; read [^byteArray b] -> int ,  Reads up to b.length bytes of data from this input stream into an array of bytes.
; read [^byteArray b, ^int off, ^int len] -> int ,   Reads up to len bytes of data from this input stream into an array of bytes.
