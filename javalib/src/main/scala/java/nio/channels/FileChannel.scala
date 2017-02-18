package java.nio.channels;

import java.io.IOException;

/*Code implementation of this abstract class come from  AbstractInterruptibleChannel
    To simplify integration in scala-native, and since they are the only methods
    used in FileOutputStream and FileInputStream, they are moved here.
 */
abstract class FileChannel {

  @volatile
  private var closed: Boolean = false

  def isOpen(): Boolean = synchronized {
    return !closed
  }

  @throws(classOf[IOException])
  def close(): Unit = {
    if (!closed) {
      synchronized {
        if (!closed) {
          closed = true;
          implCloseChannel();
        }
      }
    }
  }

  @throws(classOf[IOException])
  protected def implCloseChannel()
}
