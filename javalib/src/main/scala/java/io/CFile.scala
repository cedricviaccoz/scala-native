package java.io
import scalanative.native._

@extern 
object CFile{

    @name("scalanative_file_findfirst")
    def fileFindFirst(path: CString, charbuf: CString): Ptr[_]/*UDATA*/ = extern
    
    @name("scalanative_file_findnext")
    def fileFindNext(findhandle: Ptr[_]/*UDATA*/, charbuf: CString): Int = extern

    @name("file_findclose") 
    def fileFindClose(findhandle: Ptr[_]/*UDATA*/): Unit = extern

    @name("scalanative_set_last_mod")
    def setLastModNative(path: CString, time: Long): Int = extern

    @name("scalanative_set_read_only_native")
    def setReadOnlyNative(path: CString): Int = extern
    
    @name("scalanative_file_mkdir")
    def fileMkDir(path: CString): Int = extern

    @name("scalanative_file_length")
    def fileLength(path: CString): Long = extern

    @name("scalanative_last_mod")
    def lastModNative(path: CString): CSize = extern

    @name("scalanative_file_open")
    def newFileNative(path: CString): CInt = extern

    @name("scalanative_file_descriptor_close")
    def fileDescriptorClose(fd: CInt): CInt = extern

    @name("scalanative_separator_char")
    def separatorChar(): Char = extern

    @name("scalanative_path_separator_char")
    def pathSeparatorChar(): Char = extern

    @name("scalanative_is_case_sensitive")
    def isCaseSensitiveImpl(): Int = extern

    @name("scalanative_get_platform_roots")
    def getPlatformRoots(rootStrings: Ptr[CChar]) = extern

    @name("scalanative_file_attr")
    def fileAttribute(path: CString): Int = extern

    @name("scalanative_get_os_encoding")
    def getOsEncoding(): CString = extern

    @name("scalanative_get_temp_dir")
    def getTempDir(): CString = extern

}