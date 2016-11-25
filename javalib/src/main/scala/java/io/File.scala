package java.io

import scala.transient
import scalanative.native._, stdlib._, stdio._, string._
import scalanative.posix.unistd._

import java.util.ArrayList

class File private () extends Serializable with Comparable[File] {
    import File._
  	
    private var path: String = null

    def compareTo(file: File): Int = {
        if(caseSensitive) getPath().compareTo(file.getPath())
        else this.getPath().compareToIgnoreCase(file.getPath())
    }

  	@transient 
    var properPath: Array[Byte] = null

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

    def getPath(): String = return path 

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


    def fixSlashes(origPath: String): String = {
        def fixSlashesRec(path: List[Char]): List[Char] =
        path match{
            case ':'::'/'::'/'::xs => ':'::separatorChar::separatorChar::fixSlashesRec(xs)
            case '/'::'/'::xs => separatorChar::fixSlashesRec(xs)
            case '/'::Nil => Nil
            case '/'::xs => separatorChar::fixSlashesRec(xs)
            case x::xs => x::fixSlashesRec(xs)
            case Nil => List()
        }
        fixSlashesRec(origPath.toList).mkString
}
//apparently this java like function bugs at newPath(length) = pathChar

/*    private def fixSlashes(origPath: String): String = {
        var uncIndex: Int = 1
        var length: Int = origPath.length() 
        var newLength: Int = 0
        if (separatorChar == '/') {
            uncIndex = 0;
        } else if (length > 2 && origPath(1) == ':') {
            uncIndex = 2;
        }
        var foundSlash: Boolean = false
        var newPath: Array[Char] = origPath.toCharArray()
        for(i <- 0 until length) {
            var pathChar: Char = newPath(i)
            if ((separatorChar == '\\' && pathChar == '\\')
                || pathChar == '/') {
                /* UNC Name requires 2 leading slashes */
                if ((foundSlash && i == uncIndex) || !foundSlash) {
                    newLength += 1
                    newPath(newLength) = separatorChar
                    foundSlash = true
                }
            } else {
                // check for leading slashes before a drive
                if (pathChar == ':'
                        && uncIndex > 0
                        && (newLength == 2 || (newLength == 3 && newPath(1) == separatorChar))
                        && (newPath(0) == separatorChar)) {
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
                && (newLength > (uncIndex + 1) || (newLength == 2 && newPath(0) != separatorChar))) {
            newLength -= 1
        }
        return new String(newPath, 0, newLength)
    }*/

    def canRead(): Boolean = {
        if (path.length() == 0) {
            return false
        }
        var pp: Array[Byte] = setProperPath()
        return existsImpl(pp) && !isWriteOnlyImpl(pp)
    }

    def canWrite(): Boolean = {
        // Cannot use exists() since that does an unwanted read-check.
        var exists: Boolean = false
        if (path.length() > 0) {
            exists = existsImpl(setProperPath())
        }
        return exists && !isReadOnlyImpl(setProperPath())
    }

    def delete(): Boolean = {
        var propPath: Array[Byte] = setProperPath()
//Decisive.
println("propPath successfully generated")
        if ((path.length() != 0) && isDirectoryImpl(propPath)) {
            return deleteDirImpl(propPath)
        }
        return deleteFileImpl(propPath)
    }

    //native funct.
    @throws(classOf[IOException])
    private def deleteDirImpl(filePath: Array[Byte]): Boolean = {
        var pathCopy: CString = filePathCopy(filePath)
        val result: Int = remove(pathCopy)
        return result == 0
    }

    //native funct.
    @throws(classOf[IOException])
    private def deleteFileImpl(filePath: Array[Byte]): Boolean = {
        var pathCopy: CString = filePathCopy(filePath)
        var result: Int = unlink(pathCopy)   
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

        var pathCopy: CString = stackalloc[CChar](HyMaxPath)
        val length: Int = filePath.length
        if(length > (HyMaxPath-1)){
            throw new IOException("Path length of "+length+" characters exceeds maximum supported length of "+HyMaxPath)
        }
        for(i <- 0 until length){
            pathCopy(i) = filePath(i)
        }
        pathCopy(length) = '\0'
        return pathCopy
    }

//TODO : Wait for a fix from @densh
    def deleteOnExit(): Unit = ???//atexit{ () => delete(); () }

    
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
        return existsImpl(setProperPath());
    }

    //native funct.
    def existsImpl(filePath: Array[Byte]): Boolean = {
//Decisive.
println("in existsImpl")
        var pathCopy: CString = filePathCopy(filePath)
        return (CFile.fileAttribute(pathCopy) >= 0)
    }

    def getAbsolutePath(): String = HyUtil.toUTF8String(setProperPath())

    def getAbsoluteFile(): File = new File(this.getAbsolutePath())

    @throws(classOf[IOException])
    def getCanonicalPath(): String = {
        var result: Array[Byte] = setProperPath()
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
            /*On Unix getCanonImpl just retrun a copy of the filePath given as
              argument, for windows though it would need a native implementation*/
            //newResult = getCanonImpl(newResult)
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
                    && path(1) == File.separatorChar) {
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
        return isDirectoryImpl(setProperPath())
    }

    //native funct.
    private def isDirectoryImpl(filePath: Array[Byte]): Boolean = {
        var pathCopy: CString = filePathCopy(filePath)
        return (CFile.fileAttribute(pathCopy) == 0)
    }

    def isFile(): Boolean = {
        if (path.length() == 0) {
            return false
        }
        return isFileImpl(setProperPath())
    }


    //native funct.
    private def isFileImpl(filePath: Array[Byte]): Boolean = {
        var pathCopy: CString = filePathCopy(filePath)
        return (CFile.fileAttribute(pathCopy) == 1)
    }

    def isHidden(): Boolean = if(path.length() == 0) false 
                              else isHiddenImpl(setProperPath())

    //native funct.
    private def isHiddenImpl(filePath: Array[Byte]): Boolean = {
        var pathCopy: CString = filePathCopy(filePath)
        val length: CSize = strlen(pathCopy)
        val existsResult: CInt = CFile.fileAttribute(pathCopy)
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
        return (access(pathCopy, fcntl.W_OK) !=0)
    }

    //native funct.
    private def isWriteOnlyImpl(filePath: Array[Byte]): Boolean = {
        val pathCopy: CString = filePathCopy(filePath)
        return (access(pathCopy, fcntl.R_OK) !=0)
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
        val size: CInt = readlink(link, link, HyMaxPath-1);
        if (size <= 0) return false
        link(size) = 0
        return true
    }

    def lastMofified(): Long = {
        val result: Long = lastModifiedImpl(setProperPath())
        result match{
            case 0 => 0
            case -1 => 0
            case _ => result
        }
    }

    //native funct.
    private def lastModifiedImpl(filePath: Array[Byte]): Long = {
        var pathCopy: CString = filePathCopy(filePath)
        return CFile.lastModNative(pathCopy)
    }

    def setLastMofified(time: Long): Boolean = {
        //message corresponding to luni.B2 from apache messages.properties 
        if (time < 0) throw new IllegalArgumentException("time must be positive")
        return (setLastModifiedImpl(setProperPath(), time));
    }

    //native funct.
    private def setLastModifiedImpl(filePath: Array[Byte], 
                                    time: Long): Boolean = {
        var pathCopy: CString = filePathCopy(filePath)
        CFile.setLastModNative(pathCopy, time) match{
            case 0 => false
            case 1 => true 
        }   
    }

    def setReadOnly(): Boolean = setReadOnlyImpl(setProperPath())

    //native funct.
    def setReadOnlyImpl(path: Array[Byte]): Boolean = {
        var pathCopy: CString = filePathCopy(path)
        CFile.setReadOnlyNative(pathCopy) match{
            case 0 => false
            case 1 => true
        }

    }

    def length(): Long = lengthImpl(setProperPath())

    //native funct.
    private def lengthImpl(filePath: Array[Byte]): Long = {
        val pathCopy: CString = filePathCopy(filePath)
        val result: Long = CFile.fileLength(pathCopy)
        if(result < 0){
            return 0L
        }else return result
    }

    def list(): Array[java.lang.String] = {
        if(path.length() == 0) return null
        var bs: Array[Byte] = setProperPath()
        if(!isDirectoryImpl(bs) || !existsImpl(bs) || isWriteOnlyImpl(bs)) return null

        var implList: Array[Array[Byte]] = listImpl(bs)
        if(implList == null) return new Array[String](0)

        val result: Array[String] = new Array[String](implList.length)
        for(index <- 0 until implList.length){
            result(index) = HyUtil.toUTF8String(implList(index))
        }
        return result
    }

    def listFiles(): Array[File] = {
        val tempNames: Array[String] = list()
        if( tempNames == null) return null
        val resultLength = tempNames.length
        val results: Array[File] = new Array[File](resultLength)
        for(i <- 0 until resultLength){
            results(i) = new File(this, tempNames(i))
        }
        return results
    }

    def listFiles(filter: FilenameFilter): Array[File] = {
        val tempNames: Array[String] = list(filter)
        if( tempNames == null) return null
        val resultLength = tempNames.length
        val results: Array[File] = new Array[File](resultLength)
        for(i <- 0 until resultLength){
            results(i) = new File(this, tempNames(i))
        }
        return results
    }

    def listFiles(filter: FileFilter): Array[File] = {
        if(path.length() == 0) return null
        var bs: Array[Byte] = setProperPath()
        if(!isDirectoryImpl(bs) || !existsImpl(bs) || isWriteOnlyImpl(bs)) return null

        var implList: Array[Array[Byte]] = listImpl(bs)
        if(implList == null) return new Array[File](0)

        val tempResult: ArrayList[File] = new ArrayList[File]()
        for(index <- 0 until implList.length){
            val aName = HyUtil.toString(implList(index))
            val aFile = new File(this, aName)
            if(filter == null || filter.accept(aFile)){
                tempResult.add(aFile)
            }
        }
        return tempResult.toArray(new Array[File](tempResult.size()))

    }

    def list(filter: FilenameFilter): Array[java.lang.String] = {
        if(path.length() == 0) return null
        var bs: Array[Byte] = setProperPath()
        if(!isDirectoryImpl(bs) || !existsImpl(bs) || isWriteOnlyImpl(bs)) return null

        var implList: Array[Array[Byte]] = listImpl(bs)
        if(implList == null) return new Array[String](0)

        val tempResult: ArrayList[String] = new ArrayList[String]()
        for(index <- 0 until implList.length){
            val aName = HyUtil.toString(implList(index))
            if(filter == null || filter.accept(this, aName)){
                tempResult.add(aName)
            }
        }

        return tempResult.toArray(new Array[String](tempResult.size()))
    }

    //native funct.
    @throws(classOf[IOException])
    private def listImpl(filePath: Array[Byte]): Array[Array[Byte]] = synchronized {
        
        @struct 
        class dirEntry(
            //need to check if I can stackalloc this way in the struct
            var pathEntry: CString = stackalloc[CChar](HyMaxPath),
            var next: Ptr[dirEntry]
        )

        var dirList: Ptr[dirEntry] = null
        var currentEntry: Ptr[dirEntry] = null
        
        var result: Int = 0 
        var index: Int = 0
        var numEntries: Int = 0

        var pathCopy: CString = stackalloc[CChar](HyMaxPath+1)
        var filename: CString = stackalloc[CChar](HyMaxPath)
        var length: Int = filePath.length

        var answer: Array[Array[Byte]] = null

        if(length > (HyMaxPath-1)){
            throw new IOException("Path length of "+length+" characters exceeds maximum supported length of "+HyMaxPath)
        }

        for(i <- 0 until length) pathCopy(i) = filePath(i)

        if((length >= 1) && (pathCopy(length -1) != '/') 
            && pathCopy(length -1) != '\\' ){
            pathCopy(length) = separatorChar.toByte 
            length += 1
        }


        var findhandle = CFile.fileFindFirst(pathCopy, filename)

        //not sure about this since it probably sent back a pointer...
        //TODO: findhandle is a pointer, this will alway yield false
        if(findhandle == -1)
            return null

        while (result > -1){
            if (strcmp(toCString("."), filename) != 0 && (strcmp(toCString(".."), filename) != 0)){
                
                if (numEntries > 0){
                    (!currentEntry).next = malloc(sizeof[dirEntry]).cast[Ptr[dirEntry]]
                    currentEntry = (!currentEntry).next
                }else{
                    dirList = malloc(sizeof[dirEntry]).cast[Ptr[dirEntry]]
                    currentEntry = dirList
                }

                if (currentEntry == null){
                    CFile.fileFindClose(findhandle);
                    return cleanup
                }
                strcpy((!currentEntry).pathEntry, filename)
                numEntries += 1
              }
            result = CFile.fileFindNext(findhandle, filename);
        }
        CFile.fileFindClose(findhandle);

        if (numEntries == 0) return null
        answer = new Array[Array[Byte]](numEntries)

        def cleanup: Array[Array[Byte]] = {
            for (fileindex <- 0 until numEntries){
                val entrylen: Int = strlen((!dirList).pathEntry).toInt;
                currentEntry = dirList
                if (answer != null){  
                    val entrypath: Array[Byte] = new Array[Byte](entrylen)
                    for(i <- 0 until entrylen) entrypath(i) = (!dirList).pathEntry(i)
                    answer(fileindex) = entrypath
                }
                dirList = (!dirList).next;
                free(currentEntry)
            }
            return answer
        }

        return cleanup
    }

    def mkdir(): Boolean = mkdirImpl(setProperPath())

    //native funct.
    private def mkdirImpl(filePath: Array[Byte]): Boolean = {
        val pathCopy: CString = filePathCopy(filePath)
        CFile.fileMkDir(pathCopy) == 0
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
        newFileImpl(setProperPath()) match{
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
//Decisive.
println("Made the pathCopy")
        val portFD: CInt = CFile.newFileNative(pathCopy)
//Decisive
println("Made the newFile creation")
        portFD match {
            case -1 => 
                return -1
            //this case is whenever whe can't create the file because it already exists
            case -2 => 
                return 1
            case _ =>   
                CFile.fileDescriptorClose(portFD)
                return 0      
        }
    }

   private[io] def setProperPath(): Array[Byte] = {
        if (properPath != null) {
            return properPath;
        }
        if (isAbsolute()) {
            var pathBytes: Array[Byte] = HyUtil.getUTF8Bytes(path);
            properPath = pathBytes;
            return properPath
        }
        // Check security by getting user.dir when the path is not absolute
        
        var userdir: String = getUserDir()
        if(userdir == null){
            throw new IOException("getcwd() error in trying to get user directory")
        }

        if (path.length() == 0) {
            properPath = HyUtil.getUTF8Bytes(userdir)
            return properPath
        }
        var length: Int = userdir.length()
        
        // Handle windows-like path
        if (path(0) == '\\') {
            if (length > 1 && userdir(1) == ':') {
                properPath = HyUtil.getUTF8Bytes(userdir.substring(0, 2) + path)
                return properPath
            }
            path = path.substring(1);
        }

        // Handle separator
        var result: String = userdir
        if (userdir(length - 1) != File.separatorChar) {
        if (path(0) != File.separatorChar) {
                result += separator
            }
        } else if (path(0) == File.separatorChar) {
            result = result.substring(0, length - 2)
        }
        result += path;
        properPath = HyUtil.getUTF8Bytes(result)
        return properPath
    }


    private def getUserDir(): String = {
        var buff: CString = stackalloc[CChar](4096)
        var res: CString = getcwd(buff, 4095)
        fromCString(res)  
    }

    def renameTo(des: java.io.File): Boolean = 
        renameToImpl(setProperPath(), des.setProperPath())

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
                        directory: File): File = {
        // Force a prefix null check first
        if (prefix.length() < 3) {
            throw new IllegalArgumentException("Prefix must be at least 3 characters")
        }
        var newSuffix: String = if (suffix == null) ".tmp" else suffix
        var tmpDirFile: File = null
        if (directory == null) {
            var tmpDir: String = fromCString(CFile.getTempDir())
            tmpDirFile = new File(tmpDir)
        } else {
            tmpDirFile = directory;
        }
        var result: File = null
        do {
            result = genTempFile(prefix, newSuffix, tmpDirFile)
        } while (!result.createNewFile())
        return result
    }

    @throws(classOf[IOException])
    def genTempFile(prefix: String, 
                        suffix: String,
                        directory: File): File = {
        var identify: Int = 0
        synchronized {
            if (counter == 0) {
                val newInt: Int = new java.util.Random().nextInt()
                counter = ((newInt / 65535) & 0xFFFF) + 0x2710
                counterBase = counter
            }
            counter += 1
            identify = counter
        }

        val newName: StringBuilder = new StringBuilder();
        newName.append(prefix);
        newName.append(counterBase);
        newName.append(identify);
        newName.append(suffix);
        return new File(directory, newName.toString());
    }
}


/*@extern*/  object fcntl{
    val W_OK: Int = 2
    val R_OK: Int = 4
    val X_OK: Int = 1
    val F_OK: Int = 0
}


//way to handle break in Scala.
object Break extends Exception{}


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
