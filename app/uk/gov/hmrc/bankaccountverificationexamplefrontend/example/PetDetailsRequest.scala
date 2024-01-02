/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.bankaccountverificationexamplefrontend.example

import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, FormError, Mapping}
import play.api.libs.json.{Json, Reads, Writes}
import uk.gov.hmrc.bankaccountverificationexamplefrontend.{Enumerable, WithName}

case class PrepopulatedAccountInformation(accountType: Option[String], accountName: Option[String], sortCode: Option[String], accountNumber: Option[String], rollNumber: Option[String])

object PrepopulatedAccountInformation {
  object formats {
    implicit val reads: Reads[PrepopulatedAccountInformation] = Json.reads[PrepopulatedAccountInformation]
    implicit val writes: Writes[PrepopulatedAccountInformation] = Json.writes[PrepopulatedAccountInformation]
  }
}

sealed trait PetTypeEnum
object PetTypeEnum extends Enumerable.Implicits {
  private case object Cat extends WithName("cat") with PetTypeEnum
  private case object Dog extends WithName("dog") with PetTypeEnum
  private case object Bunny extends WithName("bunny") with PetTypeEnum
  private case object Other extends WithName("other") with PetTypeEnum
  case object Error extends WithName("") with PetTypeEnum

  val values: Seq[PetTypeEnum] = Seq(Cat, Dog, Bunny, Other)

  implicit val enumerable: Enumerable[PetTypeEnum] =
    Enumerable(values.map(v => v.toString -> v): _*)
}

case class PetDetailsRequest(petName: String, petType: PetTypeEnum, age: Int, description: Option[String])

object PetDetailsRequest {
  import PetTypeEnum._

  object formats {
    implicit val accountTypeReads: Reads[PetDetailsRequest] = Json.reads[PetDetailsRequest]
    implicit val accountTypeWrites: Writes[PetDetailsRequest] = Json.writes[PetDetailsRequest]
  }

  val prepopulatedAccountInformationMapping: Mapping[PrepopulatedAccountInformation] = mapping(
    "accountType" -> optional(text),
    "accountName" -> optional(text),
    "sortCode" -> optional(text),
    "accountNumber" -> optional(text),
    "rollNumber" -> optional(text))(PrepopulatedAccountInformation.apply)(PrepopulatedAccountInformation.unapply)

  val form: Form[PetDetailsRequest] =
    Form(
      mapping(
        "petName" -> nonEmptyText(maxLength = 64).verifying(petNameConstraint()),
        "petType" -> petTypeMapping,
        "age" -> number,
        "description" -> optional(text)
      )(PetDetailsRequest.apply)(PetDetailsRequest.unapply)
    )

  // Need to do this as if the radio buttons are not selected then we don't get the parameter at all.
  private def petTypeMapping: Mapping[PetTypeEnum] = {
    def permissiveStringFormatter: Formatter[PetTypeEnum] =
      new Formatter[PetTypeEnum] {
        def bind(key: String, data: Map[String, String]): Either[Seq[FormError], PetTypeEnum] =
          Right[Seq[FormError], PetTypeEnum] {
            val kv = data.getOrElse(key, "")
            PetTypeEnum.enumerable.withName(kv).getOrElse(Error)
          }
        def unbind(key: String, value: PetTypeEnum): Map[String, String] = Map(key -> value.toString)
      }

    of[PetTypeEnum](permissiveStringFormatter).verifying(petTypeConstraint())
  }

  private def petNameConstraint(): Constraint[String] =
    Constraint[String](Some("constraints.petName"), Seq()) { input =>
      if (input.trim.isEmpty) Invalid(ValidationError("error.petName.required"))
      else Valid
    }


  private def petTypeConstraint(): Constraint[PetTypeEnum] =
    Constraint[PetTypeEnum](Some("constraints.petType"), Seq()) { input =>
      if (input == Error) Invalid(ValidationError("error.petType.required"))
      else if (PetTypeEnum.values.contains(input)) Valid
      else Invalid(ValidationError("error.petType.required"))
    }
}

case class MorePetDetailsRequest(moreDetails: Option[String])

object MorePetDetailsRequest {
  object formats {
    implicit val morePetDetailsRequestReads: Reads[MorePetDetailsRequest] = Json.reads[MorePetDetailsRequest]
    implicit val morePetDetailsRequestWrites: Writes[MorePetDetailsRequest] = Json.writes[MorePetDetailsRequest]
  }

  val form: Form[MorePetDetailsRequest] =
    Form(
      mapping(
        "moreDetails" -> optional(text),
      )(MorePetDetailsRequest.apply)(MorePetDetailsRequest.unapply)
    )
}
