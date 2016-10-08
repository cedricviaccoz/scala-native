package java.io

class File private () extends Serializable with Comparable[File] {
  	def this(path: String) = this()
  	def this(parent: String, child: String) = this()
  	def this(parent: File, child: String) = this()

  	def compareTo(file: File): scala.Int = {
        if(caseSensitive) this.getPath().compareTo(file.getPath())
        else this.getPath().compareToIgnoreCase(file.getPath())
    }


  	private var path: String;

  	/*TODO: find how to make transient work in Scala.*/
  	//@transient var properPath: Array[Byte]; 


	def this(File dirPath, String name): File = {
		if(name == null) throw NullPointerException()
		if(dirPath == null) path = fixSashes(name)
		else path = calculatePath(dirPath, name)
		this()
	}

    def this(path: String): File {
        this.path = fixSlashes(path)
        this()
    }

    def File(URI uri): File {
        checkURI(uri);
        this.path = fixSlashes(uri.getPath());
        this()
    }

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
    			name = name.substring(separatorIndex, name.length(	))
    		}

    		val pathLength: Int = path.length()
    		if(pathLength > 0 && path(pathLength-1 == separatorChar){
    			path + name;
    		} else path + separatorChar + name
    	}
    	else path
    }

    //according to apache, only Windows systems are case insensitive
    private def isCaseSensitiveImpl: Boolean = {
        !System.getProperty("os.name").toLowerCase().contains("win")
    }
}

object File{
	val separatorChar: Char;
	val separator: String;
	val pathSeparatorChar: Char;
	val pathSeparator: String;
	private var counter: Int = 0;
	private var counterBase: Int = 0;
	private class TempFileLocker{}
	private val tempFileLocker: TempFileLocker = new TempFileLocker()
	private var caseSensitive: Boolean;
}
