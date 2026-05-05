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

package models

import org.scalacheck.{Gen, Shrink}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*

class RichJsValueSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  val nonEmptyAlphaStr: Gen[String] = Gen.alphaStr.suchThat(_.nonEmpty)

  def buildJsObj[B](keys: Seq[String], values: Seq[B])(implicit writes: Writes[B]): JsObject =
    keys.zip(values).foldLeft(JsObject.empty) { case (acc, (key, value)) =>
      acc + (key -> Json.toJson[B](value))
    }

  "set" - {

    "must return an error if the path is empty" in {
      Json.obj().set(JsPath, Json.obj()) mustEqual JsError("path cannot be empty")
    }

    "must set a value on a JsObject" in {
      val gen = for {
        originalKey   <- nonEmptyAlphaStr
        originalValue <- nonEmptyAlphaStr
        pathKey       <- nonEmptyAlphaStr suchThat (_ != originalKey)
        newValue      <- nonEmptyAlphaStr
      } yield (originalKey, originalValue, pathKey, newValue)

      forAll(gen) { case (originalKey, originalValue, pathKey, newValue) =>
        val value = Json.obj(originalKey -> originalValue)
        val path = JsPath \ pathKey
        value.set(path, JsString(newValue)) mustEqual JsSuccess(Json.obj(originalKey -> originalValue, pathKey -> newValue))
      }
    }

    "must set a nested value on a JsObject" in {
      val value = Json.obj("foo" -> Json.obj())
      val path = JsPath \ "foo" \ "bar"
      value.set(path, JsString("baz")).asOpt.value mustEqual Json.obj("foo" -> Json.obj("bar" -> "baz"))
    }

    "must add a value to an empty JsArray" in {
      forAll(nonEmptyAlphaStr) { newValue =>
        Json.arr().set(JsPath \ 0, JsString(newValue)) mustEqual JsSuccess(Json.arr(newValue))
      }
    }

    "must add a value to the end of a JsArray" in {
      forAll(nonEmptyAlphaStr, nonEmptyAlphaStr) { (oldValue, newValue) =>
        Json.arr(oldValue).set(JsPath \ 1, JsString(newValue)) mustEqual JsSuccess(Json.arr(oldValue, newValue))
      }
    }

    "must change a value in an existing JsArray" in {
      forAll(nonEmptyAlphaStr, nonEmptyAlphaStr, nonEmptyAlphaStr) { (first, second, newValue) =>
        Json.arr(first, second).set(JsPath \ 0, JsString(newValue)) mustEqual JsSuccess(Json.arr(newValue, second))
      }
    }

    "must set a nested value on a JsArray" in {
      Json.arr(Json.arr("foo")).set(JsPath \ 0 \ 0, JsString("bar")).asOpt.value mustEqual Json.arr(Json.arr("bar"))
    }

    "must change the value of an existing key" in {
      forAll(nonEmptyAlphaStr, nonEmptyAlphaStr, nonEmptyAlphaStr) { (key, oldValue, newValue) =>
        Json.obj(key -> oldValue).set(JsPath \ key, JsString(newValue)) mustEqual JsSuccess(Json.obj(key -> newValue))
      }
    }

    "must return an error when trying to set a key on a non-JsObject" in {
      val value = Json.arr()
      value.set(JsPath \ "foo", JsString("bar")) mustEqual JsError(s"cannot set a key on $value")
    }

    "must return an error when trying to set an index on a non-JsArray" in {
      val value = Json.obj()
      value.set(JsPath \ 0, JsString("bar")) mustEqual JsError(s"cannot set an index on $value")
    }

    "must return an error when trying to set an index other than zero on an empty array" in {
      Json.arr().set(JsPath \ 1, JsString("bar")) mustEqual JsError("array index out of bounds: 1, []")
    }

    "must return an error when trying to set an index out of bounds" in {
      Json.arr("bar", "baz").set(JsPath \ 3, JsString("fork")) mustEqual JsError("array index out of bounds: 3, [\"bar\",\"baz\"]")
    }

    "must set into an array which does not exist" in {
      Json.obj().set(JsPath \ "foo" \ 0, JsString("bar")) mustEqual JsSuccess(Json.obj("foo" -> Json.arr("bar")))
    }

    "must set into an object which does not exist" in {
      Json.obj().set(JsPath \ "foo" \ "bar", JsString("baz")) mustEqual
        JsSuccess(Json.obj("foo" -> Json.obj("bar" -> "baz")))
    }

    "must set nested objects and arrays" in {
      Json.obj().set(JsPath \ "foo" \ 0 \ "bar" \ 0, JsString("baz")) mustEqual
        JsSuccess(Json.obj("foo" -> Json.arr(Json.obj("bar" -> Json.arr("baz")))))
    }

    "must return an error when trying to set with a recursive search path" in {
      Json.obj("foo" -> "bar").set(JsPath \\ "foo", JsString("baz")) mustEqual JsError("recursive search not supported")
    }
  }

  "remove" - {

    "must return an error if the path is empty" in {
      Json.obj().remove(JsPath) mustEqual JsError("path cannot be empty")
    }

    "must return an error if the path does not contain a value" in {
      val gen = for {
        originalKey   <- nonEmptyAlphaStr
        originalValue <- nonEmptyAlphaStr
        pathKey       <- nonEmptyAlphaStr suchThat (_ != originalKey)
      } yield (originalKey, originalValue, pathKey)

      forAll(gen) { case (originalKey, originalValue, pathKey) =>
        Json.obj(originalKey -> originalValue).remove(JsPath \ pathKey) mustEqual JsError("cannot find value at path")
      }
    }

    "must remove a value given a keyPathNode" in {
      val gen = for {
        keys          <- Gen.listOf(nonEmptyAlphaStr)
        values        <- Gen.listOf(nonEmptyAlphaStr)
        keyToRemove   <- nonEmptyAlphaStr
        valueToRemove <- nonEmptyAlphaStr
      } yield (keys, values, keyToRemove, valueToRemove)

      forAll(gen) { case (keys, values, keyToRemove, valueToRemove) =>
        val initial = keys.zip(values).foldLeft(JsObject.empty) { case (acc, (k, v)) => acc + (k -> JsString(v)) }
        val withExtra = initial + (keyToRemove -> Json.toJson(valueToRemove))
        withExtra.remove(JsPath \ keyToRemove) mustEqual JsSuccess(initial)
      }
    }

    "must remove a value given an index node" in {
      val gen = for {
        key    <- nonEmptyAlphaStr
        values <- Gen.nonEmptyListOf(nonEmptyAlphaStr)
        index  <- Gen.choose(0, values.size - 1)
      } yield (key, values, index)

      forAll(gen) { case (key, values, indexToRemove) =>
        val arr = values.map(Json.toJson[String])
        val initial = buildJsObj(Seq(key), Seq(arr))
        val removed = initial.remove(JsPath \ key \ indexToRemove)
        val expected = buildJsObj(
          Seq(key),
          Seq(arr.slice(0, indexToRemove) ++ arr.slice(indexToRemove + 1, arr.length))
        )
        removed mustBe JsSuccess(expected)
      }
    }

    "must remove from one of many arrays" in {
      val input = Json.obj(
        "key"  -> JsArray(Seq(Json.toJson(1), Json.toJson(2))),
        "key2" -> JsArray(Seq(Json.toJson(1), Json.toJson(2)))
      )
      input.remove(JsPath \ "key" \ 0) mustBe JsSuccess(
        Json.obj("key" -> JsArray(Seq(Json.toJson(2))), "key2" -> JsArray(Seq(Json.toJson(1), Json.toJson(2))))
      )
    }

    "must remove a value when there are nested arrays" in {
      val input = Json.obj(
        "key"  -> JsArray(Seq(JsArray(Seq(Json.toJson(1), Json.toJson(2))), Json.toJson(2))),
        "key2" -> JsArray(Seq(Json.toJson(1), Json.toJson(2)))
      )
      input.remove(JsPath \ "key" \ 0 \ 0) mustBe JsSuccess(
        Json.obj(
          "key"  -> JsArray(Seq(JsArray(Seq(Json.toJson(2))), Json.toJson(2))),
          "key2" -> JsArray(Seq(Json.toJson(1), Json.toJson(2)))
        )
      )
    }

    "must remove the value if the last value is deleted from an array" in {
      val input = Json.obj(
        "key"  -> JsArray(Seq(Json.toJson(1))),
        "key2" -> JsArray(Seq(Json.toJson(1), Json.toJson(2)))
      )
      input.remove(JsPath \ "key" \ 0) mustBe JsSuccess(
        Json.obj(
          "key"  -> JsArray(),
          "key2" -> JsArray(Seq(Json.toJson(1), Json.toJson(2)))
        )
      )
    }

    "must return an error when trying to remove an index from a non-array" in {
      val value = Json.obj("foo" -> "bar")
      value.remove(JsPath \ 0) mustEqual JsError(s"cannot remove an index on $value")
    }

    "must return an error when trying to remove a key from a non-object" in {
      val value = Json.arr("a", "b")
      value.remove(JsPath \ "foo") mustEqual JsError(s"cannot remove a key on $value")
    }

    "must return an error when using a recursive search path" in {
      Json.obj("foo" -> "bar").remove(JsPath \\ "foo") mustEqual JsError("recursive search not supported")
    }

    "must return an error for an out-of-bounds array index" in {
      Json.arr("a", "b").remove(JsPath \ 5) mustEqual JsError("array index out of bounds: 5, [\"a\",\"b\"]")
    }
  }

  "RichJsObject" - {

    "setObject must set a value and return a JsObject" in {
      val obj = Json.obj("existing" -> "value")
      obj.setObject(JsPath \ "newKey", JsString("newValue")) mustEqual
        JsSuccess(Json.obj("existing" -> "value", "newKey" -> "newValue"))
    }

    "setObject must return a JsError when the operation produces a non-object" in {
      Json.obj().setObject(JsPath \ 0, JsString("x")).isError mustBe true
    }

    "removeObject must remove a key and return a JsObject" in {
      Json.obj("keep" -> "yes", "remove" -> "me").removeObject(JsPath \ "remove") mustEqual
        JsSuccess(Json.obj("keep" -> "yes"))
    }

    "removeObject must return a JsError when the path is not found" in {
      Json.obj("key" -> "value").removeObject(JsPath \ "missing").isError mustBe true
    }
  }
}
