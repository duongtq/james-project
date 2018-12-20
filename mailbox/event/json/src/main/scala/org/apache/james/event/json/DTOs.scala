/** **************************************************************
  * Licensed to the Apache Software Foundation (ASF) under one   *
  * or more contributor license agreements.  See the NOTICE file *
  * distributed with this work for additional information        *
  * regarding copyright ownership.  The ASF licenses this file   *
  * to you under the Apache License, Version 2.0 (the            *
  * "License"); you may not use this file except in compliance   *
  * with the License.  You may obtain a copy of the License at   *
  * *
  * http://www.apache.org/licenses/LICENSE-2.0                 *
  * *
  * Unless required by applicable law or agreed to in writing,   *
  * software distributed under the License is distributed on an  *
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
  * KIND, either express or implied.  See the License for the    *
  * specific language governing permissions and limitations      *
  * under the License.                                           *
  * ***************************************************************/

package org.apache.james.event.json

import java.time.Instant
import java.util.Date

import javax.mail.Flags.Flag
import javax.mail.{Flags => JavaMailFlags}
import org.apache.james.core.quota.QuotaValue
import org.apache.james.event.json.DTOs.SystemFlag._
import org.apache.james.mailbox.acl.{ACLDiff => JavaACLDiff}
import org.apache.james.mailbox.model.{MailboxACL, MessageId, MailboxPath => JavaMailboxPath, MessageMetaData => JavaMessageMetaData, Quota => JavaQuota, UpdatedFlags => JavaUpdatedFlags}
import org.apache.james.mailbox.{FlagsBuilder, MessageUid}

import scala.collection.JavaConverters._

object DTOs {

  object ACLDiff {
    def fromJava(javaACLDiff: JavaACLDiff): ACLDiff = ACLDiff(
      javaACLDiff.getOldACL.getEntries.asScala.toMap,
      javaACLDiff.getNewACL.getEntries.asScala.toMap)
  }

  object MailboxPath {
    def fromJava(javaMailboxPath: JavaMailboxPath): MailboxPath = MailboxPath(
      Option(javaMailboxPath.getNamespace),
      Option(javaMailboxPath.getUser),
      javaMailboxPath.getName)
  }

  object Quota {
    def toScala[T <: QuotaValue[T]](java: JavaQuota[T]): Quota[T] = Quota(
      used = java.getUsed,
      limit = java.getLimit,
      limits = java.getLimitByScope.asScala.toMap)
  }

  case class ACLDiff(oldACL: Map[MailboxACL.EntryKey, MailboxACL.Rfc4314Rights],
                     newACL: Map[MailboxACL.EntryKey, MailboxACL.Rfc4314Rights]) {
    def toJava: JavaACLDiff = new JavaACLDiff(new MailboxACL(oldACL.asJava), new MailboxACL(newACL.asJava))
  }

  case class MailboxPath(namespace: Option[String], user: Option[String], name: String) {
    def toJava: JavaMailboxPath = new JavaMailboxPath(namespace.orNull, user.orNull, name)
  }

  case class Quota[T <: QuotaValue[T]](used: T, limit: T, limits: Map[JavaQuota.Scope, T]) {
    def toJava: JavaQuota[T] =
      JavaQuota.builder[T]
        .used(used)
        .computedLimit(limit)
        .limitsByScope(limits.asJava)
        .build()
  }

  object MessageMetaData {
    def fromJava(javaMessageMetaData: JavaMessageMetaData): MessageMetaData = DTOs.MessageMetaData(
      javaMessageMetaData.getUid,
      javaMessageMetaData.getModSeq,
      Flags.fromJavaFlags(javaMessageMetaData.getFlags),
      javaMessageMetaData.getSize,
      javaMessageMetaData.getInternalDate.toInstant,
      javaMessageMetaData.getMessageId)
  }

  case class MessageMetaData(uid: MessageUid, modSeq: Long, flags: Flags, size: Long, internalDate: Instant, messageId: MessageId) {
    def toJava: JavaMessageMetaData = new JavaMessageMetaData(uid, modSeq, Flags.toJavaFlags(flags), size, Date.from(internalDate), messageId)
  }

  sealed trait Flag

  case class UserFlag(value: String) extends Flag

  trait SystemFlag extends Flag {
    def asString: String
  }

  object SystemFlag {
    case object Answered extends SystemFlag {
      val asString = "Answered"
    }
    case object Deleted extends SystemFlag {
      val asString = "Deleted"
    }
    case object Draft extends SystemFlag {
      val asString = "Draft"
    }
    case object Flagged extends SystemFlag {
      val asString = "Flagged"
    }
    case object Recent extends SystemFlag {
      val asString = "Recent"
    }
    case object Seen extends SystemFlag {
      val asString = "Seen"
    }
  }

  case class Flags(systemFlags: Seq[SystemFlag], userFlags: Seq[UserFlag])

  object Flags {

    def toJavaFlags(scalaFlags: Flags): JavaMailFlags = {
      (scalaFlags.systemFlags ++ scalaFlags.userFlags)
        .foldLeft(new FlagsBuilder)((builder, flag) =>
          flag match {
            case UserFlag(value) => builder.add(value)
            case SystemFlag.Answered => builder.add(Flag.ANSWERED)
            case SystemFlag.Deleted => builder.add(Flag.DELETED)
            case SystemFlag.Draft => builder.add(Flag.DRAFT)
            case SystemFlag.Flagged => builder.add(Flag.FLAGGED)
            case SystemFlag.Recent => builder.add(Flag.RECENT)
            case SystemFlag.Seen => builder.add(Flag.SEEN)
          })
        .build()
    }

    def fromJavaFlags(flags: JavaMailFlags): Flags = {
      Flags(
        flags.getSystemFlags.map(javaFlagToSystemFlag),
        flags.getUserFlags.map(UserFlag))
    }

    private def javaFlagToSystemFlag(flag: JavaMailFlags.Flag): SystemFlag = flag match {
      case Flag.ANSWERED => Answered
      case Flag.DELETED => Deleted
      case Flag.DRAFT => Draft
      case Flag.FLAGGED => Flagged
      case Flag.RECENT => Recent
      case Flag.SEEN => Seen
    }
  }

  object UpdatedFlags {
    def toUpdatedFlags(javaUpdatedFlags: JavaUpdatedFlags): UpdatedFlags = UpdatedFlags(
      javaUpdatedFlags.getUid,
      javaUpdatedFlags.getModSeq,
      Flags.fromJavaFlags(javaUpdatedFlags.getOldFlags),
      Flags.fromJavaFlags(javaUpdatedFlags.getNewFlags))
  }

  case class UpdatedFlags(uid: MessageUid, modSeq: Long, oldFlags: Flags, newFlags: Flags) {
    def toJava: JavaUpdatedFlags = JavaUpdatedFlags.builder()
      .uid(uid)
      .modSeq(modSeq)
      .oldFlags(Flags.toJavaFlags(oldFlags))
      .newFlags(Flags.toJavaFlags(newFlags))
      .build()
  }
}
