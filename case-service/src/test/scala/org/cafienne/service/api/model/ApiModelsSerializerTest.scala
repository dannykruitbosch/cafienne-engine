package org.cafienne.service.api.model

import org.cafienne.cmmn.akka.command.team.{CaseTeam, CaseTeamMember}
import org.cafienne.cmmn.instance.casefile.ValueMap
import org.cafienne.infrastructure.akka.http.JsonUtil
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import scala.collection.JavaConverters._ // for roles to set conversion

class ApiModelsSerializerTest extends AnyFlatSpec with Matchers  {

  val tenant = ""
  val minimalJson = "{\"definition\":\"startcase\",\"tenant\":\"\",\"caseInstanceId\":null,\"debug\":false}"
  val minimalJsonWithNull = "{\"definition\":\"startcase\",\"inputs\":null,\"caseTeam\":null,\"tenant\":\"\",\"caseInstanceId\":null,\"debug\":false}"
  val minimalJsonWithEmptyObjects = "{\"definition\":\"startcase\",\"inputs\":{},\"caseTeam\":{\"members\":[]},\"tenant\":\"\",\"caseInstanceId\":null,\"debug\":false}"
  val extendedJson = "{\"definition\":\"startcase2\",\"inputs\":{\"input1\":\"bla\",\"input2\":\"bla\",\"input3\":{\"hello\":\"world\"}},\"caseTeam\":{\"members\":[{\"roles\":[\"ADMIN\"],\"user\":\"gerald\"}]},\"tenant\":\"\",\"caseInstanceId\":\"myinstanceid\",\"debug\":false}"

  "serialize" should "create proper json of a minimal StartCase API model" in {
    val cmd = StartCase("startcase", new ValueMap(), new CaseTeam(), Some(tenant), None)
    val result = JsonUtil.toJson(cmd)
    result must be(minimalJsonWithEmptyObjects)
  }

  it should "create proper json for given null parameters at the ValueMap" in {
    val cmd = StartCase("startcase", null, null, Some(tenant), None)
    val result = JsonUtil.toJson(cmd)
    result must be(minimalJsonWithNull)
  }

  it should "create proper json of an extended StartCase API model" in {
    val input = new ValueMap("input1", "bla", "input2", "bla", "input3", new ValueMap("hello", "world"))
    val member1 = new CaseTeamMember("gerald", Set("ADMIN").asJava)
    val caseTeam = new CaseTeam(member1)
    val cmd = StartCase("startcase2", input, caseTeam, Some(tenant), Some("myinstanceid"))
    val result = JsonUtil.toJson(cmd)
    result must be(extendedJson)
  }

  "deserialize" should "create proper StartCase of a minimal json" in {
    val cmd = StartCase("startcase", null, null, Some(tenant), None)
    val result: StartCase = JsonUtil.fromJson[StartCase](minimalJson)
    result must be(cmd)
  }

  it should "create proper startcase when  given empty json objects" in {
    val cmd = StartCase("startcase", new ValueMap(), new CaseTeam(), Some(tenant), None)
    val result: StartCase = JsonUtil.fromJson[StartCase](minimalJsonWithEmptyObjects)
    result must be(cmd)
  }

  it should "create proper StartCase of extended json" in {
    val input = new ValueMap("input1", "bla", "input2", "bla", "input3", new ValueMap("hello", "world"))
    val member1 = new CaseTeamMember("gerald", Set("ADMIN").asJava)
    val caseTeam = new CaseTeam(member1)
    val cmd = StartCase("startcase2", input, caseTeam, Some(tenant), Some("myinstanceid"))
    val result: StartCase = JsonUtil.fromJson[StartCase](extendedJson)
    result must be(cmd)
  }
}
