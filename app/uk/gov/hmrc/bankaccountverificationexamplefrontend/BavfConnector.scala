/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.bankaccountverificationexamplefrontend

import javax.inject.Inject
import play.api.libs.json._
import uk.gov.hmrc.bankaccountverificationexamplefrontend.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class BavfConnector @Inject()(httpClient: HttpClient, appConfig: AppConfig) {

  def init(continueUrl: String,
           messages: Option[InitRequestMessages] = None,
           customisationsUrl: Option[String] = None)(
            implicit ec: ExecutionContext,
            hc: HeaderCarrier
          ): Future[Option[String]] = {
    import HttpReads.Implicits.readRaw
    import InitRequest.writes

    val request = InitRequest(
      "bank-account-verification-example-frontend",
      continueUrl,
      messages,
      customisationsUrl,
      address = Some(InitRequestAddress(List("Line 1", "Line 2"), Some("Town"), Some("Postcode"))))

    val url = s"${appConfig.bavfApiBaseUrl}/api/init"
    httpClient.POST[InitRequest, HttpResponse](url, request).map {
      case r if r.status == 200 =>
        Some(r.json.as[String])
      case _ =>
        None
    }
  }

  def complete(journeyId: String)(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Option[CompleteResponse]] = {
    import CompleteResponse._
    import HttpReads.Implicits.readRaw

    val url = s"${appConfig.bavfApiBaseUrl}/api/complete/$journeyId"
    httpClient.GET[HttpResponse](url).map {
      case r if r.status == 200 =>
        Some(r.json.as[CompleteResponse])
      case _ =>
        None
    }
  }
}

case class InitRequest(serviceIdentifier: String,
                       continueUrl: String,
                       messages: Option[InitRequestMessages] = None,
                       customisationsUrl: Option[String] = None,
                       address: Option[InitRequestAddress] = None)

case class InitRequestMessages(en: JsObject, cy: Option[JsObject] = None)

case class InitRequestAddress(lines: List[String], town: Option[String], postcode: Option[String])

object InitRequest {
  implicit val messagesWrites: OWrites[InitRequestMessages] = Json.writes[InitRequestMessages]
  implicit val addressWrites: OWrites[InitRequestAddress] = Json.writes[InitRequestAddress]
  implicit val writes: Writes[InitRequest] = Json.writes[InitRequest]
}

case class CompleteResponse(accountType: String,
                            personal: Option[PersonalCompleteResponse],
                            business: Option[BusinessCompleteResponse])

case class CompleteResponseAddress(lines: List[String], town: Option[String], postcode: Option[String]) {
  override def toString: String = {
    (lines ++ Seq(town, postcode).flatten).mkString("<br>")
  }
}

object CompleteResponse {
  implicit val addressReads: Reads[CompleteResponseAddress] = Json.reads[CompleteResponseAddress]
  implicit val reads: Reads[CompleteResponse] = Json.reads[CompleteResponse]
}

case class PersonalCompleteResponse(address: Option[CompleteResponseAddress],
                                    accountName: String,
                                    sortCode: String,
                                    accountNumber: String,
                                    accountNumberWithSortCodeIsValid: ReputationResponseEnum,
                                    rollNumber: Option[String],
                                    accountExists: Option[ReputationResponseEnum],
                                    nameMatches: Option[ReputationResponseEnum],
                                    addressMatches: Option[ReputationResponseEnum],
                                    nonConsented: Option[ReputationResponseEnum],
                                    subjectHasDeceased: Option[ReputationResponseEnum],
                                    nonStandardAccountDetailsRequiredForBacs: Option[ReputationResponseEnum],
                                    sortCodeBankName: Option[String])

object PersonalCompleteResponse {
  implicit val addressReads: Reads[CompleteResponseAddress] = Json.reads[CompleteResponseAddress]
  implicit val reads: Reads[PersonalCompleteResponse] = Json.reads[PersonalCompleteResponse]
}

case class BusinessCompleteResponse(address: Option[CompleteResponseAddress],
                                    companyName: String,
                                    sortCode: String,
                                    accountNumber: String,
                                    rollNumber: Option[String],
                                    accountNumberWithSortCodeIsValid: ReputationResponseEnum,
                                    accountExists: Option[ReputationResponseEnum],
                                    companyNameMatches: Option[ReputationResponseEnum],
                                    companyPostCodeMatches: Option[ReputationResponseEnum],
                                    companyRegistrationNumberMatches: Option[ReputationResponseEnum],
                                    nonStandardAccountDetailsRequiredForBacs: Option[ReputationResponseEnum],
                                    sortCodeBankName: Option[String])

object BusinessCompleteResponse {
  implicit val addressReads: Reads[CompleteResponseAddress] = Json.reads[CompleteResponseAddress]
  implicit val completeResponseReads: Reads[BusinessCompleteResponse] = Json.reads[BusinessCompleteResponse]
}

sealed trait ReputationResponseEnum

object ReputationResponseEnum extends Enumerable.Implicits {

  case object Yes extends WithName("yes") with ReputationResponseEnum

  case object No extends WithName("no") with ReputationResponseEnum

  case object Indeterminate
    extends WithName("indeterminate")
      with ReputationResponseEnum

  case object Inapplicable
    extends WithName("inapplicable")
      with ReputationResponseEnum

  case object Error extends WithName("error") with ReputationResponseEnum

  val values: Seq[ReputationResponseEnum] =
    Seq(Yes, No, Indeterminate, Inapplicable, Error)

  implicit val enumerable: Enumerable[ReputationResponseEnum] =
    Enumerable(values.map(v => v.toString -> v): _*)
}

class WithName(string: String) {
  override val toString: String = string
}
