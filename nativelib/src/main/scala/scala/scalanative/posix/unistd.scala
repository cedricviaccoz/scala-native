package scala.scalanative.posix

/**
 * Created by marius on 27.10.16.
 */
import scala.scalanative.native._

@extern
object unistd {
  def sleep(seconds: CUnsignedInt): Int                           = extern
  def usleep(usecs: CUnsignedInt): Int                            = extern
  def unlink(path: CString): CInt                                 = extern
  def access(pathname: CString, mode: CInt): CInt                 = extern
  def readlink(path: CString, buf: CString, bufsize: CSize): CInt = extern
  def getcwd(buf: CString, size: CSize): CString                  = extern
  def write(fildes: CInt, buf: Ptr[_], nbyte: CSize): CInt        = extern
  def read(fildes: CInt, buf: Ptr[_], nbyte: CSize): CInt         = extern
  def close(fildes: CInt): CInt                                   = extern
}
