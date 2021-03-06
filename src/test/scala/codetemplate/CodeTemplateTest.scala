package codetemplate

import codetemplate.CodeTemplate.Expression
import io.circe.Json

import scala.util.{Success, Try}

class CodeTemplateTest extends BaseTest {
  type JsonMsg = Message[DynamicJson, DynamicJson]
  "CodeTemplate.newCache" should {
    "create a cached compiler which can turn a script into a function" in {

      val template = CodeTemplate.newCache[JsonMsg, Json]("import io.circe.syntax._")

      val compiledResult: Try[Expression[JsonMsg, Json]] = template("""
          |        val requests = record.content.hello.world.flatMap  { json =>
          |          json.nested.map { i =>
          |            val url = s"${json("name").asString}-$i"
          |            json("name").asString match {
          |              case "first" => io.circe.Json.obj("1st" -> io.circe.Json.Null)
          |              case other   => io.circe.Json.obj("other" -> other.asJson)
          |            }
          |          }
          |        }
          |        requests.asJson
          |""".stripMargin)

      val script = compiledResult match {
        case Success(script: Expression[JsonMsg, Json]) => script
        case other                                      => fail(s"compiled result was: $other")
      }

      val data = """{
        "hello" : {
          "there" : true,
          "world" : [
          {
            "name" : "first",
            "nested" : [1,2,3]
          },
          {
            "name" : "second",
            "nested" : [4,5]
          }
          ]
        }
      }""".jason

      import DynamicJson.implicits._
      val ctxt   = Message.of(data.asDynamic).withKey(data.asDynamic).asContext()
      val result = script(ctxt).asArray.get
      result.size shouldBe 5
    }
  }
}
