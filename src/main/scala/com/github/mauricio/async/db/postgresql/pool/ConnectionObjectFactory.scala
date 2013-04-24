/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.github.mauricio.async.db.postgresql.pool

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.postgresql.DatabaseConnectionHandler
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Await
import com.github.mauricio.async.db.util.Log
import scala.util.{Success, Failure, Try}

object ConnectionObjectFactory {
  val log = Log.get[ConnectionObjectFactory]
}

class ConnectionObjectFactory( val configuration : Configuration ) extends ObjectFactory[DatabaseConnectionHandler] {

  import ConnectionObjectFactory.log

  def create: DatabaseConnectionHandler = {
    val connection = new DatabaseConnectionHandler(configuration)
    Await.result(connection.connect, 5.seconds)

    connection
  }

  def destroy(item: DatabaseConnectionHandler) {
    item.disconnect
  }

  def validate(item: DatabaseConnectionHandler): Try[DatabaseConnectionHandler] = {
    val result : Try[DatabaseConnectionHandler] = Try({
      Await.result( item.sendQuery("SELECT 0"), 5.seconds )
      item
    })

    result match {
      case Failure(e) => {
        try {
          if ( item.isConnected ) {
            item.disconnect
          }
        } catch {
          case e : Exception => log.error("Failed disconnecting object", e)
        }
        result
      }
      case Success(i) => {
        result
      }
    }
  }

}
