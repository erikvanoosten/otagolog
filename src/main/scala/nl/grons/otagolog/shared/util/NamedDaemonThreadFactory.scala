/*
 * Copyright (c) 2014-2014 Erik van Oosten All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.grons.otagolog.shared.util

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * A thread factory that creates named daemon threads. Each thread created by this class
 * will have a different name due to a count of the number of threads  created thus far
 * being included in the name of each thread. This count is held statically to ensure
 * different thread names across different instances of this class.
 *
 * Thread names are `<prefix>-<n>`, where `<prefix>` is the given {@code prefix} and `<n>`
 * is the count of threads created thus far by this class.
 *
 * @param prefix The thread name prefix
 */
class NamedDaemonThreadFactory(prefix: String) extends ThreadFactory {

  def newThread(runnable: Runnable): Thread = {
    val t: Thread = new Thread(runnable)
    t.setName(prefix + "-" + NamedDaemonThreadFactory.threadCounter.incrementAndGet)
    t.setDaemon(true)
    t
  }

}

object NamedDaemonThreadFactory {
  private final val threadCounter: AtomicInteger = new AtomicInteger(0)
}
