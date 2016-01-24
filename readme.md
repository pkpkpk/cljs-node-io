
## clojure.java.io => 'cljs-node-io.core
  - ##### https://clojure.github.io/clojure/clojure.java.io-api.html
  * [x]__as-relative-path__
    - ```(as-relative-path x) ```
    - Take an as-file-able thing and return a string if it is a relative path, else IllegalArgumentException.
  * [ ]__copy__
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
    - Options are key/value pairs and may be one of
      - __:buffer-size__  buffer size to use, default is 1024.
      - __:encoding__     encoding to use if converting between byte and char streams.  
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

## extras
  * __spit__
    - [x] sync
    - [ ] async   
  * __slurp__
    - [x] sync
     -  NOT bufferedFileReader+FileStream as in clojure. Nodejs's streams are created
      asynchronously and would require slurp to return a channel. This uses
      FS.readFileSync, fine for small files. Use FileInputStream for more flexibility
    - [ ] async
  * [x]__sslurp__
    - *super* slurp, convenience over slurp
    - automatically reads edn+json file into clj data-structures
  * [ ]__file-seq + test__  
  * [ ]__line-seq  + test __
  * [ ]__xml-seq  + test __

## File API
  * #### https://docs.oracle.com/javase/8/docs/api/java/io/File.html
  * [ ] __canExecute()__ => boolean
    - Tests whether the application can execute the file denoted by this abstract pathname.
  * [ ] __canRead()__ => boolean
    - Tests whether the application can read the file denoted by this abstract pathname.
  * [ ] __canWrite()__ => boolean
    - Tests whether the application can modify the file denoted by this abstract pathname.
  * [ ] __compareTo(File pathname)__ => int
    - Compares two abstract pathnames lexicographically.
  * [ ] __createNewFile()__ => boolean
    - Atomically creates a new, empty file named by this abstract pathname if and only if a file with this name does not yet exist.
  * [ ] __delete()__ => boolean
    - Deletes the file or directory denoted by this abstract pathname.
  * [ ] __deleteOnExit()__ => void
    - Requests that the file or directory denoted by this abstract pathname be deleted when the virtual machine terminates.
  * [ ] __equals(Object obj)__ => boolean
    - Tests this abstract pathname for equality with the given object.
  * [ ] __exists()__ => boolean
    - Tests whether the file or directory denoted by this abstract pathname exists.
  * [ ] __getAbsoluteFile()__ => File
    - Returns the absolute form of this abstract pathname.
  * [ ] __getAbsolutePath()__ => String
    - Returns the absolute pathname string of this abstract pathname.
  * [ ] __getCanonicalFile()__ => File
    - Returns the canonical form of this abstract pathname.
  * [ ] __getCanonicalPath()__ => String
    - Returns the canonical pathname string of this abstract pathname.
  * [ ] __getFreeSpace()__ => Long
    - Returns the number of unallocated bytes in the partition named by this abstract path name.
  * [ ] __getName()__ => string
    - Returns the name of the file or directory denoted by this abstract pathname.
  * [ ] __getParent()__ => string
    - Returns the pathname string of this abstract pathname's parent, or null if this pathname does not name a parent directory.
  * [ ] __getParentFile()__ => File
    - Returns the abstract pathname of this abstract pathname's parent, or null if this pathname does not name a parent directory.
  * [ ] __getPath()__ => String
    - Converts this abstract pathname into a pathname string.
  * [ ] __getTotalSpace()__ => long
    - Returns the size of the partition named by this abstract pathname.
  * [ ] __getUsableSpace()__ => long
    - Returns the number of bytes available to this virtual machine on the partition named by this abstract pathname.
  * [ ] __hashCode()__ => int
    - Computes a hash code for this abstract pathname.
  * [ ] __isAbsolute()__ => boolean
    - Tests whether this abstract pathname is absolute.
  * [x] __isDirectory()__ => boolean
    - Tests whether the file denoted by this abstract pathname is a directory.
  * [ ] __isFile()__ => boolean
    - Tests whether the file denoted by this abstract pathname is a normal file.
  * [ ] __isHidden()__ => boolean
    - Tests whether the file named by this abstract pathname is a hidden file.
  * [ ] __lastModified()__ => long
    - Returns the time that the file denoted by this abstract pathname was last modified.
  * [ ] __length()__ => long
    - Returns the length of the file denoted by this abstract pathname.
  * [ ] __list()__ => String[]
    - Returns an array of strings naming the files and directories in the directory denoted by this abstract pathname.
  * [ ] __list(FilenameFilter filter)__ => String[]
    - Returns an array of strings naming the files and directories in the directory denoted by this abstract pathname that satisfy the specified filter.
  * [ ] __listFiles()__ => File[]
    - Returns an array of abstract pathnames denoting the files in the directory denoted by this abstract pathname.
  * [ ] __listFiles(FileFilter filter)__ => File[]
    - Returns an array of abstract pathnames denoting the files and directories in the directory denoted by this abstract pathname that satisfy the specified filter.
  * [ ] __listFiles(FilenameFilter filter)__ => File[]
    - Returns an array of abstract pathnames denoting the files and directories in the directory denoted by this abstract pathname that satisfy the specified filter.
  * [ ] __mkdir()__ => boolean
    - Creates the directory named by this abstract pathname.
  * [ ] __mkdirs()__ => boolean
    - Creates the directory named by this abstract pathname, including any necessary but nonexistent parent directories.
  * [ ] __renameTo(File dest)__ => boolean
    - Renames the file denoted by this abstract pathname.
  * [ ] __setExecutable(boolean executable)__ => boolean
    - A convenience method to set the owner's execute permission for this abstract pathname.
  * [ ] __setExecutable(boolean executable, boolean ownerOnly)__ => boolean
    - Sets the owner's or everybody's execute permission for this abstract pathname.
  * [ ] __setLastModified(long time)__ => boolean
    - Sets the last-modified time of the file or directory named by this abstract pathname.
  * [ ] __setReadable(boolean readable)__ => boolean
    - A convenience method to set the owner's read permission for this abstract pathname.
  * [ ] __setReadable(boolean readable, boolean ownerOnly)__ => boolean
    - Sets the owner's or everybody's read permission for this abstract pathname.
  * [ ] __setReadOnly()__ => boolean
    - Marks the file or directory named by this abstract pathname so that only read operations are allowed.
  * [ ] __setWritable(boolean writable)__ => boolean
    - A convenience method to set the owner's write permission for this abstract pathname.
  * [ ] __setWritable(boolean writable, boolean ownerOnly)__ => boolean
    - Sets the owner's or everybody's write permission for this abstract pathname.
  * [ ] __toPath()__ => Path
    - Returns a java.nio.file.Path object constructed from the this abstract path.
  * [ ] __toString()__ => String
    - Returns the pathname string of this abstract pathname.
  * [ ] __toURI()__ => Uri
    - Constructs a file: URI that represents this abstract pathname.
  * [ ] __static File	createTempFile(String prefix, String suffix)__
    - Creates an empty file in the default temporary-file directory, using the given prefix and suffix to generate its name.
  * [ ] __static File	createTempFile(String prefix, String suffix, File directory)__
    - Creates a new empty file in the specified directory, using the given prefix and suffix strings to generate its name.
  * [ ] __static File[]	listRoots()__
    - List the available filesystem roots.



# todo
  * clj tests rely on correctness of java classes, so mock java
     stuff all needs appropriate tests and full api
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
    * openStream => opens connection to this URL and returns an input stream for reading
      https://docs.oracle.com/javase/7/docs/api/java/net/URL.html#openStream()
  * test isFd?
  * slurp + spit encodings are broken
  * delete-file should handle absolute paths, not just file objects
  * file reader needs a read, readline methods.
  * line-seq needs stream, probably must be async (breaking from clj)




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

; read [] => int , reads a byte of data from this inputstream
; read [^byteArray b] => int ,  Reads up to b.length bytes of data from this input stream into an array of bytes.
; read [^byteArray b, ^int off, ^int len] => int ,   Reads up to len bytes of data from this input stream into an array of bytes.
