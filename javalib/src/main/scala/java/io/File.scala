package java.io

import scala.transient
import scalanative.native._, stdlib._, stdio._, string._
import java.util.LinkedList
import java.util.HashMap

class File private () extends Serializable with Comparable[File] {
    import File._
  	
    private var path: String =_

    def compareTo(file: File): Int = {
        if(caseSensitive) getPath().compareTo(file.getPath())
        else this.getPath().compareToIgnoreCase(file.getPath())
    }

  	@transient var properPath: Array[Byte] = _

	def this(dir: File, name: String) = {
        this()
		if(name == null) throw new NullPointerException()
		if(dir == null) path = fixSlashes(name)
		else path = calculatePath(dir.getPath(), name)
	}

    def this(dirPath: String) = {
        this()
        path = fixSlashes(dirPath)
    }


    def this(dirPath: String, name: String) = {
        this()
        if(name == null) throw new NullPointerException()
        if(dirPath == null) path = fixSlashes(name)
        else path = calculatePath(dirPath, name)
    }

    def getPath(): String = new String(path) 

    private def calculatePath(dirPath: String, dirName: String): String = {
    	val path: String = fixSlashes(dirPath)
    	if(!dirName.isEmpty || path.isEmpty){
    		var name: String = fixSlashes(dirName)
    		var separatorIndex: Int = 0;

    		while(separatorIndex < name.length() && 
    			name(separatorIndex) == File.separatorChar){
    			separatorIndex += 1
    		}

    		if(separatorIndex > 0){
    			name = name.substring(separatorIndex, name.length())
    		}

    		val pathLength: Int = path.length()
    		if(pathLength > 0 && path(pathLength-1) == File.separatorChar){
    			path + name
    		} else path + File.separatorChar + name
    	}
    	else path
    }

    private def fixSlashes(origPath: String): String = {
        var uncIndex: Int = 1
        var length: Int = origPath.length() 
        var newLength: Int = 0
        if (File.separatorChar == '/') {
            uncIndex = 0;
        } else if (length > 2 && origPath.charAt(1) == ':') {
            uncIndex = 2;
        }

        var foundSlash: Boolean = false
        var newPath: Array[Char] = origPath.toCharArray()
        for(i <- 0 until length) {
            var pathChar: Char = newPath(i)
            if ((File.separatorChar == '\\') && (pathChar == '\\')
                || (pathChar == '/')) {
                /* UNC Name requires 2 leading slashes */
                if ((foundSlash && (i == uncIndex)) || !foundSlash) {
                    newLength += 1
                    newPath(newLength) = File.separatorChar
                    foundSlash = true
                }
            } else {
                // check for leading slashes before a drive
                if ((pathChar == ':')
                        && (uncIndex > 0)
                        && ((newLength == 2) || ((newLength == 3) && (newPath(1) == File.separatorChar)))
                        && (newPath(0) == File.separatorChar)) {
                    newPath(0) = newPath(newLength - 1)
                    newLength = 1
                    // allow trailing slash after drive letter
                    uncIndex = 2
                }
                newLength += 1
                newPath(newLength) = pathChar
                foundSlash = false
            }
        }
        // remove trailing slash
        if (foundSlash
                && (newLength > (uncIndex + 1) || (newLength == 2 && newPath(0) != File.separatorChar))) {
            newLength -= 1
        }
        return new String(newPath, 0, newLength)
    }

    def canRead(): Boolean = {
        if (path.length() == 0) {
            return false
        }
        var pp: Array[Byte] = properPath(true)
        return existsImpl(pp) && !isWriteOnlyImpl(pp)
    }

    def canWrite(): Boolean = {
        // Cannot use exists() since that does an unwanted read-check.
        var exists: Boolean = false
        if (path.length() > 0) {
            exists = existsImpl(properPath(true))
        }
        return exists && !isReadOnlyImpl(properPath(true))
    }

    def delete(): Boolean = {
        var propPath: Array[Byte] = properPath(true)
        if ((path.length() != 0) && isDirectoryImpl(propPath)) {
            return deleteDirImpl(propPath)
        }
        else return deleteFileImpl(propPath);
    }

    //native funct.
    @throws(classOf[IOException])
    private def deleteDirImpl(filePath: Array[Byte]): Boolean = {filePathCopy(filePath)
        var pathCopy: CString = filePathCopy(filePath)
        val result: Int = remove(pathCopy)
        return result == 0
    }

    //native funct.
    @throws(classOf[IOException])
    private def deleteFileImpl(filePath: Array[Byte]): Boolean = {
        var pathCopy: CString = filePathCopy(filePath)
        var result: Int = unistd.unlink(pathCopy)   
        return result == 0
    }

    /*
     *Small utilitary function to achieve modularity.
     *transform an Array of Byte to a CString 
     *add the null terminating char at the end.
     * can throw an IO exception if the path is too long
     */
    @throws(classOf[IOException])
    private def filePathCopy(filePath: Array[Byte]): CString = {
        val pathCopy: CString = stackalloc[CChar](HyMaxPath)
        val length: Int = filePath.length
        if(length > (HyMaxPath-1)){
            throw new IOException("Path length of "+length+" characters exceeds maximum supported length of "+HyMaxPath)
        }else{  
            for(i <- 0 until length){
                    pathCopy(i) = filePath(i)
            }
            pathCopy(length) = '\0'   
            return pathCopy
        }
    }

    //need to understand how to use deleteOnExit
    def deleteOnExit(): Unit =  ??? //atexit{ FunctionPtr(delete()) }

    
    override def equals(obj: Any): Boolean = {
        if (!(obj.isInstanceOf[File])) {
            return false
        }
        if (!caseSensitive) {
            return path.equalsIgnoreCase((obj.asInstanceOf[File]).getPath())
        }
        return path.equals((obj.asInstanceOf[File]).getPath())
    }

    def exists(): Boolean = {
        if (path.length() == 0) {
            return false;
        }
        return existsImpl(properPath(true));
    }

    //native funct.
    def existsImpl(filePath: Array[Byte]): Boolean = {
        var pathCopy: CString = filePathCopy(filePath)
        return (CFile.file_attr(pathCopy) >= 0)
    }

    def getAbsolutePath(): String = HyUtil.toUTF8String(properPath(true))

    def getAbsoluteFile(): File = new File(this.getAbsolutePath())

    @throws(classOf[IOException])
    def getCanonicalPath(): String = {
        var result: Array[Byte] = properPath(false)
        var absPath: String = HyUtil.toUTF8String(result)
        var canonPath: String = FileCanonPathCache.get(absPath);
        if (canonPath != null) {
            return canonPath
        }
        if(File.separatorChar == '/') {
            // resolve the full path first
            result = resolveLink(result, result.length, false)
            // resolve the parent directories
            result = resolve(result)
        }
        var numSeparators: Int = 1;
        for (i <- 0 until result.length) {
            if (result(i) == File.separatorChar) {
                numSeparators += 1
            }
        }
        var sepLocations: Array[Int] = new Array[Int](numSeparators)
        var rootLoc: Int = 0
        if (File.separatorChar != '/') {
            if (result(0) == '\\') {
                rootLoc = if(result.length > 1 && result(1) == '\\') 1 else 0
            } else {
                rootLoc = 2 // skip drive i.e. c:
            }
        }
        var newResult: Array[Byte] = new Array[Byte](result.length + 1)
        var newLength: Int = 0 
        var lastSlash: Int = 0 
        var foundDots: Int = 0
        sepLocations(lastSlash) = rootLoc

        try{
            for(i <- 0 to result.length){
                if (i < rootLoc) {
                    newLength += 1
                    newResult(newLength) = result(i)
                } else {
                    if (i == result.length || result(i) == File.separatorChar) {
                        if (i == result.length && foundDots == 0) {
                            throw Break
                        }
                        if (foundDots == 1) {
                            /* Don't write anything, just reset and continue */
                            foundDots = 0
                        }
                        if (foundDots > 1) {
                            /* Go back N levels */
                            lastSlash = if(lastSlash > (foundDots - 1)) 
                                            lastSlash - (foundDots - 1) 
                                        else 0
                            newLength = sepLocations(lastSlash) + 1
                            foundDots = 0
                        }
                        lastSlash += 1
                        sepLocations(lastSlash) = newLength
                        newLength += 1
                        newResult(newLength) = File.separatorChar.toByte
                    }
                    if (result(i) == '.') {
                        foundDots += 1
                    }
                    /* Found some dots within text, write them out */
                    if (foundDots > 0) {
                        for(j <- 0 until foundDots){
                            newLength += 1
                            newResult(newLength) = '.'.toByte
                        }
                    }
                    newLength += 1
                    newResult(newLength) = result(i)
                    foundDots = 0
                }
            }    
        } catch{ 
            case Break => endOfFunct
            case ioexcep : IOException => throw ioexcep
        }
        
        def endOfFunct: String = {
            // remove trailing slash
            if(newLength > (rootLoc + 1)
                    && newResult(newLength - 1) == File.separatorChar) {
                newLength -= 1
            }
            newResult(newLength) = 0
            newResult = getCanonImpl(newResult)
            newLength = newResult.length
            canonPath = HyUtil.toUTF8String(newResult, 0, newLength)
            FileCanonPathCache.put(absPath, canonPath)
            return canonPath
        }
        endOfFunct
    }

    @throws(classOf[IOException])
    private def resolve(newResult: Array[Byte]): Array[Byte] = {
        var last: Int = 1 

        //prev. unintialized
        var nextSize: Int = 0
        //prev. unintialized
        var linkSize: Int = 0
        var linkPath: Array[Byte] = newResult 
        //prev. unintialized
        var bytes: Array[Byte] = null
        //prev. unintialized
        var done: Boolean = false
        //prev. unintialized
        var inPlace: Boolean = false
        for (i <- 1 to newResult.length) {
            if (i == newResult.length || newResult(i) == File.separatorChar) {
                done = (i >= (newResult.length - 1))
                // if there is only one segment, do nothing
                if (done && linkPath.length == 1) {
                    return newResult
                }
                inPlace = false
                if (linkPath == newResult) {
                    bytes = newResult
                    // if there are no symbolic links, terminate the C string
                    // instead of copying
                    if (!done) {
                        inPlace = true
                        newResult(i) = '\0'
                    }
                } else {
                    nextSize = i - last + 1
                    linkSize = linkPath.length
                    if (linkPath(linkSize - 1) == File.separatorChar) {
                        linkSize -= 1
                    }
                    bytes = new Array[Byte](linkSize + nextSize)
                    System.arraycopy(linkPath, 0, bytes, 0, linkSize);
                    System.arraycopy(newResult, last - 1, bytes, linkSize,
                            nextSize)
                    // the full path has already been resolved
                }
                if (done) {
                    return bytes
                }
                linkPath = resolveLink(bytes, if(inPlace) i else bytes.length, true);
                if (inPlace) {
                    newResult(i) = '/'
                }
                last = i + 1
            }
        }
        throw new InternalError();
    }

    @throws(classOf[IOException])
    private def resolveLink(pathBytesGiven: Array[Byte],
                            length: Int,
                            resolveAbsolute: Boolean): Array[Byte] ={
        var pathBytes: Array[Byte] = pathBytesGiven
        var restart: Boolean = false

        //previously uninitialized
        var linkBytes: Array[Byte] = null;
        
        //previously uninitialized
        var temp: Array[Byte] = null;
        try{
            do {
                linkBytes = getLinkImpl(pathBytes)
                if (linkBytes == pathBytes) {
                    throw Break
                }
                if (linkBytes(0) == File.separatorChar) {
                    // link to an absolute path, if resolving absolute paths,
                    // resolve the parent dirs again
                    restart = resolveAbsolute
                    pathBytes = linkBytes
                } else {
                    var last: Int = length - 1;
                    while (pathBytes(last) != File.separatorChar) {
                        last -= 1
                    }
                    last += 1
                    temp = new Array[Byte](last + linkBytes.length)
                    System.arraycopy(pathBytes, 0, temp, 0, last);
                    System.arraycopy(linkBytes, 0, temp, last, linkBytes.length);
                    pathBytes = temp;
                }
                //can't do that in scala.
                //length = pathBytes.length;
            } while (existsImpl(pathBytes))
        } catch{
            case Break => endOfFunct
            case e: IOException => throw e
        }
        def endOfFunct: Array[Byte] = if(restart) resolve(pathBytes) else pathBytes
        endOfFunct
    } 

    @throws(classOf[IOException])
    def getCannonicalFile(): File = new File(getCanonicalPath())

    //native funct.
    private def getCanonImpl(filePath: Array[Byte]): Array[Byte] = ???

    def getName(): String = {
        val separatorIndex: Int = path.lastIndexOf(separator)
        if(separatorIndex < 0) path else path.substring(separatorIndex, path.length())
    }

    def getParent(): String = {
        val length: Int = path.length()
        var firstInPath: Int = 0
        if(File.separatorChar == '\\' && length > 2 && path(1) == ':'){
            firstInPath = 2
        }
        var index: Int = path.lastIndexOf(File.separatorChar)
        if(index == -1 && firstInPath > 0) index = 2
        if(index == -1 || path(length -1) == File.separatorChar) return null
        if(path.indexOf(File.separatorChar) == index 
            && path(firstInPath) == File.separatorChar) return path.substring(0, index+1)
        return path.substring(0, index)
    }

    def getParentFile(): File = {
        var tempParent: String = getParent()
        if(tempParent == null) return null
        return new File(tempParent)
    }

    override def hashCode(): Int = if(caseSensitive) path.hashCode ^ 1234321
    else path.toLowerCase().hashCode ^ 1234321

    def isAbsolute(): Boolean = {
        if (File.separatorChar == '\\') {
            // for windows
            if (path.length() > 1 && path(0) == File.separatorChar
                    && path.charAt(1) == File.separatorChar) {
                return true
            }
            if (path.length() > 2) {
                if ((path(0).isLetter && (path(1) == ':') 
                    && (path(2) == '/' || path(2) == '\\'))) {
                    return true
                }
            }
            return false
        }

        // for Linux
        return (path.length() > 0 && path(0) == File.separatorChar);
    }

    def isDirectory(): Boolean = {
        if (path.length() == 0) {
            return false
        }
        return isDirectoryImpl(properPath(true))
    }

    //native funct.
    private def isDirectoryImpl(filePath: Array[Byte]): Boolean = {
        var pathCopy: CString = filePathCopy(filePath)
        return (CFile.file_attr(pathCopy) == 0)
    }

    def isFile(): Boolean = {
        if (path.length() == 0) {
            return false
        }
        return isFileImpl(properPath(true))
    }


    //native funct.
    private def isFileImpl(filePath: Array[Byte]): Boolean = {
        var pathCopy: CString = filePathCopy(filePath)
        return (CFile.file_attr(pathCopy) == 1)
    }

    def isHidden(): Boolean = if(path.length() == 0) false 
                              else isHiddenImpl(properPath(true))

    //native funct.
    private def isHiddenImpl(filePath: Array[Byte]): Boolean = {
        var pathCopy: CString = filePathCopy(filePath)
        val length: CSize = strlen(pathCopy)
        val existsResult: CInt = CFile.file_attr(pathCopy)
        if (existsResult < 0) return false
        if (length == 0) return true
        var index: Long = length
        while(index >= 0) {
          if (pathCopy(index) == '.' && (index > 0 && (pathCopy(index - 1) == '/')))
            return true
          index -= 1
        }
        return false
    }

    //native funct.
    private def isReadOnlyImpl(filePath: Array[Byte]): Boolean = {
        val pathCopy: CString = filePathCopy(filePath)
        return (unistd.access(pathCopy, fcntl.W_OK) !=0)
    }

    //native funct.
    private def isWriteOnlyImpl(filePath: Array[Byte]): Boolean = {
        val pathCopy: CString = filePathCopy(filePath)
        return (unistd.access(pathCopy, fcntl.R_OK) !=0)
    }
    //native funct.
    private def getLinkImpl(filePath: Array[Byte]): Array[Byte] = {
        var answer: Array[Byte] = null
        var pathCopy: CString = filePathCopy(filePath)
        if(platformReadLink(pathCopy)){
            //need to transform pathCopy into an Array of byte
            val length: CSize = strlen(pathCopy)
            answer = new Array[Byte](length.toInt)
            var index: Int = 0
            while(index < length){
                answer(index) = pathCopy(index)
                index += 1
            }
        }else{
            answer = filePath
        }
        return answer;
    }

    //scala way to write the C function platformReadLink(char*) in hyfile.c
    private def platformReadLink(link: CString): Boolean = {
        val size: CInt = unistd.readlink(link, link, HyMaxPath-1);
        if (size <= 0) return false
        link(size) = 0
        return true
    }

    def lastMofified(): Long = {
        val result: Long = lastModifiedImpl(properPath(true))
        result match{
            case 0 => 0
            case -1 => 0
            case _ => result
        }
    }

    //native funct.
    private def lastModifiedImpl(filePath: Array[Byte]): Long = {
        var pathCopy: CString = filePathCopy(filePath)
        return CFile.lastmod(pathCopy)
    }

    def setLastMofified(time: Long): Boolean = {
        //message corresponding to luni.B2 from apache messages.properties 
        if (time < 0) throw new IllegalArgumentException("time must be positive")
        return (setLastModifiedImpl(properPath(true), time));
    }

    //native funct.
    private def setLastModifiedImpl(filePath: Array[Byte], 
                                    time: Long): Boolean = {
        var pathCopy: CString = filePathCopy(filePath)
        CFile.setlastmod(pathCopy, time) match{
            case 0 => false
            case 1 => true 
        }   
    }

    def setReadOnly(): Boolean = setReadOnlyImpl(properPath(true))

    //native funct.
    def setReadOnlyImpl(path: Array[Byte]): Boolean = {
        var pathCopy: CString = filePathCopy(path)
        CFile.setReadOnlyNative(pathCopy) match{
            case 0 => false
            case 1 => true
        }

    }

    def length(): Long = lengthImpl(properPath(true))

    //native funct.
    private def lengthImpl(filePath: Array[Byte]): Long = {
        val pathCopy: CString = filePathCopy(filePath)
        val result: Long = CFile.file_length(pathCopy)
        if(result < 0){
            return 0L
        }else return result
    }

    def list(): Array[java.lang.String] = ???

    def listFiles(): Array[File] = ???

    def listFiles(filter: FilenameFilter): Array[File] = ???

    def listFiles(filter: FileFilter): Array[File] = ???

    def list(filter: FilenameFilter): Array[java.lang.String] = ???

    //native funct.
    private def listImpl(path: Array[Byte]): Array[Array[Byte]] = synchronized {
        ???
    }

    def mkdir(): Boolean = mkdirImpl(properPath(true))

    //native funct.
    private def mkdirImpl(filePath: Array[Byte]): Boolean = {
        val pathCopy: CString = filePathCopy(filePath)
        CFile.file_mkdir(pathCopy) == 0
    }

    def mkdirs(): Boolean = {
        if(exists()) return false
        if(mkdir()) return true
        val parentDir: String = getParent()

        if(parentDir == null) return false
        return (new File(parentDir).mkdirs() && mkdir())
    }

    @throws(classOf[IOException])
    def createNewFile(): Boolean = {
        if(path.length() == 0){
            //corresponds to the entry "luni.B3" of the
            //internal Messages module from apache. 
            throw new IOException("No such file or directory")   
        }
        newFileImpl(properPath(true)) match{
            case 0 => true
            case 1 => false
            //corresponds to the entry "luni.B4" of the
            //internal Messages module from apache.
            case _ => throw new IOException("Cannot create "+ path)
        }
    }

    //native funct.
    @throws(classOf[IOException])
    private def newFileImpl(filePath: Array[Byte]): Int = {
        val pathCopy: CString = filePathCopy(filePath)
        val portFD: CInt = CFile.new_file_impl(pathCopy)
        if(portFD == -1){
//TODO:
            //should find a way to treat the case of 
            //the file can't be created because it alread exists
            //return 2 in this case
            return 1
        }
        CFile.fileDescriptor_close(portFD)
        return 0
    }

    def properPath(internal: Boolean): Array[Byte] = {
        if (properPath != null) {
            return properPath;
        }

        if (isAbsolute()) {
            var pathBytes: Array[Byte] = HyUtil.getUTF8Bytes(path);
            properPath = pathBytes;
            return properPath
        }
        // Check security by getting user.dir when the path is not absolute
        
        //prev. unintialized
        var userdir: String = ""
        /*if (internal) {
            userdir = AccessController.doPrivileged(new PriviAction[String](
                    "user.dir")); //$NON-NLS-1$
        } else*/
            userdir = fromCString(CFile.getUserDir())     

        if (path.length() == 0) {
            properPath = HyUtil.getUTF8Bytes(userdir);
            return properPath
        }
        var length: Int = userdir.length();

        // Handle windows-like path
        if (path(0) == '\\') {
            if (length > 1 && userdir(1) == ':') {
                properPath = HyUtil.getUTF8Bytes(userdir.substring(0, 2) + path);
                return properPath
            }
            path = path.substring(1);
        }

        // Handle separator
        var result: String = userdir;
        if (userdir(length - 1) != File.separatorChar) {
            if (path(0) != File.separatorChar) {
                result += separator;
            }
        } else if (path(0) == File.separatorChar) {
            result = result.substring(0, length - 2);

        }
        result += path;
        properPath = HyUtil.getUTF8Bytes(result)
        return properPath
    }

    def renameTo(des: java.io.File): Boolean = 
        renameToImpl(properPath(true), des.properPath(true))

    //native funct.
    private def renameToImpl(pathExists: Array[Byte], pathNew: Array[Byte]): Boolean = {
        //not actually sure it does rename anything even though I followed
        //the Java and C code.
        val oldPathCopy: CString = filePathCopy(pathExists)
        val newPathCopy: CString = filePathCopy(pathNew)
        rename(oldPathCopy, newPathCopy) match{
            case 0 => true
            case -1 => false
        }
    }

    override def toString(): String = path

    def getAbsoluteName(): String = {
        val f: File = getAbsoluteFile()
        var name: String = f.getPath()

        if (f.isDirectory() && name(name.length() - 1) != File.separatorChar) {
            // Directories must end with a slash
            name = new StringBuilder(name.length() + 1).append(name)
                    .append('/').toString()
        }
        if (File.separatorChar != '/') { // Must convert slashes.
            name = name.replace(File.separatorChar, '/')
        }
        return name;
    }
}

    //c file can be found in scala-native/nativelib/src/main/resources/
@extern object CFile{
    def separatorChar(): Char = extern
    def pathSeparatorChar(): Char = extern
    def isCaseSensitiveImpl(): Int = extern
    def getPlatformRoots(rootStrings: Ptr[CChar]) = extern
    def file_attr(path: CString): Int = extern
    def getOsEncoding(): CString = extern
    def getUserDir(): CString = extern
    def new_file_impl(path: CString): CInt = extern
    def fileDescriptor_close(fd: CInt): CInt = extern
    def lastmod(path: CString): CSize = extern
    def file_length(path: CString): Long = extern
    def file_mkdir(path: CString): Int = extern
    def setReadOnlyNative(path: CString): Int = extern
    def setlastmod(path: CString, time: Long): Int = extern
}

    //C function utilized to remove the file.
@extern object unistd{
    def unlink(path: CString): CInt = extern
    def access(pathname: CString, mode: CInt): CInt = extern
    def readlink(path: CString, buf: CString, bufsize: CSize) = extern
}

/*@extern*/  object fcntl{
    val W_OK: Int = 2
    val R_OK: Int = 4
    val X_OK: Int = 1
    val F_OK: Int = 0
}

@extern object apr_time{
    def apr_time_now(): Long = extern
}

//implementation of the few used methods from
//org.apache.harmony.luni.internal.io.FileCanonPathCache
object FileCanonPathCache {

    private class CacheElement private () {
        var canonicalPath: String = _
        var timestamp: Long = _    
        
        def this(canonPath: String) = {
            this()
            canonicalPath = canonPath
            timestamp = apr_time.apr_time_now()
        }
    }

    @volatile
    var timeout: Long = 30000;

    private val CACHE_SIZE: Int = 256

    private val cache : HashMap[String, CacheElement] = new HashMap[String, CacheElement](CACHE_SIZE)

    private val list: LinkedList[String] = new LinkedList[String]()

    def put(path: String, canonicalPath: String):Unit = {
        if( timeout != 0){
            var element: CacheElement = new CacheElement(canonicalPath)
            synchronized /*(lock)*/ {
                if(cache.size() >= CACHE_SIZE){
                    val oldest: String = list.removeFirst()
                    cache.remove(oldest)
                }
                cache.put(path, element)
                list.addLast(path)
            }
   
        }
    }

    def get(path: String): String = {
        var localTimeout: Long = timeout;
        if (localTimeout == 0) {
            return null
        }

        var element: CacheElement = null
        synchronized /*(lock)*/ {
            element = cache.get(path)
        }

        if (element == null) {
            return null
        }

        var time: Long = apr_time.apr_time_now();
        if (time - element.timestamp > localTimeout) {
            // remove all elements older than this one
            synchronized /* (lock) */ {
                if (cache.get(path) != null) {
                    var oldest: String = null
                    do {
                        oldest = list.removeFirst()
                        cache.remove(oldest)
                    } while (!path.equals(oldest))
                }
            }
            return null
        }

        return element.canonicalPath
    }
}

//Implementation of the few used methods from 
// org.hapache.harmony.luni.
object HyUtil{
    private val defaultEncoding: String = fromCString(CFile.getOsEncoding())
    def getBytes(name: String): Array[Byte] = name.getBytes(defaultEncoding)
    def getUTF8Bytes(name: String): Array[Byte] = name.getBytes("UTF-8")
    def toString(bytes: Array[Byte]): String = new String(bytes, 0, bytes.length, defaultEncoding)
    def toUTF8String(bytes: Array[Byte]): String = toUTF8String(bytes, 0, bytes.length)
    def toUTF8String(bytes: Array[Byte], 
                     offset: Int, 
                     length: Int): String = new String(bytes, 0, bytes.length, "UTF-8")
}


//way to handle break in Scala.
object Break extends Exception{}

object File{

    /*need to determine If I need an implementation of this C funct.*/
    //oneTimeInitialization();

    //HyMaxPath was chosen from unix MAXPATHLEN.
    val HyMaxPath: Int = 1024
	val separatorChar: Char = CFile.separatorChar()
	val pathSeparatorChar: Char = CFile.pathSeparatorChar()
    val separator: String = separatorChar.toString
	val pathSeparator: String = pathSeparatorChar.toString
	private var counter: Int = 0;
	private var counterBase: Int = 0;
	private class TempFileLocker{}
	private val tempFileLocker: TempFileLocker = new TempFileLocker()
	private val caseSensitive: Boolean = (CFile.isCaseSensitiveImpl() == 1);

    private def rootsImpl(): Array[String] = {

        var rootsString: Ptr[Byte] = stackalloc[Byte](HyMaxPath)
        val numRoots: Int = CFile.getPlatformRoots(rootsString)
        if(numRoots == 0){
            return null
        }
        var rootCopy: Ptr[Byte] = rootsString

        val answer: Array[String] = new Array[String](numRoots)
        
        var entrylen: CSize = strlen(rootCopy)
        var index: Int = 0
        while(entrylen != 0){
            var rootname: Ptr[Byte] = stackalloc[Byte](entrylen.toInt)
            strncpy(rootname, rootCopy, entrylen)
            answer(index) = fromCString(rootname)
            index += 1
            rootCopy = rootCopy + entrylen + 1
            entrylen = strlen(rootCopy)
        }
        return answer
    }


    def listRoots(): Array[File] = {
       val rootsList: Array[String] = rootsImpl()

       if(rootsList == null) new Array[File](0)
       else for(roots <- rootsList) yield new File(roots.toString())      
    }

    @throws(classOf[IOException])
    def createTempFile(prefix: String, suffix: String): File = 
        createTempFile(prefix, suffix, null)

    @throws(classOf[IOException])
    def createTempFile(prefix: String, 
                        suffix: String,
                        directory: File): File = ???

    @throws(classOf[IOException])
    def genTempFile(prefix: String, 
                        suffix: String,
                        directory: File): File = ???
}

//TODO:
//private def checkURI(uri : URI): Unit

/*@throws(classOf[IOException])
private def writeObject(stream: ObjectOutputStream): Unit */

/*@throws(classOf[IOException])
@throws(classOf[ClassNotFoundException])
private def readObject(stream: ObjectInputStream): Unit */

//def toURI(): URI

/*@throws(classOf[java.net.MalformedURLException])
def toURL(): URL*/

/*def File(uri: URI): File = {
    this()
    checkURI(uri)
    path = fixSlashes(uri.getPath())    
}*/