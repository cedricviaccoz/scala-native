package java.io

//import scala.annotation.meta.transient
import scalanative.native._, stdlib._, stdio._, string._

class File private () extends Serializable with Comparable[File] {
  	
    def compareTo(file: File): scala.Int = {
        if(caseSensitive) getPath().compareTo(file.getPath())
        else this.getPath().compareToIgnoreCase(file.getPath())
    }

    //put it in the constructor ?
  	private var path: String;

  	@transient var properPath: Array[Byte]; 

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

    /*def File(uri: URI): File {
        this()
        checkURI(uri);
        this.path = fixSlashes(uri.getPath());
    }*/

    def getPath(): String = new String(path) 

    private def calculePath(dirPath: String, name: String): String = {
    	val path: String = fixSlashes(dirPath)
    	if(!name.isEmpty || path.isEmpty){
    		var name: String = fixSlashes(name)
    		var separatorIndex: Int = 0;

    		while(separatorIndex < name.length() && 
    			name(separatorIndex) == separatorChar){
    			speratorIndex++
    		}

    		if(separatorIndex > 0){
    			name = name.substring(separatorIndex, name.length())
    		}

    		val pathLength: Int = path.length()
    		if(pathLength > 0 && path(pathLength-1 == separatorChar)){
    			path + name;
    		} else path + separatorChar + name
    	}
    	else path
    }

    /*private def checkURI(uri : URI): Unit = ???*/

    /*going full functionnal in this one, will need 
     test to be sure
     it works properly
    */
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
    }

    def canRead(): Boolean = ???

    def canWrite(): Boolean = ???
    
    def delete(): Boolean = ???

    //native funct.
    private def deleteDirImpl(filePath: Array[Byte]): Boolean = ???

    //native funct.
    private def deleteFileImpl(filePath: Array[Byte]): Boolean = ???

    def deleteOnExit(): Unit = ???

    override def equals(obj: Any): Boolean = ???

    def exists(): Boolean = ???

    //native funct.
    def existsImpl(filePath: Array[Byte]): Boolean = ???

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
    private /*synchronized*/ def listImpl(path: Array[Byte]): Array[Array[Byte]] = ???

    def mkdir(): Boolean = ???

    //native funct.
    private def mkdirImpl(filePath: Boolean): Boolean = ???

    def mkdirs(): Boolean = ???

    @throws(classOf[IOException])
    def createNewFile(): Boolean = ???

    //native funct.
    private def newFileImpl(filePath: Array[Byte]): Int = ???

    /*??? private*/ def properPath(interval: Boolean): Array[Byte] = ???

    def renameTo(des: java.io.File): Boolean = ???

    //native funct.
    private def renameToImpl(pathExists: Array[Byte], pathNew: Array[Byte]): Boolean = ???

    override def toString(): String = path

    /*def toURI(): URI = ???

    @throws(classOf[java.net.MalformedURLException])
    def toURL(): URL = ???*/

    def getAbsoluteName(): String = ???

    /*@throws(classOf[IOException])
    private def writeObject(stream: ObjectOutputStream): Unit = ???

    @throws(classOf[IOException])
    @throws(classOf[ClassNotFoundException])
    private def readObject(stream: ObjectInputStream): Unit = ???*/

}


object File{

    /*need to determine If I need an implementation of this C funct.*/
    //oneTimeInitialization();
	val separatorChar: Char = System.getProperty("file.separator", "\\")(0);
	val pathSeparatorChar: Char = System.getProperty("path.separator", ";")(0);
    val separator: String = new String(new Array[Char]{separatorChar}, 0, 1);
	val pathSeparator: String = new String(new Array[Char]{ pathSeparatorChar}, 0, 1);
	private var counter: Int = 0;
	private var counterBase: Int = 0;
	private class TempFileLocker{}
	private val tempFileLocker: TempFileLocker = new TempFileLocker()
	private var caseSensitive: Boolean = isCaseSensitiveImpl();

    //according to apache, only Windows systems are case insensitive
    private def isCaseSensitiveImpl: Boolean = {
        !System.getProperty("os.name").toLowerCase().contains("win")
    }

    private def rootsImpl(): List[CString] = {

        val HyMaxPath: Int = 1024
        var rootString: Array[Char] = new Array[Char](HyMaxPath)
        var rootCopy: Ptr[Char];
        
        /*those four lines are the implementation of
        the original platformRoots, it seems though to 
        only work for unix system....*/ 
        rootStrings(0) = '/'
        rootStrings(1) = 0.toChar
        rootStrings(2) = 0.toChar
        var numRoots: Int = 1

        rootCopy = rootStrings(0).toPtr //????
        val roots: List[String] = new List[String]()

        //the initial root (in Unix system) should only be '/'
        roots.append(rootStrings(0).toCString)
        var entrylen = strlen(rootCopy.toCString) // ????
        while(entrylen != 0){
            rootCopy = rootCopy + entrylen + 1 //Moving the pointer
            val strToAdd: CString = new CString()// ??? 
            strcpy(rootCopy, strToAdd)
            roots.append(strToAdd)
            entrylen = strlen(rootCopy)
        }
        return roots
    }


    def listRoots(): Array[File] = {
       val rootsList: List[CString] = rootsImpl()
       //implementing rootsList as Option[...] to cotourn null test ?
       if(rootsList == null) new Array[File](0)
       else{
            var result: Array[File] = new Array[File](rootsList.length())
            for(roots <- rootsList) yield new File(roots.toString())            
       }
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
