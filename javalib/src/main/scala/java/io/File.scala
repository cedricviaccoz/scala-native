package java.io


/*
	note: All the @static are here to denote
	the fact that all those field were static in their 
	java iteration, I will find a way to express them 
	correctly later.

	same thing for @transient
 */

class File private () extends Serializable with Comparable[File] {
  	def this(path: String) = this()
  	def this(parent: String, child: String) = this()
  	def this(parent: File, child: String) = this()

  	def compareTo(file: File): scala.Int = ???

    @static private val serialVersionUID: Long = 301077366599181567L;

    @static private val EMPTY_STRING: String = ""; //$NON-NLS-1$

  	private var path: String;

  	@transient var properPath: Array[Byte]; 

	/**
	 * The system dependent file separator character.
	 */
	@static val separatorChar: Char;

	/**
	 * The system dependent file separator string. The initial value of this
	 * field is the system property "file.separator".
	 */
	@static val separator: String;

    /**
     * The system dependent path separator character.
     */
    @static val pathSeparatorChar: Char;

    /**
     * The system dependent path separator string. The initial value of this
     * field is the system property "path.separator".
     */
    @static val pathSeparator: String;

    /* Temp file counter TODO : should be static*/
    @static private var counter: Int = 0;

    /* identify for different VM processes */
    @static private var counterBase: Int = 0;

    @static private class TempFileLocker{}

    @static private TempFileLocker tempFileLocker = new TempFileLocker();

    @static private var caseSensitive: Boolean;

    @static private def oneTimeInitialization(): Unit = {

    }

    //are constructor sensed to be overrided this way in Scala ?
	/**
	 * Constructs a new file using the specified directory and name.
	 * 
	 * @param dir
	 *            the directory where the file is stored.
	 * @param name
	 *            the file's name.
	 * @throws NullPointerException
	 *             if {@code name} is {@code null}.
	 */
	def this(File dirPath, String name) = {
		if(name == null) throw NullPointerException()
		if(dir == null) path = fixSashes(name)
		else path = calculatePath(dirPath, name)
	}


    /**
     * Constructs a new file using the specified path.
     * 
     * @param path
     *            the path to be used for the file.
     */
    def this(path: String) {
        // path == null check & NullPointerException thrown by fixSlashes
        this.path = fixSlashes(path)
    }

    /**
     * Constructs a new File using the path of the specified URI. {@code uri}
     * needs to be an absolute and hierarchical Unified Resource Identifier with
     * file scheme and non-empty path component, but with undefined authority,
     * query or fragment components.
     * 
     * @param uri
     *            the Unified Resource Identifier that is used to construct this
     *            file.
     * @throws IllegalArgumentException
     *             if {@code uri} does not comply with the conditions above.
     * @see #toURI
     * @see java.net.URI
     */
    public File(URI uri) {
        // check pre-conditions
        checkURI(uri);
        this.path = fixSlashes(uri.getPath());
    }
}
