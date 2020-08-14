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
import play.api.libs.json.{Json, Reads, Writes}
import uk.gov.hmrc.bankaccountverificationexamplefrontend.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class BavfConnector @Inject()(httpClient: HttpClient, appConfig: AppConfig) {

  def init(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[String]] = {
    import HttpReads.Implicits.readRaw
    import InitRequest.writes

    val continueUrl = s"${appConfig.bavfExampleBaseUrl}/bank-account-verification-example-frontend/done"
    val customisationsUrl = s"${appConfig.bavfExampleBaseUrl}/bank-account-verification"

    val url = s"${appConfig.bavfApiBaseUrl}/api/init"
    httpClient.POST[InitRequest, HttpResponse](url, InitRequest(continueUrl, Some(customisationsUrl))).map {
      case r if r.status == 200 =>
        Some(r.json.as[String])
      case _ =>
        None
    }
  }

  def complete(journeyId: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[CompleteResponse]] = {
    import HttpReads.Implicits.readRaw
    import CompleteResponse.reads

    val url = s"${appConfig.bavfApiBaseUrl}/api/complete/$journeyId"
    httpClient.GET[HttpResponse](url).map {
      case r if r.status == 200 =>
        Some(r.json.as[CompleteResponse])
      case _ =>
        None
    }
  }
}

case class InitRequest(continueUrl: String, customisationsUrl: Option[String])

object InitRequest {
  implicit val writes: Writes[InitRequest] = Json.writes[InitRequest]
}

case class CompleteResponse(accountName: String,
                            sortCode: String,
                            accountNumber: String,
                            accountNumberWithSortCodeIsValid: ReputationResponseEnum,
                            rollNumber: Option[String] = None)

object CompleteResponse {
  implicit val reads: Reads[CompleteResponse] = Json.reads[CompleteResponse]
}

sealed trait ReputationResponseEnum

object ReputationResponseEnum extends Enumerable.Implicits {

  case object Yes extends WithName("yes") with ReputationResponseEnum

  case object No extends WithName("no") with ReputationResponseEnum

  case object Indeterminate extends WithName("indeterminate") with ReputationResponseEnum

  case object Inapplicable extends WithName("inapplicable") with ReputationResponseEnum

  case object Error extends WithName("error") with ReputationResponseEnum

  val values: Seq[ReputationResponseEnum] = Seq(Yes, No, Indeterminate, Inapplicable, Error)

  implicit val enumerable: Enumerable[ReputationResponseEnum] =
    Enumerable(values.map(v => v.toString -> v): _*)
}

class WithName(string: String) {
  override val toString: String = string
}