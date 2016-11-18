package scala.scalanative.posix

/**
 * Created by marius on 27.10.16.
 */
import scala.scalanative.native.{CUnsignedInt, CString, CInt, CSize, extern}

@extern
object unistd {
  def sleep(seconds: CUnsignedInt): Int = extern
  def usleep(usecs: CUnsignedInt): Int  = extern
  def unlink(path: CString): CInt = extern  
  def access(pathname: CString, mode: CInt): CInt = extern
  def readlink(path: CString, buf: CString, bufsize: CSize): CInt = extern
}
