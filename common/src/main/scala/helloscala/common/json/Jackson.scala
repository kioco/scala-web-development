package helloscala.common.json

import java.lang.reflect.{ParameterizedType, Type => JType}

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.node.{ArrayNode, ObjectNode}
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * Jackson全局配置
 * Created by yangbajing(yangbajing@gmail.com) on 2017-03-14.
 */
object Jackson {
  // #defaultObjectMapper
  implicit val defaultObjectMapper: ObjectMapper = getObjectMapper

  private def getObjectMapper: ObjectMapper =
    new ObjectMapper().findAndRegisterModules
    //.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
    //.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
      .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
      .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
      .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
      .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
      .disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE) // 禁止反序列化时将时区转换为 Z
      .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS) // 允许序列化空的对象
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // 日期时间类型不序列化成时间戳
      .disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS) // 日期时间类型不序列化成时间戳
      .setSerializationInclusion(JsonInclude.Include.NON_NULL) // 序列化时不包含null的键
  // #defaultObjectMapper

  def stringify(value: AnyRef): String = defaultObjectMapper.writeValueAsString(value)

  def prettyString(value: AnyRef): String =
    Jackson.defaultObjectMapper.writer(new DefaultPrettyPrinter()).writeValueAsString(value)

  def readValue[A](
      content: String)(implicit ct: ClassTag[A], objectMapper: ObjectMapper = Jackson.defaultObjectMapper): A =
    objectMapper.readValue(content, ct.runtimeClass).asInstanceOf[A]

  def createObjectNode: ObjectNode = defaultObjectMapper.createObjectNode

  def createArrayNode: ArrayNode = defaultObjectMapper.createArrayNode

  def typeReference[T: TypeTag]: TypeReference[T] = {
    val t = typeTag[T]
    val mirror = t.mirror

    def mapType(t: Type): JType =
      if (t.typeArgs.isEmpty)
        mirror.runtimeClass(t)
      else
        new ParameterizedType {
          def getRawType: Class[_] = mirror.runtimeClass(t)

          def getActualTypeArguments: Array[JType] = t.typeArgs.map(mapType).toArray

          def getOwnerType: JType = null
        }

    new TypeReference[T] {
      override def getType: JType = mapType(t.tpe)
    }
  }

}
