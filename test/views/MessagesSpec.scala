/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package views

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import java.io.File
import scala.io.Source

class MessagesSpec extends AnyFreeSpec with Matchers {

  private val keyPattern = """messages\("([^"]+)"""".r

  private def keysFromFile(file: File): Seq[String] =
    keyPattern.findAllMatchIn(Source.fromFile(file).mkString).map(_.group(1)).toSeq

  private def allSourceFiles: Seq[File] = {
    def walk(dir: File): Seq[File] =
      dir.listFiles.toSeq.flatMap {
        case f if f.isDirectory => walk(f)
        case f                  => Seq(f)
      }

    walk(new File("app/views")) ++ walk(new File("app/viewmodels")) ++ walk(new File("app/controllers"))
  }

  private lazy val templateKeys: Seq[String] =
    allSourceFiles.flatMap(keysFromFile).distinct.sorted

  private def definedKeysIn(filename: String): Set[String] =
    Source
      .fromFile(s"conf/$filename")
      .getLines()
      .filterNot(l => l.trim.isEmpty || l.trim.startsWith("#"))
      .map(_.split("=").head.trim)
      .toSet

  "messages.en" - {
    "must define every key referenced in templates and viewmodels" in {
      val defined = definedKeysIn("messages.en")
      val missing = templateKeys.filterNot(defined.contains)
      withClue(s"Missing keys:\n${missing.mkString("\n")}\n") {
        missing mustBe empty
      }
    }
  }

  "messages.cy" - {
    "must define every key referenced in templates and viewmodels" in {
      val defined = definedKeysIn("messages.cy")
      val missing = templateKeys.filterNot(defined.contains)
      withClue(s"Missing keys:\n${missing.mkString("\n")}\n") {
        missing mustBe empty
      }
    }
  }
}
