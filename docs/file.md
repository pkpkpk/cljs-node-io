# cljs-node-io.file


### Global Functions

  * __File(^str path)__  -> File
  * __File(^Uri path)__  -> File  
  * __File(^str parent ^str child)__  -> File
  * __File(^File parent ^str child)__  -> File
    - Builds an in-memory reified file. See method documentation below
    - constructor params are intended to match the java.io.File api
    - see https://docs.oracle.com/javase/7/docs/api/java/io/File.html





  * __createTempFile(^String prefix)__ -> File
  * __createTempFile(^String prefix, ^String suffix)__ -> File
  * __createTempFile(^String prefix, ^String suffix, ^File directory)__ -> File
    - Creates an empty file in the default temporary-file directory, using the given prefix and suffix to generate its name. If directory is specified, reates a new empty file in the specified directory.
    - suffix defaults to ".tmp". use "" to create tempdirs
    - nothing is written to disk yet, same as regular files
    - Returns a file object for the temp file

<hr>

### File Methods
  * [x] __createNewFile()__ -> boolean
    - [x] Test
    - Atomically creates a new, empty file named by this abstract pathname iff a file with this name does not yet exist.
    - Returns true if the named file does not exist and was successfully created; false if the named file already exists
    - synchronous


  * [x] __delete()__ -> boolean
    - [x] tested for files
    - [x] tested for directories
    - Deletes the file or directory denoted by this abstract pathname.
    - If this pathname denotes a directory, then the directory must be empty in order to be deleted.
    - synchronous


  * [x] __deleteOnExit()__ -> void
    - Requests that the file or directory denoted by this abstract pathname be deleted when the virtual machine terminates.


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
    - [x] tested
    - If this abstract pathname does not denote a directory, or if an I/O error occurs. -> nil
    - Returns an array of strings naming the files and directories in the directory denoted by this abstract pathname.
    - The array will be empty if the directory is empty.


  * [x] __list( filenameFilterFn )__ -> ~~String[]~~ Vector | nil
    - [x] tested
    - Returns an array of strings naming the files and directories in the directory denoted by this abstract pathname that satisfy the specified filter.
    - filterfn(^File dir ^string name) -> boolean, true for files/dirs you wish to keep


  * [x] __listFiles()__ -> ~~File[]~~ Vector
    - [x] tested
    - Returns an array of abstract pathnames denoting the files in the directory denoted by this abstract pathname.
    - sugar for (.list f (fn [d name] (file? name)))


  * [x] __listFiles( filenameFilterFn )__ -> ~~File[]~~ Vector
    - [x] tested
    - Returns an array of abstract pathnames denoting the Files in the directory denoted by this abstract pathname that satisfy the specified filter.
    - filterfn(^File dir ^string filename) -> boolean, true for files you wish to keep
    - use (as-file name-str) to get file-filtering behavior


  * [x] __mkdir()__ -> boolean
    - [x] tested
    - Creates the directory named by this abstract pathname.
    - true if and only if the directory was created; false otherwise


  * [x] __mkdirs()__ -> boolean
    - [x] tested
    - Creates the directory named by this abstract pathname, including any necessary but nonexistent parent directories. Inclusively creates the last path destination too... consider core/make-parents if this was not your intent.
    - true iff the directory was created, along with all necessary parent directories; false otherwise
    - Note: if fails, it may have succeeded in creating some of the necessary parent directories.


  * [x] __renameTo(^string dest)__ -> boolean
    - Renames the file denoted by this abstract pathname.
    - true if and only if the renaming succeeded; false otherwise


  * [x] __toString()__ -> String
    - Returns the pathname string of this abstract pathname.


  * [x] __toURI()__ -> Uri
    - Constructs a file: URI that represents this abstract pathname.


  * [x] __canExecute()__ -> boolean
    - [ ] test
    - Return: true iff file specified by this abstract pathname exists and the application is allowed to execute the file
    - Tests whether the application can execute the file denoted by this abstract pathname.
    - has no effect on Windows
    - synchronous


  * [x] __canRead()__ -> boolean
    - [ ] test
    - Tests whether the application can read the file denoted by this abstract pathname.
    - Return: true iff file specified by this abstract pathname exists and can be read by the application; false otherwise
    - synchronous


  * [x] __canWrite()__ -> boolean
    - [ ] Test
    - Tests whether the application can modify the file denoted by this abstract pathname.
    - Return: true iff file specified by this abstract pathname exists and the application is allowed to execute the file
    - synchronous


  * [x] __setReadable(boolean readable)__ -> boolean
    - A convenience method to set the owner's read permission for this abstract pathname.


  * [x] __setReadable(boolean readable, boolean ownerOnly)__ -> boolean
    - Sets the owner's or everybody's read permission for this abstract pathname.


  * [x] __setWritable(boolean writable)__ -> boolean
    - A convenience method to set the owner's write permission for this abstract pathname.


  * [x] __setWritable(boolean writable, boolean ownerOnly)__ -> boolean
    - Sets the owner's or everybody's write permission for this abstract pathname.


  * [x] __setExecutable(boolean executable)__ -> boolean
    - A convenience method to set the owner's execute permission for this abstract pathname.


  * [x] __setExecutable(boolean executable, boolean ownerOnly)__ -> boolean
    - Sets the owner's or everybody's execute permission for this abstract pathname.


  * [x] __setReadOnly()__ -> boolean
    - Marks the file or directory named by this abstract pathname so that only read operations are allowed.


  * [x] __setLastModified(long time)__ -> boolean
    - Sets the last-modified time of the file or directory named by this abstract pathname.


  * [x] __isHidden()__ -> boolean
    - Tests whether the file named by this abstract pathname is a hidden file.

<hr>    

### not implemented :
  + compareTo
  + listFiles(FileFilter filterfn)
  + toPath
  + getFreeSpace
  + getTotalSpace
  + getUsableSpace
