package java.io

class File private () extends Serializable with Comparable[File] {
  	def this(parent: String, child: String) = this()

  	def compareTo(file: File): scala.Int = {
        if(caseSensitive) this.getPath().compareTo(file.getPath())
        else this.getPath().compareToIgnoreCase(file.getPath())
    }


  	private var path: String;

  	/*TODO: find how to make transient work in Scala.*/
  	//@transient var properPath: Array[Byte]; 


	def this(dir: File, name: String): File = {
		if(name == null) throw NullPointerException()
		if(dirPath == null) path = fixSashes(name)
		else path = calculatePath(dir.getPath(), name)
		this()
	}

    def this(path: String): File {
        this.path = fixSlashes(path)
        this()
    }

    def File(uri: URI): File {
        checkURI(uri);
        this.path = fixSlashes(uri.getPath());
        this()
    }

    def getPath(): String = new String(path) 

    private def calculePath(dirPath: String, name: Sting): String = {
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
    		if(pathLength > 0 && path(pathLength-1 == separatorChar){
    			path + name;
    		} else path + separatorChar + name
    	}
    	else path
    }

    private def checkURI(uri : URI): Unit = ???

    private def fixSlashes(origPath: String): String = {

    }
}


object File{

    /*need to determine If I need an implementation of this C funct.*/
    //oneTimeInitialization();
	val separatorChar: Char = System.getProperty("file.separator", "\\")(0);
	val separator: String = new String(new Array[Char]{pathSeparatorChar}, 0, 1);
	val pathSeparatorChar: Char;
	val pathSeparator: String = System.getProperty("path.separator", ";")(0);
	private var counter: Int = 0;
	private var counterBase: Int = 0;
	private class TempFileLocker{}
	private val tempFileLocker: TempFileLocker = new TempFileLocker()
	private var caseSensitive: Boolean = isCaseSensitiveImpl();

    //according to apache, only Windows systems are case insensitive
    private def isCaseSensitiveImpl: Boolean = {
        !System.getProperty("os.name").toLowerCase().contains("win")
    }

    private def rootsImpl(): Array[Array[Byte]] = ???
}
