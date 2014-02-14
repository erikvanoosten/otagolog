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

package nl.grons.otagolog.server.net

import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.channel.ChannelHandlerContext
import io.netty.buffer.ByteBuf

/**
 * A [[LengthFieldBasedFrameDecoder]] which produces frames of type [[ByteBuf]].
 *
 * @param maxFrameSize  the maximum byte length of a frame.
 *                      if the length of the received frame is greater
 *                      than this value, [[io.netty.handler.codec.TooLongFrameException]]
 *                      will be raised. Defaults to 1 MiByte.
 */
class LengthFieldBasedByteBufDecoder(maxFrameSize: Int = 1048576)
    extends LengthFieldBasedFrameDecoder(maxFrameSize, 0, 4, 0, 0) {

  override def extractFrame(ctx: ChannelHandlerContext, buffer: ByteBuf, index: Int, length: Int): ByteBuf = {
    val slice = buffer.slice(index, length)
    slice.retain()
    slice
  }

}
