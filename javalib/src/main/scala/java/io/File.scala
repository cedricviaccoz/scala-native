package java.io

import scala.transient
import scalanative.native._, stdlib._, stdio._, string._

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
    			name(separatorIndex) == separatorChar){
    			separatorIndex += 1
    		}

    		if(separatorIndex > 0){
    			name = name.substring(separatorIndex, name.length())
    		}

    		val pathLength: Int = path.length()
    		if(pathLength > 0 && path(pathLength-1) == separatorChar){
    			path + name
    		} else path + separatorChar + name
    	}
    	else path
    }

    /*
    //Okay apparently it is not the time to go full functionnal 
    @throws(classOf[NullPointerException])
    private def fixSlashes(origPath: String): String = {
        def fixSlashesRec(path: List[Char]): List[Char] =
        path match{
            case ':'::'/'::'/'::xs => ':'::separatorChar::separatorChar::fixSlashesRec(xs)
            case '/'::'/'::xs => separatorChar::fixSlashesRec(xs)
            case '/'::Nil => Nil
            case '/'::xs => separatorChar::fixSlashesRec(xs)
            case x::xs => x::fixSlashesRec(xs)
            case Nil => List()
        }
        if(origPath == null) throw new NullPointerException()
        else fixSlashesRec(origPath.toList).mkString
    }*/

    private def fixSlashes(origPath: String): String = {
        var uncIndex: Int = 1
        var length: Int = origPath.length() 
        var newLength: Int = 0
        if (separatorChar == '/') {
            uncIndex = 0;
        } else if (length > 2 && origPath.charAt(1) == ':') {
            uncIndex = 2;
        }

        var foundSlash: Boolean = false
        var newPath: Array[Char] = origPath.toCharArray()
        for(i <- 0 until length) {
            var pathChar: Char = newPath(i)
            if ((separatorChar == '\\') && (pathChar == '\\')
                || (pathChar == '/')) {
                /* UNC Name requires 2 leading slashes */
                if ((foundSlash && (i == uncIndex)) || !foundSlash) {
                    newLength += 1
                    newPath(newLength) = separatorChar
                    foundSlash = true
                }
            } else {
                // check for leading slashes before a drive
                if ((pathChar == ':')
                        && (uncIndex > 0)
                        && ((newLength == 2) || ((newLength == 3) && (newPath(1) == separatorChar)))
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
     *Small utilitary function to achieve modularity
     *transform an Array of Byte to a CString 
     *add the null terminating char at the end.
     * can throw an IO exception if the path is too long
     */
    @throws(classOf[IOException])
    private def filePathCopy(filePath: Array[Byte]): CString = {
        var pathCopy: CString = stackalloc[CChar](HyMaxPath)
        val length = filePath.length()
        if(length > (HyMaxPath-1)){
            //PathTooLongIOException don't exist, so I'm doing it myself
            throw new IOException("too long path")
        }else{  
            for(i <- 0 until length){
                    pathCopy(i) = filePath(i)
                }
            pathCopy(length) = '\0'   
            return pathCopy
        }
    }


    def deleteOnExit(): Unit = ???

    
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

    def getAbsolutePath(): String = ???

    def getAbsoluteFile(): File = ???

    @throws(classOf[IOException])
    def getCannonicalPath(): String = ???

    @throws(classOf[IOException])
    private def resolve(newResult: Array[Byte]): Array[Byte] = ???

    @throws(classOf[IOException])
    private def resolveLink(pathBytes: Array[Byte],
                            length: Int,
                            resolveAbsolute: Boolean): Array[Byte] = ??? 

    @throws(classOf[IOException])
    def getCannonicalFile(): File = ???

    //native funct.
    private def getCannonImpl(filePath: Array[Byte]): Array[Byte] = ???

    def getName(): String = ???

    def getParent(): String = ???

    def getParentFile(): File = ???

    override def hashCode(): Int = ???

    def isAbsolute(): Boolean = ???

    def isDirectory(): Boolean = ???

    //native funct.
    private def isDirectoryImpl(filePath: Array[Byte]): Boolean = ???

    def isFile(): Boolean = ???

    //native funct.
    private def isFileImpl(filePath: Array[Byte]) = ???

    def isHidden(): Boolean = ???

    //native funct.
    private def isHiddenImpl(filePath: Array[Byte]): Boolean = ???

    //native funct.
    private def isReadOnlyImpl(filePath: Array[Byte]): Boolean = ???

    //native funct.
    private def isWriteOnlyImpl(filePath: Array[Byte]): Boolean = ???

    //native funct.
    private def getLinkImpl(filePath: Array[Byte]): Array[Byte] = ???

    def lastMofified(): Long = ???

    //native funct.
    private def lastModifiedImpl(filePath: Array[Byte]): Long = ???

    def setLastMofified(time: Long): Boolean = ???

    //native funct.
    private def setLastModifiedImpl(filePath: Array[Byte], 
                                    time: Long): Boolean = ???

    def setReadOnly(): Boolean = ???

    //native funct.
    def setReadOnlyImpl(path: Array[Byte]): Boolean = ???

    def length(): Long = ???

    //native funct.
    private def lengthImpl(filePath: Array[Byte]): Long = ???

    def list(): Array[java.lang.String] = ???

    def listFiles(): Array[File] = ???

    def listFiles(filter: FilenameFilter): Array[File] = ???

    def listFiles(filter: FileFilter): Array[File] = ???

    def list(filter: FilenameFilter): Array[java.lang.String] = ???

    //native funct.
    private def listImpl(path: Array[Byte]): Array[Array[Byte]] = synchronized {
        ???
    }

    def mkdir(): Boolean = ???

    //native funct.
    private def mkdirImpl(filePath: Boolean): Boolean = ???

    def mkdirs(): Boolean = ???

    @throws(classOf[IOException])
    def createNewFile(): Boolean = {
        if(path.length( == 0)){
            //corresponds to the entry "luni.B3" of the
            //internal Messages module from apache. 
            throw new IOException("No such file or directory")   
        }
        newFileImpl(properPath(true)) match{
            case 0 => true
            case 1 => false
            //corresponds to the entry "luni.B4" of the
            //internal Messages module from apache.
            case _ => throw new IOException("Cannot create\:"+ path)
        }
    }

    //native funct.
    private def newFileImpl(filePath: Array[Byte]): Int = {
        var pathCopy = filePathCopy(filePath)
        

    }

    def properPath(interval: Boolean): Array[Byte] = ???

    def renameTo(des: java.io.File): Boolean = ???

    //native funct.
    private def renameToImpl(pathExists: Array[Byte], pathNew: Array[Byte]): Boolean = ???

    override def toString(): String = path


    def getAbsoluteName(): String = ???

}


    //c file can be found in scala-native/nativelib/src/main/resources/
@extern object CFile{
    def separatorChar(): Char = extern
    def pathSeparatorChar(): Char = extern
    def isCaseSensitiveImpl(): Int = extern
    def getPlatformRoots(rootStrings: Ptr[CChar]) = extern
    def file_attr(path: CString): Int = extern
}

    //C function utilized to remove the file.
@extern object unistd{
    def unlink(path: CString): CInt = extern
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

    //REQUIRE TESTING !
    def listRoots(): Array[File] = {
       val rootsList: Array[String] = rootsImpl()

       if(rootsList == null) new Array[File](0)
       else for(roots <- rootsList) yield new File(roots.toString())      
    }

    @throws(classOf[IOException])
    def createTempFile(prefix: String, suffix: String): File = ???

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
private def readObject(stream: ObjectInputStream): Unit = ???*/

//def toURI(): URI

/*@throws(classOf[java.net.MalformedURLException])
def toURL(): URL = ???*/

/*def File(uri: URI): File = {
    this()
    checkURI(uri)
    path = fixSlashes(uri.getPath())    
}*/